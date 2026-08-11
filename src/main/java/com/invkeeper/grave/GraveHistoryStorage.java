package com.invkeeper.grave;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.*;

public class GraveHistoryStorage {

    private final Plugin plugin;
    private final File historyDir;
    private final int retentionDays;
    private final int maxEntriesPerPlayer;

    public GraveHistoryStorage(Plugin plugin, int retentionDays, int maxEntriesPerPlayer) {
        this.plugin = plugin;
        this.retentionDays = retentionDays;
        this.maxEntriesPerPlayer = maxEntriesPerPlayer;
        this.historyDir = new File(plugin.getDataFolder(), "graves" + File.separator + "history");
        if (!historyDir.exists()) historyDir.mkdirs();
    }

    public void archive(Grave grave) {
        File file = new File(historyDir, grave.getGraveId().toString() + ".yml");
        YamlConfiguration y = new YamlConfiguration();
        y.set("graveId", grave.getGraveId().toString());
        y.set("ownerUuid", grave.getOwnerUuid().toString());
        y.set("ownerName", grave.getOwnerName());
        y.set("worldName", grave.getWorldName());
        y.set("x", grave.getX()); y.set("y", grave.getY()); y.set("z", grave.getZ());
        y.set("createdAt", grave.getCreatedAt());
        y.set("expireAt", grave.getExpireAt());
        y.set("state", grave.getState().name());
        GraveContents snap = grave.getOriginalContents() != null ? grave.getOriginalContents() : grave.getContents();
        writeContents(y, "contents", snap);
        y.set("itemCount", snap.countItems());
        y.set("totalExp", snap.getTotalExp());
        y.set("recoveredAt", grave.getRecoveredAt());
        y.set("recoveredBy", grave.getRecoveredBy().name());
        if (grave.getLooterUuid() != null) {
            y.set("looterUuid", grave.getLooterUuid().toString());
            y.set("looterName", grave.getLooterName());
        }
        y.set("archivedAt", System.currentTimeMillis());
        try {
            y.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("[InvKeeper] 히스토리 저장 실패 " + grave.getGraveId() + ": " + e.getMessage());
        }
        prune(grave.getOwnerUuid());
    }

    /**
     * 히스토리 엔트리의 컨텐츠만 갱신 (어드민이 히스토리 뷰에서 아이템 회수 시).
     */
    public void saveEntryContents(Grave g) {
        File file = new File(historyDir, g.getGraveId().toString() + ".yml");
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        writeContents(y, "contents", g.getContents());
        y.set("itemCount", g.getContents().countItems());
        y.set("totalExp", g.getContents().getTotalExp());
        try {
            y.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("[InvKeeper] 히스토리 컨텐츠 갱신 실패 " + g.getGraveId() + ": " + e.getMessage());
        }
    }

    public void delete(UUID graveId) {
        File f = new File(historyDir, graveId.toString() + ".yml");
        if (f.exists()) f.delete();
    }

    private static void writeContents(YamlConfiguration y, String path, GraveContents c) {
        ItemStack[] eq = c.getEquipment();
        y.set(path + ".equipment.helmet", GraveStorage.encode(eq[3]));
        y.set(path + ".equipment.chestplate", GraveStorage.encode(eq[2]));
        y.set(path + ".equipment.leggings", GraveStorage.encode(eq[1]));
        y.set(path + ".equipment.boots", GraveStorage.encode(eq[0]));
        y.set(path + ".offhand", GraveStorage.encode(c.getOffhand()));
        for (int i = 0; i < 36; i++) {
            ItemStack item = c.getInventory()[i];
            if (item != null && !item.getType().isAir()) {
                y.set(path + ".inventory." + i, GraveStorage.encode(item));
            }
        }
        y.set(path + ".totalExp", c.getTotalExp());
    }

