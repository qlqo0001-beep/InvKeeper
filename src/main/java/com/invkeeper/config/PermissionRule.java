package com.invkeeper.config;

public final class PermissionRule {
    private final String permission;
    private final int priority;
    private final double inventoryDropPercent;
    private final double expDropPercent;
    private final double pvpInventoryDropPercent;
    private final double pvpExpDropPercent;

    public PermissionRule(String permission, int priority, double inventoryDropPercent, double expDropPercent) {
        this(permission, priority, inventoryDropPercent, expDropPercent, inventoryDropPercent, expDropPercent);
    }

    public PermissionRule(String permission, int priority, double inventoryDropPercent, double expDropPercent,
                          double pvpInventoryDropPercent, double pvpExpDropPercent) {
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

    public double getPvpInventoryDropPercent() {
        return pvpInventoryDropPercent;
    }

    public double getPvpExpDropPercent() {
        return pvpExpDropPercent;
    }
}
