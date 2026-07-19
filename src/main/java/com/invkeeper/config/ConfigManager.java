package com.invkeeper.config;

import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfigManager {
    private final Plugin plugin;
    private final Map<String, WorldRule> worldRules = new LinkedHashMap<>();
    private WorldRule defaultWorldRule = new WorldRule(0, 0);
    private final List<PermissionRule> permissionRules = new ArrayList<>();

    private boolean forceKeepInventoryFalse = true;

    private boolean useMmoConsumable;
    private String mmoConsumableType;
    private String mmoConsumableId;
    private boolean useVanillaConsumable;
    private Material vanillaConsumableMaterial;
    private String vanillaConsumableName;
    private List<String> vanillaConsumableLore;
    private Integer vanillaConsumableCustomModelData;

    private int timedDurationMinutes;
    private boolean useMmoTimed;
    private String mmoTimedType;
    private String mmoTimedId;
    private boolean useVanillaTimed;
    private Material vanillaTimedMaterial;
    private String vanillaTimedName;
    private List<String> vanillaTimedLore;
    private Integer vanillaTimedCustomModelData;

    private String deathMessage;
    private String protectedMessage;
    private String timedAlreadyActiveMessage;
    private String timedActivatedMessage;
    private String timedRemainingFiveMinutesMessage;
    private String timedRemainingOneMinuteMessage;

    public ConfigManager(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        forceKeepInventoryFalse = getBooleanSafe(config, "force-keep-inventory-false", true);

        worldRules.clear();
        defaultWorldRule = new WorldRule(0, 0);
        ConfigurationSection worldSection = config.getConfigurationSection("rules.world");
        if (worldSection == null) {
            Bukkit.getLogger().warning("[InvKeeper] rules.world 섹션을 찾을 수 없습니다. 기본값을 사용합니다.");
        } else {
            for (String key : worldSection.getKeys(false)) {
                ConfigurationSection ruleSection = worldSection.getConfigurationSection(key);
                if (ruleSection == null) {
                    Bukkit.getLogger().warning("[InvKeeper] rules.world." + key + " 항목이 올바르지 않습니다. 건너뜁니다.");
                    continue;
                }
                double inventory = parseDoubleObject(ruleSection.get("inventory-drop-percent"), 0);
                double exp = parseDoubleObject(ruleSection.get("exp-drop-percent"), 0);
                WorldRule rule = new WorldRule(inventory, exp);
                if ("default".equalsIgnoreCase(key)) {
                    defaultWorldRule = rule;
                }
                worldRules.put(key, rule);
            }
        }
        if (!worldRules.containsKey("default")) {
            Bukkit.getLogger().warning("[InvKeeper] rules.world.default가 config에 없습니다. 기본값 0/0을 사용합니다.");
        }

        permissionRules.clear();
        ConfigurationSection permissionsSection = config.getConfigurationSection("rules");
        if (permissionsSection != null) {
            List<?> rawPermissions = permissionsSection.getList("permissions", Collections.emptyList());
            for (Object raw : rawPermissions) {
                if (!(raw instanceof Map<?, ?>)) {
                    continue;
                }
                Map<?, ?> map = (Map<?, ?>) raw;
                String permission = getStringObject(map.get("permission"), "").trim();
                if (permission.isEmpty()) {
                    continue;
                }
                int priority = parseIntObject(map.get("priority"), 0);
                double inventory = parseDoubleObject(map.get("inventory-drop-percent"), 0);
                double exp = parseDoubleObject(map.get("exp-drop-percent"), 0);
                permissionRules.add(new PermissionRule(permission, priority, inventory, exp));
            }
        }
        Map<Integer, List<String>> dup = new LinkedHashMap<>();
        for (PermissionRule rule : permissionRules) {
            dup.computeIfAbsent(rule.getPriority(), k -> new ArrayList<>()).add(rule.getPermission());
        }
        for (Map.Entry<Integer, List<String>> entry : dup.entrySet()) {
            if (entry.getValue().size() > 1) {
                Bukkit.getLogger().severe("[InvKeeper] priority " + entry.getKey() + "이 '" + String.join("'와 '", entry.getValue()) + "'에서 중복되었습니다! config.yml을 확인해주세요.");
            }
        }

        permissionRules.sort((left, right) -> Integer.compare(right.getPriority(), left.getPriority()));

        ConfigurationSection consumableSection = config.getConfigurationSection("protection-items.consumable-item");
        if (consumableSection == null) {
            Bukkit.getLogger().warning("[InvKeeper] protection-items.consumable-item 설정을 찾을 수 없습니다. 기본값으로 무시됩니다.");
            useMmoConsumable = false;
            useVanillaConsumable = false;
        } else {
            useMmoConsumable = getBooleanSafe(consumableSection, "use-mmoitems", false);
            mmoConsumableType = getStringSafe(consumableSection, "mmoitems-type", "");
            mmoConsumableId = getStringSafe(consumableSection, "mmoitems-id", "");
            useVanillaConsumable = getBooleanSafe(consumableSection, "use-vanilla", false);
            vanillaConsumableMaterial = parseMaterial(getStringSafe(consumableSection, "vanilla-material", "PAPER"), Material.PAPER);
            vanillaConsumableName = getStringSafe(consumableSection, "vanilla-name", "&b인벤토리 보호권 &7(소모용)");
            vanillaConsumableLore = getStringListSafe(consumableSection, "vanilla-lore");
            vanillaConsumableCustomModelData = parseOptionalInt(consumableSection, "custom-model-data");
        }

        ConfigurationSection timedSection = config.getConfigurationSection("protection-items.timed-item");
        if (timedSection == null) {
            Bukkit.getLogger().warning("[InvKeeper] protection-items.timed-item 설정을 찾을 수 없습니다. 기본값으로 무시됩니다.");
            useMmoTimed = false;
            useVanillaTimed = false;
            timedDurationMinutes = 30;
        } else {
            timedDurationMinutes = parseIntObject(timedSection.get("duration-minutes"), 30);
            if (timedDurationMinutes < 0) {
                timedDurationMinutes = 30;
            }
            useMmoTimed = getBooleanSafe(timedSection, "use-mmoitems", false);
            mmoTimedType = getStringSafe(timedSection, "mmoitems-type", "");
            mmoTimedId = getStringSafe(timedSection, "mmoitems-id", "");
            useVanillaTimed = getBooleanSafe(timedSection, "use-vanilla", false);
            vanillaTimedMaterial = parseMaterial(getStringSafe(timedSection, "vanilla-material", "PAPER"), Material.PAPER);
            vanillaTimedName = getStringSafe(timedSection, "vanilla-name", "&b인벤토리 보호권 &7(시간형)");
            vanillaTimedLore = getStringListSafe(timedSection, "vanilla-lore");
            vanillaTimedCustomModelData = parseOptionalInt(timedSection, "custom-model-data");
        }

        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection == null) {
            Bukkit.getLogger().warning("[InvKeeper] messages 설정을 찾을 수 없습니다. 기본 메시지를 사용합니다.");
            deathMessage = "&c인벤토리 {inv_percent}%, 경험치 {exp_percent}%를 잃었습니다. 아이템 {items_dropped}개, 경험치 {exp_dropped}점 손실";
            protectedMessage = "&a보호 아이템 덕분에 아무것도 잃지 않았습니다!";
            timedAlreadyActiveMessage = "&e이미 보호 상태입니다. (남은 시간: {remaining})";
            timedActivatedMessage = "&a인벤토리 보호가 {duration}분간 활성화되었습니다.";
            timedRemainingFiveMinutesMessage = "&e보호 상태가 5분 남았습니다. 남은 시간: {remaining}";
            timedRemainingOneMinuteMessage = "&e보호 상태가 1분 남았습니다. 남은 시간: {remaining}";
        } else {
            deathMessage = getStringSafe(messagesSection, "death", "&c인벤토리 {inv_percent}%, 경험치 {exp_percent}%를 잃었습니다.");
            protectedMessage = getStringSafe(messagesSection, "protected", "&a보호 아이템 덕분에 아무것도 잃지 않았습니다!");
            timedAlreadyActiveMessage = getStringSafe(messagesSection, "timed-already-active", "&e이미 보호 상태입니다. (남은 시간: {remaining})");
            timedActivatedMessage = getStringSafe(messagesSection, "timed-activated", "&a인벤토리 보호가 {duration}분간 활성화되었습니다.");
            timedRemainingFiveMinutesMessage = getStringSafe(messagesSection, "timed-remaining-five-minutes", "&e보호 상태가 5분 남았습니다. 남은 시간: {remaining}");
            timedRemainingOneMinuteMessage = getStringSafe(messagesSection, "timed-remaining-one-minute", "&e보호 상태가 1분 남았습니다. 남은 시간: {remaining}");
        }
    }

    public Map<String, WorldRule> getWorldRules() {
        return Collections.unmodifiableMap(worldRules);
    }

    public WorldRule getDefaultWorldRule() {
        return defaultWorldRule;
    }

    public List<PermissionRule> getPermissionRules() {
        return Collections.unmodifiableList(permissionRules);
    }

    public boolean isForceKeepInventoryFalse() {
        return forceKeepInventoryFalse;
    }

    public boolean isUseMmoConsumable() {
        return useMmoConsumable;
    }

    public String getMmoConsumableType() {
        return mmoConsumableType;
    }

    public String getMmoConsumableId() {
        return mmoConsumableId;
    }

    public boolean isUseVanillaConsumable() {
        return useVanillaConsumable;
    }

    public Material getVanillaConsumableMaterial() {
        return vanillaConsumableMaterial;
    }

    public String getVanillaConsumableName() {
        return vanillaConsumableName;
    }

    public List<String> getVanillaConsumableLore() {
        return Collections.unmodifiableList(vanillaConsumableLore);
    }

    public Integer getVanillaConsumableCustomModelData() {
        return vanillaConsumableCustomModelData;
    }

    public int getTimedDurationMinutes() {
        return timedDurationMinutes;
    }

    public boolean isUseMmoTimed() {
        return useMmoTimed;
    }

    public String getMmoTimedType() {
        return mmoTimedType;
    }

    public String getMmoTimedId() {
        return mmoTimedId;
    }

    public boolean isUseVanillaTimed() {
        return useVanillaTimed;
    }

    public Material getVanillaTimedMaterial() {
        return vanillaTimedMaterial;
    }

    public String getVanillaTimedName() {
        return vanillaTimedName;
    }

    public List<String> getVanillaTimedLore() {
        return Collections.unmodifiableList(vanillaTimedLore);
    }

    public Integer getVanillaTimedCustomModelData() {
        return vanillaTimedCustomModelData;
    }

    public String getDeathMessage() {
        return deathMessage;
    }

    public String getProtectedMessage() {
        return protectedMessage;
    }

    public String getTimedAlreadyActiveMessage() {
        return timedAlreadyActiveMessage;
    }

    public String getTimedActivatedMessage() {
        return timedActivatedMessage;
    }

    public String getTimedRemainingFiveMinutesMessage() {
        return timedRemainingFiveMinutesMessage;
    }

    public String getTimedRemainingOneMinuteMessage() {
        return timedRemainingOneMinuteMessage;
    }

    public double[] resolveDropPercents(org.bukkit.entity.Player player, String worldName) {
        PermissionRule selected = resolveEffectiveRule(player, worldName);
        return new double[]{selected.getInventoryDropPercent(), selected.getExpDropPercent()};
    }

    public PermissionRule resolveEffectiveRule(org.bukkit.entity.Player player, String worldName) {
        WorldRule worldRule = resolveWorldRule(worldName);
        List<PermissionRule> candidates = new ArrayList<>();
        candidates.add(new PermissionRule("__world__", 0, worldRule.getInventoryDropPercent(), worldRule.getExpDropPercent()));
        for (PermissionRule permissionRule : permissionRules) {
            if (player.hasPermission(permissionRule.getPermission())) {
                candidates.add(permissionRule);
            }
        }
        candidates.sort((left, right) -> Integer.compare(right.getPriority(), left.getPriority()));
        return candidates.get(0);
    }

    public WorldRule resolveWorldRule(String worldName) {
        if (worldName == null) {
            return defaultWorldRule;
        }
        WorldRule worldRule = worldRules.get(worldName);
        return worldRule != null ? worldRule : defaultWorldRule;
    }

    private static boolean getBooleanSafe(ConfigurationSection section, String path, boolean defaultValue) {
        if (section == null) {
            return defaultValue;
        }
        try {
            if (!section.contains(path)) {
                return defaultValue;
            }
            return section.getBoolean(path, defaultValue);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[InvKeeper] 설정값 '" + path + "'을(를) 불러오는 중 오류가 발생했습니다. 기본값을 사용합니다.");
            return defaultValue;
        }
    }

    private static String getStringSafe(ConfigurationSection section, String path, String defaultValue) {
        if (section == null) {
            return defaultValue;
        }
        try {
            String value = section.getString(path);
            if (value == null) {
                return defaultValue;
            }
            return value;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[InvKeeper] 설정값 '" + path + "'을(를) 불러오는 중 오류가 발생했습니다. 기본값을 사용합니다.");
            return defaultValue;
        }
    }

    private static List<String> getStringListSafe(ConfigurationSection section, String path) {
        if (section == null) {
            return List.of();
        }
        try {
            List<String> list = section.getStringList(path);
            return list != null ? List.copyOf(list) : List.of();
        } catch (Exception e) {
            Bukkit.getLogger().warning("[InvKeeper] 문자열 목록 '" + path + "'을(를) 불러오는 중 오류가 발생했습니다.");
            return List.of();
        }
    }

    private static Material parseMaterial(String raw, Material fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Material.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[InvKeeper] 올바르지 않은 재질 이름: " + raw + ". " + fallback + "를 사용합니다.");
            return fallback;
        }
    }

    private static Integer parseOptionalInt(ConfigurationSection section, String path) {
        if (section == null || !section.contains(path)) {
            return null;
        }
        Object value = section.get(path);
        int parsed = parseIntObject(value, Integer.MIN_VALUE);
        return parsed == Integer.MIN_VALUE ? null : parsed;
    }

    private static String getStringObject(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }

    private static int parseIntObject(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static double parseDoubleObject(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
