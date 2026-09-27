package com.invkeeper.config;

public final class PermissionRule {
    private final String permission;
    private final int priority;
    private final double inventoryDropPercent;
    private final double expDropPercent;
    // PvP 사망 시 드랍률. null = 설정 안 됨 (ConfigManager.resolveDropPercents에서 대체값 결정)
    private final Double pvpInventoryDropPercent;
    private final Double pvpExpDropPercent;

    public PermissionRule(String permission, int priority, double inventoryDropPercent, double expDropPercent) {
        this(permission, priority, inventoryDropPercent, expDropPercent, null, null);
    }

    public PermissionRule(String permission, int priority, double inventoryDropPercent, double expDropPercent,
                          Double pvpInventoryDropPercent, Double pvpExpDropPercent) {
        this.permission = permission;
        this.priority = priority;
        this.inventoryDropPercent = inventoryDropPercent;
        this.expDropPercent = expDropPercent;
        this.pvpInventoryDropPercent = pvpInventoryDropPercent;
        this.pvpExpDropPercent = pvpExpDropPercent;
    }

    public String getPermission() {
        return permission;
    }

    public int getPriority() {
        return priority;
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
