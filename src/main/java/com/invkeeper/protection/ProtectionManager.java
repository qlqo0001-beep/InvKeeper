package com.invkeeper.protection;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.config.ProtectionItemConfig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

public class ProtectionManager {
    public enum ProtectionResult {
        NONE,
        CONSUMABLE,
        TIMED
    }

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final MMOItemsHook mmoItemsHook;
    private final VanillaProtectionItems vanillaProtectionItems;
    private final TimedProtectionStore timedProtectionStore;
    private final com.invkeeper.soulbind.SoulbindManager soulbindManager;

    public ProtectionManager(Plugin plugin, ConfigManager configManager, com.invkeeper.soulbind.SoulbindManager soulbindManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.mmoItemsHook = new MMOItemsHook(plugin);
        this.vanillaProtectionItems = new VanillaProtectionItems(plugin, configManager);
        this.timedProtectionStore = new TimedProtectionStore(plugin);
        this.soulbindManager = soulbindManager;
    }

    /**
     * Re-initialize the MMOItems hook. Call this on /invkeeper reload to pick up
     * late-loaded MMOItems plugin.
     */
    public void refreshMmoHook() {
        this.mmoItemsHook.refresh();
    }

    public ProtectionResult checkAndConsumeProtection(Player player) {
        if (timedProtectionStore.isActive(player)) {
            return ProtectionResult.TIMED;
        }
        return consumeConsumable(player) ? ProtectionResult.CONSUMABLE : ProtectionResult.NONE;
    }

    public boolean isTimedItem(ItemStack item) {
        return findMatchingTimedConfig(item) != null;
    }

    public boolean isConsumableItem(ItemStack item) {
        return findMatchingConsumableConfig(item) != null;
    }

    public boolean consumeConsumable(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot <= 40; slot++) {
            ItemStack item = inventory.getItem(slot);
            ProtectionItemConfig matched = findMatchingConsumableConfig(item);
            if (matched != null) {
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
        ProtectionItemConfig matched = findMatchingTimedConfig(item);
        if (matched == null) {
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

    public long getTimedDurationSeconds(ItemStack item) {
        ProtectionItemConfig matched = findMatchingTimedConfig(item);
        return matched == null ? 0L : (long) matched.getDurationMinutes() * 60L;
    }

    public ProtectionItemConfig findMatchingTimedConfig(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }

        String itemKey = vanillaProtectionItems.getItemKey(item);
        if (itemKey != null) {
            ProtectionItemConfig candidate = configManager.getProtectionItemConfig(itemKey);
            if (candidate != null && candidate.getKind() == ProtectionItemConfig.Kind.TIMED_PROTECTION && candidate.isUseVanilla()) {
                return candidate;
            }
        }

        if (!mmoItemsHook.isAvailable()) {
            return null;
        }

        for (ProtectionItemConfig candidate : configManager.getProtectionItemConfigs().values()) {
            if (candidate.getKind() != ProtectionItemConfig.Kind.TIMED_PROTECTION) continue;
            if (!candidate.isUseMmo()) continue;
            if (mmoItemsHook.matches(item, candidate.getMmoItemsType(), candidate.getMmoItemsId())) {
                return candidate;
            }
        }
        return null;
    }

    public ProtectionItemConfig findMatchingConsumableConfig(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }

        String itemKey = vanillaProtectionItems.getItemKey(item);
        if (itemKey != null) {
            ProtectionItemConfig candidate = configManager.getProtectionItemConfig(itemKey);
            if (candidate != null && candidate.getKind() == ProtectionItemConfig.Kind.CONSUMABLE_PROTECTION && candidate.isUseVanilla()) {
                return candidate;
            }
        }

        if (!mmoItemsHook.isAvailable()) {
            return null;
        }

        for (ProtectionItemConfig candidate : configManager.getProtectionItemConfigs().values()) {
            if (candidate.getKind() != ProtectionItemConfig.Kind.CONSUMABLE_PROTECTION) continue;
            if (!candidate.isUseMmo()) continue;
            if (mmoItemsHook.matches(item, candidate.getMmoItemsType(), candidate.getMmoItemsId())) {
                return candidate;
            }
        }
        return null;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MMOItemsHook getMmoItemsHook() {
        return mmoItemsHook;
    }

    public VanillaProtectionItems getVanillaProtectionItems() {
        return vanillaProtectionItems;
    }

    public TimedProtectionStore getTimedProtectionStore() {
        return timedProtectionStore;
    }

    public com.invkeeper.soulbind.SoulbindManager getSoulbindManager() {
        return soulbindManager;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Scan player's inventory and bind up to maxToBind item stacks that match the provided protection item config.
     */
    public int bindMatchingItemsInInventory(org.bukkit.entity.Player player, ProtectionItemConfig config, int maxToBind) {
        if (player == null || config == null || maxToBind <= 0) return 0;
        int bound = 0;
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        for (int slot = 0; slot <= 40 && bound < maxToBind; slot++) {
            org.bukkit.inventory.ItemStack item = inv.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            // Check vanilla first
            String key = vanillaProtectionItems.getItemKey(item);
            if (key != null && key.equals(config.getKey())) {
                if (!soulbindManager.isSoulbound(item) && config.isSoulbindEnabled()) {
                    long expiry = config.isSoulbindInfinite() ? -1L : (System.currentTimeMillis() + (long) config.getSoulbindDurationMinutes() * 60L * 1000L);
                    soulbindManager.applySoulbind(item, player.getUniqueId(), expiry);
                    inv.setItem(slot, item);
                    bound++;
                }
                continue;
            }
            // MMOItems matching
            if (config.isUseMmo() && mmoItemsHook.isAvailable()) {
                try {
                    if (mmoItemsHook.matches(item, config.getMmoItemsType(), config.getMmoItemsId())) {
                        if (!soulbindManager.isSoulbound(item) && config.isSoulbindEnabled()) {
                            long expiry = config.isSoulbindInfinite() ? -1L : (System.currentTimeMillis() + (long) config.getSoulbindDurationMinutes() * 60L * 1000L);
                            soulbindManager.applySoulbind(item, player.getUniqueId(), expiry);
                            inv.setItem(slot, item);
                            bound++;
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[InvKeeper] MMOItems 검사 중 오류: " + e.getMessage());
                }
            }
        }
        return bound;
    }
}