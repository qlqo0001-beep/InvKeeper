package com.invkeeper.grave;

import com.invkeeper.config.GraveHologramConfig;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GraveHologramManager {

    private final Plugin plugin;
    private final GraveHologramConfig config;
    private final Map<UUID, TextDisplay> holograms = new ConcurrentHashMap<>();

    public GraveHologramManager(Plugin plugin, GraveHologramConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void spawn(Grave grave) {
        if (!config.isEnabled()) return;
        remove(grave.getGraveId());
        World world = Bukkit.getWorld(grave.getWorldName());
        if (world == null) return;
        Location loc = new Location(world, grave.getX() + 0.5, grave.getY() + config.getOffsetY(), grave.getZ() + 0.5);
        TextDisplay td = (TextDisplay) world.spawnEntity(loc, EntityType.TEXT_DISPLAY);
        td.setBillboard(Display.Billboard.CENTER);
        td.setSeeThrough(false);
        td.setPersistent(false);
        updateText(td, grave);
        holograms.put(grave.getGraveId(), td);
    }

    public void updateAll(Grave grave) {
        TextDisplay td = holograms.get(grave.getGraveId());
        if (td != null && td.isValid()) updateText(td, grave);
        else spawn(grave);
    }

    public void remove(UUID graveId) {
        TextDisplay td = holograms.remove(graveId);
        if (td != null && td.isValid()) td.remove();
    }

    public void shutdown() {
        for (TextDisplay td : holograms.values()) {
            if (td.isValid()) td.remove();
        }
        holograms.clear();
    }

    private void updateText(TextDisplay td, Grave grave) {
        StringBuilder sb = new StringBuilder();
        String line1 = config.getLineFormat()
                .replace("{player}", grave.getOwnerName());
        if (grave.getExpireAt() != -1) {
            long sec = grave.getRemainingMillis() / 1000L;
            line1 = line1.replace("{remaining}", MessageUtil.formatDuration(grave.getRemainingMillis(), "{minutes}분 {seconds_padded}초"));
        } else {
            line1 = config.getLineFormatUnlimited().replace("{player}", grave.getOwnerName());
        }
        sb.append(MessageUtil.color(line1));

        if (grave.getState() == GraveState.BEING_LOOTED) {
            LootSession session = getSession(grave.getActiveLootSessionId());
            String line2 = config.getLootingLineFormat();
            if (session != null) {
                long sec = session.getRemainingSeconds();
                line2 = line2.replace("{remaining}", MessageUtil.formatDuration(session.getRemainingMillis(), "{minutes}분 {seconds_padded}초"));
            }
            sb.append("\n").append(MessageUtil.color(line2));
        } else if (grave.getState() == GraveState.LOOTED) {
            String line2 = config.getLootedLineFormat()
                    .replace("{looter}", grave.getLooterName() != null ? grave.getLooterName() : "?")
                    .replace("{owner}", grave.getOwnerName());
            sb.append("\n").append(MessageUtil.color(line2));
        }
        td.setText(sb.toString());
    }

    // Circular dependency broken via lazy lookup; set by GraveManager after construction
    private java.util.function.Function<UUID, LootSession> sessionLookup;

    public void setSessionLookup(java.util.function.Function<UUID, LootSession> lookup) {
        this.sessionLookup = lookup;
    }

    private LootSession getSession(UUID sessionId) {
        if (sessionId == null || sessionLookup == null) return null;
        return sessionLookup.apply(sessionId);
    }

    public boolean hasHologram(UUID graveId) {
        TextDisplay td = holograms.get(graveId);
        return td != null && td.isValid();
    }
}
