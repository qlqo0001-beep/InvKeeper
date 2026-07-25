package com.invkeeper.listener;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.protection.ProtectionManager;
import com.invkeeper.soulbind.SoulbindManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class SoulbindPickupListener implements Listener {
    private final ProtectionManager protectionManager;
    private final ConfigManager configManager;
    private final SoulbindManager soulbindManager;
    private final org.bukkit.plugin.Plugin plugin;
    private final java.util.Map<java.util.UUID, Long> lastPickupMessageTime = new java.util.HashMap<>();

    public SoulbindPickupListener(ProtectionManager protectionManager, ConfigManager configManager, org.bukkit.plugin.Plugin plugin) {
        this.protectionManager = protectionManager;
        this.configManager = configManager;
        this.soulbindManager = protectionManager.getSoulbindManager();
        this.plugin = plugin;
    }

    private boolean canBypass(Player player) {
        return player.hasPermission("invkeeper.admin") || player.hasPermission("invkeeper.soulbind.bypass");
    }

    private String formatItemName(org.bukkit.Material material) {
        if (material == null) return "알 수 없는 아이템";
        String name = material.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return name;
    }

    private boolean canSendPickupMessage(Player player) {
        long cooldownSeconds = configManager.getSoulbindPickupMessageCooldownSeconds();
        if (cooldownSeconds <= 0) return true; // 쿨다운 없음
        long now = System.currentTimeMillis();
        long last = lastPickupMessageTime.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldownSeconds * 1000L) {
            return false;
        }
        lastPickupMessageTime.put(player.getUniqueId(), now);
        return true;
    }

    @EventHandler
    public void onEntityPickup(EntityPickupItemEvent event) {
        try {
            if (!(event.getEntity() instanceof Player player)) return;
            ItemStack item = event.getItem().getItemStack();
            if (!soulbindManager.isSoulbound(item)) return;
            
            // Check if soulbind has expired and remove it
            if (soulbindManager.checkAndRemoveExpired(item)) {
                // Soulbind was expired and removed, update the item stack
                event.getItem().setItemStack(item);
                // Allow pickup since soulbind is now removed
                return;
            }
            
            var owner = soulbindManager.getOwnerUuid(item);
            if (owner == null) {
                // If owner not set, bind to picker (whole stack)
                soulbindManager.applySoulbind(item, player.getUniqueId(), soulbindManager.getExpiryMillis(item));
                // Update the entity itemstack
                event.getItem().setItemStack(item);
                // Verify the update was applied (for debugging purposes)
                ItemStack verifyStack = event.getItem().getItemStack();
                if (verifyStack == null || !soulbindManager.isSoulbound(verifyStack)) {
                    plugin.getLogger().warning("[InvKeeper] 픽업 아이템 소울바인드 적용 후 검증 실패: 아이템이 업데이트되지 않았을 수 있습니다.");
                }
                MessageUtil.send(player, configManager.getSoulboundAppliedMessage().replace("{owner}", player.getName()).replace("{remaining}", soulbindManager.formatRemaining(item, configManager.getTimeFormat())));
                return;
            }
            if (!owner.equals(player.getUniqueId())) {
                // Allow bypass permission holders to pick up
                if (canBypass(player)) return;
                // Not owner -> cancel pickup
                event.setCancelled(true);
                String ownerName = owner == null ? "?" : (Bukkit.getPlayer(owner) != null ? Bukkit.getPlayer(owner).getName() : Bukkit.getOfflinePlayer(owner).getName());
                if (ownerName == null) ownerName = owner.toString();
                // Send message with cooldown
                if (canSendPickupMessage(player)) {
                    String itemName = item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null
                            ? item.getItemMeta().getDisplayName()
                            : formatItemName(item.getType());
                    MessageUtil.send(player, configManager.getSoulboundCantPickupMessage()
                            .replace("{owner}", ownerName)
                            .replace("{item_name}", itemName));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[InvKeeper] 아이템 픽업 처리 중 오류: " + e.getMessage());
        }
    }
}
