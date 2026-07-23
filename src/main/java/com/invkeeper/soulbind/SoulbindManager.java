package com.invkeeper.soulbind;

import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class SoulbindManager {
    public enum SoulbindType {
        NONE,
        TIME,
        STACK
    }

    private final NamespacedKey ownerKey;
    private final NamespacedKey expiryKey;
    private final NamespacedKey stacksKey;
    private final NamespacedKey loreTextKey;
    private final Plugin plugin;

    public SoulbindManager(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.ownerKey = new NamespacedKey(plugin, "soulbind_owner");
        this.expiryKey = new NamespacedKey(plugin, "soulbind_expiry");
        this.stacksKey = new NamespacedKey(plugin, "soulbind_stacks");
        this.loreTextKey = new NamespacedKey(plugin, "soulbind_lore_text");
    }

    /**
     * Get the soulbind type of an item.
     */
    public SoulbindType getSoulbindType(ItemStack item) {
        if (item == null) return SoulbindType.NONE;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return SoulbindType.NONE;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        boolean hasExpiry = pdc.has(expiryKey, PersistentDataType.LONG);
        boolean hasStacks = pdc.has(stacksKey, PersistentDataType.INTEGER);

        if (hasExpiry) return SoulbindType.TIME;
        if (hasStacks) return SoulbindType.STACK;
        return SoulbindType.NONE;
    }

    /**
     * Apply time-based soulbind to the given ItemStack (modifies meta).
     * expiryMillis: epoch millis, -1 for infinite.
     */
    public void applySoulbind(ItemStack item, UUID owner, long expiryMillis) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (owner != null) {
            pdc.set(ownerKey, PersistentDataType.STRING, owner.toString());
        }
        pdc.set(expiryKey, PersistentDataType.LONG, expiryMillis);
        // Remove any stack data to ensure clean state
        pdc.remove(stacksKey);
        item.setItemMeta(meta);
    }

    /**
     * Apply stack-based soulbind to the given ItemStack.
     * stacks: number of deaths the item can withstand, -1 for infinite.
     */
    public void applyStackSoulbind(ItemStack item, UUID owner, int stacks) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (owner != null) {
            pdc.set(ownerKey, PersistentDataType.STRING, owner.toString());
        }
        pdc.set(stacksKey, PersistentDataType.INTEGER, stacks);
        // Remove any expiry data to ensure clean state
        pdc.remove(expiryKey);
        item.setItemMeta(meta);
    }

    public void removeSoulbind(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(ownerKey);
        pdc.remove(expiryKey);
        pdc.remove(stacksKey);
        pdc.remove(loreTextKey);
        item.setItemMeta(meta);
    }

    public boolean isSoulbound(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(ownerKey, PersistentDataType.STRING)
            || pdc.has(expiryKey, PersistentDataType.LONG)
            || pdc.has(stacksKey, PersistentDataType.INTEGER);
    }

    /**
     * Paper-only fast path for isSoulbound(). Reads the PDC directly off the
     * ItemStack (Paper's ItemStack#getPersistentDataContainer()) instead of cloning the
     * full ItemMeta via getItemMeta(). Meant for hot loops (like the periodic
     * expiry scanner) that check many items where most aren't soulbound at all.
     * Falls back to isSoulbound() if this isn't actually running on Paper.
     */
    public boolean isSoulboundFast(ItemStack item) {
        if (item == null) return false;
        try {
            PersistentDataContainerView pdc = item.getPersistentDataContainer();
            return pdc.has(ownerKey, PersistentDataType.STRING)
                || pdc.has(expiryKey, PersistentDataType.LONG)
                || pdc.has(stacksKey, PersistentDataType.INTEGER);
        } catch (NoSuchMethodError | Exception notPaper) {
            return isSoulbound(item);
        }
    }

    public UUID getOwnerUuid(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String s = pdc.get(ownerKey, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns expiry epoch millis, or -1 if not set / infinite.
     */
    public long getExpiryMillis(ItemStack item) {
        if (item == null) return -1L;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return -1L;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Long v = pdc.get(expiryKey, PersistentDataType.LONG);
        return v == null ? -1L : v;
    }

    /**
     * Returns remaining stacks, or -1 if not set / infinite.
     */
    public int getStacks(ItemStack item) {
        if (item == null) return -1;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return -1;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer v = pdc.get(stacksKey, PersistentDataType.INTEGER);
        return v == null ? -1 : v;
    }

    /**
     * Decrement stacks by 1. If stacks is -1 (infinite), does nothing.
     */
    public void decrementStacks(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer v = pdc.get(stacksKey, PersistentDataType.INTEGER);
        if (v == null || v < 0) return; // infinite or no stack data
        int newStacks = v - 1;
        if (newStacks <= 0) {
            // Will be cleaned up by caller (checkAndRemoveExpired)
            pdc.set(stacksKey, PersistentDataType.INTEGER, 0);
        } else {
            pdc.set(stacksKey, PersistentDataType.INTEGER, newStacks);
        }
        item.setItemMeta(meta);
    }

    /**
     * Check if the soulbind on this item has expired (time-based) or
     * stacks reached 0 (stack-based) and remove it if so.
     * Also removes the soulbind lore lines.
     * @return true if the soulbind was expired/consumed and removed, false otherwise
     */
    public boolean checkAndRemoveExpired(ItemStack item) {
        if (item == null) return false;
        if (!isSoulbound(item)) return false;

        SoulbindType type = getSoulbindType(item);

        if (type == SoulbindType.TIME) {
            long expiry = getExpiryMillis(item);
            // -1 means infinite, never expires
            if (expiry < 0) return false;

            // Check if expired
            if (System.currentTimeMillis() > expiry) {
                removeLoreLines(item);
                removeSoulbind(item);
                return true;
            }
        } else if (type == SoulbindType.STACK) {
            int stacks = getStacks(item);
            // -1 means infinite, never expires
            if (stacks < 0) return false;

            // Check if stacks depleted
            if (stacks <= 0) {
                removeLoreLines(item);
                removeSoulbind(item);
                return true;
            }
        }

        return false;
    }

    /**
     * Update the item's lore to include soulbind information.
     * Uses PDC to store the exact lore text for reliable matching even after server restart.
     * Always removes old soulbind lore line and appends a new one.
     * Supports both time-based ({expiry}) and stack-based ({stacks}) lore formats.
     */
    public void updateLore(ItemStack item, String timeLoreFormat, String stackLoreFormat, String ownerName, long expiryMillis, int stacks) {
        if (item == null) return;
        if (timeLoreFormat == null || timeLoreFormat.isEmpty()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String newLoreLine;

        if (stacks >= 0) {
            // Stack-based soulbind
            if (stackLoreFormat == null || stackLoreFormat.isEmpty()) return;
            String stacksStr = stacks < 0 ? "무한" : String.valueOf(stacks);
            newLoreLine = stackLoreFormat
                    .replace("{owner}", ownerName != null ? ownerName : "?")
                    .replace("{stacks}", stacksStr);
        } else {
            // Time-based soulbind
            String expiryStr = MessageUtil.formatExpiry(expiryMillis);
            newLoreLine = timeLoreFormat
                    .replace("{owner}", ownerName != null ? ownerName : "?")
                    .replace("{expiry}", expiryStr);
        }

        String coloredLoreLine = MessageUtil.color(newLoreLine);

        // Get the previously stored lore text from PDC (survives server restarts)
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String oldLoreText = pdc.get(loreTextKey, PersistentDataType.STRING);

        List<String> currentLore = meta.getLore();
        if (currentLore == null) {
            currentLore = new ArrayList<>();
        }

        // Remove the old soulbind lore line if we have its text stored
        if (oldLoreText != null && !oldLoreText.isEmpty()) {
            currentLore.remove(oldLoreText);
        }

        // Add the new soulbind lore line
        currentLore.add(coloredLoreLine);

        // Store the new lore text in PDC for future matching
        pdc.set(loreTextKey, PersistentDataType.STRING, coloredLoreLine);

        meta.setLore(currentLore);
        item.setItemMeta(meta);
    }

    /**
     * Convenience method: update lore for time-based soulbind (no stacks parameter).
     */
    public void updateLore(ItemStack item, String loreFormat, String ownerName, long expiryMillis) {
        updateLore(item, loreFormat, null, ownerName, expiryMillis, -1);
    }

    /**
     * Remove soulbind-related lore lines from the item.
     * Uses PDC stored lore text for reliable matching.
     */
    public void removeLoreLines(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String oldLoreText = pdc.get(loreTextKey, PersistentDataType.STRING);
        if (oldLoreText == null || oldLoreText.isEmpty()) return;

        List<String> currentLore = meta.getLore();
        if (currentLore == null || currentLore.isEmpty()) return;

        currentLore.remove(oldLoreText);
        meta.setLore(currentLore);
        item.setItemMeta(meta);
    }

    public boolean isActiveForPlayer(ItemStack item, Player player) {
        if (item == null || player == null) return false;
        UUID owner = getOwnerUuid(item);
        if (owner == null) return false;
        if (!owner.equals(player.getUniqueId())) return false;

        SoulbindType type = getSoulbindType(item);
        if (type == SoulbindType.TIME) {
            long expiry = getExpiryMillis(item);
            if (expiry < 0) return true; // infinite
            return expiry - System.currentTimeMillis() > 0;
        } else if (type == SoulbindType.STACK) {
            int stacks = getStacks(item);
            if (stacks < 0) return true; // infinite
            return stacks > 0;
        }
        return false;
    }

    public long remainingMillis(ItemStack item) {
        long expiry = getExpiryMillis(item);
        if (expiry < 0) return Long.MAX_VALUE;
        long rem = expiry - System.currentTimeMillis();
        return Math.max(0, rem);
    }

    public String formatRemaining(ItemStack item, String format) {
        long rem = remainingMillis(item);
        if (rem == Long.MAX_VALUE) {
            return "무한";
        }
        return MessageUtil.formatDuration(rem, format);
    }

    /**
     * Resolve an owner UUID to a display name.
     * Tries online player name first, then offline player name, then UUID string as fallback.
     */
    public String resolveOwnerName(java.util.UUID ownerUuid) {
        if (ownerUuid == null) return "?";
        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(ownerUuid);
        if (p != null) return p.getName();
        org.bukkit.OfflinePlayer off = org.bukkit.Bukkit.getOfflinePlayer(ownerUuid);
        String name = off.getName();
        return name != null ? name : ownerUuid.toString();
    }
}
