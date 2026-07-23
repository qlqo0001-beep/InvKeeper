package com.invkeeper.protection;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProtectionAlertManager implements Runnable {
    private static final long FIVE_MINUTES_MS = 300_000L;
    private static final long ONE_MINUTE_MS = 60_000L;

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
        if (!protectionManager.getTimedProtectionStore().isActive(player)) {
            reminderStates.remove(player.getUniqueId());
            return;
        }

        long remainingMillis = protectionManager.getTimedProtectionStore().getRemainingMillis(player);
        ReminderState state = reminderStates.computeIfAbsent(player.getUniqueId(), uuid -> new ReminderState());

        if (!state.fiveMinuteSent && remainingMillis <= FIVE_MINUTES_MS) {
            MessageUtil.send(player, configManager.getTimedRemainingFiveMinutesMessage().replace("{remaining}", formatDuration(remainingMillis)));
            state.fiveMinuteSent = true;
        }

        if (!state.oneMinuteSent && remainingMillis <= ONE_MINUTE_MS) {
            MessageUtil.send(player, configManager.getTimedRemainingOneMinuteMessage().replace("{remaining}", formatDuration(remainingMillis)));
            state.oneMinuteSent = true;
        }
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d분 %02d초", minutes, seconds);
    }

    private static final class ReminderState {
        private boolean fiveMinuteSent;
        private boolean oneMinuteSent;
    }
}
