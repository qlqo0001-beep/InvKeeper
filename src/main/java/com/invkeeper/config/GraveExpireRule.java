package com.invkeeper.config;

/**
 * PermissionRule과 동일한 우선순위 패턴으로,
 * 무덤 만료 쿨타임을 권한별로 결정합니다.
 */
public final class GraveExpireRule {

    private final String permission;
    private final int priority;
    private final int seconds; // -1 = 무제한

    public GraveExpireRule(String permission, int priority, int seconds) {
        this.permission = permission;
        this.priority = priority;
        this.seconds = seconds;
    }

    public String getPermission() { return permission; }
    public int getPriority() { return priority; }
    public int getSeconds() { return seconds; }
}
