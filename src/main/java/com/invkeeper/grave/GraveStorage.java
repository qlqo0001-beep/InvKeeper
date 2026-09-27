package com.invkeeper.grave;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GraveStorage {

    private final Plugin plugin;
    private final File gravesDir;

    public GraveStorage(Plugin plugin) {
        this.plugin = plugin;
        this.gravesDir = new File(plugin.getDataFolder(), "graves");
        if (!gravesDir.exists()) gravesDir.mkdirs();
    }

    /**
     * 무덤을 저장합니다. 디스크 쓰기(느린 I/O)는 메인 스레드를 막지 않도록 비동기로 수행됩니다.
     * (사망/무덤 아이템 회수 등 게임플레이 도중 매우 자주 호출되어, 동기 쓰기 시 TPS/MSPT가 흔들리는 원인이었음)
     */
    public void save(Grave grave) {
        persist(grave, true);
    }

    /**
     * 동기(블로킹) 저장. 플러그인 종료(onDisable) 시에만 사용합니다.
     * 서버 프로세스가 곧바로 종료될 수 있어, 비동기 쓰기가 완료되기 전에 데이터가 유실될 위험이 있기 때문입니다.
     */
    public void saveSync(Grave grave) {
        persist(grave, false);
    }

    private void persist(Grave grave, boolean async) {
        UUID graveId = grave.getGraveId();
        File file = new File(gravesDir, graveId.toString() + ".yml");
        YamlConfiguration yaml = buildYaml(grave);

        if (!async) {
            try {
                yaml.save(file);
            } catch (Exception e) {
                plugin.getLogger().severe("[InvKeeper] 무덤 저장 실패 " + graveId + ": " + e.getMessage());
            }
            return;
        }

        String text;
        try {
            text = yaml.saveToString();
        } catch (Exception e) {
            plugin.getLogger().severe("[InvKeeper] 무덤 저장 실패 " + graveId + ": " + e.getMessage());
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeText(file, text, graveId));
    }

    private YamlConfiguration buildYaml(Grave grave) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("graveId", grave.getGraveId().toString());
        yaml.set("ownerUuid", grave.getOwnerUuid().toString());
        yaml.set("ownerName", grave.getOwnerName());
        yaml.set("worldName", grave.getWorldName());
        yaml.set("x", grave.getX());
        yaml.set("y", grave.getY());
        yaml.set("z", grave.getZ());
        yaml.set("createdAt", grave.getCreatedAt());
        yaml.set("expireAt", grave.getExpireAt());
        yaml.set("state", grave.getState().name());
        yaml.set("blockType", grave.getBlockType());
        if (grave.getOriginalBlockData() != null) {
            yaml.set("originalBlockData", grave.getOriginalBlockData().getAsString());
        }
        if (grave.getLooterUuid() != null) {
            yaml.set("looterUuid", grave.getLooterUuid().toString());
            yaml.set("looterName", grave.getLooterName());
        }
        if (grave.getActiveLootSessionId() != null) {
            yaml.set("activeLootSessionId", grave.getActiveLootSessionId().toString());
        }
        if (grave.getLockedUntil() > 0 || grave.getLockExtraCastSeconds() > 0) {
            yaml.set("lockedUntil", grave.getLockedUntil());
            yaml.set("lockExtraCastSeconds", grave.getLockExtraCastSeconds());
        }
        yaml.set("recoveredAt", grave.getRecoveredAt());
        yaml.set("recoveredBy", grave.getRecoveredBy().name());

        writeContents(yaml, "contents", grave.getContents());
        if (grave.getOriginalContents() != null) {
            writeContents(yaml, "original-contents", grave.getOriginalContents());
        }
        return yaml;
    }

    private void writeText(File file, String text, UUID graveId) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            w.write(text);
        } catch (IOException e) {
            plugin.getLogger().severe("[InvKeeper] 무덤 저장 실패 " + graveId + ": " + e.getMessage());
        }
    }

    private static void writeContents(YamlConfiguration y, String path, GraveContents c) {
        ItemStack[] eq = c.getEquipment();
        y.set(path + ".equipment.helmet", encode(eq[3]));
        y.set(path + ".equipment.chestplate", encode(eq[2]));
        y.set(path + ".equipment.leggings", encode(eq[1]));
        y.set(path + ".equipment.boots", encode(eq[0]));
        y.set(path + ".offhand", encode(c.getOffhand()));
        for (int i = 0; i < 36; i++) {
            ItemStack item = c.getInventory()[i];
            if (item != null && !item.getType().isAir()) {
                y.set(path + ".inventory." + i, encode(item));
            }
        }
        y.set(path + ".totalExp", c.getTotalExp());
    }

    public List<Grave> loadAll() {
        List<Grave> list = new ArrayList<>();
        File[] files = gravesDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return list;
        for (File f : files) {
            try {
                Grave g = loadOne(f);
                if (g != null) list.add(g);
            } catch (Exception e) {
                plugin.getLogger().severe("[InvKeeper] 무덤 로드 실패 " + f.getName() + ": " + e.getMessage());
            }
        }
        return list;
    }

    public void delete(UUID graveId) {
        File f = new File(gravesDir, graveId.toString() + ".yml");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> { if (f.exists()) f.delete(); });
    }
    private Grave loadOne(File f) {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        UUID id = UUID.fromString(y.getString("graveId"));
        UUID ou = UUID.fromString(y.getString("ownerUuid"));
        String on = y.getString("ownerName", "?");
        String wn = y.getString("worldName", "world");
        int x = y.getInt("x"), y0 = y.getInt("y"), z = y.getInt("z");
        long ca = y.getLong("createdAt"), ea = y.getLong("expireAt", -1);
        GraveState st = GraveState.valueOf(y.getString("state", "ACTIVE"));
        String bt = y.getString("blockType", "VANILLA:BARREL");
        BlockData obd = null;
        try { if (y.contains("originalBlockData")) obd = Bukkit.createBlockData(y.getString("originalBlockData")); } catch (Exception ignored) {}

        GraveContents gc = readContents(y, "contents");
        Grave g = new Grave(id, ou, on, wn, x, y0, z, ca, ea, bt, obd, gc);
        g.setState(st);
        if (y.contains("original-contents")) g.setOriginalContents(readContents(y, "original-contents"));
        String lu = y.getString("looterUuid");
        if (lu != null && !lu.isEmpty()) g.setLooter(UUID.fromString(lu), y.getString("looterName", "?"));
        String sid = y.getString("activeLootSessionId");
        if (sid != null && !sid.isEmpty()) g.setActiveLootSessionId(UUID.fromString(sid));
        g.setLock(y.getLong("lockedUntil", 0), y.getInt("lockExtraCastSeconds", 0));
        return g;
    }

    private static GraveContents readContents(YamlConfiguration y, String path) {
        ItemStack[] eq = new ItemStack[4];
        eq[3] = decode(y.getString(path + ".equipment.helmet"));
        eq[2] = decode(y.getString(path + ".equipment.chestplate"));
        eq[1] = decode(y.getString(path + ".equipment.leggings"));
        eq[0] = decode(y.getString(path + ".equipment.boots"));
        ItemStack oh = decode(y.getString(path + ".offhand"));
        ItemStack[] inv = new ItemStack[36];
        if (y.contains(path + ".inventory")) {
            for (String k : y.getConfigurationSection(path + ".inventory").getKeys(false)) {
                try { int s = Integer.parseInt(k); if (s >= 0 && s < 36) inv[s] = decode(y.getString(path + ".inventory." + k)); }
                catch (NumberFormatException ignored) {}
            }
        }
        int te = y.getInt(path + ".totalExp", 0);
        return new GraveContents(eq, oh, inv, te);
    }

    public static String encode(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
            boos.writeObject(item);
            boos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public static ItemStack decode(String b64) {
        if (b64 == null || b64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(b64);
            return (ItemStack) new BukkitObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
        } catch (Exception e) {
            return null;
        }
    }
}
