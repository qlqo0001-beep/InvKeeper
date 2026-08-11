package com.invkeeper.listener;

import com.invkeeper.grave.*;
import com.invkeeper.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GraveInteractListener implements Listener {

    private final GraveManager graveManager;

    public GraveInteractListener(GraveManager graveManager) {
        this.graveManager = graveManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Grave grave = graveManager.getByLocation(event.getClickedBlock().getLocation());
        if (grave == null) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        boolean isOwner = playerId.equals(grave.getOwnerUuid());
        // 주인이 bypass 권한(op 등)을 가져도 우선: 주인 열기 = 도굴 취소
        boolean isBypass = !isOwner && player.hasPermission("invkeeper.grave.bypass");

        // Owner opens — cancels loot if in progress
        if (isOwner) {
            if (grave.getState() == GraveState.BEING_LOOTED) {
                LootSession session = graveManager.getLootSessionManager().getSessionForGrave(grave.getGraveId());
                if (session != null) {
                    Player looter = org.bukkit.Bukkit.getPlayer(session.getLooterUuid());
                    if (looter != null) {
                        MessageUtil.send(looter, graveManager.getConfigManager().getGraveLootCancelledMessage()
                            .replace("{owner}", grave.getOwnerName()));
                    }
                }
                graveManager.getLootSessionManager().cancelSession(grave.getGraveId());
            }
            new GraveInventoryView(graveManager).open(player, grave);
            return;
        }

        // Bypass: admin opens silently
        if (isBypass) {
            new GraveInventoryView(graveManager).open(player, grave);
            return;
        }

        // LOOTED state: anyone can open
        if (grave.getState() == GraveState.LOOTED) {
            new GraveInventoryView(graveManager).open(player, grave);
            return;
        }

        // Not owner, not LOOTED: try loot item
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && !hand.getType().isAir()) {
            GraveLootItemUseListener.handleLootItemUse(player, grave, hand, graveManager);
            return;
        }

        MessageUtil.send(player, graveManager.getConfigManager().getGraveNotOwnerMessage()
            .replace("{owner}", grave.getOwnerName()));
    }
}
