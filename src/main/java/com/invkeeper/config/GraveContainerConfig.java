package com.invkeeper.config;

/**
 * 무덤 컨테이너 블록 설정.
 */
public final class GraveContainerConfig {

    public enum ContainerType { VANILLA, CUSTOM_BLOCK }

    private final ContainerType type;
    private final String vanillaMaterial; // Material enum name
    private final String customBlockId;
    private final String customBlockProvider;

    public GraveContainerConfig(ContainerType type, String vanillaMaterial,
                                String customBlockId, String customBlockProvider) {
        this.type = type;
        this.vanillaMaterial = vanillaMaterial;
        this.customBlockId = customBlockId;
        this.customBlockProvider = customBlockProvider;
    }

    public ContainerType getType() { return type; }
    public String getVanillaMaterial() { return vanillaMaterial; }
    public String getCustomBlockId() { return customBlockId; }
    public String getCustomBlockProvider() { return customBlockProvider; }
}
