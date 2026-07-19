package com.invkeeper.listener;

import com.invkeeper.config.ConfigManager;
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
        if (protectionManager.getTimedProtectionStore().isActive(player)) {
            long remainingMillis = protectionManager.getTimedProtectionStore().getRemainingMillis(player);
            String formatted = formatDuration(remainingMillis);
            MessageUtil.send(player, configManager.getTimedAlreadyActiveMessage().replace("{remaining}", formatted));
            return;
        }

        protectionManager.getTimedProtectionStore().activate(player, protectionManager.getTimedDurationSeconds());
        protectionManager.consumeTimedItemInMainHand(player);
        MessageUtil.send(player, configManager.getTimedActivatedMessage().replace("{duration}", String.valueOf(configManager.getTimedDurationMinutes())));
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d분 %02d초", minutes, seconds);
    }
}
