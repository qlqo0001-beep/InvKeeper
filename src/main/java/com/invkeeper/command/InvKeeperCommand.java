package com.invkeeper.command;

import com.invkeeper.command.handler.GiveHandler;
import com.invkeeper.command.handler.GraveAdminHandler;
import com.invkeeper.command.handler.SoulbindAdminHandler;
import com.invkeeper.config.ConfigManager;
import com.invkeeper.grave.GraveManager;
import com.invkeeper.config.PermissionRule;
import com.invkeeper.protection.ProtectionManager;
import com.invkeeper.soulbind.SoulbindManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class InvKeeperCommand implements CommandExecutor, TabCompleter {
    private final ConfigManager configManager;
    private final ProtectionManager protectionManager;
    private final SoulbindManager soulbindManager;
    private final com.invkeeper.InvKeeperPlugin plugin;
    private final GiveHandler giveHandler;
    private final SoulbindAdminHandler soulbindAdminHandler;
    private final GraveAdminHandler graveAdminHandler;

    public InvKeeperCommand(ConfigManager configManager, ProtectionManager protectionManager,
                            com.invkeeper.InvKeeperPlugin plugin, GraveManager graveManager) {
        this.configManager = configManager;
        this.protectionManager = protectionManager;
        this.soulbindManager = protectionManager.getSoulbindManager();
        this.plugin = plugin;
        this.giveHandler = new GiveHandler(configManager, protectionManager);
        this.soulbindAdminHandler = new SoulbindAdminHandler(configManager, protectionManager, soulbindManager);
        this.graveAdminHandler = new GraveAdminHandler(graveManager);
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
                    return giveHandler.handle(sender, args);
                case "status":
                    return handleStatus(sender);
                case "soulbind":
                    return soulbindAdminHandler.handle(sender, args);
                case "grave":
                    return graveAdminHandler.handle(sender, args);
                case "notice":
                    return handleNotice(sender, args);
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
        MessageUtil.send(sender, "&e  /invkeeper notice [on|off]");
        MessageUtil.send(sender, "&e  /invkeeper reload");
        MessageUtil.send(sender, "&e  /invkeeper give <player> <item-key> [amount]");
        MessageUtil.send(sender, "&e  /invkeeper soulbind unbind <player> [slot|all]");
        MessageUtil.send(sender, "&e  /invkeeper soulbind inspect <player>");
        MessageUtil.send(sender, "&e  /invkeeper grave list");
        MessageUtil.send(sender, "&e  /invkeeper grave history <player>");
        MessageUtil.send(sender, "&e  /invkeeper grave reload");
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("invkeeper.admin")) {
            MessageUtil.send(sender, "&c권한이 없습니다.");
            return true;
        }
        
        try {
            configManager.load();
            plugin.reloadAlertManager();
            plugin.getDangerNoticeListener().clearLastNotices();
            protectionManager.refreshMmoHook();
            MessageUtil.send(sender, "&aInvKeeper 설정을 다시 불러왔습니다.");
            // Summary log
            MessageUtil.send(sender, "&f월드 규칙 " + configManager.getWorldRules().size() + "개, "
                    + "권한 규칙 " + configManager.getPermissionRules().size() + "개, "
                    + "보호 아이템 " + configManager.getProtectionItemConfigs().size() + "개 로드 완료");
        } catch (Exception e) {
            MessageUtil.send(sender, "&c리로드 중 오류가 발생했습니다: " + e.getMessage());
            protectionManager.getPlugin().getLogger().warning("[InvKeeper] 리로드 중 오류: " + e.getMessage());
        }
        return true;
    }

    private boolean handleNotice(CommandSender sender, String[] args) {
        if (!sender.hasPermission("invkeeper.notice")) {
            MessageUtil.send(sender, "&c권한이 없습니다.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&c플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (!configManager.isDangerNoticeEnabled()) {
            MessageUtil.send(player, configManager.getDangerNoticeServerDisabledMessage());
            return true;
        }
        var notice = plugin.getDangerNoticeListener();
        // 인자가 없으면 현재 상태를 반전
        boolean turnOn;
        if (args.length >= 2 && args[1].equalsIgnoreCase("on")) {
            turnOn = true;
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("off")) {
            turnOn = false;
        } else if (args.length >= 2) {
            MessageUtil.send(player, "&e사용법: /invkeeper notice [on|off]");
            return true;
        } else {
            turnOn = notice.isOptedOut(player);
        }
        MessageUtil.send(player, turnOn ? configManager.getDangerNoticeToggleOnMessage() : configManager.getDangerNoticeToggleOffMessage());
        notice.setOptedOut(player, !turnOn);
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission("invkeeper.status")) {
            MessageUtil.send(sender, "&c권한이 없습니다.");
            return true;
        }
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
        double[] pvpPercents = configManager.resolveDropPercents(player, player.getWorld().getName(), true);
        if (pvpPercents[0] != selectedRule.getInventoryDropPercent() || pvpPercents[1] != selectedRule.getExpDropPercent()) {
            MessageUtil.send(player, "&fPvP 사망 시 드랍: &b인벤토리 " + pvpPercents[0] + "%, 경험치 " + pvpPercents[1] + "%");
        }
        if (!configManager.isPvpProtectionItemsWork()) {
            MessageUtil.send(player, "&7(PvP 사망 시 보호권이 적용되지 않습니다)");
        }
        if (protectionManager.getTimedProtectionStore().isActive(player)) {
            String remaining = MessageUtil.formatDuration(protectionManager.getTimedProtectionStore().getRemainingMillis(player), configManager.getTimeFormat());
            MessageUtil.send(player, "&a시간보호권 활성 중: &b" + remaining);
        } else {
            MessageUtil.send(player, "&c시간보호권 비활성 상태입니다.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("status");
            if (sender.hasPermission("invkeeper.notice")) {
                options.add("notice");
            }
            if (sender.hasPermission("invkeeper.admin")) {
                options.add("reload");
                options.add("give");
                options.add("soulbind");
                options.add("grave");
            }
            return filter(options, args[0]);
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
            if (first.equals("grave")) {
                return filter(List.of("list", "history", "reload"), args[1]);
            }
            if (first.equals("notice")) {
                return filter(List.of("on", "off"), args[1]);
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
            if (first.equals("grave") && args[1].equalsIgnoreCase("history")) {
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