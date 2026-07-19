package com.invkeeper.command;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.config.PermissionRule;
import com.invkeeper.protection.ProtectionManager;
import com.invkeeper.protection.VanillaProtectionItems;
import com.invkeeper.protection.VanillaProtectionItems.Kind;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class InvKeeperCommand implements CommandExecutor, TabCompleter {
    private final ConfigManager configManager;
    private final ProtectionManager protectionManager;

    public InvKeeperCommand(ConfigManager configManager, ProtectionManager protectionManager) {
        this.configManager = configManager;
        this.protectionManager = protectionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            MessageUtil.send(sender, "&e사용법: /invkeeper status | /invkeeper reload | /invkeeper give <player> <consumable|timed> [amount]");
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("reload")) {
            if (!sender.hasPermission("invkeeper.admin")) {
                MessageUtil.send(sender, "&c권한이 없습니다.");
                return true;
            }
            configManager.load();
            MessageUtil.send(sender, "&aInvKeeper 설정을 다시 불러왔습니다.");
            return true;
        }

        if (subcommand.equals("give")) {
            if (!sender.hasPermission("invkeeper.admin")) {
                MessageUtil.send(sender, "&c권한이 없습니다.");
                return true;
            }
            if (args.length < 3) {
                MessageUtil.send(sender, "&c사용법: /invkeeper give <player> <consumable|timed> [amount]");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                MessageUtil.send(sender, "&c대상 플레이어가 온라인 상태가 아닙니다.");
                return true;
            }
            Kind kind;
            if (args[2].equalsIgnoreCase("consumable")) {
                kind = Kind.CONSUMABLE;
            } else if (args[2].equalsIgnoreCase("timed")) {
                kind = Kind.TIMED;
            } else {
                MessageUtil.send(sender, "&c아이템 종류는 consumable 또는 timed 중 하나여야 합니다.");
                return true;
            }
            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                    if (amount <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    MessageUtil.send(sender, "&c유효한 개수를 입력해야 합니다.");
                    return true;
                }
            }

            ItemStack item = protectionManager.getVanillaProtectionItems().create(kind);
            item.setAmount(amount);
            var remainder = target.getInventory().addItem(item);
            if (!remainder.isEmpty()) {
                for (ItemStack leftover : remainder.values()) {
                    if (leftover != null && !leftover.getType().isAir()) {
                        target.getWorld().dropItemNaturally(target.getLocation(), leftover);
                    }
                }
                MessageUtil.send(target, "&e인벤토리가 가득 차서 일부 아이템이 바닥에 떨어졌습니다.");
            }
            MessageUtil.send(sender, "&a" + target.getName() + "님에게 보호 아이템을 지급했습니다.");
            MessageUtil.send(target, "&a보호 아이템을 지급받았습니다.");
            return true;
        }

        if (subcommand.equals("status")) {
            if (!(sender instanceof Player player)) {
                MessageUtil.send(sender, "&c플레이어만 사용할 수 있는 명령어입니다.");
                return true;
            }
            PermissionRule selectedRule = configManager.resolveEffectiveRule(player, player.getWorld().getName());
            String ruleName = "__world__".equals(selectedRule.getPermission())
                    ? "월드 규칙"
                    : selectedRule.getPermission() + " (priority " + selectedRule.getPriority() + ")";
            MessageUtil.send(player, "&eInvKeeper 상태");
            MessageUtil.send(player, "&f월드: &b" + player.getWorld().getName());
            MessageUtil.send(player, "&f적용 규칙: &b" + ruleName);
            MessageUtil.send(player, "&f인벤토리 드랍: &b" + selectedRule.getInventoryDropPercent() + "%");
            MessageUtil.send(player, "&f경험치 드랍: &b" + selectedRule.getExpDropPercent() + "%");
            if (protectionManager.getTimedProtectionStore().isActive(player)) {
                String remaining = formatDuration(protectionManager.getTimedProtectionStore().getRemainingMillis(player));
                MessageUtil.send(player, "&a시간보호권 활성 중: &b" + remaining);
            } else {
                MessageUtil.send(player, "&c시간보호권 비활성 상태입니다.");
            }
            return true;
        }

        MessageUtil.send(sender, "&e사용법: /invkeeper status | /invkeeper reload | /invkeeper give <player> <consumable|timed> [amount]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("status", "reload", "give"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return filter(players, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filter(List.of("consumable", "timed"), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return filter(List.of("1", "5", "10"), args[3]);
        }
        return Collections.emptyList();
    }

    private static List<String> filter(List<String> options, String prefix) {
        if (prefix == null) {
            return List.copyOf(options);
        }
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d분 %02d초", minutes, seconds);
    }
}
