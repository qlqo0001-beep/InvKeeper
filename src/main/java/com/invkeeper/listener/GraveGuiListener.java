package com.invkeeper.listener;

import com.invkeeper.grave.*;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class GraveGuiListener implements Listener {

    private final GraveManager graveManager;

    public GraveGuiListener(GraveManager graveManager) {
        this.graveManager = graveManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryView view = e.getView();
        if (!(view.getTopInventory().getHolder() instanceof GraveInventoryView.GHolder)) return;
        e.setCancelled(true);
        GraveInventoryView.GHolder holder = (GraveInventoryView.GHolder) view.getTopInventory().getHolder();
        Grave grave = graveManager.getByGraveId(holder.graveId());
        if (grave == null) { e.getWhoClicked().closeInventory(); return; }
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        GraveInventoryView giv = new GraveInventoryView(graveManager);

        if (slot == giv.getRecoverAllSlot()) {
            giv.recoverAll(p, grave);
            p.closeInventory(); // 회수 후 GUI 자동 닫기
            return;
        }
        if (slot >= 54) return;

        GraveContents c = grave.getContents();
        if (giv.isExpSlot(slot) && c.getTotalExp() > 0) {
            p.giveExp(c.getTotalExp()); c.setTotalExp(0);
            graveManager.saveGrave(grave);
            giv.refresh(view.getTopInventory(), grave);
            return;
        }
        ItemStack item = view.getTopInventory().getItem(slot);
        if (item != null && !item.getType().isAir() && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
            giv.moveToPlayer(p, grave, slot);
            giv.refresh(view.getTopInventory(), grave);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof GraveInventoryView.GHolder) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof GraveInventoryView.GHolder)) return;
        GraveInventoryView.GHolder h = (GraveInventoryView.GHolder) e.getInventory().getHolder();
        Grave g = graveManager.getByGraveId(h.graveId());
        if (g == null) return;
        Player p = (Player) e.getPlayer();

        // 히스토리 뷰(어드민 원상태 열람)는 삭제하지 않고 유지
        if (graveManager.isHistoryView(g.getGraveId())) {
            graveManager.saveGrave(g); // 잔여 내용 저장
            graveManager.closeHistoryView(g.getGraveId());
            return;
        }

        if (p.getUniqueId().equals(g.getOwnerUuid()) && g.getContents().isEmpty()) {
            graveManager.removeGrave(g, true);
            String m = graveManager.getConfigManager().getGraveFullyRecoveredMessage();
            if (m != null) MessageUtil.send(p, m);
        } else if (!p.getUniqueId().equals(g.getOwnerUuid()) && g.getState() == GraveState.LOOTED) {
            g.markRecovered(RecoveryType.LOOTER);
            graveManager.saveGrave(g);
        }
    }
}