package com.invkeeper.grave;

import org.bukkit.inventory.ItemStack;
import java.util.Arrays;

/**
 * 무덤에 저장되는 플레이어 인벤토리 + 경험치.
 * GUI 오픈 시 이 구조체를 기반으로 InventoryView를 재구성합니다.
 *
 * 슬롯 레이아웃 (54칸 InventoryView 기준):
 *   0~3 : 장비 (helmet, chestplate, leggings, boots)
 *   4   : 왼손(offhand)
 *   9~44: 인벤토리 4줄 (36칸)
 *   49  : "모두 회수" 버튼
 *   나머지 빈 슬롯: 경험치병 아이콘 (totalExp > 0 일 때)
 */
public final class GraveContents {

    private final ItemStack[] equipment;   // length 4
    private ItemStack offhand;              // nullable
    private final ItemStack[] inventory;   // length 36
    private int totalExp;

    public GraveContents(ItemStack[] equipment, ItemStack offhand, ItemStack[] inventory, int totalExp) {
        this.equipment = new ItemStack[4];
        for (int i = 0; i < 4 && i < equipment.length; i++) {
            this.equipment[i] = equipment[i] != null ? equipment[i].clone() : null;
        }
        this.offhand = offhand != null ? offhand.clone() : null;
        this.inventory = new ItemStack[36];
        for (int i = 0; i < 36 && i < inventory.length; i++) {
            this.inventory[i] = inventory[i] != null ? inventory[i].clone() : null;
        }
        this.totalExp = Math.max(0, totalExp);
    }

    public ItemStack[] getEquipment() {
        ItemStack[] copy = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            copy[i] = equipment[i] != null ? equipment[i].clone() : null;
        }
        return copy;
    }

    public void setEquipment(int index, ItemStack item) {
        if (index >= 0 && index < 4) {
            equipment[index] = item != null ? item.clone() : null;
        }
    }

    public ItemStack getOffhand() {
        return offhand != null ? offhand.clone() : null;
    }

    public void setOffhand(ItemStack item) {
        this.offhand = item != null ? item.clone() : null;
    }

    public ItemStack[] getInventory() {
        ItemStack[] copy = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            copy[i] = inventory[i] != null ? inventory[i].clone() : null;
        }
        return copy;
    }

    public void setInventory(int index, ItemStack item) {
        if (index >= 0 && index < 36) {
            inventory[index] = item != null ? item.clone() : null;
        }
    }

    public ItemStack getInventoryItem(int index) {
        if (index >= 0 && index < 36) {
            return inventory[index] != null ? inventory[index].clone() : null;
        }
        return null;
    }

    public int getTotalExp() {
        return totalExp;
    }

    public void setTotalExp(int totalExp) {
        this.totalExp = Math.max(0, totalExp);
    }

    /**
     * 모든 슬롯이 비었고 경험치도 0인지 확인.
     */
    public boolean isEmpty() {
        if (totalExp > 0) return false;
        for (ItemStack item : equipment) {
            if (item != null && !item.getType().isAir()) return false;
        }
        if (offhand != null && !offhand.getType().isAir()) return false;
        for (ItemStack item : inventory) {
            if (item != null && !item.getType().isAir()) return false;
        }
        return true;
    }

    /**
     * 총 아이템 개수 (경험치는 제외).
     */
    public int countItems() {
        int count = 0;
        for (ItemStack item : equipment) {
            if (item != null && !item.getType().isAir()) count++;
        }
        if (offhand != null && !offhand.getType().isAir()) count++;
        for (ItemStack item : inventory) {
            if (item != null && !item.getType().isAir()) count++;
        }
        return count;
    }

    @Override
    public String toString() {
        return "GraveContents{items=" + countItems() + ", exp=" + totalExp + "}";
    }
}
