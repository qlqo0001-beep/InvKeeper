package com.invkeeper.listener;

import com.invkeeper.config.ConfigManager;
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

    public PlayerDeathListener(ProtectionManager protectionManager, ConfigManager configManager) {
        this.protectionManager = protectionManager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);

        if (protectionManager.checkAndConsumeProtection(player)) {
            MessageUtil.send(player, configManager.getProtectedMessage());
            return;
        }

        double[] percents = configManager.resolveDropPercents(player, player.getWorld().getName());
        int inventoryPercent = clamp((int) Math.round(percents[0]), 0, 100);
        int expPercent = clamp((int) Math.round(percents[1]), 0, 100);

        int totalOccupiedSlots = countOccupiedSlots(player);
        int droppedItems = dropInventory(player, inventoryPercent);
        int droppedExp = dropExperience(player, expPercent);

        int actualLostPercent = totalOccupiedSlots <= 0 ? 0 : clamp((int) Math.round(droppedItems * 100.0 / totalOccupiedSlots), 0, 100);

        MessageUtil.send(player, configManager.getDeathMessage()
                .replace("{inv_percent}", String.valueOf(actualLostPercent))
                .replace("{exp_percent}", String.valueOf(expPercent))
                .replace("{items_dropped}", String.valueOf(droppedItems))
                .replace("{exp_dropped}", String.valueOf(droppedExp)));
    }

    private int dropInventory(Player player, int percent) {
        if (percent <= 0) {
            return 0;
        }
        PlayerInventory inventory = player.getInventory();
        int slotsToDrop = clamp((int) Math.round(41 * percent / 100.0), 0, 41);
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < 41; slot++) {
            slots.add(slot);
        }
        Collections.shuffle(slots);
        Location location = player.getLocation();
        int droppedCount = 0;
        for (int i = 0; i < slotsToDrop; i++) {
            int slot = slots.get(i);
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            player.getWorld().dropItemNaturally(location, item);
            inventory.setItem(slot, null);
            droppedCount++;
        }
        return droppedCount;
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

    private static int countOccupiedSlots(Player player) {
        int occupied = 0;
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < 41; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
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
