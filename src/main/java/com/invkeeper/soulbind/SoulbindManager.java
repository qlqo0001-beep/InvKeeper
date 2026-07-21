package com.invkeeper.soulbind;

import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class SoulbindManager {
    private final NamespacedKey ownerKey;
    private final NamespacedKey expiryKey;
    private final NamespacedKey loreTextKey;
    private final Plugin plugin;

    public SoulbindManager(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.ownerKey = new NamespacedKey(plugin, "soulbind_owner");
        this.expiryKey = new NamespacedKey(plugin, "soulbind_expiry");
        this.loreTextKey = new NamespacedKey(plugin, "soulbind_lore_text");
    }

    /**
     * Apply soulbind to the given ItemStack (modifies meta). Applies to the whole stack.
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
        item.setItemMeta(meta);
    }

    public void removeSoulbind(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(ownerKey);
        pdc.remove(expiryKey);
        pdc.remove(loreTextKey);
        item.setItemMeta(meta);
    }

    public boolean isSoulbound(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(ownerKey, PersistentDataType.STRING) || pdc.has(expiryKey, PersistentDataType.LONG);
    }

    /**
     * Paper-only fast path for isSoulbound(). Reads the PDC directly off the
     * ItemStack (Paper's ItemStack#getPersistentDataContainer(), added well
     * before this project's declared api-version 1.21) instead of cloning the
     * full ItemMeta via getItemMeta(). Meant for hot loops (like the periodic
     * expiry scanner) that check many items where most aren't soulbound at all -
     * skipping the meta clone for those items is the whole point.
     * Falls back to isSoulbound() if this isn't actually running on Paper.
     */
    public boolean isSoulboundFast(ItemStack item) {
        if (item == null) return false;
        try {
            PersistentDataContainer pdc = item.getPersistentDataContainer();
            return pdc.has(ownerKey, PersistentDataType.STRING) || pdc.has(expiryKey, PersistentDataType.LONG);
        } catch (NoSuchMethodError | Exception notPaper) {
            // Not running on Paper (or API mismatch) - fall back to the safe path.
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
     * Check if the soulbind on this item has expired and remove it if so.
     * Also removes the soulbind lore lines.
     * @return true if the soulbind was expired and removed, false otherwise
     */
    public boolean checkAndRemoveExpired(ItemStack item) {
        if (item == null) return false;
        if (!isSoulbound(item)) return false;
        
        long expiry = getExpiryMillis(item);
        // -1 means infinite, never expires
        if (expiry < 0) return false;
        
        // Check if expired
        if (System.currentTimeMillis() > expiry) {
            // Remove the lore line FIRST, while the PDC still holds the exact
            // text we stored (removeSoulbind() below clears that PDC entry,
            // so doing this in the other order would leave the old lore line
            // stuck on the item forever with no way to find it again).
            removeLoreLines(item);
            // Then clear the owner/expiry/loreText PDC entries.
            removeSoulbind(item);
            return true;
        }
        
        return false;
    }

    /**
     * Update the item's lore to include soulbind information.
     * Uses PDC to store the exact lore text for reliable matching even after server restart.
     * Always removes old soulbind lore line and appends a new one.
     */
    public void updateLore(ItemStack item, String loreFormat, String ownerName, long expiryMillis) {
        if (item == null || loreFormat == null || loreFormat.isEmpty()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String expiryStr = MessageUtil.formatExpiry(expiryMillis);
        String newLoreLine = loreFormat
                .replace("{owner}", ownerName != null ? ownerName : "?")
                .replace("{expiry}", expiryStr);
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
        long expiry = getExpiryMillis(item);
        if (expiry < 0) return true; // infinite
        return expiry - System.currentTimeMillis() > 0;
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
}