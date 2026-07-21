package com.invkeeper.config;

import org.bukkit.Material;

import java.util.List;
import java.util.Objects;

public final class ProtectionItemConfig {
    public enum Kind {
        CONSUMABLE_PROTECTION,
        TIMED_PROTECTION,
        SOULBIND_TOOL,
        SOULBIND_UNBIND_TOOL;

        public boolean isProtection() {
            return this == CONSUMABLE_PROTECTION || this == TIMED_PROTECTION;
        }

        public boolean isSoulbindTool() {
            return this == SOULBIND_TOOL || this == SOULBIND_UNBIND_TOOL;
        }

        public boolean isUnbindTool() {
            return this == SOULBIND_UNBIND_TOOL;
        }
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
    private final int soulbindApplyDuration; // For SOULBIND_TOOL: duration applied to target items

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
            int soulbindApplyDuration) {
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
}
