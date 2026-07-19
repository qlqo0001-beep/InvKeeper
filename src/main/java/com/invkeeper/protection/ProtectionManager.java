package com.invkeeper.protection;

import com.invkeeper.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

public class ProtectionManager {
    private final ConfigManager configManager;
    private final MMOItemsHook mmoItemsHook;
    private final VanillaProtectionItems vanillaProtectionItems;
    private final TimedProtectionStore timedProtectionStore;

    public ProtectionManager(Plugin plugin, ConfigManager configManager) {
        this.configManager = configManager;
        this.mmoItemsHook = new MMOItemsHook(plugin);
        this.vanillaProtectionItems = new VanillaProtectionItems(plugin, configManager);
        this.timedProtectionStore = new TimedProtectionStore(plugin);
    }

    public boolean checkAndConsumeProtection(Player player) {
        if (timedProtectionStore.isActive(player)) {
            return true;
        }
        return consumeConsumable(player);
    }

    public boolean isTimedItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (configManager.isUseVanillaTimed() && vanillaProtectionItems.isKind(item, VanillaProtectionItems.Kind.TIMED)) {
            return true;
        }
        if (configManager.isUseMmoTimed() && mmoItemsHook.isAvailable()) {
            return mmoItemsHook.matches(item, configManager.getMmoTimedType(), configManager.getMmoTimedId());
        }
        return false;
    }

    public boolean isConsumableItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (configManager.isUseVanillaConsumable() && vanillaProtectionItems.isKind(item, VanillaProtectionItems.Kind.CONSUMABLE)) {
            return true;
        }
        if (configManager.isUseMmoConsumable() && mmoItemsHook.isAvailable()) {
            return mmoItemsHook.matches(item, configManager.getMmoConsumableType(), configManager.getMmoConsumableId());
        }
        return false;
    }

    public boolean consumeConsumable(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot <= 40; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (isConsumableItem(item)) {
                ItemStack clone = item.clone();
                int amount = clone.getAmount() - 1;
                if (amount <= 0) {
                    inventory.setItem(slot, null);
                } else {
                    clone.setAmount(amount);
                    inventory.setItem(slot, clone);
                }
                return true;
            }
        }
        return false;
    }

    public void consumeTimedItemInMainHand(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack item = inventory.getItemInMainHand();
        if (!isTimedItem(item)) {
            return;
        }
        ItemStack clone = item.clone();
        int amount = clone.getAmount() - 1;
        if (amount <= 0) {
            inventory.setItemInMainHand(null);
        } else {
            clone.setAmount(amount);
            inventory.setItemInMainHand(clone);
        }
    }

    public long getTimedDurationSeconds() {
        return (long) configManager.getTimedDurationMinutes() * 60L;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public VanillaProtectionItems getVanillaProtectionItems() {
        return vanillaProtectionItems;
    }

    public TimedProtectionStore getTimedProtectionStore() {
        return timedProtectionStore;
    }
}
