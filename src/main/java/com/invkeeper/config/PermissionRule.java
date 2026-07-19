package com.invkeeper.config;

public final class PermissionRule {
    private final String permission;
    private final int priority;
    private final double inventoryDropPercent;
    private final double expDropPercent;

    public PermissionRule(String permission, int priority, double inventoryDropPercent, double expDropPercent) {
        this.permission = permission;
        this.priority = priority;
        this.inventoryDropPercent = inventoryDropPercent;
        this.expDropPercent = expDropPercent;
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
}
