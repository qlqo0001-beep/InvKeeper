package com.invkeeper.config;

import com.invkeeper.config.ProtectionItemConfig;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ConfigManager {
    private final Plugin plugin;
    private final Map<String, WorldRule> worldRules = new LinkedHashMap<>();
    private WorldRule defaultWorldRule = new WorldRule(0, 0);
    private final List<PermissionRule> permissionRules = new ArrayList<>();

    private boolean forceKeepInventoryFalse = true;

    private final Map<String, ProtectionItemConfig> protectionItemConfigs = new LinkedHashMap<>();

    private String deathMessage;
    private String protectedMessage;
    private String timedProtectedMessage;
    private String timedAlreadyActiveMessage;
    private String timedActivatedMessage;
    private String timedRemainingFiveMinutesMessage;
    private String timedRemainingOneMinuteMessage;
    private String timeFormat;

    private String soulboundAppliedMessage;
    private String soulboundExtendedMessage;
    private String soulboundUnboundMessage;
    private String soulboundCantPickupMessage;
    private String soulboundForcedDroppedMessage;
    private String soulboundAlreadyInfiniteMessage;
    private String soulboundLoreFormat;
    private String soulboundLoreFormatStack;
    private String soulboundConflictTypeMessage;
    private String soulboundMaxStackMessage;
    private String timezone;
    private int soulbindScanBatches = 5;
    private int maxSoulbindStack = -1;
    private int soulbindPickupMessageCooldownSeconds = 5;
    private int soulbindUseMessageCooldownSeconds = 3;

    public ConfigManager(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void load() {
        loadConfig();
        loadItems();
        loadMessages();
        MessageUtil.setTimezone(timezone);
    }

    private void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        forceKeepInventoryFalse = getBooleanSafe(config, "force-keep-inventory-false", true);
        soulbindScanBatches = Math.max(1, getIntSafe(config, "soulbind-scan-batches", 5));
        timezone = getStringSafe(config, "timezone", "Asia/Seoul");
        maxSoulbindStack = parseIntObject(config.get("max-soulbind-stack"), -1);
        soulbindPickupMessageCooldownSeconds = Math.max(0, getIntSafe(config, "soulbind-pickup-message-cooldown-seconds", 5));
        soulbindUseMessageCooldownSeconds = Math.max(0, getIntSafe(config, "soulbind-use-message-cooldown-seconds", 3));

        worldRules.clear();
        defaultWorldRule = new WorldRule(0, 0);
        ConfigurationSection worldSection = config.getConfigurationSection("rules.world");
        if (worldSection == null) {
            Bukkit.getLogger().warning("[InvKeeper] rules.world 섹션을 찾을 수 없습니다.");
        } else {
            for (String key : worldSection.getKeys(false)) {
                ConfigurationSection ruleSection = worldSection.getConfigurationSection(key);
                if (ruleSection == null) continue;
                double inventory = parseDoubleObject(ruleSection.get("inventory-drop-percent"), 0);
                double exp = parseDoubleObject(ruleSection.get("exp-drop-percent"), 0);
                WorldRule rule = new WorldRule(inventory, exp);
                if ("default".equalsIgnoreCase(key)) defaultWorldRule = rule;
                worldRules.put(key, rule);
                // Warn if world name doesn't exist (but don't block - world might not be loaded yet)
                if (!"default".equalsIgnoreCase(key) && Bukkit.getWorld(key) == null) {
                    Bukkit.getLogger().warning("[InvKeeper] 월드 규칙 '" + key + "'에 해당하는 월드가 현재 로드되어 있지 않습니다. (나중에 로드될 수 있음)");
                }
            }
        }

        permissionRules.clear();
        ConfigurationSection permissionsSection = config.getConfigurationSection("rules");
        if (permissionsSection != null) {
            List<?> rawPermissions = permissionsSection.getList("permissions", Collections.emptyList());
            for (Object raw : rawPermissions) {
                if (!(raw instanceof Map<?, ?>)) continue;
                Map<?, ?> map = (Map<?, ?>) raw;
                String permission = getStringObject(map.get("permission"), "").trim();
                if (permission.isEmpty()) continue;
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
                Bukkit.getLogger().severe("[InvKeeper] priority " + entry.getKey() + " 중복: " + String.join(", ", entry.getValue()));
            }
        }
        permissionRules.sort((left, right) -> Integer.compare(right.getPriority(), left.getPriority()));
    }

    private void loadItems() {
        protectionItemConfigs.clear();
        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            Bukkit.getLogger().warning("[InvKeeper] items.yml을 찾을 수 없습니다.");
            return;
        }
        FileConfiguration itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        ConfigurationSection itemsSection = itemsConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            Bukkit.getLogger().warning("[InvKeeper] items.yml에 items 섹션이 없습니다.");
            return;
        }
        for (String itemKey : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
            if (itemSection == null) continue;
            ProtectionItemConfig itemConfig = parseProtectionItemConfig(itemKey, itemSection);
            if (itemConfig != null) {
                protectionItemConfigs.put(itemKey, itemConfig);
            }
        }
        if (protectionItemConfigs.isEmpty()) {
            Bukkit.getLogger().warning("[InvKeeper] items.yml에 정의된 아이템이 없습니다.");
        }
    }

    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            Bukkit.getLogger().warning("[InvKeeper] messages.yml을 찾을 수 없습니다. 기본 메시지를 사용합니다.");
            setDefaultMessages();
            return;
        }
        FileConfiguration messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        deathMessage = getStringSafe(messagesConfig, "death", "&c인벤토리 {inv_percent}% ({items_dropped}개), 경험치 {exp_percent}% ({exp_dropped}exp)를 잃었습니다.");
        protectedMessage = getStringSafe(messagesConfig, "protected", "&a인벤토리 보호권을 소모하여 아무것도 잃지 않았습니다!");
        timedProtectedMessage = getStringSafe(messagesConfig, "timed-protected", "&a인벤토리 보호 상태 임으로 아무것도 잃지 않았습니다! (남은 시간: {remaining})");
        timedAlreadyActiveMessage = getStringSafe(messagesConfig, "timed-already-active", "&e이미 보호 상태입니다. (남은 시간: {remaining})");
        timedActivatedMessage = getStringSafe(messagesConfig, "timed-activated", "&a인벤토리 보호가 {duration}분간 활성화되었습니다.");
        timedRemainingFiveMinutesMessage = getStringSafe(messagesConfig, "timed-remaining-five-minutes", "&e보호 상태가 5분 남았습니다. 남은 시간: {remaining}");
        timedRemainingOneMinuteMessage = getStringSafe(messagesConfig, "timed-remaining-one-minute", "&e보호 상태가 1분 남았습니다. 남은 시간: {remaining}");
        timeFormat = getStringSafe(messagesConfig, "time-format", "{minutes}분 {seconds_padded}초");
        soulboundAppliedMessage = getStringSafe(messagesConfig, "soulbound-applied", "&a아이템에 영혼각인이 적용되었습니다. (대상: {owner}, 지속시간: {remaining})");
        soulboundExtendedMessage = getStringSafe(messagesConfig, "soulbound-extended", "&a이미 각인된 아이템의 유지시간이 연장되었습니다. (남은 시간: {remaining})");
        soulboundUnboundMessage = getStringSafe(messagesConfig, "soulbound-unbound", "&a아이템의 영혼각인이 해제되었습니다.");
        soulboundCantPickupMessage = getStringSafe(messagesConfig, "soulbound-cant-pickup", "&c{item_name}은(는) {owner}의 각인 아이템입니다. 획득할 수 없습니다.");
        soulboundForcedDroppedMessage = getStringSafe(messagesConfig, "soulbound-forced-dropped", "&e이 플레이어가 소유자가 아니라서 아이템을 강제로 드랍했습니다.");
        soulboundAlreadyInfiniteMessage = getStringSafe(messagesConfig, "soulbound-already-infinite", "&e이 아이템은 이미 무한 각인 상태입니다.");
        soulboundLoreFormat = getStringSafe(messagesConfig, "soulbound-lore-format", "&7각인: &b{owner} &7| 만료: &b{expiry}");
        soulboundLoreFormatStack = getStringSafe(messagesConfig, "soulbound-lore-format-stack", "&7각인: &b{owner} &7| 횟수: &b{stacks}");
        soulboundConflictTypeMessage = getStringSafe(messagesConfig, "soulbound-conflict-type", "&c이 아이템은 {type} 각인 상태입니다. 다른 타입의 각인을 적용할 수 없습니다.");
        soulboundMaxStackMessage = getStringSafe(messagesConfig, "soulbound-max-stack", "&c최대 각인 스택({max})을 초과하여 적용할 수 없습니다.");
    }

    private void setDefaultMessages() {
        deathMessage = "&c인벤토리 {inv_percent}% ({items_dropped}개), 경험치 {exp_percent}% ({exp_dropped}exp)를 잃었습니다.";
        protectedMessage = "&a인벤토리 보호권을 소모하여 아무것도 잃지 않았습니다!";
        timedProtectedMessage = "&a인벤토리 보호 상태 임으로 아무것도 잃지 않았습니다! (남은 시간: {remaining})";
        timedAlreadyActiveMessage = "&e이미 보호 상태입니다. (남은 시간: {remaining})";
        timedActivatedMessage = "&a인벤토리 보호가 {duration}분간 활성화되었습니다.";
        timedRemainingFiveMinutesMessage = "&e보호 상태가 5분 남았습니다. 남은 시간: {remaining}";
        timedRemainingOneMinuteMessage = "&e보호 상태가 1분 남았습니다. 남은 시간: {remaining}";
        timeFormat = "{minutes}분 {seconds_padded}초";
        soulboundAppliedMessage = "&a아이템에 영혼각인이 적용되었습니다. (대상: {owner}, 지속시간: {remaining})";
        soulboundExtendedMessage = "&a이미 각인된 아이템의 유지시간이 연장되었습니다. (남은 시간: {remaining})";
        soulboundUnboundMessage = "&a아이템의 영혼각인이 해제되었습니다.";
        soulboundCantPickupMessage = "&c{item_name}은(는) {owner}의 각인 아이템입니다. 획득할 수 없습니다.";
        soulboundForcedDroppedMessage = "&e이 플레이어가 소유자가 아니라서 아이템을 강제로 드랍했습니다.";
        soulboundAlreadyInfiniteMessage = "&e이 아이템은 이미 무한 각인 상태입니다.";
        soulboundLoreFormat = "&7각인: &b{owner} &7| 만료: &b{expiry}";
        soulboundLoreFormatStack = "&7각인: &b{owner} &7| 횟수: &b{stacks}";
        soulboundConflictTypeMessage = "&c이 아이템은 {type} 각인 상태입니다. 다른 타입의 각인을 적용할 수 없습니다.";
        soulboundMaxStackMessage = "&c최대 각인 스택({max})을 초과하여 적용할 수 없습니다.";
    }

    public Map<String, WorldRule> getWorldRules() { return Collections.unmodifiableMap(worldRules); }
    public WorldRule getDefaultWorldRule() { return defaultWorldRule; }
    public List<PermissionRule> getPermissionRules() { return Collections.unmodifiableList(permissionRules); }
    public boolean isForceKeepInventoryFalse() { return forceKeepInventoryFalse; }
    public int getSoulbindScanBatches() { return soulbindScanBatches; }
    public int getMaxSoulbindStack() { return maxSoulbindStack; }
    public int getSoulbindPickupMessageCooldownSeconds() { return soulbindPickupMessageCooldownSeconds; }
    public int getSoulbindUseMessageCooldownSeconds() { return soulbindUseMessageCooldownSeconds; }

    public String getDeathMessage() { return deathMessage; }
    public String getProtectedMessage() { return protectedMessage; }
    public String getTimedProtectedMessage() { return timedProtectedMessage; }
    public String getTimedAlreadyActiveMessage() { return timedAlreadyActiveMessage; }
    public String getTimedActivatedMessage() { return timedActivatedMessage; }
    public String getTimedRemainingFiveMinutesMessage() { return timedRemainingFiveMinutesMessage; }
    public String getTimedRemainingOneMinuteMessage() { return timedRemainingOneMinuteMessage; }
    public String getTimeFormat() { return timeFormat == null ? "{minutes}분 {seconds_padded}초" : timeFormat; }
    public String getSoulboundAppliedMessage() { return soulboundAppliedMessage; }
    public String getSoulboundExtendedMessage() { return soulboundExtendedMessage; }
    public String getSoulboundUnboundMessage() { return soulboundUnboundMessage; }
    public String getSoulboundCantPickupMessage() { return soulboundCantPickupMessage; }
    public String getSoulboundForcedDroppedMessage() { return soulboundForcedDroppedMessage; }
    public String getSoulboundAlreadyInfiniteMessage() { return soulboundAlreadyInfiniteMessage; }
    public String getSoulboundLoreFormat() { return soulboundLoreFormat; }
    public String getSoulboundLoreFormatStack() { return soulboundLoreFormatStack; }
    public String getSoulboundConflictTypeMessage() { return soulboundConflictTypeMessage; }
    public String getSoulboundMaxStackMessage() { return soulboundMaxStackMessage; }

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
        if (worldName == null) return defaultWorldRule;
        WorldRule worldRule = worldRules.get(worldName);
        return worldRule != null ? worldRule : defaultWorldRule;
    }

    public Map<String, ProtectionItemConfig> getProtectionItemConfigs() { return Collections.unmodifiableMap(protectionItemConfigs); }
    public ProtectionItemConfig getProtectionItemConfig(String itemKey) { return protectionItemConfigs.get(itemKey); }
    public List<String> getProtectionItemKeys() { return List.copyOf(protectionItemConfigs.keySet()); }

    public List<ProtectionItemConfig> getProtectionItemConfigsByKind(ProtectionItemConfig.Kind kind) {
        List<ProtectionItemConfig> results = new ArrayList<>();
        for (ProtectionItemConfig config : protectionItemConfigs.values()) {
            if (config.getKind() == kind) results.add(config);
        }
        return results;
    }

    private ProtectionItemConfig parseProtectionItemConfig(String itemKey, ConfigurationSection section) {
        if (section == null) return null;

        ProtectionItemConfig.Kind kind = parseKind(section, itemKey);
        if (kind == null) return null;

        boolean[] useTypes = parseUseType(section);
        boolean useMmo = useTypes[0];
        boolean useVanilla = useTypes[1];

        String mmoItemsType = getStringSafe(section, "mmoitems-type", "");
        String mmoItemsId = getStringSafe(section, "mmoitems-id", "");
        int durationMinutes = (kind == ProtectionItemConfig.Kind.TIMED_PROTECTION)
                ? Math.max(parseIntObject(section.get("duration-minutes"), 30), 0)
                : 0;

        Material vanillaMaterial = parseMaterial(getStringSafe(section, "vanilla-material", "PAPER"), Material.PAPER);
        String vanillaName = getStringSafe(section, "vanilla-name", "");
        List<String> vanillaLore = getStringListSafe(section, "vanilla-lore");
        Integer customModelData = parseOptionalInt(section, "custom-model-data");

        SoulbindFields soulbind = parseSoulbindFields(section, kind);
        int applyDuration = parseSoulbindApplyDuration(section, kind);
        int stacks = parseSoulbindStacks(section, kind);

        return new ProtectionItemConfig(
                itemKey, kind, useMmo, useVanilla,
                mmoItemsType, mmoItemsId, durationMinutes,
                vanillaMaterial, vanillaName, vanillaLore, customModelData,
                soulbind.enabled, soulbind.durationMinutes, soulbind.infinite,
                applyDuration, stacks);
    }

    /** Small typed holder for soulbind parse results (avoids mixing boolean/int in an array). */
    private static final class SoulbindFields {
        final boolean enabled;
        final int durationMinutes;
        final boolean infinite;

        SoulbindFields(boolean enabled, int durationMinutes, boolean infinite) {
            this.enabled = enabled;
            this.durationMinutes = durationMinutes;
            this.infinite = infinite;
        }
    }

    private ProtectionItemConfig.Kind parseKind(ConfigurationSection section, String itemKey) {
        String kindRaw = getStringSafe(section, "kind", "").trim();
        if (kindRaw.isEmpty()) {
            plugin.getLogger().warning("[InvKeeper] '" + itemKey + "'의 kind 값이 비어있습니다.");
            return null;
        }
        // 하위 호환: SOULBIND_TOOL → SOULBIND_TOOL_TIME
        if ("SOULBIND_TOOL".equalsIgnoreCase(kindRaw)) {
            plugin.getLogger().info("[InvKeeper] '" + itemKey + "'의 kind가 SOULBIND_TOOL입니다. SOULBIND_TOOL_TIME으로 자동 변환합니다.");
            kindRaw = "SOULBIND_TOOL_TIME";
        }
        try {
            return ProtectionItemConfig.Kind.valueOf(kindRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[InvKeeper] '" + itemKey + "'의 kind 값이 올바르지 않습니다: " + kindRaw);
            return null;
        }
    }

    private boolean[] parseUseType(ConfigurationSection section) {
        boolean useMmo = false;
        boolean useVanilla = false;
        String useType = getStringSafe(section, "use-type", "").trim();
        if (!useType.isEmpty()) {
            String normalized = useType.toLowerCase(Locale.ROOT).replace(" ", "");
            if (normalized.contains("mmo")) useMmo = true;
            if (normalized.contains("vanilla")) useVanilla = true;
        } else {
            useMmo = section.contains("mmoitems-type") || section.contains("mmoitems-id");
            useVanilla = section.contains("vanilla-material") || section.contains("vanilla-name") || section.contains("vanilla-lore");
        }
        return new boolean[]{useMmo, useVanilla};
    }

    private SoulbindFields parseSoulbindFields(ConfigurationSection section, ProtectionItemConfig.Kind kind) {
        boolean soulbindEnabled = false;
        int soulbindDurationMinutes = 0;
        boolean soulbindInfinite = false;
        if (kind.isProtection() && section.contains("soulbind")) {
            Object raw = section.get("soulbind");
            if (raw instanceof Boolean) {
                soulbindEnabled = (Boolean) raw;
                soulbindInfinite = soulbindEnabled;
            } else if (raw instanceof ConfigurationSection) {
                ConfigurationSection sb = (ConfigurationSection) raw;
                soulbindEnabled = getBooleanSafe(sb, "enabled", false);
                soulbindDurationMinutes = parseIntObject(sb.get("duration-minutes"), 0);
                // Auto-set infinite if duration is 0 or -1
                soulbindInfinite = getBooleanSafe(sb, "infinite", false) || soulbindDurationMinutes <= 0;
            } else if (raw instanceof java.util.Map<?, ?>) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) raw;
                soulbindEnabled = parseBooleanObject(map.get("enabled"), false);
                soulbindDurationMinutes = parseIntObject(map.get("duration-minutes"), 0);
                // Auto-set infinite if duration is 0 or -1
                soulbindInfinite = parseBooleanObject(map.get("infinite"), false) || soulbindDurationMinutes <= 0;
            }
        }
        return new SoulbindFields(soulbindEnabled, soulbindDurationMinutes, soulbindInfinite);
    }

    private int parseSoulbindApplyDuration(ConfigurationSection section, ProtectionItemConfig.Kind kind) {
        // Only SOULBIND_TOOL_TIME uses apply duration
        if (kind != ProtectionItemConfig.Kind.SOULBIND_TOOL_TIME) {
            return 0;
        }
        // 하위 호환: soulbind-duration (신규) 우선, soulbind-apply-duration (구) fallback
        if (section.contains("soulbind-duration")) {
            return Math.max(parseIntObject(section.get("soulbind-duration"), 0), 0);
        }
        if (section.contains("soulbind-apply-duration")) {
            plugin.getLogger().info("[InvKeeper] '" + section.getName() + "'에 soulbind-apply-duration이 사용되었습니다. soulbind-duration 사용을 권장합니다.");
            return Math.max(parseIntObject(section.get("soulbind-apply-duration"), 0), 0);
        }
        return 0;
    }

    private int parseSoulbindStacks(ConfigurationSection section, ProtectionItemConfig.Kind kind) {
        // Only SOULBIND_TOOL_STACK uses stacks
        if (kind != ProtectionItemConfig.Kind.SOULBIND_TOOL_STACK) {
            return -1;
        }
        // Use configured value (max-soulbind-stack is checked at runtime in SoulbindInventoryListener)
        if (section.contains("soulbind-stacks")) {
            return parseIntObject(section.get("soulbind-stacks"), 1);
        }
        return 1;
    }

    private static boolean getBooleanSafe(ConfigurationSection section, String path, boolean defaultValue) {
        if (section == null) return defaultValue;
        try {
            if (!section.contains(path)) return defaultValue;
            return section.getBoolean(path, defaultValue);
        } catch (Exception e) { return defaultValue; }
    }

    private static int getIntSafe(ConfigurationSection section, String path, int defaultValue) {
        if (section == null) return defaultValue;
        try {
            if (!section.contains(path)) return defaultValue;
            return section.getInt(path, defaultValue);
        } catch (Exception e) { return defaultValue; }
    }

    private static String getStringSafe(ConfigurationSection section, String path, String defaultValue) {
        if (section == null) return defaultValue;
        try {
            String value = section.getString(path);
            return value != null ? value : defaultValue;
        } catch (Exception e) { return defaultValue; }
    }

    private static List<String> getStringListSafe(ConfigurationSection section, String path) {
        if (section == null) return List.of();
        try {
            List<String> list = section.getStringList(path);
            return list != null ? List.copyOf(list) : List.of();
        } catch (Exception e) { return List.of(); }
    }

    private Material parseMaterial(String raw, Material fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try { return Material.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[InvKeeper] 잘못된 Material 이름: '" + raw + "', 기본값 '" + fallback + "' 사용");
            return fallback;
        }
    }

    private static Integer parseOptionalInt(ConfigurationSection section, String path) {
        if (section == null || !section.contains(path)) return null;
        int parsed = parseIntObject(section.get(path), Integer.MIN_VALUE);
        return parsed == Integer.MIN_VALUE ? null : parsed;
    }

    private static String getStringObject(Object value, String defaultValue) {
        if (value == null) return defaultValue;
        return value instanceof String ? (String) value : value.toString();
    }

    private static int parseIntObject(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt(((String) value).trim()); }
            catch (NumberFormatException ignored) { return defaultValue; }
        }
        return defaultValue;
    }

    private static boolean parseBooleanObject(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) {
            String s = ((String) value).trim().toLowerCase();
            if (s.equals("true") || s.equals("yes") || s.equals("1")) return true;
            if (s.equals("false") || s.equals("no") || s.equals("0")) return false;
        }
        return defaultValue;
    }

    private static double parseDoubleObject(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble(((String) value).trim()); }
            catch (NumberFormatException ignored) { return defaultValue; }
        }
        return defaultValue;
    }
}