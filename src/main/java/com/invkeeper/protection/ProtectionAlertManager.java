package com.invkeeper.protection;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProtectionAlertManager implements Runnable {
    private static final long FIVE_MINUTES_MS = 300_000L;
    private static final long ONE_MINUTE_MS = 60_000L;

    private final ProtectionManager protectionManager;
    private final ConfigManager configManager;
    private final Map<UUID, ReminderState> reminderStates = new ConcurrentHashMap<>();

    public ProtectionAlertManager(Plugin plugin, ProtectionManager protectionManager, ConfigManager configManager) {
        this.protectionManager = protectionManager;
        this.configManager = configManager;
        Bukkit.getScheduler().runTaskTimer(plugin, this, 20L, 20L);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayer(player);
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
