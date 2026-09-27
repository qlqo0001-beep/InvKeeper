package com.invkeeper.listener;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.grave.GraveManager;
import com.invkeeper.protection.ProtectionManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerDeathListener implements Listener {
    private final ProtectionManager protectionManager;
    private final ConfigManager configManager;
    private final GraveManager graveManager;

    public PlayerDeathListener(ProtectionManager protectionManager, ConfigManager configManager,
                               GraveManager graveManager) {
        this.protectionManager = protectionManager;
        this.configManager = configManager;
        this.graveManager = graveManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 다른 플레이어에게 죽었으면 PvP 사망 (자기 자신의 화살/TNT 등은 PvE로 취급)
        Player killer = event.getEntity().getKiller();
        boolean pvp = killer != null && !killer.getUniqueId().equals(event.getEntity().getUniqueId());
        try {
            Player player = event.getEntity();
            if (player == null) return;
            
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

            if (pvp && !configManager.isPvpProtectionItemsWork()) {
                // PvP 사망에는 보호권이 적용되지 않음 (소모하지 않음)
                if (protectionManager.hasProtection(player)) {
                    MessageUtil.send(player, configManager.getPvpProtectionIgnoredMessage());
                }
            } else {
                ProtectionManager.ProtectionResult result = protectionManager.checkAndConsumeProtection(player);
                if (result != ProtectionManager.ProtectionResult.NONE) {
                    if (result == ProtectionManager.ProtectionResult.TIMED) {
                        long remainingMillis = protectionManager.getTimedProtectionStore().getRemainingMillis(player);
                        String remaining = MessageUtil.formatDuration(remainingMillis, configManager.getTimeFormat());
                        MessageUtil.send(player, configManager.getTimedProtectedMessage().replace("{remaining}", remaining));
                    } else {
                        MessageUtil.send(player, configManager.getProtectedMessage());
                    }
                    return;
                }
            }
        } catch (Exception e) {
            // Log exception but don't crash the plugin
            protectionManager.getPlugin().getLogger().warning("[InvKeeper] 사망 처리 중 오류 발생: " + e.getMessage());
        }

        // Phase 1: Remove expired TIME-based soulbinds before drop calculation
        Player player = event.getEntity();
        PlayerInventory inventory = player.getInventory();
        com.invkeeper.soulbind.SoulbindManager soulbind = protectionManager.getSoulbindManager();
        if (soulbind != null) {
            for (int slot = 0; slot <= 40; slot++) {
                ItemStack item = inventory.getItem(slot);
                if (item == null || item.getType().isAir()) continue;
                if (soulbind.getSoulbindType(item) == com.invkeeper.soulbind.SoulbindManager.SoulbindType.TIME) {
                    soulbind.checkAndRemoveExpired(item);
                }
            }
        }

        double[] percents = configManager.resolveDropPercents(player, player.getWorld().getName(), pvp);
        int inventoryPercent = clamp((int) Math.round(percents[0]), 0, 100);
        int expPercent = clamp((int) Math.round(percents[1]), 0, 100);

        // ── Grave system path ────────────────────────────────
        // disabled-worlds에서는 무덤 대신 아래 기존 드랍 방식으로 처리
        if (graveManager != null && graveManager.isGraveWorld(player.getWorld())) {
            // 무덤 자물쇠는 드랍 선정 전에 1개 소모 (무덤이 생성되지 않으면 되돌림)
            ProtectionManager.TakenGraveLock lock = protectionManager.takeGraveLock(player);
            long lockedUntil = 0;
            int lockExtraCastSeconds = 0;
            if (lock != null) {
                int lockSeconds = lock.getConfig().getLockSeconds();
                lockedUntil = lockSeconds > 0 ? System.currentTimeMillis() + lockSeconds * 1000L : 0;
                lockExtraCastSeconds = lock.getConfig().getExtraCastSeconds();
            }

            // Use same percent-based selection logic as the old dropInventory
            List<Integer> occupiedSlots = new ArrayList<>();
            for (int slot = 0; slot < 41; slot++) {
                ItemStack item = inventory.getItem(slot);
                if (item != null && !item.getType().isAir()
                    && (soulbind == null || !soulbind.isSoulbound(item))) {
                    occupiedSlots.add(slot);
                }
            }
            int slotsToDrop = occupiedSlots.isEmpty() ? 0
                : clamp((int) Math.floor(occupiedSlots.size() * inventoryPercent / 100.0), 0, occupiedSlots.size());
            Collections.shuffle(occupiedSlots);

            ItemStack[] eq = new ItemStack[4];  // eq[3]=helmet, eq[2]=chest, eq[1]=leggings, eq[0]=boots
            ItemStack oh = null;
            ItemStack[] inv = new ItemStack[36];

            for (int i = 0; i < slotsToDrop; i++) {
                int slot = occupiedSlots.get(i);
                ItemStack item = inventory.getItem(slot);
                if (item == null || item.getType().isAir()) continue;
                if (soulbind != null && soulbind.isSoulbound(item)) continue;

                if (slot == 39) eq[3] = item.clone();
                else if (slot == 38) eq[2] = item.clone();
                else if (slot == 37) eq[1] = item.clone();
                else if (slot == 36) eq[0] = item.clone();
                else if (slot == 40) oh = item.clone();
                else if (slot >= 0 && slot < 36) inv[slot] = item.clone();

                inventory.setItem(slot, null);
            }

            int totalExp = calculateExpLoss(player, expPercent);
            com.invkeeper.grave.Grave grave = graveManager.createGrave(player, player.getLocation(), eq, oh, inv, totalExp,
                    lockedUntil, lockExtraCastSeconds);
            if (lock != null) {
                if (grave == null) {
                    // 잃은 것이 없어 무덤이 생성되지 않음 -> 자물쇠 되돌림
                    lock.refund(player);
                } else {
                    MessageUtil.send(player, configManager.getGraveLockAppliedMessage()
                            .replace("{lock_seconds}", String.valueOf(lock.getConfig().getLockSeconds()))
                            .replace("{extra_cast_seconds}", String.valueOf(lock.getConfig().getExtraCastSeconds())));
                }
            }

            decrementStackSoulbinds(player, inventory, soulbind);

            int actualLostPercent = occupiedSlots.isEmpty() ? 0
                : clamp((int) Math.round(slotsToDrop * 100.0 / occupiedSlots.size()), 0, 100);
            sendDeathMessage(player, pvp ? killer : null, actualLostPercent, expPercent, slotsToDrop, totalExp);
            return;
        }

        int totalOccupiedSlots = countOccupiedSlots(player);
        int droppedItems = dropInventory(player, inventoryPercent);
        int droppedExp = dropExperience(player, expPercent);

        int actualLostPercent = totalOccupiedSlots <= 0 ? 0 : clamp((int) Math.round(droppedItems * 100.0 / totalOccupiedSlots), 0, 100);

        decrementStackSoulbinds(player, inventory, soulbind);

        sendDeathMessage(player, pvp ? killer : null, actualLostPercent, expPercent, droppedItems, droppedExp);
    }

    private void sendDeathMessage(Player player, Player killer, int invPercent, int expPercent, int itemsDropped, int expDropped) {
        String message = killer != null
                ? configManager.getDeathPvpMessage().replace("{killer}", killer.getName())
                : configManager.getDeathMessage();
        MessageUtil.send(player, message
                .replace("{inv_percent}", String.valueOf(invPercent))
                .replace("{exp_percent}", String.valueOf(expPercent))
                .replace("{items_dropped}", String.valueOf(itemsDropped))
                .replace("{exp_dropped}", String.valueOf(expDropped)));
    }

    /**
     * Phase 3: Decrement STACK soulbinds after drop calculation (item was protected this death)
     */
    private void decrementStackSoulbinds(Player player, PlayerInventory inventory,
                                         com.invkeeper.soulbind.SoulbindManager soulbind) {
        if (soulbind != null) {
            for (int slot = 0; slot <= 40; slot++) {
                ItemStack item = inventory.getItem(slot);
                if (item == null || item.getType().isAir()) continue;
                if (soulbind.getSoulbindType(item) == com.invkeeper.soulbind.SoulbindManager.SoulbindType.STACK) {
                    java.util.UUID ownerUuid = soulbind.getOwnerUuid(item);
                    String ownerName = ownerUuid != null ? soulbind.resolveOwnerName(ownerUuid) : player.getName();
                    long expiryMillis = soulbind.getExpiryMillis(item);
                    int newStacks = soulbind.getStacks(item);
                    soulbind.decrementStacks(item);
                    newStacks = soulbind.getStacks(item);
                    // Update lore to reflect new stack count
                    soulbind.updateLore(item, 
                        configManager.getSoulboundLoreFormat(),
                        configManager.getSoulboundLoreFormatStack(),
                        ownerName, expiryMillis, newStacks);
                    // Check if stacks reached 0 and remove if so
                    soulbind.checkAndRemoveExpired(item);
                }
            }
        }
    }

    private int dropInventory(Player player, int percent) {
        if (percent <= 0) {
            return 0;
        }
        PlayerInventory inventory = player.getInventory();
        List<Integer> occupiedSlots = new ArrayList<>();
        for (int slot = 0; slot < 41; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                // Skip soulbound items entirely (they should not be dropped on death)
                if (protectionManager.getSoulbindManager() != null && protectionManager.getSoulbindManager().isSoulbound(item)) {
                    continue;
                }
                occupiedSlots.add(slot);
            }
        }

        if (occupiedSlots.isEmpty()) {
            return 0;
        }

        int slotsToDrop = clamp((int) Math.floor(occupiedSlots.size() * percent / 100.0), 0, occupiedSlots.size());
        Collections.shuffle(occupiedSlots);
        Location location = player.getLocation();
        int droppedCount = 0;
        for (int i = 0; i < slotsToDrop; i++) {
            int slot = occupiedSlots.get(i);
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            // Double-check: skip soulbound items if encountered
            if (protectionManager.getSoulbindManager() != null && protectionManager.getSoulbindManager().isSoulbound(item)) {
                continue;
            }
            player.getWorld().dropItemNaturally(location, item);
            inventory.setItem(slot, null);
            droppedCount++;
        }
        return droppedCount;
    }

    /**
     * Calculates XP loss without spawning orbs (for grave path).
     */
    private int calculateExpLoss(Player player, int percent) {
        int totalExp = totalExp(player.getLevel(), player.getExp());
        if (totalExp <= 0 || percent <= 0) {
            setFromTotalExp(player, totalExp);
            return 0;
        }
        int dropAmount = clamp((int) Math.round(totalExp * percent / 100.0), 0, totalExp);
        int remaining = totalExp - dropAmount;
        setFromTotalExp(player, remaining);
        return dropAmount;
    }

    private int dropExperience(Player player, int percent) {
        int totalExp = totalExp(player.getLevel(), player.getExp());
        if (totalExp <= 0 || percent <= 0) {
            setFromTotalExp(player, totalExp);
            return 0;
        }
        int dropAmount = clamp((int) Math.round(totalExp * percent / 100.0), 0, totalExp);
        int remaining = totalExp - dropAmount;
        if (dropAmount > 0) {
            player.getWorld().spawn(player.getLocation(), ExperienceOrb.class, orb -> orb.setExperience(dropAmount));
        }
        setFromTotalExp(player, remaining);
        return dropAmount;
    }

    private int countOccupiedSlots(Player player) {
        int occupied = 0;
        PlayerInventory inventory = player.getInventory();
        com.invkeeper.soulbind.SoulbindManager soulbind = protectionManager.getSoulbindManager();
        for (int slot = 0; slot < 41; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                // Exclude soulbound items to match dropInventory() logic
                if (soulbind != null && soulbind.isSoulbound(item)) {
                    continue;
                }
                occupied++;
            }
        }
        return occupied;
    }

    private static int totalExp(int level, float progress) {
        int total = 0;
        for (int i = 0; i < level; i++) {
            total += expToNextLevel(i);
        }
        total += Math.round(expToNextLevel(level) * progress);
        return total;
    }

    private static int expToNextLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        }
        if (level >= 15) {
            return 37 + (level - 15) * 5;
        }
        return 7 + level * 2;
    }

    private static void setFromTotalExp(Player player, int experience) {
        if (player == null) {
            return;
        }
        int level = 0;
        int remaining = experience;
        while (remaining >= expToNextLevel(level)) {
            remaining -= expToNextLevel(level);
            level++;
        }
        player.setLevel(level);
        player.setExp(expToNextLevel(level) == 0 ? 0F : (float) remaining / (float) expToNextLevel(level));
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
