package com.invkeeper.config;

public final class WorldRule {
    private final double inventoryDropPercent;
    private final double expDropPercent;

    public WorldRule(double inventoryDropPercent, double expDropPercent) {
        this.inventoryDropPercent = inventoryDropPercent;
        this.expDropPercent = expDropPercent;
    }

    public double getInventoryDropPercent() {
        return inventoryDropPercent;
    }

    public double getExpDropPercent() {
        return expDropPercent;
    }
}
