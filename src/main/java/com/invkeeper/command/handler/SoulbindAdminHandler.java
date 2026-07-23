package com.invkeeper.command.handler;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.protection.ProtectionManager;
import com.invkeeper.soulbind.SoulbindManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SoulbindAdminHandler {
    private final ConfigManager configManager;
    private final ProtectionManager protectionManager;
    private final SoulbindManager soulbindManager;

    public SoulbindAdminHandler(ConfigManager configManager, ProtectionManager protectionManager, SoulbindManager soulbindManager) {
        this.configManager = configManager;
        this.protectionManager = protectionManager;
        this.soulbindManager = soulbindManager;
    }

    public boolean handle(CommandSender sender, String[] args) {
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
                return handleUnbind(sender, args);
            case "inspect":
                return handleInspect(sender, args);
            default:
                MessageUtil.send(sender, "&c알 수 없는 soulbind 액션입니다. 사용 가능: unbind, inspect");
                return true;
        }
    }

    private boolean handleUnbind(CommandSender sender, String[] args) {
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

    private boolean handleInspect(CommandSender sender, String[] args) {
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
}