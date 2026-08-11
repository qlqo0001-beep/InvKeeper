package com.invkeeper.grave;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LootSessionManager {

    private final Plugin plugin;
    private final LootSessionStorage storage;
    private final GraveManager graveManager;
    private final Map<UUID, LootSession> sessions = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public LootSessionManager(Plugin plugin, LootSessionStorage storage, GraveManager graveManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.graveManager = graveManager;
    }

    public void loadFromDisk() {
        storage.loadAll().forEach(s -> sessions.put(s.getSessionId(), s));
    }

    public void startTicking() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public LootSession startSession(UUID graveId, UUID looterUuid, String looterName, long castTimeSeconds) {
        // already in progress?
        for (LootSession s : sessions.values()) {
            if (s.getGraveId().equals(graveId)) return null;
        }
        UUID sid = UUID.randomUUID();
        long now = System.currentTimeMillis();
        LootSession session = new LootSession(sid, graveId, looterUuid, looterName, now, now + castTimeSeconds * 1000L);
        sessions.put(sid, session);
        storage.save(session);
        Grave grave = graveManager.getByGraveId(graveId);
        if (grave != null) {
            grave.setState(GraveState.BEING_LOOTED);
            grave.setActiveLootSessionId(sid);
            graveManager.saveGrave(grave);
        }
        return session;
    }

    public void cancelSession(UUID graveId) {
        Grave grave = graveManager.getByGraveId(graveId);
        UUID sid = grave != null ? grave.getActiveLootSessionId() : null;
        if (sid == null) { // 재시작 후 세션ID가 복원 안 된 경우에도 검색으로 취소
            LootSession s = getSessionForGrave(graveId);
            if (s != null) sid = s.getSessionId();
        }
        if (sid != null) {
            sessions.remove(sid);
            storage.delete(sid);
            if (grave != null && grave.getState() == GraveState.BEING_LOOTED) {
                grave.setState(GraveState.ACTIVE);
                grave.setActiveLootSessionId(null);
                graveManager.saveGrave(grave);
            }
        }
    }

    private void tick() {
        List<UUID> toRemove = new ArrayList<>();
        for (LootSession s : sessions.values()) {
            Grave grave = graveManager.getActiveGrave(s.getGraveId());
            if (grave == null || grave.getState() != GraveState.BEING_LOOTED) {
                toRemove.add(s.getSessionId()); // 유령 세션 정리
                continue;
            }
            if (s.isCompleted()) toRemove.add(s.getSessionId());
        }
        for (UUID sid : toRemove) {
            LootSession s = sessions.remove(sid);
            storage.delete(sid);
            Grave grave = graveManager.getActiveGrave(s.getGraveId());
            if (grave != null && grave.getState() == GraveState.BEING_LOOTED) {
                grave.setState(GraveState.LOOTED);
                grave.setLooter(s.getLooterUuid(), s.getLooterName());
                grave.setActiveLootSessionId(null);
                graveManager.saveGrave(grave);
                plugin.getLogger().info("[InvKeeper] loot complete: " + s.getGraveId());
            }
        }
    }

    public LootSession getSession(UUID sessionId) { return sessions.get(sessionId); }
    public LootSession getSessionForGrave(UUID graveId) {
        for (LootSession s : sessions.values()) {
            if (s.getGraveId().equals(graveId)) return s;
        }
        return null;
    }
    public void shutdown() { if (tickTask != null) tickTask.cancel(); }
}
