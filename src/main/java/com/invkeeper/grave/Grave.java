package com.invkeeper.grave;

import org.bukkit.block.data.BlockData;
import java.util.UUID;

/**
 * 단일 무덤 엔티티.
 * 무덤 생성 시점에 모든 속성이 결정되고, 이후 상태 전이만 발생합니다.
 */
public final class Grave {

    private final UUID graveId;
    private final UUID ownerUuid;
    private final String ownerName;
    private final String worldName;
    private final int x, y, z;
    private final long createdAt;
    private final long expireAt; // -1 = 무제한
    private final String blockType; // 예: "VANILLA:BARREL"
    private final BlockData originalBlockData;

    private GraveState state;
    private final GraveContents contents;

    private UUID looterUuid;
    private String looterName;
    private UUID activeLootSessionId;

    private long recoveredAt;       // 0 = 미회수
    private RecoveryType recoveredBy;

    // 사망 시점 원본 스냅샷 (히스토리 보존용)
    private GraveContents originalContents;

    public Grave(UUID graveId, UUID ownerUuid, String ownerName, String worldName,
                 int x, int y, int z, long createdAt, long expireAt,
                 String blockType, BlockData originalBlockData,
                 GraveContents contents) {
        this.graveId = graveId;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.createdAt = createdAt;
        this.expireAt = expireAt;
        this.blockType = blockType;
        this.originalBlockData = originalBlockData;
        this.contents = contents;
        this.state = GraveState.ACTIVE;
        this.recoveredBy = RecoveryType.NONE;
    }

    // ── Getters ──────────────────────────────────────────────

    public UUID getGraveId() { return graveId; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public String getWorldName() { return worldName; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public long getCreatedAt() { return createdAt; }
    public long getExpireAt() { return expireAt; }
    public boolean isExpiryEnabled() { return expireAt != -1; }
    public String getBlockType() { return blockType; }
    public BlockData getOriginalBlockData() { return originalBlockData; }
    public GraveState getState() { return state; }
    public GraveContents getContents() { return contents; }
    public UUID getLooterUuid() { return looterUuid; }
    public String getLooterName() { return looterName; }
    public UUID getActiveLootSessionId() { return activeLootSessionId; }
    public long getRecoveredAt() { return recoveredAt; }
    public RecoveryType getRecoveredBy() { return recoveredBy; }

    public GraveContents getOriginalContents() { return originalContents; }
    public void setOriginalContents(GraveContents originalContents) {
        if (originalContents != null) {
            this.originalContents = new GraveContents(
                originalContents.getEquipment(), originalContents.getOffhand(),
                originalContents.getInventory(), originalContents.getTotalExp());
        } else {
            this.originalContents = null;
        }
    }

    // ── Setters (state transitions) ──────────────────────────

    public void setState(GraveState state) { this.state = state; }

    public void setLooter(UUID uuid, String name) {
        this.looterUuid = uuid;
        this.looterName = name;
    }

    public void setActiveLootSessionId(UUID sessionId) {
        this.activeLootSessionId = sessionId;
    }

    public void markRecovered(RecoveryType type) {
        this.recoveredAt = System.currentTimeMillis();
        this.recoveredBy = type;
    }

    /**
     * 만료되었는지 확인합니다. (-1이면 무제한)
     */
    public boolean isExpired() {
        return expireAt != -1 && System.currentTimeMillis() >= expireAt;
    }

    /**
     * 남은 시간을 밀리초로 반환. 무제한이면 -1.
     */
    public long getRemainingMillis() {
        if (expireAt == -1) return -1;
        long remaining = expireAt - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    @Override
    public String toString() {
        return "Grave{id=" + graveId + ", owner=" + ownerName
                + ", pos=(" + worldName + "," + x + "," + y + "," + z + ")"
                + ", state=" + state + ", contents=" + contents + "}";
    }
}
