package com.invkeeper.listener;

import com.invkeeper.config.ConfigManager;
import org.bukkit.GameRule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldLoadListener implements Listener {
    private final ConfigManager configManager;

    public WorldLoadListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (!configManager.isForceKeepInventoryFalse()) {
            return;
        }
        event.getWorld().setGameRule(GameRule.KEEP_INVENTORY, false);
    }
}
