package com.invkeeper.grave;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.Plugin;

public class GraveTickManager {

    private final Plugin plugin;
    private final GraveManager graveManager;
    private final GraveHologramManager hologramManager;
    private BukkitTask task;

    public GraveTickManager(Plugin plugin, GraveManager graveManager) {
        this.plugin = plugin;
        this.graveManager = graveManager;
        this.hologramManager = graveManager.getHologramManager();
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        try {
            long now = System.currentTimeMillis();
            for (Grave grave : graveManager.getAllActive()) {
                if (grave.getExpireAt() != -1 && now >= grave.getExpireAt()) {
                    graveManager.expireGrave(grave);
                    continue;
                }
                hologramManager.updateAll(grave);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[InvKeeper] 틱 처리 오류: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (task != null) task.cancel();
    }
}
