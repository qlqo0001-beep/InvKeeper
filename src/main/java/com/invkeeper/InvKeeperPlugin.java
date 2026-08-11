package com.invkeeper;

import com.invkeeper.command.InvKeeperCommand;
import com.invkeeper.config.ConfigManager;
import com.invkeeper.grave.GraveManager;
import com.invkeeper.grave.GraveTickManager;
import com.invkeeper.listener.*;
import com.invkeeper.protection.ProtectionAlertManager;
import com.invkeeper.protection.ProtectionManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class InvKeeperPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private ProtectionManager protectionManager;
    private com.invkeeper.soulbind.SoulbindManager soulbindManager;
    private ProtectionAlertManager protectionAlertManager;
    private GraveManager graveManager;
    private GraveTickManager graveTickManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("items.yml", false);
        // Save resource files including gui.yml
        saveResource("gui.yml", false);

        configManager = new ConfigManager(this);
        configManager.load();
        soulbindManager = new com.invkeeper.soulbind.SoulbindManager(this);
        protectionManager = new ProtectionManager(this, configManager, soulbindManager);

        // ── Grave system ──────────────────────────────────────
        graveManager = new GraveManager(this, configManager);
        if (graveManager.isEnabled()) {
            graveManager.loadAll();
            graveTickManager = new GraveTickManager(this, graveManager);
            graveTickManager.start();
            getLogger().info("무덤 시스템 활성화됨");
        } else {
            getLogger().info("무덤 시스템 비활성화 (config.grave.enabled = false)");
        }

        if (configManager.isForceKeepInventoryFalse()) {
            for (var world : Bukkit.getWorlds()) {
                world.setGameRule(GameRule.KEEP_INVENTORY, false);
            }
        }

        // Register cleanup listener for memory leak prevention
        getServer().getPluginManager().registerEvents(new CleanupListener(), this);

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(protectionManager, configManager, graveManager), this);
        getServer().getPluginManager().registerEvents(new ProtectionItemUseListener(protectionManager, configManager), this);
        getServer().getPluginManager().registerEvents(new WorldLoadListener(configManager), this);
        getServer().getPluginManager().registerEvents(new SoulbindPickupListener(protectionManager, configManager, this), this);
        getServer().getPluginManager().registerEvents(new SoulbindInventoryListener(protectionManager, configManager), this);
        getServer().getPluginManager().registerEvents(new SoulbindTransferListener(protectionManager, configManager), this);
        getServer().getPluginManager().registerEvents(new SoulbindUseListener(protectionManager, configManager), this);

        // ── Grave listeners ──────────────────────────────────
        getServer().getPluginManager().registerEvents(new GraveInteractListener(graveManager), this);
        getServer().getPluginManager().registerEvents(new GraveProtectionListener(graveManager), this);
        getServer().getPluginManager().registerEvents(new GraveGuiListener(graveManager), this);
        getServer().getPluginManager().registerEvents(new GraveAdminGuiListener(graveManager), this);

        // Create and store reference to ProtectionAlertManager
        protectionAlertManager = new ProtectionAlertManager(this, protectionManager, configManager);

        var command = getCommand("invkeeper");
        if (command != null) {
            InvKeeperCommand executor = new InvKeeperCommand(configManager, protectionManager, this, graveManager);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        boolean mmoInstalled = getServer().getPluginManager().getPlugin("MMOItems") != null;
        getLogger().info("MMOItems 설치 여부: " + (mmoInstalled ? "설치됨" : "미설치"));

        // Initialize bStats metrics
        int pluginId = 32889;
        org.bstats.bukkit.Metrics metrics = new org.bstats.bukkit.Metrics(this, pluginId);
        metrics.addCustomChart(new org.bstats.charts.SimplePie("mmoitems_installed", () -> String.valueOf(mmoInstalled)));
        getLogger().info("bStats 메트릭스 초기화 완료 (플러그인 ID: " + pluginId + ")");

        // Summary log
        getLogger().info("월드 규칙 " + configManager.getWorldRules().size() + "개, "
                + "권한 규칙 " + configManager.getPermissionRules().size() + "개, "
                + "보호 아이템 " + configManager.getProtectionItemConfigs().size() + "개 로드 완료");
        getLogger().info("Plugin Enabled");
    }

    @Override
    public void onDisable() {
        // Clean up all resources to prevent memory leaks
        getLogger().info("[InvKeeper] 플러그인 비활성화 중...");

        // Clear timed protection data
        if (protectionManager != null && protectionManager.getTimedProtectionStore() != null) {
            // TimedProtectionStore uses PDC, no explicit cleanup needed
        }

        // Clear alert manager states
        if (protectionAlertManager != null) protectionAlertManager.shutdown();

        // Shutdown grave system
        if (graveTickManager != null) graveTickManager.shutdown();
        if (graveManager != null) graveManager.shutdown();

        // Clear references
        if (configManager != null) {
            // ConfigManager doesn't hold player references
        }

        getLogger().info("[InvKeeper] 플러그인 비활성화 완료");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }
    public GraveManager getGraveManager() { return graveManager; }

    /**
     * Shuts down the current alert manager (if any) and creates a fresh one using the
     * latest config. This keeps a single owned instance instead of letting callers create
     * their own, which would orphan the previous scheduler task.
     */
    public void reloadAlertManager() {
        if (protectionAlertManager != null) {
            protectionAlertManager.shutdown();
        }
        protectionAlertManager = new ProtectionAlertManager(this, protectionManager, configManager);
    }

    /**
     * Internal listener for cleanup tasks (PlayerQuitEvent, etc.)
     */
    private class CleanupListener implements Listener {
        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            // Clear reminder states to prevent memory leak
            if (protectionAlertManager != null) {
                protectionAlertManager.clearPlayerReminders(player.getUniqueId());
            }
            // Note: TimedProtectionStore uses PDC (PersistentDataContainer) which persists across logouts.
            // We intentionally do NOT clear timed protection on logout so players retain their protection
            // when they log back in. The protection will expire naturally based on the stored timestamp.
        }
    }
}
