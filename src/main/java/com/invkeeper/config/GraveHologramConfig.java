package com.invkeeper.config;

/**
 * 무덤 홀로그램 설정.
 */
public final class GraveHologramConfig {

    private final boolean enabled;
    private final double offsetY;
    private final int updateIntervalTicks;
    private final String lineFormat;
    private final String lineFormatUnlimited;
    private final String lootingLineFormat;
    private final String lootedLineFormat;

    public GraveHologramConfig(boolean enabled, double offsetY, int updateIntervalTicks,
                               String lineFormat, String lineFormatUnlimited,
                               String lootingLineFormat, String lootedLineFormat) {
        this.enabled = enabled;
        this.offsetY = offsetY;
        this.updateIntervalTicks = updateIntervalTicks;
        this.lineFormat = lineFormat;
        this.lineFormatUnlimited = lineFormatUnlimited;
        this.lootingLineFormat = lootingLineFormat;
        this.lootedLineFormat = lootedLineFormat;
    }

    public boolean isEnabled() { return enabled; }
    public double getOffsetY() { return offsetY; }
    public int getUpdateIntervalTicks() { return updateIntervalTicks; }
    public String getLineFormat() { return lineFormat; }
    public String getLineFormatUnlimited() { return lineFormatUnlimited; }
    public String getLootingLineFormat() { return lootingLineFormat; }
    public String getLootedLineFormat() { return lootedLineFormat; }
}
