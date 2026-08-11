package com.invkeeper.grave;

import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GraveListGui {

    private static final int ITEMS_PER_PAGE = 45;
    private final GraveManager graveManager;
    private final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();

    public GraveListGui(GraveManager graveManager) { this.graveManager = graveManager; }

    public void open(Player player, int page) {
        List<Grave> graves = graveManager.getAllActive();
        int totalPages = Math.max(1, (int) Math.ceil((double) graves.size() / ITEMS_PER_PAGE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        playerPages.put(player.getUniqueId(), page);

        List<UUID> graveIds = new ArrayList<>();
        for (Grave g : graves) graveIds.add(g.getGraveId());

        Inventory inv = Bukkit.createInventory(new ListGuiHolder(graveIds, page), 54,
            MessageUtil.color("&8활성 무덤 목록 (" + (page + 1) + "/" + totalPages + ")"));

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, graves.size());
        for (int i = start; i < end; i++) {
            Grave g = graves.get(i);
            ItemStack icon = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(MessageUtil.color("&e" + g.getOwnerName() + " 의 무덤"));
                List<String> lore = new ArrayList<>();
                lore.add(MessageUtil.color("&7위치: &f" + g.getWorldName() + " " + g.getX() + "," + g.getY() + "," + g.getZ()));
                lore.add(MessageUtil.color("&7상태: &f" + g.getState().name()));
                String remain = g.getExpireAt() == -1 ? "무제한" : MessageUtil.formatDuration(g.getRemainingMillis(), "{minutes}분 {seconds_padded}초");
                lore.add(MessageUtil.color("&7남은시간: &f" + remain));
                lore.add(MessageUtil.color("&7아이템: &f" + g.getContents().countItems() + "개, 경험치: " + g.getContents().getTotalExp() + "exp"));
                lore.add(MessageUtil.color("&a클릭하여 텔레포트"));
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(i - start, icon);
        }

        // Navigation buttons
        if (page > 0) {
            inv.setItem(45, navItem(Material.ARROW, "&a이전 페이지", page - 1));
        }
        if (page < totalPages - 1) {
            inv.setItem(53, navItem(Material.ARROW, "&a다음 페이지", page + 1));
        }

        player.openInventory(inv);
    }

    private ItemStack navItem(Material mat, String name, int targetPage) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.color(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    public int getPlayerPage(UUID playerId) {
        return playerPages.getOrDefault(playerId, 0);
    }

    public record ListGuiHolder(List<UUID> graveIds, int page) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}
