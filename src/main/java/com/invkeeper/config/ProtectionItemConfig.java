package com.invkeeper.config;

import org.bukkit.Material;

import java.util.List;
import java.util.Objects;

public final class ProtectionItemConfig {
    public enum Kind {
        CONSUMABLE_PROTECTION,
        TIMED_PROTECTION,
        SOULBIND_TOOL,
        SOULBIND_TOOL_TIME,
        SOULBIND_TOOL_STACK,
        SOULBIND_UNBIND_TOOL,
        GRAVE_LOOT_TOOL,        // 도굴 아이템
        GRAVE_LOCK;             // 무덤 자물쇠 (사망 시 소지하면 무덤에 자물쇠가 걸림)

        public boolean isProtection() {
            return this == CONSUMABLE_PROTECTION || this == TIMED_PROTECTION;
        }

        public boolean isSoulbindTool() {
            return this == SOULBIND_TOOL || this == SOULBIND_TOOL_TIME
                || this == SOULBIND_TOOL_STACK || this == SOULBIND_UNBIND_TOOL;
        }

        public boolean isUnbindTool() { return this == SOULBIND_UNBIND_TOOL; }
        public boolean isTimeTool() { return this == SOULBIND_TOOL || this == SOULBIND_TOOL_TIME; }
        public boolean isStackTool() { return this == SOULBIND_TOOL_STACK; }
        public boolean isGraveLootTool() { return this == GRAVE_LOOT_TOOL; }
        public boolean isGraveLock() { return this == GRAVE_LOCK; }
    }

    private final String key;
    private final Kind kind;
    private final boolean useMmo;
    private final boolean useVanilla;
    private final String mmoItemsType;
    private final String mmoItemsId;
    private final int durationMinutes;
    private final Material vanillaMaterial;
    private final String vanillaName;
    private final List<String> vanillaLore;
    private final Integer customModelData;

    // Soulbind settings (only meaningful for protection items)
    private final boolean soulbindEnabled;
    private final int soulbindDurationMinutes;
    private final boolean soulbindInfinite;
    private final int soulbindApplyDuration; // For SOULBIND_TOOL_TIME: duration applied to target items (분)
    private final int soulbindStacks;         // For SOULBIND_TOOL_STACK: stacks applied to target items, -1 = infinite
    private final int castTimeSeconds;        // For GRAVE_LOOT_TOOL: cast time in seconds
    private final int lockSeconds;            // For GRAVE_LOCK: 사망 후 도굴을 시작할 수 없는 시간(초)
    private final int extraCastSeconds;       // For GRAVE_LOCK: 도굴 시전 시간 증가량(초)

    public ProtectionItemConfig(
            String key,
            Kind kind,
            boolean useMmo,
            boolean useVanilla,
            String mmoItemsType,
            String mmoItemsId,
            int durationMinutes,
            Material vanillaMaterial,
            String vanillaName,
            List<String> vanillaLore,
            Integer customModelData,
            boolean soulbindEnabled,
            int soulbindDurationMinutes,
            boolean soulbindInfinite,
            int soulbindApplyDuration,
            int soulbindStacks,
            int castTimeSeconds,
            int lockSeconds,
            int extraCastSeconds) {
        this.key = Objects.requireNonNull(key, "key");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.useMmo = useMmo;
        this.useVanilla = useVanilla;
        this.mmoItemsType = Objects.requireNonNullElse(mmoItemsType, "");
        this.mmoItemsId = Objects.requireNonNullElse(mmoItemsId, "");
        this.durationMinutes = Math.max(durationMinutes, 0);
        this.vanillaMaterial = Objects.requireNonNullElse(vanillaMaterial, Material.PAPER);
        this.vanillaName = Objects.requireNonNullElse(vanillaName, "");
        this.vanillaLore = List.copyOf(Objects.requireNonNullElse(vanillaLore, List.of()));
        this.customModelData = customModelData;
        this.soulbindEnabled = soulbindEnabled;
        this.soulbindDurationMinutes = Math.max(0, soulbindDurationMinutes);
        this.soulbindInfinite = soulbindInfinite;
        this.soulbindApplyDuration = Math.max(0, soulbindApplyDuration);
        this.soulbindStacks = soulbindStacks;
        this.castTimeSeconds = Math.max(0, castTimeSeconds);
        this.lockSeconds = Math.max(0, lockSeconds);
        this.extraCastSeconds = Math.max(0, extraCastSeconds);
    }

    public String getKey() {
        return key;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isUseMmo() {
        return useMmo;
    }

    public boolean isUseVanilla() {
        return useVanilla;
    }

    public String getMmoItemsType() {
        return mmoItemsType;
    }

    public String getMmoItemsId() {
        return mmoItemsId;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public Material getVanillaMaterial() {
        return vanillaMaterial;
    }

    public String getVanillaName() {
        return vanillaName;
    }

    public List<String> getVanillaLore() {
        return vanillaLore;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public boolean isSoulbindEnabled() {
        return soulbindEnabled;
    }

    public int getSoulbindDurationMinutes() {
        return soulbindDurationMinutes;
    }

    public boolean isSoulbindInfinite() {
        return soulbindInfinite;
    }

    public int getSoulbindApplyDuration() {
        return soulbindApplyDuration;
    }

    public int getSoulbindStacks() {
        return soulbindStacks;
    }
    public int getCastTimeSeconds() { return castTimeSeconds; }
    public int getLockSeconds() { return lockSeconds; }
    public int getExtraCastSeconds() { return extraCastSeconds; }
}