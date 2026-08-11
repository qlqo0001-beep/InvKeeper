package com.invkeeper.grave;

import com.invkeeper.config.*;
import com.invkeeper.grave.block.GraveBlockProvider;
import com.invkeeper.grave.block.VanillaBlockProvider;
import com.invkeeper.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GraveManager {

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final GraveStorage storage;
    private final GraveHistoryStorage historyStorage;
    private final GraveHologramManager hologramManager;
    private final LootSessionManager lootSessionManager;
    private final GraveBlockProvider blockProvider;

    private final Map<UUID, Grave> byGraveId = new ConcurrentHashMap<>();
    private final Map<String, Grave> byLocation = new ConcurrentHashMap<>();
    // 어드민이 히스토리에서 열어본 "원상태" 뷰 (byGraveId에는 등록되지 않음)
    private final Map<UUID, Grave> historyViews = new ConcurrentHashMap<>();

    private boolean enabled;
    private GraveContainerConfig containerConfig;
    private String titleFormat;
    private String deathTimeFormat;
    private int maxGravesPerPlayer;
    private boolean expireEnabled;
    private int defaultExpireSeconds;
    private List<GraveExpireRule> expireRules;
    private List<String> disabledWorlds;

    public GraveManager(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storage = new GraveStorage(plugin);
        this.blockProvider = new VanillaBlockProvider();
        this.hologramManager = new GraveHologramManager(plugin, configManager.getGraveHologramConfig());
        int rd = configManager.getGraveHistoryRetentionDays();
        int me = configManager.getGraveHistoryMaxEntries();
        this.historyStorage = new GraveHistoryStorage(plugin, rd, me);
        this.lootSessionManager = new LootSessionManager(plugin, new LootSessionStorage(plugin), this);
        this.hologramManager.setSessionLookup(lootSessionManager::getSession);
        reloadConfig();
    }

    public void reloadConfig() {
        this.enabled = configManager.isGraveEnabled();
        this.containerConfig = configManager.getGraveContainerConfig();
        this.titleFormat = configManager.getGraveTitleFormat();
        this.deathTimeFormat = configManager.getGraveDeathTimeFormat();
        this.maxGravesPerPlayer = configManager.getGraveMaxPerPlayer();
        this.expireEnabled = configManager.isGraveExpireEnabled();
        this.defaultExpireSeconds = configManager.getGraveExpireDefaultSeconds();
        this.expireRules = configManager.getGraveExpireRules();
        this.disabledWorlds = configManager.getGraveDisabledWorlds();
    }
    public void loadAll() {
        byGraveId.clear();
        byLocation.clear();
        int loaded = 0;
        for (Grave g : storage.loadAll()) {
            try {
                if (g.isExpired()) {
                    historyStorage.archive(g);
                    storage.delete(g.getGraveId());
                    removeBlock(g);
                } else {
                    byGraveId.put(g.getGraveId(), g);
                    byLocation.put(locKey(g), g);
                    hologramManager.spawn(g);
                    loaded++;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("[InvKeeper] 무덤 로드 처리 오류 " + g.getGraveId() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("[InvKeeper] 무덤 " + loaded + "개 로드됨");
        lootSessionManager.loadFromDisk();
        lootSessionManager.startTicking();
    }

    public Grave createGrave(Player player, Location deathLoc, ItemStack[] eq, ItemStack oh,
                             ItemStack[] inv, int totalExp) {
        if (!enabled) return null;
        if (disabledWorlds.contains(deathLoc.getWorld().getName().toLowerCase())) return null;
        boolean has = false;
        for (ItemStack it : eq) if (it != null && !it.getType().isAir()) has = true;
        if (oh != null && !oh.getType().isAir()) has = true;
        for (ItemStack it : inv) if (it != null && !it.getType().isAir()) has = true;
        if (!has && totalExp <= 0) return null;
        long expireAt = -1;
        if (expireEnabled) {
            int sec = defaultExpireSeconds;
            GraveExpireRule best = ConfigManager.resolveBestRule(player, expireRules);
            if (best != null) sec = best.getSeconds();   // -1(무제한) 포함 반영
            if (sec > 0) expireAt = System.currentTimeMillis() + sec * 1000L;
            else expireAt = -1; // 0 또는 -1 = 무제한
        }
        UUID ou = player.getUniqueId();
        List<Grave> pg = getGravesByOwner(ou);
        if (maxGravesPerPlayer > 0 && pg.size() >= maxGravesPerPlayer) {
            Grave old = pg.get(0);
            for (Grave g : pg) if (g.getCreatedAt() < old.getCreatedAt()) old = g;
            removeGrave(old, false);
            String mm = configManager.getGraveMaxReachedMessage();
            if (mm != null) MessageUtil.send(player, mm.replace("{max}", String.valueOf(maxGravesPerPlayer)));
        }
        String bt = "VANILLA:" + containerConfig.getVanillaMaterial();
        org.bukkit.block.data.BlockData orig = deathLoc.getBlock().getBlockData().clone();
        UUID gid = UUID.randomUUID();
        GraveContents gc = new GraveContents(Arrays.copyOf(eq, 4), oh != null ? oh.clone() : null, Arrays.copyOf(inv, 36), totalExp);
        Grave grave = new Grave(gid, ou, player.getName(), deathLoc.getWorld().getName(),
            deathLoc.getBlockX(), deathLoc.getBlockY(), deathLoc.getBlockZ(),
            System.currentTimeMillis(), expireAt, bt, orig, gc);
        grave.setOriginalContents(gc); // 사망 시점 원본 스냅샷 (히스토리용)
        blockProvider.place(deathLoc, bt);
        byGraveId.put(gid, grave);
        byLocation.put(locKey(grave), grave);
        try {
            storage.save(grave);
        } catch (Exception e) {
            plugin.getLogger().severe("[InvKeeper] 무덤 생성 저장 실패 " + gid + ": " + e.getMessage());
        }
        hologramManager.spawn(grave);
        String msg = configManager.getGraveCreatedMessage();
        if (msg != null) {
            MessageUtil.send(player, msg
                .replace("{world}", grave.getWorldName())
                .replace("{x}", String.valueOf(grave.getX()))
                .replace("{y}", String.valueOf(grave.getY()))
                .replace("{z}", String.valueOf(grave.getZ())));
        }
        return grave;
    }
    public Grave getByLocation(Location loc) {
        return byLocation.get(loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ());
    }
    public Grave getByGraveId(UUID id) {
        Grave g = byGraveId.get(id);
        return g != null ? g : historyViews.get(id);
    }
    public Grave getActiveGrave(UUID id) { return byGraveId.get(id); }
    public List<Grave> getGravesByOwner(UUID owner) {
        List<Grave> list = new ArrayList<>();
        for (Grave g : byGraveId.values()) if (g.getOwnerUuid().equals(owner)) list.add(g);
        return list;
    }
    public List<Grave> getAllActive() { return new ArrayList<>(byGraveId.values()); }

    public void removeGrave(Grave grave, boolean ownerRecovery) {
        byGraveId.remove(grave.getGraveId());
        byLocation.remove(locKey(grave));
        storage.delete(grave.getGraveId());
        hologramManager.remove(grave.getGraveId());
        if (grave.getActiveLootSessionId() != null) lootSessionManager.cancelSession(grave.getGraveId());
        removeBlock(grave);
        if (ownerRecovery) grave.markRecovered(RecoveryType.OWNER);
        historyStorage.archive(grave);
    }
    public void expireGrave(Grave grave) { removeGrave(grave, false); }
    public void saveGrave(Grave grave) {
        if (historyViews.containsKey(grave.getGraveId())) {
            historyStorage.saveEntryContents(grave);
            return;
        }
        storage.save(grave);
    }

    private void removeBlock(Grave grave) {
        World w = Bukkit.getWorld(grave.getWorldName());
        if (w == null) return;
        blockProvider.remove(new Location(w, grave.getX(), grave.getY(), grave.getZ()), grave.getOriginalBlockData());
    }

    public GraveBlockProvider getBlockProvider() { return blockProvider; }
    public GraveHologramManager getHologramManager() { return hologramManager; }
    public LootSessionManager getLootSessionManager() { return lootSessionManager; }
    public GraveHistoryStorage getHistoryStorage() { return historyStorage; }
    public boolean isEnabled() { return enabled; }
    public ConfigManager getConfigManager() { return configManager; }
    public Plugin getPlugin() { return plugin; }

    // ── 히스토리 뷰 (어드민 원상태 열람) ──────────────────────
    public boolean isHistoryView(UUID id) { return historyViews.containsKey(id); }

    public Grave createHistoryView(GraveHistoryStorage.Entry en) {
        Grave g = new Grave(en.graveId, en.ownerUuid, en.ownerName, en.worldName,
            en.x, en.y, en.z, en.createdAt, en.expireAt, "VANILLA:BARREL", null, en.contents);
        try { g.setState(GraveState.valueOf(en.state)); } catch (Exception ignored) {}
        historyViews.put(en.graveId, g);
        return g;
    }

    public void openHistoryView(Player p, GraveHistoryStorage.Entry en) {
        Grave view = historyViews.get(en.graveId);
        if (view == null) {
            Grave active = byGraveId.get(en.graveId);
            if (active != null) {
                view = active;
            } else if (en.contents != null && !en.contents.isEmpty()) {
                view = createHistoryView(en);
            } else {
                String nm = configManager.getGraveHistoryNoItemsMessage();
                if (nm != null) MessageUtil.send(p, nm);
                return;
            }
        }
        // 우클릭=가상 GUI 열기만 담당(텔레포트는 좌클릭 전용). 가상 GUI는 위치와 무관하게 저장 내용으로 열림.
        Grave finalView = view;
        Bukkit.getScheduler().runTaskLater(plugin, () -> new GraveInventoryView(this).open(p, finalView), 3L);
    }

    public void closeHistoryView(UUID id) { historyViews.remove(id); }

    /**
     * 청크 로드 시 무덤 블록 무결성 확인/복구 + 홀로그램 재스폰.
     */
    public void repairChunk(org.bukkit.Chunk chunk) {
        for (Grave g : byGraveId.values()) {
            if (!g.getWorldName().equals(chunk.getWorld().getName())) continue;
            if ((g.getX() >> 4) != chunk.getX() || (g.getZ() >> 4) != chunk.getZ()) continue;
            try {
                Location loc = new Location(chunk.getWorld(), g.getX(), g.getY(), g.getZ());
                if (!blockProvider.matches(loc.getBlock(), g.getBlockType())) {
                    blockProvider.place(loc, g.getBlockType());
                }
                if (!hologramManager.hasHologram(g.getGraveId())) {
                    hologramManager.spawn(g);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[InvKeeper] 청크 복구 오류 " + g.getGraveId() + ": " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        for (Grave g : byGraveId.values()) storage.save(g);
        hologramManager.shutdown();
        lootSessionManager.shutdown();
    }

    private String locKey(Grave g) { return g.getWorldName() + ":" + g.getX() + ":" + g.getY() + ":" + g.getZ(); }
}