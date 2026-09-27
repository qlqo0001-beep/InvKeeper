package com.invkeeper.config;

public final class WorldRule {
    private final double inventoryDropPercent;
    private final double expDropPercent;
    private final double pvpInventoryDropPercent;
    private final double pvpExpDropPercent;

    public WorldRule(double inventoryDropPercent, double expDropPercent) {
        this(inventoryDropPercent, expDropPercent, inventoryDropPercent, expDropPercent);
    }

    public WorldRule(double inventoryDropPercent, double expDropPercent,
                     double pvpInventoryDropPercent, double pvpExpDropPercent) {
        this.inventoryDropPercent = inventoryDropPercent;
        this.expDropPercent = expDropPercent;
        this.pvpInventoryDropPercent = pvpInventoryDropPercent;
        this.pvpExpDropPercent = pvpExpDropPercent;
    }

    public double getInventoryDropPercent() {
        return inventoryDropPercent;
    }

    public double getExpDropPercent() {
        return expDropPercent;
    }

    public double getPvpInventoryDropPercent() {
        return pvpInventoryDropPercent;
    }

    public double getPvpExpDropPercent() {
        return pvpExpDropPercent;
    }
}
