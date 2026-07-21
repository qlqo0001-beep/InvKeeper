package com.invkeeper.command;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.config.PermissionRule;
import com.invkeeper.config.ProtectionItemConfig;
import com.invkeeper.protection.ProtectionManager;
import com.invkeeper.soulbind.SoulbindManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class InvKeeperCommand implements CommandExecutor, TabCompleter {
    private final ConfigManager configManager;
    private final ProtectionManager protectionManager;
    private final SoulbindManager soulbindManager;
    private final com.invkeeper.InvKeeperPlugin plugin;

    public InvKeeperCommand(ConfigManager configManager, ProtectionManager protectionManager, com.invkeeper.InvKeeperPlugin plugin) {
        this.configManager = configManager;
        this.protectionManager = protectionManager;
        this.soulbindManager = protectionManager.getSoulbindManager();
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length == 0) {
                sendUsage(sender);
                return true;
            }

            String subcommand = args[0].toLowerCase(Locale.ROOT);
            switch (subcommand) {
                case "reload":
                    return handleReload(sender);
                case "give":
                    return handleGive(sender, args);
                case "status":
                    return handleStatus(sender);
                case "soulbind":
                    return handleSoulbind(sender, args);
                default:
                    sendUsage(sender);
                    return true;
            }
        } catch (Exception e) {
            MessageUtil.send(sender, "&c명령 실행 중 오류가 발생했습니다: " + e.getMessage());
            protectionManager.getPlugin().getLogger().warning("[InvKeeper] 명령어 처리 중 오류: " + e.getMessage());
            return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        MessageUtil.send(sender, "&e사용법:");
        MessageUtil.send(sender, "&e  /invkeeper status");
        MessageUtil.send(sender, "&e  /invkeeper reload");
        MessageUtil.send(sender, "&e  /invkeeper give <player> <item-key> [amount]");
        MessageUtil.send(sender, "&e  /invkeeper soulbind unbind <player> [slot|all]");
        MessageUtil.send(sender, "&e  /invkeeper soulbind inspect <player>");
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("invkeeper.admin")) {
            MessageUtil.send(sender, "&c권한이 없습니다.");
            return true;
        }
        
        try {
            // Reload config
            configManager.load();

            // Recreate alert manager with new config (owned by the plugin, not this command)
            plugin.reloadAlertManager();

            MessageUtil.send(sender, "&aInvKeeper 설정을 다시 불러왔습니다.");
        } catch (Exception e) {
            MessageUtil.send(sender, "&c리로드 중 오류가 발생했습니다: " + e.getMessage());
            protectionManager.getPlugin().getLogger().warning("[InvKeeper] 리로드 중 오류: " + e.getMessage());
        }
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("invkeeper.admin")) {
            MessageUtil.send(sender, "&c권한이 없습니다.");
            return true;
        }
        if (args.length < 3) {
            MessageUtil.send(sender, "&c사용법: /invkeeper give <player> <item-key> [amount]");
            return true;
        }

        ItemStack item = null;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.send(sender, "&c대상 플레이어가 온라인 상태가 아닙니다.");
            return true;
        }

        String itemKey = args[2];
        ProtectionItemConfig itemConfig = configManager.getProtectionItemConfig(itemKey);
        if (itemConfig == null) {
            MessageUtil.send(sender, "&c존재하지 않는 보호 아이템 키입니다. 사용 가능한 키: " + String.join(", ", configManager.getProtectionItemKeys()));
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

        if (itemConfig.isUseMmo()) {
            if (!protectionManager.getMmoItemsHook().isAvailable()) {
                MessageUtil.send(sender, "&cMMOItems가 서버에 설치되어 있지 않거나 사용할 수 없습니다. MMOItems 설치를 확인하세요.");
                return true;
            }
            boolean gaveViaCommand = protectionManager.getMmoItemsHook().giveItemByCommand(target, itemConfig.getMmoItemsType(), itemConfig.getMmoItemsId(), amount);
            if (gaveViaCommand) {
                MessageUtil.send(sender, "&a" + target.getName() + "님에게 MMOItems 보호 아이템을 지급했습니다.");
                MessageUtil.send(target, "&a보호 아이템을 지급받았습니다.");
                // Schedule a short delayed scan to bind MMOItems that were just given
                int giveAmount = amount;
                Bukkit.getScheduler().runTaskLater(protectionManager.getPlugin(), () -> {
                    try {
                        int bound = protectionManager.bindMatchingItemsInInventory(target, itemConfig, giveAmount);
                        if (bound > 0 && itemConfig.isSoulbindEnabled()) {
                            String remaining;
                            if (itemConfig.isSoulbindInfinite()) {
                                remaining = "무한";
                            } else {
                                long millis = (long) itemConfig.getSoulbindDurationMinutes() * 60L * 1000L;
                                remaining = com.invkeeper.util.MessageUtil.formatDuration(millis, configManager.getTimeFormat());
                            }
                            MessageUtil.send(target, configManager.getSoulboundAppliedMessage().replace("{owner}", target.getName()).replace("{remaining}", remaining));
                        }
                    } catch (Exception e) {
                        protectionManager.getPlugin().getLogger().warning("[InvKeeper] MMOItems 지급 후 각인 처리 중 오류: " + e.getMessage());
                    }
                }, 2L);
            } else {
                MessageUtil.send(sender, "&cMMOItems 지급 명령을 실행할 수 없습니다. 아이템이 존재하지 않거나 명령 형식/권한 문제가 있을 수 있습니다. type=" + itemConfig.getMmoItemsType() + " id=" + itemConfig.getMmoItemsId());
            }
            return true;
        } else if (itemConfig.isUseVanilla()) {
            // Prepare soulbind lore placeholders if soulbind is enabled
            String ownerName = null;
            String remainingTime = null;
            if (itemConfig.isSoulbindEnabled()) {
                ownerName = target.getName();
                if (itemConfig.isSoulbindInfinite()) {
                    remainingTime = "무한";
                } else {
                    long millis = (long) itemConfig.getSoulbindDurationMinutes() * 60L * 1000L;
                    remainingTime = com.invkeeper.util.MessageUtil.formatDuration(millis, configManager.getTimeFormat());
                }
            }
            item = protectionManager.getVanillaProtectionItems().create(itemConfig, ownerName, remainingTime);
            if (item != null && amount > 1) {
                item.setAmount(amount);
            }
            // Apply soulbind on give if configured
            if (item != null && protectionManager.getConfigManager().getProtectionItemConfig(itemConfig.getKey()) != null) {
                var cfg = protectionManager.getConfigManager().getProtectionItemConfig(itemConfig.getKey());
                if (cfg.isSoulbindEnabled()) {
                    long expiry = cfg.isSoulbindInfinite() ? -1L : (System.currentTimeMillis() + (long)cfg.getSoulbindDurationMinutes() * 60L * 1000L);
                    protectionManager.getSoulbindManager().applySoulbind(item, target.getUniqueId(), expiry);
                }
            }
        }

        if (item == null || item.getType().isAir()) {
            MessageUtil.send(sender, "&c지급할 수 있는 보호 아이템이 없습니다. 설정을 확인해주세요.");
            return true;
        }
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

    private boolean handleStatus(CommandSender sender) {
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
            String remaining = MessageUtil.formatDuration(protectionManager.getTimedProtectionStore().getRemainingMillis(player), configManager.getTimeFormat());
            MessageUtil.send(player, "&a시간보호권 활성 중: &b" + remaining);
        } else {
            MessageUtil.send(player, "&c시간보호권 비활성 상태입니다.");
        }
        return true;
    }

    private boolean handleSoulbind(CommandSender sender, String[] args) {
        if (!sender.hasPermission("invkeeper.admin")) {
            MessageUtil.send(sender, "&c권한이 없습니다.");
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "&c사용법: /invkeeper soulbind unbind <player> [slot|all] | /invkeeper soulbind inspect <player>");
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "unbind":
                return handleSoulbindUnbind(sender, args);
            case "inspect":
                return handleSoulbindInspect(sender, args);
            default:
                MessageUtil.send(sender, "&c알 수 없는 soulbind 액션입니다. 사용 가능: unbind, inspect");
                return true;
        }
    }

    private boolean handleSoulbindUnbind(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "&c사용법: /invkeeper soulbind unbind <player> [slot|all]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            MessageUtil.send(sender, "&c대상 플레이어가 온라인 상태가 아닙니다.");
            return true;
        }

        // Determine scope: specific slot number or "all"
        boolean unbindAll = false;
        int specificSlot = -1;
        if (args.length >= 4) {
            String scope = args[3].toLowerCase(Locale.ROOT);
            if (scope.equals("all")) {
                unbindAll = true;
            } else {
                try {
                    specificSlot = Integer.parseInt(scope);
                    if (specificSlot < 0 || specificSlot > 40) {
                        MessageUtil.send(sender, "&c슬롯 번호는 0~40 사이여야 합니다. (0~8: 핫바, 9~35: 메인, 36~39: 방어구, 40: 오프핸드)");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    MessageUtil.send(sender, "&c슬롯은 숫자 또는 'all'을 입력하세요.");
                    return true;
                }
            }
        } else {
            // Default: unbind from main hand slot
            specificSlot = target.getInventory().getHeldItemSlot();
        }

        PlayerInventory inv = target.getInventory();
        int unboundCount = 0;

        if (unbindAll) {
            // Scan all 41 slots
            for (int slot = 0; slot <= 40; slot++) {
                ItemStack item = inv.getItem(slot);
                if (item != null && !item.getType().isAir() && soulbindManager.isSoulbound(item)) {
                    soulbindManager.removeLoreLines(item);
                    soulbindManager.removeSoulbind(item);
                    inv.setItem(slot, item);
                    unboundCount++;
                }
            }
        } else {
            // Specific slot
            ItemStack item = inv.getItem(specificSlot);
            if (item == null || item.getType().isAir()) {
                MessageUtil.send(sender, "&c해당 슬롯에 아이템이 없습니다.");
                return true;
            }
            if (!soulbindManager.isSoulbound(item)) {
                MessageUtil.send(sender, "&c해당 아이템은 각인 상태가 아닙니다.");
                return true;
            }
            soulbindManager.removeLoreLines(item);
            soulbindManager.removeSoulbind(item);
            inv.setItem(specificSlot, item);
            unboundCount = 1;
        }

        if (unboundCount > 0) {
            MessageUtil.send(sender, "&a" + target.getName() + "님의 각인 아이템 " + unboundCount + "개의 각인을 해제했습니다.");
            MessageUtil.send(target, "&a관리자에 의해 " + unboundCount + "개의 아이템 각인이 해제되었습니다.");
        } else {
            MessageUtil.send(sender, "&e" + target.getName() + "님의 인벤토리에 각인된 아이템이 없습니다.");
        }
        return true;
    }

    private boolean handleSoulbindInspect(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "&c사용법: /invkeeper soulbind inspect <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            MessageUtil.send(sender, "&c대상 플레이어가 온라인 상태가 아닙니다.");
            return true;
        }

        PlayerInventory inv = target.getInventory();
        List<String> soulboundItems = new ArrayList<>();
        int totalCount = 0;

        for (int slot = 0; slot <= 40; slot++) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            if (!soulbindManager.isSoulbound(item)) continue;

            UUID owner = soulbindManager.getOwnerUuid(item);
            String ownerName = owner == null ? "없음" : (Bukkit.getPlayer(owner) != null ? Bukkit.getPlayer(owner).getName() : Bukkit.getOfflinePlayer(owner).getName());
            if (ownerName == null) ownerName = owner.toString();

            String slotName = getSlotName(slot);
            String remaining = soulbindManager.formatRemaining(item, configManager.getTimeFormat());
            String itemName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
                    ? item.getItemMeta().getDisplayName()
                    : item.getType().name();

            soulboundItems.add("&f슬롯 " + slot + " (" + slotName + "): &b" + itemName + " &7x" + item.getAmount()
                    + " &f| 소유자: &b" + ownerName + " &f| 남은 시간: &b" + remaining);
            totalCount += item.getAmount();
        }

        MessageUtil.send(sender, "&e" + target.getName() + "님의 각인 아이템 목록 (&b" + soulboundItems.size() + "개 슬롯, 총 " + totalCount + "개&e)");
        if (soulboundItems.isEmpty()) {
            MessageUtil.send(sender, "&c각인된 아이템이 없습니다.");
        } else {
            for (String line : soulboundItems) {
                MessageUtil.send(sender, line);
            }
        }
        return true;
    }

    private static String getSlotName(int slot) {
        if (slot >= 0 && slot <= 8) return "핫바";
        if (slot >= 9 && slot <= 35) return "메인 인벤토리";
        if (slot >= 36 && slot <= 39) return "방어구";
        if (slot == 40) return "오프핸드";
        return "알 수 없음";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("status", "reload", "give", "soulbind"), args[0]);
        }
        if (args.length == 2) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (first.equals("give")) {
                List<String> players = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    players.add(player.getName());
                }
                return filter(players, args[1]);
            }
            if (first.equals("soulbind")) {
                return filter(List.of("unbind", "inspect"), args[1]);
            }
        }
        if (args.length == 3) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (first.equals("give")) {
                return filter(configManager.getProtectionItemKeys(), args[2]);
            }
            if (first.equals("soulbind")) {
                List<String> players = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    players.add(player.getName());
                }
                return filter(players, args[2]);
            }
        }
        if (args.length == 4) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (first.equals("give")) {
                return filter(List.of("1", "5", "10"), args[3]);
            }
            if (first.equals("soulbind") && args[1].equalsIgnoreCase("unbind")) {
                return filter(List.of("all", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "36", "37", "38", "39", "40"), args[3]);
            }
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
}
