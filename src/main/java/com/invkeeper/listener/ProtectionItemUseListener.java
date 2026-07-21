package com.invkeeper.listener;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.config.ProtectionItemConfig;
import com.invkeeper.protection.ProtectionManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ProtectionItemUseListener implements Listener {
    private final ProtectionManager protectionManager;
    private final ConfigManager configManager;

    public ProtectionItemUseListener(ProtectionManager protectionManager, ConfigManager configManager) {
        this.protectionManager = protectionManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        try {
            if (event.getHand() != EquipmentSlot.HAND) {
                return;
            }
            Action action = event.getAction();
            if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
                return;
            }
            ItemStack item = event.getItem();
            if (!protectionManager.isTimedItem(item)) {
                return;
            }

            event.setCancelled(true);
            Player player = event.getPlayer();
            if (player == null) return;
            
            if (protectionManager.getTimedProtectionStore().isActive(player)) {
                long remainingMillis = protectionManager.getTimedProtectionStore().getRemainingMillis(player);
                String formatted = MessageUtil.formatDuration(remainingMillis, configManager.getTimeFormat());
                MessageUtil.send(player, configManager.getTimedAlreadyActiveMessage().replace("{remaining}", formatted));
                return;
            }

            ProtectionItemConfig timedConfig = protectionManager.findMatchingTimedConfig(item);
            if (timedConfig == null) {
                return;
            }

            protectionManager.getTimedProtectionStore().activate(player, protectionManager.getTimedDurationSeconds(item));
            protectionManager.consumeTimedItemInMainHand(player);
            MessageUtil.send(player, configManager.getTimedActivatedMessage().replace("{duration}", String.valueOf(timedConfig.getDurationMinutes())));
        } catch (Exception e) {
            if (event.getPlayer() != null) {
                protectionManager.getPlugin().getLogger().warning("[InvKeeper] 보호 아이템 사용 중 오류: " + e.getMessage());
            }
        }
    }

}
