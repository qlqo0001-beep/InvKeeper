package com.invkeeper.config;

import org.bukkit.Material;
import java.util.List;
import java.util.Objects;

/**
 * gui.yml에서 로드하는 무덤 GUI 설정.
 */
public final class GraveGuiConfig {

    private final String title;
    private final Material expBottleMaterial;
    private final String expBottleName;
    private final List<String> expBottleLore;
    private final int recoverAllSlot;
    private final Material recoverAllMaterial;
    private final String recoverAllName;
    private final List<String> recoverAllLore;
    private final String recoveryStatusOwner;
    private final String recoveryStatusLooter;
    private final String recoveryStatusNone;

    public GraveGuiConfig(String title,
                          Material expBottleMaterial, String expBottleName, List<String> expBottleLore,
                          int recoverAllSlot, Material recoverAllMaterial,
                          String recoverAllName, List<String> recoverAllLore,
                          String recoveryStatusOwner, String recoveryStatusLooter, String recoveryStatusNone) {
        this.title = Objects.requireNonNull(title, "title");
        this.expBottleMaterial = Objects.requireNonNullElse(expBottleMaterial, Material.EXPERIENCE_BOTTLE);
        this.expBottleName = Objects.requireNonNullElse(expBottleName, "&e경험치 {amount}exp");
        this.expBottleLore = List.copyOf(Objects.requireNonNullElse(expBottleLore, List.of()));
        this.recoverAllSlot = recoverAllSlot >= 0 ? recoverAllSlot : 49;
        this.recoverAllMaterial = Objects.requireNonNullElse(recoverAllMaterial, Material.NETHER_STAR);
        this.recoverAllName = Objects.requireNonNullElse(recoverAllName, "&a&l모두 회수");
        this.recoverAllLore = List.copyOf(Objects.requireNonNullElse(recoverAllLore, List.of()));
        this.recoveryStatusOwner = Objects.requireNonNullElse(recoveryStatusOwner, "&a&l[ 주인 회수 완료 ]");
        this.recoveryStatusLooter = Objects.requireNonNullElse(recoveryStatusLooter, "&c&l[ 도굴꾼 회수 ]");
        this.recoveryStatusNone = Objects.requireNonNullElse(recoveryStatusNone, "&7[ 미회수 ]");
    }

    public String getTitle() { return title; }
    public Material getExpBottleMaterial() { return expBottleMaterial; }
    public String getExpBottleName() { return expBottleName; }
    public List<String> getExpBottleLore() { return expBottleLore; }
    public int getRecoverAllSlot() { return recoverAllSlot; }
    public Material getRecoverAllMaterial() { return recoverAllMaterial; }
    public String getRecoverAllName() { return recoverAllName; }
    public List<String> getRecoverAllLore() { return recoverAllLore; }
    public String getRecoveryStatusOwner() { return recoveryStatusOwner; }
    public String getRecoveryStatusLooter() { return recoveryStatusLooter; }
    public String getRecoveryStatusNone() { return recoveryStatusNone; }
}
