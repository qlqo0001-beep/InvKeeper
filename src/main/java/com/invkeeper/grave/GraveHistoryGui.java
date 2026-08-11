package com.invkeeper.grave;

import com.invkeeper.config.GraveGuiConfig;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GraveHistoryGui {

    private static final int ITEMS_PER_PAGE = 45;
    private final GraveManager graveManager;
    private final GraveHistoryStorage historyStorage;
    private final GraveGuiConfig guiConfig;
    // static: 명령 핸들러가 채운 캐시를 GUI 리스너(별도 인스턴스)와 공유하기 위함
    private static final Map<UUID, List<GraveHistoryStorage.Entry>> playerCache = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();

    public GraveHistoryGui(GraveManager gm) {
        this.graveManager = gm;
        this.historyStorage = gm.getHistoryStorage();
        this.guiConfig = gm.getConfigManager().getGraveGuiConfig();
    }

    public void open(Player p, UUID targetUuid) {
        List<GraveHistoryStorage.Entry> list = new ArrayList<>();
        for (Grave g : graveManager.getGravesByOwner(targetUuid)) list.add(toEntry(g));
        list.addAll(historyStorage.loadPlayer(targetUuid));
        list.sort((a, b) -> Long.compare(b.archivedAt, a.archivedAt));
        playerCache.put(p.getUniqueId(), list);
        if (list.isEmpty()) { MessageUtil.send(p, "&7조회할 무덤 히스토리가 없습니다."); return; }
        showPage(p, 0);
    }

    private GraveHistoryStorage.Entry toEntry(Grave g) {
        return new GraveHistoryStorage.Entry(g.getGraveId(), g.getOwnerUuid(), g.getOwnerName(),
            g.getWorldName(), g.getX(), g.getY(), g.getZ(), g.getCreatedAt(), g.getExpireAt(),
            g.getState().name(), g.getContents().countItems(), g.getContents().getTotalExp(),
            g.getRecoveredAt(), g.getRecoveredBy().name(), g.getLooterName(), g.getCreatedAt(),
            g.getContents());
    }

    public void showPage(Player p, int page) {
        List<GraveHistoryStorage.Entry> entries = playerCache.get(p.getUniqueId());
        if (entries == null || entries.isEmpty()) return;
        int tp = Math.max(1, (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, tp - 1));
        playerPages.put(p.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(new HistoryGuiHolder(page), 54,
            MessageUtil.color("&8무덤 히스토리 (" + (page + 1) + "/" + tp + ")"));
        int s = page * ITEMS_PER_PAGE;
        int e = Math.min(s + ITEMS_PER_PAGE, entries.size());
        for (int i = s; i < e; i++) {
            GraveHistoryStorage.Entry en = entries.get(i);
            // 회수됨/도굴됨/미회수 기준 색유리 판별
            Material mat;
            if ("OWNER".equals(en.recoveredBy)) mat = Material.YELLOW_STAINED_GLASS_PANE;                       // 회수됨
            else if ("LOOTER".equals(en.recoveredBy) || "LOOTED".equals(en.state)) mat = Material.RED_STAINED_GLASS_PANE; // 도굴됨
            else mat = Material.LIME_STAINED_GLASS_PANE;                                                        // 미회수
            ItemStack icon = new ItemStack(mat);
            ItemMeta m = icon.getItemMeta();
            if (m != null) {
                m.setDisplayName(MessageUtil.color("&e#" + (i + 1) + " " + en.ownerName));
                List<String> lore = new ArrayList<>();
                lore.add(MessageUtil.color("&7위치: &f" + en.worldName + " " + en.x + "," + en.y + "," + en.z));
                lore.add(MessageUtil.color("&7상태: &f" + en.state));
                lore.add(MessageUtil.color("&7원상태 아이템: &f" + en.itemCount + "개 / 경험치:" + en.totalExp));
                String rs;
                if ("OWNER".equals(en.recoveredBy)) rs = guiConfig.getRecoveryStatusOwner();
                else if ("LOOTER".equals(en.recoveredBy) || "LOOTED".equals(en.state)) rs = guiConfig.getRecoveryStatusLooter();
                else rs = guiConfig.getRecoveryStatusNone();
                lore.add(MessageUtil.color("&7회수: " + rs));
                if (en.looterName != null && !en.looterName.isEmpty())
                    lore.add(MessageUtil.color("&7도굴자: &c" + en.looterName));
                lore.add(MessageUtil.color("&e좌클릭: 텔레포트  &7|  &e우클릭: 열기"));
                m.setLore(lore); icon.setItemMeta(m);
            }
            inv.setItem(i - s, icon);
        }
        if (page > 0) inv.setItem(45, nav(Material.ARROW, "&a이전"));
        if (page < tp - 1) inv.setItem(53, nav(Material.ARROW, "&a다음"));
        p.openInventory(inv);
    }

    /**
     * 현재 페이지 캐시에서 슬롯에 해당하는 히스토리 엔트리 반환.
     */
    public GraveHistoryStorage.Entry getCachedEntry(Player p, int slot) {
        List<GraveHistoryStorage.Entry> entries = playerCache.get(p.getUniqueId());
        if (entries == null) return null;
        int pg = playerPages.getOrDefault(p.getUniqueId(), 0);
        int idx = pg * ITEMS_PER_PAGE + slot;
        if (idx < 0 || idx >= entries.size()) return null;
        return entries.get(idx);
    }

    public int getPlayerPage(UUID pid) { return playerPages.getOrDefault(pid, 0); }

    private ItemStack nav(Material mat, String name) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        if (m != null) { m.setDisplayName(MessageUtil.color(name)); i.setItemMeta(m); }
        return i;
    }
    public record HistoryGuiHolder(int page) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}