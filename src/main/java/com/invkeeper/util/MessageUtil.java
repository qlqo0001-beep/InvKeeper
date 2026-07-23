package com.invkeeper.util;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;

public final class MessageUtil {
    private MessageUtil() {
    }

    public static String color(String text) {
        if (text == null) {
            return null;
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static void send(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }
        sender.sendMessage(color(message));
    }

    public static String formatDuration(long millis, String format) {
        long totalSeconds = Math.max(0, millis / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        String secondsPadded = String.format("%02d", seconds);
        String result = format;
        if (result == null) result = "{minutes}분 {seconds_padded}초";
        result = result.replace("{total_seconds}", String.valueOf(totalSeconds));
        result = result.replace("{minutes}", String.valueOf(minutes));
        result = result.replace("{seconds}", String.valueOf(seconds));
        result = result.replace("{seconds_padded}", secondsPadded);
        return result;
    }

    private static java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();

    /**
     * Set the timezone for expiry formatting. Called from ConfigManager on reload.
     * @param timezoneId e.g. "Asia/Seoul", "UTC", "America/New_York"
     */
    public static void setTimezone(String timezoneId) {
        if (timezoneId == null || timezoneId.isBlank()) {
            zoneId = java.time.ZoneId.systemDefault();
            return;
        }
        try {
            zoneId = java.time.ZoneId.of(timezoneId);
        } catch (java.time.zone.ZoneRulesException | IllegalArgumentException e) {
            java.util.logging.Logger.getLogger(MessageUtil.class.getName())
                    .warning("[InvKeeper] 잘못된 타임존: '" + timezoneId + "', 시스템 기본 타임존 사용");
            zoneId = java.time.ZoneId.systemDefault();
        }
    }

    /**
     * Get the current timezone ID being used for expiry formatting.
     */
    public static String getTimezoneId() {
        return zoneId.getId();
    }

    /**
     * Format an expiry epoch millis into a human-readable date/time string.
     * Returns "무한" if expiryMillis is -1.
     * Example: "2026-07-20 14:30" (timezone based)
     */
    public static String formatExpiry(long expiryMillis) {
        if (expiryMillis < 0) {
            return "무한";
        }
        java.time.Instant instant = java.time.Instant.ofEpochMilli(expiryMillis);
        java.time.ZonedDateTime zdt = instant.atZone(zoneId);
        return String.format("%04d-%02d-%02d %02d:%02d",
                zdt.getYear(), zdt.getMonthValue(), zdt.getDayOfMonth(),
                zdt.getHour(), zdt.getMinute());
    }
}
