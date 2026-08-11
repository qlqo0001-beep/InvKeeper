package com.invkeeper.grave.block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

/**
 * 바닐라 Material을 무덤 블록으로 사용하는 기본 구현체.
 * blockId 형식: "VANILLA:BARREL", "VANILLA:CHEST" 등
 */
public class VanillaBlockProvider implements GraveBlockProvider {

    @Override
    public void place(Location loc, String blockId) {
        Material material = parseMaterial(blockId);
        if (material != null && material.isBlock()) {
            loc.getBlock().setType(material);
        }
    }

    @Override
    public void remove(Location loc, BlockData original) {
        if (original != null) {
            loc.getBlock().setBlockData(original);
        } else {
            loc.getBlock().setType(Material.AIR);
        }
    }

    @Override
    public boolean matches(Block block, String blockId) {
        Material material = parseMaterial(blockId);
        return material != null && block.getType() == material;
    }

    /**
     * "VANILLA:BARREL" 형식에서 Material 이름을 추출합니다.
     */
    public static Material parseMaterial(String blockId) {
        if (blockId == null) return null;
        String name = blockId;
        if (name.contains(":")) {
            name = name.substring(name.indexOf(':') + 1);
        }
        try {
            return Material.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
