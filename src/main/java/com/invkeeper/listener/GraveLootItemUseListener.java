package com.invkeeper.listener;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.config.ProtectionItemConfig;
import com.invkeeper.grave.*;
import com.invkeeper.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GraveLootItemUseListener {

    public static boolean handleLootItemUse(Player player, Grave grave, ItemStack hand, GraveManager graveManager) {
        ConfigManager cfg = graveManager.getConfigManager();

        // Self-loot block
        if (player.getUniqueId().equals(grave.getOwnerUuid())) {
            MessageUtil.send(player, cfg.getGraveLootSelfBlockedMessage());
            return true;
        }

        // Already being looted
        if (grave.getState() == GraveState.BEING_LOOTED) {
            MessageUtil.send(player, cfg.getGraveLootAlreadyInProgressMessage());
            return true;
        }

        // Already looted
        if (grave.getState() == GraveState.LOOTED) {
            new GraveInventoryView(graveManager).open(player, grave);
            return true;
        }

        // Check if hand is a grave loot tool
        ProtectionItemConfig itemCfg = findLootTool(player, cfg);
        if (itemCfg == null || itemCfg.getKind() != ProtectionItemConfig.Kind.GRAVE_LOOT_TOOL) {
            MessageUtil.send(player, cfg.getGraveLootItemRequiredMessage());
            return true;
        }

        int castSeconds = itemCfg.getCastTimeSeconds();
        if (castSeconds <= 0) castSeconds = 300;

        // Consume item
        hand.setAmount(hand.getAmount() - 1);
        if (hand.getAmount() <= 0) {
            player.getInventory().setItemInMainHand(null);
        }

        // Start session
        LootSession session = graveManager.getLootSessionManager().startSession(
            grave.getGraveId(), player.getUniqueId(), player.getName(), castSeconds);

        if (session == null) {
            MessageUtil.send(player, cfg.getGraveLootAlreadyInProgressMessage());
            return true;
        }

        MessageUtil.send(player, cfg.getGraveLootStartCasterMessage()
            .replace("{seconds}", String.valueOf(castSeconds)));

        Player owner = org.bukkit.Bukkit.getPlayer(grave.getOwnerUuid());
        if (owner != null) {
            MessageUtil.send(owner, cfg.getGraveLootStartOwnerAlertMessage());
        }

        return true;
    }

    private static ProtectionItemConfig findLootTool(Player player, ConfigManager cfg) {
        for (ProtectionItemConfig pic : cfg.getProtectionItemConfigs().values()) {
            if (pic.getKind() != ProtectionItemConfig.Kind.GRAVE_LOOT_TOOL) continue;
            if (pic.isUseVanilla()) {
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item == null || item.getType().isAir()) continue;
                    if (item.getType() == pic.getVanillaMaterial()) {
                        // Check PDC
                        var key = new org.bukkit.NamespacedKey("invkeeper", "item_kind");
                        var meta = item.getItemMeta();
                        if (meta != null && meta.getPersistentDataContainer().has(key)) {
                            String kind = meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
                            if ("GRAVE_LOOT_TOOL".equals(kind)) return pic;
                        }
                        // Fallback: material match only
                        return pic;
                    }
                }
            }
        }
        return null;
    }
}
