package com.invkeeper.protection;

import com.invkeeper.config.ConfigManager;
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
    private final ConfigManager configManager;

    public VanillaProtectionItems(Plugin plugin, ConfigManager configManager) {
        this.kindKey = new NamespacedKey(plugin, "protect_item_kind");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
    }

    public ItemStack create(Kind kind) {
        Material material = Material.PAPER;
        String displayName = "";
        List<String> lore = List.of();
        Integer customModelData = null;
        if (kind == Kind.CONSUMABLE) {
            material = configManager.getVanillaConsumableMaterial();
            displayName = configManager.getVanillaConsumableName();
            lore = configManager.getVanillaConsumableLore();
            customModelData = configManager.getVanillaConsumableCustomModelData();
        } else if (kind == Kind.TIMED) {
            material = configManager.getVanillaTimedMaterial();
            displayName = configManager.getVanillaTimedName();
            lore = replaceDurationPlaceholder(configManager.getVanillaTimedLore(), configManager.getTimedDurationMinutes());
            customModelData = configManager.getVanillaTimedCustomModelData();
        }
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
            meta.getPersistentDataContainer().set(kindKey, PersistentDataType.STRING, kind.name());
            item.setItemMeta(meta);
        }
        return item;
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

    private static List<String> replaceDurationPlaceholder(List<String> lore, int durationMinutes) {
        List<String> replaced = new ArrayList<>();
        for (String line : lore) {
            if (line == null) {
                continue;
            }
            replaced.add(line.replace("{duration}", String.valueOf(durationMinutes)));
        }
        return replaced;
    }
}
