package com.invkeeper.protection;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.config.ProtectionItemConfig;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VanillaProtectionItems {
    public enum Kind {
        CONSUMABLE,
        TIMED
    }

    private final NamespacedKey kindKey;
    private final NamespacedKey itemKey;
    private final ConfigManager configManager;

    public VanillaProtectionItems(Plugin plugin, ConfigManager configManager) {
        this.kindKey = new NamespacedKey(plugin, "protect_item_kind");
        this.itemKey = new NamespacedKey(plugin, "protect_item_key");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
    }

    /**
     * Create a vanilla protection item with soulbind lore placeholder replacement.
     * @param config the protection item configuration
     * @param ownerName owner display name for {owner} placeholder (null to skip replacement)
     * @param remainingTime remaining time string for {remaining} placeholder (null to skip replacement)
     */
    public ItemStack create(ProtectionItemConfig config, String ownerName, String remainingTime) {
        Material material = config.getVanillaMaterial();
        String displayName = config.getVanillaName();
        List<String> lore = config.getVanillaLore();
        if (config.getKind() == ProtectionItemConfig.Kind.TIMED_PROTECTION) {
            lore = replaceDurationPlaceholder(lore, config.getDurationMinutes());
        }
        if (ownerName != null) {
            lore = replacePlaceholder(lore, "{owner}", ownerName);
        }
        if (remainingTime != null) {
            lore = replacePlaceholder(lore, "{remaining}", remainingTime);
        }
        Integer customModelData = config.getCustomModelData();

        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.color(displayName));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(MessageUtil.color(line));
            }
            meta.setLore(coloredLore);
            if (customModelData != null) {
                meta.setCustomModelData(customModelData);
            }
            meta.getPersistentDataContainer().set(kindKey, PersistentDataType.STRING, config.getKind().name());
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, config.getKey());
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Create a vanilla protection item without soulbind placeholder replacement.
     */
    public ItemStack create(ProtectionItemConfig config) {
        return create(config, null, null);
    }

    public boolean isKind(ItemStack item, Kind kind) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        String stored = meta.getPersistentDataContainer().get(kindKey, PersistentDataType.STRING);
        return stored != null && stored.equals(kind.name());
    }

    public String getItemKey(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    private static List<String> replacePlaceholder(List<String> lore, String placeholder, String value) {
        List<String> replaced = new ArrayList<>();
        for (String line : lore) {
            if (line == null) {
                continue;
            }
            replaced.add(line.replace(placeholder, value));
        }
        return replaced;
    }

    private static List<String> replaceDurationPlaceholder(List<String> lore, int durationMinutes) {
        return replacePlaceholder(lore, "{duration}", String.valueOf(durationMinutes));
    }
}
