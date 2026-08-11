package com.invkeeper.grave.block;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

/**
 * 무덤 블록의 배치·제거·식별을 추상화하는 인터페이스.
 * 바닐라, ItemsAdder, Oraxen 등 다양한 블록 제공자를 플러그인으로 추가 가능.
 */
public interface GraveBlockProvider {

    /**
     * 지정한 위치에 blockId에 해당하는 블록을 배치합니다.
     */
    void place(Location loc, String blockId);

    /**
     * 지정한 위치의 무덤 블록을 제거하고 original 블록으로 복구합니다.
     */
    void remove(Location loc, BlockData original);

    /**
     * 주어진 블록이 blockId와 일치하는지 확인합니다.
     */
    boolean matches(Block block, String blockId);
}