    private static GraveContents readContents(YamlConfiguration y, String path) {
        ItemStack[] eq = new ItemStack[4];
        eq[3] = GraveStorage.decode(y.getString(path + ".equipment.helmet"));
        eq[2] = GraveStorage.decode(y.getString(path + ".equipment.chestplate"));
        eq[1] = GraveStorage.decode(y.getString(path + ".equipment.leggings"));
        eq[0] = GraveStorage.decode(y.getString(path + ".equipment.boots"));
        ItemStack oh = GraveStorage.decode(y.getString(path + ".offhand"));
        ItemStack[] inv = new ItemStack[36];
        if (y.contains(path + ".inventory")) {
            for (String k : y.getConfigurationSection(path + ".inventory").getKeys(false)) {
                try { int s = Integer.parseInt(k); if (s >= 0 && s < 36) inv[s] = GraveStorage.decode(y.getString(path + ".inventory." + k)); }
                catch (NumberFormatException ignored) {}
            }
        }
        int te = y.getInt(path + ".totalExp", 0);
        return new GraveContents(eq, oh, inv, te);
    }
    public List<Entry> loadPlayer(UUID ownerUuid) {
        List<Entry> list = new ArrayList<>();
        File[] files = historyDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return list;
        String uid = ownerUuid.toString();
        for (File f : files) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            if (!uid.equals(y.getString("ownerUuid"))) continue;
            list.add(buildEntry(y, ownerUuid));
        }
        list.sort((a, b) -> Long.compare(b.archivedAt, a.archivedAt));
        return list;
    }

    private List<Entry> loadAllPlayer(UUID ownerUuid) {
        List<Entry> list = new ArrayList<>();
        File[] files = historyDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return list;
        String uid = ownerUuid.toString();
        for (File f : files) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            if (!uid.equals(y.getString("ownerUuid"))) continue;
            list.add(buildEntry(y, ownerUuid));
        }
        return list;
    }

    private Entry buildEntry(YamlConfiguration y, UUID ownerUuid) {
        return new Entry(
            UUID.fromString(y.getString("graveId")), ownerUuid,
            y.getString("ownerName", "?"), y.getString("worldName", "world"),
            y.getInt("x"), y.getInt("y"), y.getInt("z"),
            y.getLong("createdAt"), y.getLong("expireAt"),
            y.getString("state", "ACTIVE"),
            y.getInt("itemCount"), y.getInt("totalExp"),
            y.getLong("recoveredAt"), y.getString("recoveredBy", "NONE"),
            y.getString("looterName"), y.getLong("archivedAt", 0),
            y.contains("contents") ? readContents(y, "contents") : null
        );
    }

    private void prune(UUID ownerUuid) {
        String uid = ownerUuid.toString();
        File[] files = historyDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        long cutoff = retentionDays > 0 ? System.currentTimeMillis() - (long) retentionDays * 86400000L : 0;
        for (File f : files) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            if (!uid.equals(y.getString("ownerUuid"))) continue;
            if (retentionDays > 0 && y.getLong("archivedAt", 0) < cutoff) { f.delete(); continue; }
        }
        if (maxEntriesPerPlayer > 0) {
            List<Entry> all = loadAllPlayer(ownerUuid);
            if (all.size() > maxEntriesPerPlayer) {
                for (int i = maxEntriesPerPlayer; i < all.size(); i++) {
                    new File(historyDir, all.get(i).graveId.toString() + ".yml").delete();
                }
            }
        }
    }

    public static final class Entry {
        public final UUID graveId, ownerUuid;
        public final String ownerName, worldName;
        public final int x, y, z;
        public final long createdAt, expireAt;
        public final String state;
        public final int itemCount, totalExp;
        public final long recoveredAt;
        public final String recoveredBy, looterName;
        public final long archivedAt;
        public final GraveContents contents;

        public Entry(UUID graveId, UUID ownerUuid, String ownerName, String worldName,
                     int x, int y, int z, long createdAt, long expireAt,
                     String state, int itemCount, int totalExp,
                     long recoveredAt, String recoveredBy, String looterName,
                     long archivedAt, GraveContents contents) {
            this.graveId = graveId; this.ownerUuid = ownerUuid;
            this.ownerName = ownerName; this.worldName = worldName;
            this.x = x; this.y = y; this.z = z;
            this.createdAt = createdAt; this.expireAt = expireAt;
            this.state = state; this.itemCount = itemCount; this.totalExp = totalExp;
            this.recoveredAt = recoveredAt; this.recoveredBy = recoveredBy;
            this.looterName = looterName; this.archivedAt = archivedAt;
            this.contents = contents;
        }
    }
}
