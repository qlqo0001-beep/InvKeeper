package com.invkeeper.protection;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class TimedProtectionStore {
    private final NamespacedKey expiryKey;

    public TimedProtectionStore(Plugin plugin) {
        this.expiryKey = new NamespacedKey(plugin, "timed_protection_expiry");
    }

    public boolean isActive(Player player) {
        if (player == null) {
            return false;
        }
        Long expiry = player.getPersistentDataContainer().get(expiryKey, PersistentDataType.LONG);
        if (expiry == null) {
            return false;
        }
        return expiry - System.currentTimeMillis() > 0;
    }

    public long getRemainingMillis(Player player) {
        if (player == null) {
            return 0L;
        }
        Long expiry = player.getPersistentDataContainer().get(expiryKey, PersistentDataType.LONG);
        if (expiry == null) {
            return 0L;
        }
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Returns the stored expiry epoch millis (even if already past), or -1 if none.
     */
    public long getExpiryMillis(Player player) {
        if (player == null) {
            return -1L;
        }
        Long expiry = player.getPersistentDataContainer().get(expiryKey, PersistentDataType.LONG);
        return expiry == null ? -1L : expiry;
    }

    public void activate(Player player, long durationSeconds) {
        if (player == null) {
            return;
        }
        long expiry = System.currentTimeMillis() + Math.max(0, durationSeconds) * 1000L;
        player.getPersistentDataContainer().set(expiryKey, PersistentDataType.LONG, expiry);
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        player.getPersistentDataContainer().remove(expiryKey);
    }
}
