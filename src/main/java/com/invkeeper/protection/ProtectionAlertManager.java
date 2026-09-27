package com.invkeeper.protection;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ProtectionAlertManager implements Runnable {
    private final ProtectionManager protectionManager;
    private final ConfigManager configManager;
    private final Map<UUID, ReminderState> reminderStates = new HashMap<>();
    private final int taskId;
    // Read once at construction; reloadAlertManager() recreates this class after
    // ConfigManager.load() runs, so /invkeeper reload picks up config changes.
    private final int soulbindScanBatches;
    private int runCount = 0;

    public ProtectionAlertManager(Plugin plugin, ProtectionManager protectionManager, ConfigManager configManager) {
        this.protectionManager = protectionManager;
        this.configManager = configManager;
        // Config already clamps this to >= 1, but clamp again here defensively
        // since a batch count of 0 would cause a divide-by-zero below.
        this.soulbindScanBatches = Math.max(1, configManager.getSoulbindScanBatches());
        this.taskId = Bukkit.getScheduler().runTaskTimer(plugin, this, 20L, 20L).getTaskId();
    }

    /**
     * Clear all reminder states (for shutdown/reload)
     */
    public void shutdown() {
        Bukkit.getScheduler().cancelTask(taskId);
        reminderStates.clear();
    }

    /**
     * Clear reminder state for a specific player (for PlayerQuitEvent)
     */
    public void clearPlayerReminders(UUID playerUuid) {
        reminderStates.remove(playerUuid);
    }

    @Override
    public void run() {
        java.util.List<Player> online = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());

        // Reminders still run every second for every player - this part is cheap
        // (just PDC/number lookups, no ItemMeta cloning).
        for (Player player : online) {
            checkPlayer(player);
        }

        // Soulbind expiry scan: only one rotating batch of players per run.
        com.invkeeper.soulbind.SoulbindManager soulbind = protectionManager.getSoulbindManager();
        if (soulbind != null && !online.isEmpty()) {
            int batch = runCount % soulbindScanBatches;
            for (int i = batch; i < online.size(); i += soulbindScanBatches) {
                scanPlayerSoulbinds(online.get(i), soulbind);
            }
        }
        runCount++;
    }

    private void scanPlayerSoulbinds(Player player, com.invkeeper.soulbind.SoulbindManager soulbind) {
        for (int slot = 0; slot <= 40; slot++) {
            org.bukkit.inventory.ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            // Paper-only fast pre-check: reads the PDC directly off the ItemStack
            // without cloning the full ItemMeta. Most items in an inventory are not
            // soulbound at all, so this lets us skip the expensive getItemMeta()
            // path (used inside checkAndRemoveExpired) for the common case.
            if (!soulbind.isSoulboundFast(item)) continue;
            if (soulbind.checkAndRemoveExpired(item)) {
                // Only update if soulbind was actually removed
                player.getInventory().setItem(slot, item);
            }
        }
    }

    private void checkPlayer(Player player) {
        TimedProtectionStore store = protectionManager.getTimedProtectionStore();
        if (!store.isActive(player)) {
            // A state exists only if protection was active on a previous tick while
            // the player was online; otherwise it ran out while they were offline.
            boolean expiredWhileOnline = reminderStates.remove(player.getUniqueId()) != null;
            long expiry = store.getExpiryMillis(player);
            if (expiry >= 0) {
                // Clear the stale expiry so the expiry notice is sent only once.
                store.clear(player);
                if (expiredWhileOnline) {
                    MessageUtil.send(player, configManager.getTimedExpiredMessage());
                } else {
                    MessageUtil.send(player, configManager.getTimedExpiredOfflineMessage()
                            .replace("{expired_at}", MessageUtil.formatExpiry(expiry)));
                }
            }
            return;
        }

        long remainingMillis = store.getRemainingMillis(player);
        ReminderState state = reminderStates.computeIfAbsent(player.getUniqueId(), uuid -> new ReminderState());

        for (Map.Entry<Long, String> alert : configManager.getTimedRemainingAlerts().entrySet()) {
            long thresholdSeconds = alert.getKey();
            if (remainingMillis <= thresholdSeconds * 1000L && state.sentThresholds.add(thresholdSeconds)) {
                MessageUtil.send(player, alert.getValue().replace("{remaining}", MessageUtil.formatDuration(remainingMillis, configManager.getTimeFormat())));
            }
        }
    }

    private static final class ReminderState {
        private final Set<Long> sentThresholds = new HashSet<>();
    }
}
