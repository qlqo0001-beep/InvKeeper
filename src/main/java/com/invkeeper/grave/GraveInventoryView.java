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

import java.util.UUID;
import java.util.stream.Collectors;

public class GraveInventoryView {

    private final GraveGuiConfig guiConfig;
    private final GraveManager graveManager;

    public GraveInventoryView(GraveManager gm) {
        this.graveManager = gm;
        this.guiConfig = gm.getConfigManager().getGraveGuiConfig();
    }

    public void open(Player p, Grave g) {
        Inventory inv = Bukkit.createInventory(new GHolder(g.getGraveId()), 54,
            MessageUtil.color(guiConfig.getTitle().replace("{player}", g.getOwnerName())));
        populate(inv, g);
        p.openInventory(inv);
    }
    public void refresh(Inventory inv, Grave grave) { inv.clear(); populate(inv, grave); }
    public int getRecoverAllSlot() { return guiConfig.getRecoverAllSlot(); }

    private void populate(Inventory inv, Grave grave) {
        inv.clear();
        ItemStack gray = fillerPane();
        for (int i = 0; i < 54; i++) inv.setItem(i, gray);
        for (int i = 9; i <= 44; i++) inv.setItem(i, null); // 인벤토리 4줄은 회색 안 채움

        GraveContents c = grave.getContents();
        ItemStack[] eq = c.getEquipment();
        if (eq[3] != null && !eq[3].getType().isAir()) inv.setItem(0, eq[3].clone());
        if (eq[2] != null && !eq[2].getType().isAir()) inv.setItem(1, eq[2].clone());
        if (eq[1] != null && !eq[1].getType().isAir()) inv.setItem(2, eq[1].clone());
        if (eq[0] != null && !eq[0].getType().isAir()) inv.setItem(3, eq[0].clone());
        ItemStack oh = c.getOffhand();
        if (oh != null && !oh.getType().isAir()) inv.setItem(4, oh.clone());
        if (c.getTotalExp() > 0) inv.setItem(5, expBottle(c.getTotalExp()));
        ItemStack[] invItems = c.getInventory();
        for (int i = 0; i < 36; i++) {
            if (invItems[i] != null && !invItems[i].getType().isAir()) inv.setItem(9 + i, invItems[i].clone());
        }
        inv.setItem(getRecoverAllSlot(), recoverBtn());
    }

    public void recoverAll(Player p, Grave g) {
        GraveContents c = g.getContents();
        ItemStack[] eq = c.getEquipment();
        equipOrGive(p, eq[3]); c.setEquipment(3, null);
        equipOrGive(p, eq[2]); c.setEquipment(2, null);
        equipOrGive(p, eq[1]); c.setEquipment(1, null);
        equipOrGive(p, eq[0]); c.setEquipment(0, null);
        ItemStack oh = c.getOffhand();
        if (oh != null && !oh.getType().isAir()) {
            if (p.getInventory().getItemInOffHand() == null || p.getInventory().getItemInOffHand().getType().isAir())
                p.getInventory().setItemInOffHand(oh);
            else giveOrDrop(p, oh);
            c.setOffhand(null);
        }
        for (int i = 0; i < 36; i++) {
            ItemStack it = c.getInventoryItem(i);
            if (it != null && !it.getType().isAir()) { giveOrDrop(p, it); c.setInventory(i, null); }
        }
        if (c.getTotalExp() > 0) { p.giveExp(c.getTotalExp()); c.setTotalExp(0); }
        graveManager.saveGrave(g);
    }

    public void moveToPlayer(Player p, Grave g, int slot) {
        GraveContents c = g.getContents();
        ItemStack item = null;
        if (slot == 0 && c.getEquipment()[3] != null) { item = c.getEquipment()[3]; c.setEquipment(3, null); }
        else if (slot == 1 && c.getEquipment()[2] != null) { item = c.getEquipment()[2]; c.setEquipment(2, null); }
        else if (slot == 2 && c.getEquipment()[1] != null) { item = c.getEquipment()[1]; c.setEquipment(1, null); }
        else if (slot == 3 && c.getEquipment()[0] != null) { item = c.getEquipment()[0]; c.setEquipment(0, null); }
        else if (slot == 4) { item = c.getOffhand(); c.setOffhand(null); }
        else if (slot >= 9 && slot < 45) { int i = slot - 9; item = c.getInventoryItem(i); c.setInventory(i, null); }
        if (item != null && !item.getType().isAir()) {
            if (slot <= 3) equipOrGive(p, item);
            else if (slot == 4) {
                if (p.getInventory().getItemInOffHand() == null || p.getInventory().getItemInOffHand().getType().isAir())
                    p.getInventory().setItemInOffHand(item);
                else giveOrDrop(p, item);
            } else giveOrDrop(p, item);
            graveManager.saveGrave(g);
        }
    }

    private void equipOrGive(Player p, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        String n = item.getType().name();
        if ((n.endsWith("_HELMET") || n.endsWith("_HEAD") || n.equals("CARVED_PUMPKIN")) && isEmpty(p.getInventory().getHelmet())) {
            p.getInventory().setHelmet(item); return;
        }
        if (n.endsWith("_CHESTPLATE") && isEmpty(p.getInventory().getChestplate())) { p.getInventory().setChestplate(item); return; }
        if (n.endsWith("_LEGGINGS") && isEmpty(p.getInventory().getLeggings())) { p.getInventory().setLeggings(item); return; }
        if (n.endsWith("_BOOTS") && isEmpty(p.getInventory().getBoots())) { p.getInventory().setBoots(item); return; }
        giveOrDrop(p, item);
    }

    private boolean isEmpty(ItemStack item) { return item == null || item.getType().isAir(); }

    private void giveOrDrop(Player p, ItemStack item) {
        var r = p.getInventory().addItem(item);
        for (ItemStack left : r.values())
            if (left != null && !left.getType().isAir()) p.getWorld().dropItemNaturally(p.getLocation(), left);
    }

    private ItemStack expBottle(int amt) {
        ItemStack i = new ItemStack(guiConfig.getExpBottleMaterial());
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName(MessageUtil.color(guiConfig.getExpBottleName().replace("{amount}", String.valueOf(amt))));
            m.setLore(guiConfig.getExpBottleLore().stream().map(MessageUtil::color).collect(Collectors.toList()));
            i.setItemMeta(m);
        }
        return i;
    }

    private ItemStack fillerPane() {
        ItemStack i = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = i.getItemMeta();
        // 이름을 빈 값으로: 완전 빈 문자열은 클라이언트가 기본명을 표시할 수 있어 공백 한 칸 사용
        if (m != null) { m.setDisplayName(" "); i.setItemMeta(m); }
        return i;
    }

    private ItemStack recoverBtn() {
        ItemStack i = new ItemStack(guiConfig.getRecoverAllMaterial());
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName(MessageUtil.color(guiConfig.getRecoverAllName()));
            m.setLore(guiConfig.getRecoverAllLore().stream().map(MessageUtil::color).collect(Collectors.toList()));
            i.setItemMeta(m);
        }
        return i;
    }

    public boolean isExpSlot(int s) { return s == 5; }

    public record GHolder(UUID graveId) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}