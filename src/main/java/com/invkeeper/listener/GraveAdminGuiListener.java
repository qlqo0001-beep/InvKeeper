package com.invkeeper.listener;

import com.invkeeper.grave.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

public class GraveAdminGuiListener implements Listener {

    private final GraveManager graveManager;

    public GraveAdminGuiListener(GraveManager graveManager) {
        this.graveManager = graveManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        var holder = e.getInventory().getHolder();

        // List GUI
        if (holder instanceof GraveListGui.ListGuiHolder lh) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            GraveListGui listGui = new GraveListGui(graveManager);
            if (slot == 45 && lh.page() > 0) { listGui.open(p, lh.page() - 1); return; }
            if (slot == 53) { listGui.open(p, lh.page() + 1); return; }
            if (slot >= 0 && slot < 45) {
                int idx = lh.page() * 45 + slot;
                if (idx < lh.graveIds().size()) {
                    Grave g = graveManager.getByGraveId(lh.graveIds().get(idx));
                    if (g != null) {
                        World w = p.getServer().getWorld(g.getWorldName());
                        if (w != null) p.teleport(new Location(w, g.getX() + 0.5, g.getY(), g.getZ() + 0.5));
                    }
                }
            }
            return;
        }

        // History GUI
        if (holder instanceof GraveHistoryGui.HistoryGuiHolder hh) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            GraveHistoryGui historyGui = new GraveHistoryGui(graveManager);
            if (slot == 45 && hh.page() > 0) { historyGui.showPage(p, hh.page() - 1); return; }
            if (slot == 53) { historyGui.showPage(p, hh.page() + 1); return; }
            if (slot >= 0 && slot < 45) {
                GraveHistoryStorage.Entry en = historyGui.getCachedEntry(p, slot);
                if (en == null) return;
                if (e.isLeftClick()) {
                    // 좌클릭: 텔레포트
                    World w = p.getServer().getWorld(en.worldName);
                    if (w != null) p.teleport(new Location(w, en.x + 0.5, en.y + 1, en.z + 0.5));
                } else if (e.isRightClick()) {
                    // 우클릭: 가상 GUI 열기 (openHistoryView가 텔레포트 포함)
                    graveManager.openHistoryView(p, en);
                }
            }
        }
    }
}
