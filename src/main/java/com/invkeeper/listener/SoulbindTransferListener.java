package com.invkeeper.listener;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.protection.ProtectionManager;
import com.invkeeper.soulbind.SoulbindManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class SoulbindTransferListener implements Listener {
    private final ProtectionManager protectionManager;
    private final ConfigManager configManager;
    private final SoulbindManager soulbindManager;

    public SoulbindTransferListener(ProtectionManager protectionManager, ConfigManager configManager) {
        this.protectionManager = protectionManager;
        this.configManager = configManager;
        this.soulbindManager = protectionManager.getSoulbindManager();
    }

    private boolean canBypass(Player player) {
        return player.hasPermission("invkeeper.admin") || player.hasPermission("invkeeper.soulbind.bypass");
    }

    /**
     * 호퍼/드로퍼 등 자동 아이템 이동 시 각인 아이템 차단
     */
    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) return;
        if (!soulbindManager.isSoulbound(item)) return;
        UUID owner = soulbindManager.getOwnerUuid(item);
        // If soulbound to someone, disallow automated transfers (hopper, etc.)
        if (owner != null) {
            event.setCancelled(true);
        }
    }

    /**
     * 호퍼가 아이템을 픽업할 때 각인 아이템 차단 (이중 방어)
     */
    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        Inventory inventory = event.getInventory();
        // Only block hoppers from picking up soulbound items
        if (inventory.getType() != InventoryType.HOPPER) {
            return;
        }
        ItemStack item = event.getItem().getItemStack();
        if (item == null || item.getType().isAir()) return;
        if (!soulbindManager.isSoulbound(item)) return;
        UUID owner = soulbindManager.getOwnerUuid(item);
        if (owner != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Shift-click 이동 시 비소유자 차단 및 강제 드랍
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (!event.isShiftClick()) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            
            // Check if soulbind has expired and remove it
            if (soulbindManager.isSoulbound(clicked)) {
                soulbindManager.checkAndRemoveExpired(clicked);
            }
            
            if (!soulbindManager.isSoulbound(clicked)) return; // No longer soulbound, allow transfer
            UUID owner = soulbindManager.getOwnerUuid(clicked);
            if (owner == null) return; // unowned can be moved/picked
            if (owner.equals(player.getUniqueId())) return; // owner can move
            if (canBypass(player)) return; // bypass permission

            // Not owner and no bypass -> force drop the item and prevent transfer
            event.setCancelled(true);
            // remove from inventory slot
            int slot = event.getSlot();
            event.getInventory().setItem(slot, null);
            // drop
            player.getWorld().dropItemNaturally(player.getLocation(), clicked);
            MessageUtil.send(player, configManager.getSoulboundForcedDroppedMessage());
        } catch (Exception e) {
            protectionManager.getPlugin().getLogger().warning("[InvKeeper] 인벤토리 이동 처리 중 오류: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * 드래그 이동 시 비소유자 아이템이 포함되어 있으면 취소
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (canBypass(player)) return;

            // Check the item being dragged (old cursor - the item being moved)
            ItemStack oldCursor = event.getOldCursor();
            if (oldCursor != null && !oldCursor.getType().isAir()) {
                if (soulbindManager.isSoulbound(oldCursor)) {
                    UUID owner = soulbindManager.getOwnerUuid(oldCursor);
                    if (owner != null && !owner.equals(player.getUniqueId())) {
                        event.setCancelled(true);
                        String itemName = oldCursor.getItemMeta() != null && oldCursor.getItemMeta().hasDisplayName()
                                ? oldCursor.getItemMeta().getDisplayName()
                                : oldCursor.getType().name();
                        MessageUtil.send(player, configManager.getSoulboundCantPickupMessage()
                                .replace("{item_name}", itemName)
                                .replace("{owner}", soulbindManager.resolveOwnerName(owner)));
                        return;
                    }
                }
            }

            // Check if any items in the affected slots are soulbound to another player
            for (int slot : event.getInventorySlots()) {
                ItemStack item = event.getView().getItem(slot);
                if (item == null || item.getType().isAir()) continue;
                if (soulbindManager.isSoulbound(item)) {
                    UUID owner = soulbindManager.getOwnerUuid(item);
                    if (owner != null && !owner.equals(player.getUniqueId())) {
                        event.setCancelled(true);
                        String itemName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
                                ? item.getItemMeta().getDisplayName()
                                : item.getType().name();
                        MessageUtil.send(player, configManager.getSoulboundCantPickupMessage()
                                .replace("{item_name}", itemName)
                                .replace("{owner}", soulbindManager.resolveOwnerName(owner)));
                        return;
                    }
                }
            }
        } catch (Exception e) {
            protectionManager.getPlugin().getLogger().warning("[InvKeeper] 인벤토리 드래그 처리 중 오류: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}