package com.invkeeper.grave;

import java.util.UUID;

/**
 * 도굴 시전 세션.
 * 서버 재시작 후에도 영속화되어 남은 시간을 이어서 진행합니다.
 */
public final class LootSession {

    private final UUID sessionId;
    private final UUID graveId;
    private final UUID looterUuid;
    private final String looterName;
    private final long startedAt;
    private final long endsAt;

    public LootSession(UUID sessionId, UUID graveId, UUID looterUuid, String looterName,
                       long startedAt, long endsAt) {
        this.sessionId = sessionId;
        this.graveId = graveId;
        this.looterUuid = looterUuid;
        this.looterName = looterName;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
    }

    public UUID getSessionId() { return sessionId; }
    public UUID getGraveId() { return graveId; }
    public UUID getLooterUuid() { return looterUuid; }
    public String getLooterName() { return looterName; }
    public long getStartedAt() { return startedAt; }
    public long getEndsAt() { return endsAt; }

    /**
     * 세션이 완료되었는지 확인합니다.
     */
    public boolean isCompleted() {
        return System.currentTimeMillis() >= endsAt;
    }

    /**
     * 남은 시간을 밀리초로 반환.
     */
    public long getRemainingMillis() {
        long remaining = endsAt - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * 남은 시간을 초로 반환.
     */
    public long getRemainingSeconds() {
        return getRemainingMillis() / 1000L;
    }

    @Override
    public String toString() {
        return "LootSession{id=" + sessionId + ", graveId=" + graveId
                + ", looter=" + looterName + ", remaining=" + getRemainingSeconds() + "s}";
    }
}
