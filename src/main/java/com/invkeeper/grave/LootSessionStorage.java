package com.invkeeper.grave;

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

    public void save(LootSession session) {
        File f = new File(sessionsDir, session.getSessionId().toString() + ".yml");
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
        if (f.exists()) f.delete();
    }
}
