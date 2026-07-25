package com.invkeeper.listener;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.protection.ProtectionManager;
import com.invkeeper.soulbind.SoulbindManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoulbindUseListener implements Listener {
    private final ProtectionManager protectionManager;
    private final ConfigManager configManager;
    private final SoulbindManager soulbindManager;
    private final Map<UUID, Long> lastUseMessageTime = new HashMap<>();

    public SoulbindUseListener(ProtectionManager protectionManager, ConfigManager configManager) {
        this.protectionManager = protectionManager;
        this.configManager = configManager;
        this.soulbindManager = protectionManager.getSoulbindManager();
    }

    private boolean canBypass(Player player) {
        return player.hasPermission("invkeeper.admin") || player.hasPermission("invkeeper.soulbind.bypass");
    }

    private boolean isBlockedForUse(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (soulbindManager.isSoulbound(item)) {
            soulbindManager.checkAndRemoveExpired(item);
        }
        if (!soulbindManager.isSoulbound(item)) return false;
        UUID owner = soulbindManager.getOwnerUuid(item);
        if (owner == null) return false;
        if (owner.equals(player.getUniqueId())) return false;
        if (canBypass(player)) return false;
        return true;
    }

    private boolean canSendUseMessage(Player player) {
        long cooldownSeconds = configManager.getSoulbindUseMessageCooldownSeconds();
        if (cooldownSeconds <= 0) return true;
        long now = System.currentTimeMillis();
        long last = lastUseMessageTime.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldownSeconds * 1000L) {
            return false;
        }
        lastUseMessageTime.put(player.getUniqueId(), now);
        return true;
    }

    private void notifyBlocked(Player player, ItemStack item, UUID owner) {
        if (!canSendUseMessage(player)) return;
        String itemName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : item.getType().name();
        MessageUtil.send(player, configManager.getSoulboundCantPickupMessage()
                .replace("{item_name}", itemName)
                .replace("{owner}", soulbindManager.resolveOwnerName(owner)));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        try {
            if (event.getAction() != Action.RIGHT_CLICK_AIR
                    && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            if (item == null || item.getType().isAir()) return;
            if (!isBlockedForUse(player, item)) return;

            event.setCancelled(true);
            notifyBlocked(player, item, soulbindManager.getOwnerUuid(item));
        } catch (Exception e) {
            protectionManager.getPlugin().getLogger().warning("[InvKeeper] 아이템 사용 처리 중 오류: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        try {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();
            if (!isBlockedForUse(player, item)) return;

            event.setCancelled(true);
            notifyBlocked(player, item, soulbindManager.getOwnerUuid(item));
        } catch (Exception e) {
            protectionManager.getPlugin().getLogger().warning("[InvKeeper] 블록 채굴 처리 중 오류: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        try {
            ItemStack item = event.getItemInHand();
            Player player = event.getPlayer();
            if (!isBlockedForUse(player, item)) return;

            event.setCancelled(true);
            notifyBlocked(player, item, soulbindManager.getOwnerUuid(item));
        } catch (Exception e) {
            protectionManager.getPlugin().getLogger().warning("[InvKeeper] 블록 설치 처리 중 오류: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        try {
            if (!(event.getDamager() instanceof Player player)) return;
            ItemStack item = player.getInventory().getItemInMainHand();
            if (!isBlockedForUse(player, item)) return;

            event.setCancelled(true);
            notifyBlocked(player, item, soulbindManager.getOwnerUuid(item));
        } catch (Exception e) {
            protectionManager.getPlugin().getLogger().warning("[InvKeeper] 엔티티 공격 처리 중 오류: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        try {
            if (!(event.getEntity() instanceof Player player)) return;
            ItemStack bow = event.getBow();
            if (bow == null || bow.getType().isAir()) return;
            if (!isBlockedForUse(player, bow)) return;

            event.setCancelled(true);
            notifyBlocked(player, bow, soulbindManager.getOwnerUuid(bow));
        } catch (Exception e) {
            protectionManager.getPlugin().getLogger().warning("[InvKeeper] 활 발사 처리 중 오류: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        try {
            if (!(event.getEntity().getShooter() instanceof Player player)) return;
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            ItemStack item = mainHand != null && !mainHand.getType().isAir() ? mainHand : offHand;
            if (item == null || item.getType().isAir()) return;
            if (!isBlockedForUse(player, item)) return;

            event.setCancelled(true);
            notifyBlocked(player, item, soulbindManager.getOwnerUuid(item));
        } catch (Exception e) {
            protectionManager.getPlugin().getLogger().warning("[InvKeeper] 투사체 발사 처리 중 오류: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}