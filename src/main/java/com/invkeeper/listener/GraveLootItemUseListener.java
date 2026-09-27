package com.invkeeper.listener;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.config.ProtectionItemConfig;
import com.invkeeper.grave.*;
import com.invkeeper.protection.ProtectionManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GraveLootItemUseListener {

    public static boolean handleLootItemUse(Player player, Grave grave, ItemStack hand, GraveManager graveManager,
                                            ProtectionManager protectionManager) {
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

        // Locked by a grave lock: looting can't start yet (tool is not consumed)
        if (grave.isLocked()) {
            MessageUtil.send(player, cfg.getGraveLootLockedMessage()
                .replace("{remaining}", MessageUtil.formatDuration(grave.getLockRemainingMillis(), cfg.getTimeFormat())));
            return true;
        }

        // The item in the main hand itself must be a grave loot tool (it is the one consumed below)
        ProtectionItemConfig itemCfg = protectionManager.findMatchingConfig(hand, ProtectionItemConfig.Kind.GRAVE_LOOT_TOOL);
        if (itemCfg == null) {
            MessageUtil.send(player, cfg.getGraveLootItemRequiredMessage());
            return true;
        }

        int castSeconds = itemCfg.getCastTimeSeconds();
        if (castSeconds <= 0) castSeconds = 300;
        castSeconds += grave.getLockExtraCastSeconds();

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
}
