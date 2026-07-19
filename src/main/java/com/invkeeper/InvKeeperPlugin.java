package com.invkeeper;

import com.invkeeper.command.InvKeeperCommand;
import com.invkeeper.config.ConfigManager;
import com.invkeeper.listener.PlayerDeathListener;
import com.invkeeper.listener.ProtectionItemUseListener;
import com.invkeeper.listener.WorldLoadListener;
import com.invkeeper.protection.ProtectionAlertManager;
import com.invkeeper.protection.ProtectionManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.plugin.java.JavaPlugin;

public class InvKeeperPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private ProtectionManager protectionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.load();
        protectionManager = new ProtectionManager(this, configManager);

        if (configManager.isForceKeepInventoryFalse()) {
            for (var world : Bukkit.getWorlds()) {
                world.setGameRule(GameRule.KEEP_INVENTORY, false);
            }
        }

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(protectionManager, configManager), this);
        getServer().getPluginManager().registerEvents(new ProtectionItemUseListener(protectionManager, configManager), this);
        getServer().getPluginManager().registerEvents(new WorldLoadListener(configManager), this);

        new ProtectionAlertManager(this, protectionManager, configManager);

        var command = getCommand("invkeeper");
        if (command != null) {
            InvKeeperCommand executor = new InvKeeperCommand(configManager, protectionManager);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        boolean mmoInstalled = getServer().getPluginManager().getPlugin("MMOItems") != null;
        getLogger().info("MMOItems 설치 여부: " + (mmoInstalled ? "설치됨" : "미설치"));
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }
}
