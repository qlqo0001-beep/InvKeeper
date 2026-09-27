package com.invkeeper.config;

public final class WorldRule {
    private final double inventoryDropPercent;
    private final double expDropPercent;
    // PvP 사망 시 드랍률. null = 설정 안 됨 (ConfigManager.resolveDropPercents에서 대체값 결정)
    private final Double pvpInventoryDropPercent;
    private final Double pvpExpDropPercent;

    public WorldRule(double inventoryDropPercent, double expDropPercent) {
        this(inventoryDropPercent, expDropPercent, null, null);
    }

    public WorldRule(double inventoryDropPercent, double expDropPercent,
                     Double pvpInventoryDropPercent, Double pvpExpDropPercent) {
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

    public Double getPvpInventoryDropPercent() {
        return pvpInventoryDropPercent;
    }

    public Double getPvpExpDropPercent() {
        return pvpExpDropPercent;
    }
}
