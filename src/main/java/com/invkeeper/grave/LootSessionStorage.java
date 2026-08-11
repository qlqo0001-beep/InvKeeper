package com.invkeeper.grave;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.*;

public class LootSessionStorage {

    private final Plugin plugin;
    private final File sessionsDir;

    public LootSessionStorage(Plugin plugin) {
        this.plugin = plugin;
        this.sessionsDir = new File(plugin.getDataFolder(), "graves" + File.separator + "sessions");
        if (!sessionsDir.exists()) sessionsDir.mkdirs();
    }

    /**
     * LootSession은 완전 불변(UUID/String/long)이라 다른 스레드에서 안전하게 다룰 수 있어,
     * 조립부터 디스크 쓰기까지 전부 비동기로 넘겨 메인 스레드 블로킹을 피합니다.
     * (도굴 시작/취소가 잦은 서버에서 무덤을 열 때마다 발생하는 렉의 원인 중 하나였음)
     */
    public void save(LootSession session) {
        File f = new File(sessionsDir, session.getSessionId().toString() + ".yml");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            YamlConfiguration y = new YamlConfiguration();
            y.set("sessionId", session.getSessionId().toString());
            y.set("graveId", session.getGraveId().toString());
            y.set("looterUuid", session.getLooterUuid().toString());
            y.set("looterName", session.getLooterName());
            y.set("startedAt", session.getStartedAt());
            y.set("endsAt", session.getEndsAt());
            try { y.save(f); } catch (IOException e) {
                plugin.getLogger().warning("[InvKeeper] session save: " + e.getMessage());
            }
        });
    }

    public List<LootSession> loadAll() {
        List<LootSession> list = new ArrayList<>();
        File[] files = sessionsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return list;
        for (File f : files) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            list.add(new LootSession(
                UUID.fromString(y.getString("sessionId")),
                UUID.fromString(y.getString("graveId")),
                UUID.fromString(y.getString("looterUuid")),
                y.getString("looterName", "?"),
                y.getLong("startedAt"),
                y.getLong("endsAt")
            ));
        }
        return list;
    }

    public void delete(UUID sessionId) {
        File f = new File(sessionsDir, sessionId.toString() + ".yml");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> { if (f.exists()) f.delete(); });
    }
}
