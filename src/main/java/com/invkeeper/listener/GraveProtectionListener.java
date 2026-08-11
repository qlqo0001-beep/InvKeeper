package com.invkeeper.listener;

import com.invkeeper.grave.Grave;
import com.invkeeper.grave.GraveManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public class GraveProtectionListener implements Listener {

    private final GraveManager graveManager;

    public GraveProtectionListener(GraveManager graveManager) {
        this.graveManager = graveManager;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Grave g = graveManager.getByLocation(e.getBlock().getLocation());
        if (g != null) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> graveManager.getByLocation(b.getLocation()) != null);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> graveManager.getByLocation(b.getLocation()) != null);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        graveManager.repairChunk(e.getChunk());
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) {
            if (graveManager.getByLocation(b.getLocation()) != null) { e.setCancelled(true); return; }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) {
            if (graveManager.getByLocation(b.getLocation()) != null) { e.setCancelled(true); return; }
        }
    }
}
