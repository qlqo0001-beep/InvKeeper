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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class ConfigManager {
    private final Plugin plugin;
    private final Map<String, WorldRule> worldRules = new LinkedHashMap<>();
    private WorldRule defaultWorldRule = new WorldRule(0, 0);
    private final List<PermissionRule> permissionRules = new ArrayList<>();

    private boolean forceKeepInventoryFalse = true;
    private boolean pvpProtectionItemsWork = true;
    private boolean dangerNoticeEnabled = true;

    private final Map<String, ProtectionItemConfig> protectionItemConfigs = new LinkedHashMap<>();

    private String deathMessage;
    private String protectedMessage;
    private String timedProtectedMessage;
    private String timedAlreadyActiveMessage;
    private String timedActivatedMessage;
    // 남은 시간(초) -> 알림 메시지, 남은 시간이 큰 순서로 정렬
    private Map<Long, String> timedRemainingAlerts = new TreeMap<>(Comparator.reverseOrder());
    private String timedExpiredMessage;
    private String timedExpiredOfflineMessage;
    private String timeFormat;
    private String deathPvpMessage;
    private String pvpProtectionIgnoredMessage;
    private String dangerNoticeMessage;
    private String dangerNoticePvpMessage;
    private String dangerNoticeSafeMessage;
    private String dangerNoticeProtectedMessage;
    private String dangerNoticeToggleOnMessage;
    private String dangerNoticeToggleOffMessage;
    private String dangerNoticeServerDisabledMessage;
    private String graveLockAppliedMessage;
    private String graveLootLockedMessage;

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

    // ── Grave settings ──────────────────────────────────────
    private boolean graveEnabled = true;
    private GraveContainerConfig graveContainerConfig;
    private String graveTitleFormat = "{player} 의 무덤 {death_time}";
    private String graveDeathTimeFormat = "{year}-{month}-{day} {hour}:{minute}";
    private int graveMaxPerPlayer = 5;
    private boolean graveExpireEnabled = true;
    private int graveExpireDefaultSeconds = 3600;
    private final List<GraveExpireRule> graveExpireRules = new ArrayList<>();
    private GraveHologramConfig graveHologramConfig;
    private int graveHistoryRetentionDays = 30;
    private int graveHistoryMaxEntries = 50;
    private List<String> graveDisabledWorlds = new ArrayList<>();
    private GraveGuiConfig graveGuiConfig;
    // ── Grave messages ─────────────────────────────────────
    private String graveCreatedMessage;
    private String graveNotOwnerMessage;
    private String graveOpenedMessage;
    private String graveMaxReachedMessage;
    private String graveExpiredMessage;
    private String graveFullyRecoveredMessage;
    private String graveLootStartCasterMessage;
    private String graveLootStartOwnerAlertMessage;
    private String graveLootCancelledMessage;
    private String graveLootBlockedOwnerMessage;
    private String graveLootAlreadyInProgressMessage;
    private String graveLootSelfBlockedMessage;
    private String graveLootCompleteCasterMessage;
    private String graveLootItemRequiredMessage;
    private String graveHistoryEmptiedMessage;
    private String graveHistoryNoItemsMessage;

    public ConfigManager(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void load() {
        loadConfig();
        loadItems();
        loadMessages();
        loadGraveMessages();
        loadGraveGuiConfig();
        MessageUtil.setTimezone(timezone);
    }

    private void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        forceKeepInventoryFalse = getBooleanSafe(config, "force-keep-inventory-false", true);
        pvpProtectionItemsWork = getBooleanSafe(config, "pvp.protection-items-work", true);
        dangerNoticeEnabled = getBooleanSafe(config, "danger-notice.enabled", true);
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
                Double pvpInventory = parseOptionalDoubleObject(ruleSection.get("pvp-inventory-drop-percent"));
                Double pvpExp = parseOptionalDoubleObject(ruleSection.get("pvp-exp-drop-percent"));
                WorldRule rule = new WorldRule(inventory, exp, pvpInventory, pvpExp);
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
                Double pvpInventory = parseOptionalDoubleObject(map.get("pvp-inventory-drop-percent"));
                Double pvpExp = parseOptionalDoubleObject(map.get("pvp-exp-drop-percent"));
                permissionRules.add(new PermissionRule(permission, priority, inventory, exp, pvpInventory, pvpExp));
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
        loadGraveConfig(config);
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
        timedRemainingAlerts = loadTimedRemainingAlerts(messagesConfig);
        timedExpiredMessage = getStringSafe(messagesConfig, "timed-expired", "&c인벤토리 보호가 종료되었습니다. &7이제부터 사망 시 아이템을 잃을 수 있습니다.");
        timedExpiredOfflineMessage = getStringSafe(messagesConfig, "timed-expired-offline", "&c자리를 비운 사이 인벤토리 보호가 종료되었습니다. &8(종료: {expired_at}) &7사망 시 아이템을 잃을 수 있으니 주의하세요.");
        timeFormat = getStringSafe(messagesConfig, "time-format", "{minutes}분 {seconds_padded}초");
        deathPvpMessage = getStringSafe(messagesConfig, "death-pvp", "&c{killer}에게 사망하여 인벤토리 {inv_percent}% ({items_dropped}개), 경험치 {exp_percent}% ({exp_dropped}exp)를 잃었습니다.");
        pvpProtectionIgnoredMessage = getStringSafe(messagesConfig, "pvp-protection-ignored", "&cPvP로 사망하여 보호권이 적용되지 않았습니다.");
        dangerNoticeMessage = getStringSafe(messagesConfig, "danger-notice", "&c⚠ 위험 지역 &7({world}) &f사망 시 인벤토리 {inv_percent}%, 경험치 {exp_percent}%를 잃습니다.");
        dangerNoticePvpMessage = getStringSafe(messagesConfig, "danger-notice-pvp", "&7└ PvP 사망 시: 인벤토리 {pvp_inv_percent}%, 경험치 {pvp_exp_percent}%");
        dangerNoticeSafeMessage = getStringSafe(messagesConfig, "danger-notice-safe", "&a안전 지역 &7({world}) &f사망해도 아무것도 잃지 않습니다.");
        dangerNoticeProtectedMessage = getStringSafe(messagesConfig, "danger-notice-protected", "&b└ 보호 중 &7(남은 시간: {remaining})");
        dangerNoticeToggleOnMessage = getStringSafe(messagesConfig, "danger-notice-toggle-on", "&a위험 지역 안내를 켰습니다.");
        dangerNoticeToggleOffMessage = getStringSafe(messagesConfig, "danger-notice-toggle-off", "&e위험 지역 안내를 껐습니다. &7(/invkeeper notice on 으로 다시 켤 수 있습니다)");
        dangerNoticeServerDisabledMessage = getStringSafe(messagesConfig, "danger-notice-server-disabled", "&c서버에서 위험 지역 안내 기능이 꺼져 있습니다.");
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
        loadGraveMessages();
    }

    /**
     * timed-remaining-alerts 섹션(키: 남은 시간(초), 값: 메시지)을 읽는다.
     * 섹션이 없으면 구버전 messages.yml 호환을 위해 기존 5분/1분 키를 사용한다.
     */
    private static Map<Long, String> loadTimedRemainingAlerts(ConfigurationSection messagesConfig) {
        Map<Long, String> alerts = new TreeMap<>(Comparator.reverseOrder());
        ConfigurationSection section = messagesConfig == null ? null : messagesConfig.getConfigurationSection("timed-remaining-alerts");
        if (section == null) {
            alerts.put(300L, getStringSafe(messagesConfig, "timed-remaining-five-minutes", "&e보호 상태가 5분 남았습니다. 남은 시간: {remaining}"));
            alerts.put(60L, getStringSafe(messagesConfig, "timed-remaining-one-minute", "&e보호 상태가 1분 남았습니다. 남은 시간: {remaining}"));
            return alerts;
        }
        for (String key : section.getKeys(false)) {
            long seconds;
            try {
                seconds = Long.parseLong(key.trim());
            } catch (NumberFormatException e) {
                seconds = -1;
            }
            if (seconds <= 0) {
                Bukkit.getLogger().warning("[InvKeeper] timed-remaining-alerts의 키 '" + key + "'는 1 이상의 초 단위 숫자여야 합니다. 무시합니다.");
                continue;
            }
            alerts.put(seconds, getStringSafe(section, key, ""));
        }
        return alerts;
    }

    private void setDefaultMessages() {
        deathMessage = "&c인벤토리 {inv_percent}% ({items_dropped}개), 경험치 {exp_percent}% ({exp_dropped}exp)를 잃었습니다.";
        protectedMessage = "&a인벤토리 보호권을 소모하여 아무것도 잃지 않았습니다!";
        timedProtectedMessage = "&a인벤토리 보호 상태 임으로 아무것도 잃지 않았습니다! (남은 시간: {remaining})";
        timedAlreadyActiveMessage = "&e이미 보호 상태입니다. (남은 시간: {remaining})";
        timedActivatedMessage = "&a인벤토리 보호가 {duration}분간 활성화되었습니다.";
        timedRemainingAlerts = loadTimedRemainingAlerts(null);
        timedExpiredMessage = "&c인벤토리 보호가 종료되었습니다. &7이제부터 사망 시 아이템을 잃을 수 있습니다.";
        timedExpiredOfflineMessage = "&c자리를 비운 사이 인벤토리 보호가 종료되었습니다. &8(종료: {expired_at}) &7사망 시 아이템을 잃을 수 있으니 주의하세요.";
        timeFormat = "{minutes}분 {seconds_padded}초";
        deathPvpMessage = "&c{killer}에게 사망하여 인벤토리 {inv_percent}% ({items_dropped}개), 경험치 {exp_percent}% ({exp_dropped}exp)를 잃었습니다.";
        pvpProtectionIgnoredMessage = "&cPvP로 사망하여 보호권이 적용되지 않았습니다.";
        dangerNoticeMessage = "&c⚠ 위험 지역 &7({world}) &f사망 시 인벤토리 {inv_percent}%, 경험치 {exp_percent}%를 잃습니다.";
        dangerNoticePvpMessage = "&7└ PvP 사망 시: 인벤토리 {pvp_inv_percent}%, 경험치 {pvp_exp_percent}%";
        dangerNoticeSafeMessage = "&a안전 지역 &7({world}) &f사망해도 아무것도 잃지 않습니다.";
        dangerNoticeProtectedMessage = "&b└ 보호 중 &7(남은 시간: {remaining})";
        dangerNoticeToggleOnMessage = "&a위험 지역 안내를 켰습니다.";
        dangerNoticeToggleOffMessage = "&e위험 지역 안내를 껐습니다. &7(/invkeeper notice on 으로 다시 켤 수 있습니다)";
        dangerNoticeServerDisabledMessage = "&c서버에서 위험 지역 안내 기능이 꺼져 있습니다.";
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
    public boolean isPvpProtectionItemsWork() { return pvpProtectionItemsWork; }
    public boolean isDangerNoticeEnabled() { return dangerNoticeEnabled; }
    public int getSoulbindScanBatches() { return soulbindScanBatches; }
    public int getMaxSoulbindStack() { return maxSoulbindStack; }
    public int getSoulbindPickupMessageCooldownSeconds() { return soulbindPickupMessageCooldownSeconds; }
    public int getSoulbindUseMessageCooldownSeconds() { return soulbindUseMessageCooldownSeconds; }

    public String getDeathMessage() { return deathMessage; }
    public String getProtectedMessage() { return protectedMessage; }
    public String getTimedProtectedMessage() { return timedProtectedMessage; }
    public String getTimedAlreadyActiveMessage() { return timedAlreadyActiveMessage; }
    public String getTimedActivatedMessage() { return timedActivatedMessage; }
    public Map<Long, String> getTimedRemainingAlerts() { return Collections.unmodifiableMap(timedRemainingAlerts); }
    public String getTimedExpiredMessage() { return timedExpiredMessage; }
    public String getTimedExpiredOfflineMessage() { return timedExpiredOfflineMessage; }
    public String getDeathPvpMessage() { return deathPvpMessage; }
    public String getPvpProtectionIgnoredMessage() { return pvpProtectionIgnoredMessage; }
    public String getDangerNoticeMessage() { return dangerNoticeMessage; }
    public String getDangerNoticePvpMessage() { return dangerNoticePvpMessage; }
    public String getDangerNoticeSafeMessage() { return dangerNoticeSafeMessage; }
    public String getDangerNoticeProtectedMessage() { return dangerNoticeProtectedMessage; }
    public String getDangerNoticeToggleOnMessage() { return dangerNoticeToggleOnMessage; }
    public String getDangerNoticeToggleOffMessage() { return dangerNoticeToggleOffMessage; }
    public String getDangerNoticeServerDisabledMessage() { return dangerNoticeServerDisabledMessage; }
    public String getGraveLockAppliedMessage() { return graveLockAppliedMessage; }
    public String getGraveLootLockedMessage() { return graveLootLockedMessage; }
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
    // ── Grave getters ────────────────────────────────────────
    public boolean isGraveEnabled() { return graveEnabled; }
    public GraveContainerConfig getGraveContainerConfig() { return graveContainerConfig; }
    public String getGraveTitleFormat() { return graveTitleFormat; }
    public String getGraveDeathTimeFormat() { return graveDeathTimeFormat; }
    public int getGraveMaxPerPlayer() { return graveMaxPerPlayer; }
    public boolean isGraveExpireEnabled() { return graveExpireEnabled; }
    public int getGraveExpireDefaultSeconds() { return graveExpireDefaultSeconds; }
    public List<GraveExpireRule> getGraveExpireRules() { return Collections.unmodifiableList(graveExpireRules); }
    public GraveHologramConfig getGraveHologramConfig() { return graveHologramConfig; }
    public int getGraveHistoryRetentionDays() { return graveHistoryRetentionDays; }
    public int getGraveHistoryMaxEntries() { return graveHistoryMaxEntries; }
    public List<String> getGraveDisabledWorlds() { return Collections.unmodifiableList(graveDisabledWorlds); }
    public GraveGuiConfig getGraveGuiConfig() { return graveGuiConfig; }
    public String getGraveCreatedMessage() { return graveCreatedMessage; }
    public String getGraveNotOwnerMessage() { return graveNotOwnerMessage; }
    public String getGraveOpenedMessage() { return graveOpenedMessage; }
    public String getGraveMaxReachedMessage() { return graveMaxReachedMessage; }
    public String getGraveExpiredMessage() { return graveExpiredMessage; }
    public String getGraveFullyRecoveredMessage() { return graveFullyRecoveredMessage; }
    public String getGraveLootStartCasterMessage() { return graveLootStartCasterMessage; }
    public String getGraveLootStartOwnerAlertMessage() { return graveLootStartOwnerAlertMessage; }
    public String getGraveLootCancelledMessage() { return graveLootCancelledMessage; }
    public String getGraveLootBlockedOwnerMessage() { return graveLootBlockedOwnerMessage; }
    public String getGraveLootAlreadyInProgressMessage() { return graveLootAlreadyInProgressMessage; }
    public String getGraveLootSelfBlockedMessage() { return graveLootSelfBlockedMessage; }
    public String getGraveLootCompleteCasterMessage() { return graveLootCompleteCasterMessage; }
    public String getGraveLootItemRequiredMessage() { return graveLootItemRequiredMessage; }
    public String getGraveHistoryEmptiedMessage() { return graveHistoryEmptiedMessage; }
    public String getGraveHistoryNoItemsMessage() { return graveHistoryNoItemsMessage; }
    public String getTimezone() { return timezone == null ? "Asia/Seoul" : timezone; }

    /**
     * [인벤토리 %, 경험치 %]를 반환한다. 일반(PvE) 값은 priority로 선택된 규칙의 값.
     * PvP 값은 항목(인벤토리/경험치)별로 다음 순서 중 처음 설정된 값을 사용한다:
     *   1. 적용된 권한 규칙의 pvp 값  2. 월드 규칙의 pvp 값  3. 적용된 규칙의 일반 값
     */
    public double[] resolveDropPercents(org.bukkit.entity.Player player, String worldName, boolean pvp) {
        PermissionRule selected = resolveEffectiveRule(player, worldName);
        if (!pvp) {
            return new double[]{selected.getInventoryDropPercent(), selected.getExpDropPercent()};
        }
        WorldRule worldRule = resolveWorldRule(worldName);
        return new double[]{
                firstNonNull(selected.getPvpInventoryDropPercent(), worldRule.getPvpInventoryDropPercent(), selected.getInventoryDropPercent()),
                firstNonNull(selected.getPvpExpDropPercent(), worldRule.getPvpExpDropPercent(), selected.getExpDropPercent())};
    }

    private static double firstNonNull(Double first, Double second, double fallback) {
        if (first != null) return first;
        if (second != null) return second;
        return fallback;
    }

    public PermissionRule resolveEffectiveRule(org.bukkit.entity.Player player, String worldName) {
        WorldRule worldRule = resolveWorldRule(worldName);
        List<PermissionRule> candidates = new ArrayList<>();
        candidates.add(new PermissionRule("__world__", 0, worldRule.getInventoryDropPercent(), worldRule.getExpDropPercent(),
                worldRule.getPvpInventoryDropPercent(), worldRule.getPvpExpDropPercent()));
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
        int castTimeSeconds = getIntSafe(section, "cast-time-seconds", 300);
        int lockSeconds = kind == ProtectionItemConfig.Kind.GRAVE_LOCK ? getIntSafe(section, "lock-seconds", 0) : 0;
        int extraCastSeconds = kind == ProtectionItemConfig.Kind.GRAVE_LOCK ? getIntSafe(section, "extra-cast-seconds", 0) : 0;

        return new ProtectionItemConfig(
                itemKey, kind, useMmo, useVanilla,
                mmoItemsType, mmoItemsId, durationMinutes,
                vanillaMaterial, vanillaName, vanillaLore, customModelData,
                soulbind.enabled, soulbind.durationMinutes, soulbind.infinite,
                applyDuration, stacks, castTimeSeconds, lockSeconds, extraCastSeconds);
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
        if ((kind.isProtection() || kind.isGraveLock()) && section.contains("soulbind")) {
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

    // ── Grave config loading ──────────────────────────────────

    private void loadGraveConfig(FileConfiguration config) {
        ConfigurationSection gs = config.getConfigurationSection("grave");
        if (gs == null) {
            graveContainerConfig = new GraveContainerConfig(GraveContainerConfig.ContainerType.VANILLA, "BARREL", "", "");
            graveHologramConfig = new GraveHologramConfig(true, 1.0, 20, "{player} 의 무덤 {remaining}", "{player} 의 무덤", "도굴중 {remaining}", "{looter}님이 도굴을 한 {owner}의 무덤", "&6[잠김] {remaining}");
            return;
        }
        graveEnabled = gs.getBoolean("enabled", true);
        ConfigurationSection cs = gs.getConfigurationSection("container");
        if (cs != null) {
            String typeStr = cs.getString("type", "VANILLA");
            GraveContainerConfig.ContainerType ct = "CUSTOM_BLOCK".equalsIgnoreCase(typeStr) ? GraveContainerConfig.ContainerType.CUSTOM_BLOCK : GraveContainerConfig.ContainerType.VANILLA;
            graveContainerConfig = new GraveContainerConfig(ct, cs.getString("vanilla-material", "BARREL"), cs.getString("custom-block-id", ""), cs.getString("custom-block-provider", ""));
        } else {
            graveContainerConfig = new GraveContainerConfig(GraveContainerConfig.ContainerType.VANILLA, "BARREL", "", "");
        }
        graveTitleFormat = gs.getString("title-format", "{player} 의 무덤 {death_time}");
        graveDeathTimeFormat = gs.getString("death-time-format", "{year}-{month}-{day} {hour}:{minute}");
        graveMaxPerPlayer = gs.getInt("max-graves-per-player", 5);
        ConfigurationSection es = gs.getConfigurationSection("expire");
        if (es != null) {
            graveExpireEnabled = es.getBoolean("enabled", true);
            graveExpireDefaultSeconds = es.getInt("default-seconds", 3600);
            List<?> overrides = es.getList("permission-overrides", Collections.emptyList());
            graveExpireRules.clear();
            for (Object obj : overrides) {
                if (!(obj instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) obj;
                graveExpireRules.add(new GraveExpireRule(getStringObject(m.get("permission"), ""), parseIntObject(m.get("priority"), 0), parseIntObject(m.get("seconds"), 3600)));
            }
        }
        ConfigurationSection hs = config.getConfigurationSection("grave.hologram");
        if (hs != null) {
            graveHologramConfig = new GraveHologramConfig(hs.getBoolean("enabled", true), hs.getDouble("offset-y", 1.0), hs.getInt("update-interval-ticks", 20), hs.getString("line-format", "{player} 의 무덤 {remaining}"), hs.getString("line-format-unlimited", "{player} 의 무덤"), hs.getString("looting-line-format", "도굴중 {remaining}"), hs.getString("looted-line-format", "{looter}님이 도굴을 한 {owner}의 무덤"), hs.getString("locked-line-format", "&6[잠김] {remaining}"));
        } else {
            graveHologramConfig = new GraveHologramConfig(true, 1.0, 20, "{player} 의 무덤 {remaining}", "{player} 의 무덤", "도굴중 {remaining}", "{looter}님이 도굴을 한 {owner}의 무덤", "&6[잠김] {remaining}");
        }
        ConfigurationSection hcs = gs.getConfigurationSection("history");
        if (hcs != null) { graveHistoryRetentionDays = hcs.getInt("retention-days", 30); graveHistoryMaxEntries = hcs.getInt("max-entries-per-player", 50); }
        graveDisabledWorlds = gs.getStringList("disabled-worlds").stream().map(String::toLowerCase).collect(java.util.stream.Collectors.toList());
    }
    private void loadGraveMessages() {
        FileConfiguration msg = loadYml("messages.yml");
        graveCreatedMessage = msg.getString("grave-created", "&e({world}, {x}, {y}, {z})");
        graveNotOwnerMessage = msg.getString("grave-not-owner", "&c주인은 {owner} 입니다.");
        graveOpenedMessage = msg.getString("grave-opened", "&a무덤을 열었습니다.");
        graveMaxReachedMessage = msg.getString("grave-max-reached", "&c최대 개수({max}) 도달");
        graveExpiredMessage = msg.getString("grave-expired", "&7무덤이 사라졌습니다.");
        graveFullyRecoveredMessage = msg.getString("grave-fully-recovered", "&e전량 회수");
        graveLootStartCasterMessage = msg.getString("grave-loot-start-caster", "&a도굴 시작 {seconds}초");
        graveLootStartOwnerAlertMessage = msg.getString("grave-loot-start-owner-alert", "&c누군가 당신의 무덤을 도굴중입니다!");
        graveLootCancelledMessage = msg.getString("grave-loot-cancelled", "&c{owner}이 확인하여 취소");
        graveLootBlockedOwnerMessage = msg.getString("grave-loot-blocked-owner", "&a무덤 도굴을 막았습니다!");
        graveLootAlreadyInProgressMessage = msg.getString("grave-loot-already-in-progress", "&c이미 도굴중");
        graveLockAppliedMessage = msg.getString("grave-lock-applied", "&6무덤에 자물쇠를 걸었습니다. &7(도굴 불가 {lock_seconds}초, 도굴 시간 +{extra_cast_seconds}초)");
        graveLootLockedMessage = msg.getString("grave-loot-locked", "&c자물쇠가 걸린 무덤입니다. {remaining} 후 도굴할 수 있습니다.");
        graveLootSelfBlockedMessage = msg.getString("grave-loot-self-blocked", "&c자신의 무덤 불가");
        graveLootCompleteCasterMessage = msg.getString("grave-loot-complete-caster", "&a도굴 완료!");
        graveLootItemRequiredMessage = msg.getString("grave-loot-item-required", "&c도굴 아이템 필요");
        graveHistoryEmptiedMessage = msg.getString("grave-history-emptied", "&a히스토리 아이템을 모두 회수했습니다.");
        graveHistoryNoItemsMessage = msg.getString("grave-history-no-items", "&c이 무덤에는 남아있는 아이템이 없습니다.");
    }

    private void loadGraveGuiConfig() {
        FileConfiguration gui = loadYml("gui.yml");
        ConfigurationSection ggs = gui.getConfigurationSection("grave-gui");
        if (ggs == null) { graveGuiConfig = new GraveGuiConfig("{player} 의 무덤", null, null, null, 49, null, null, null, null, null, null); return; }
        ConfigurationSection eb = ggs.getConfigurationSection("exp-bottle");
        Material ebm = org.bukkit.Material.EXPERIENCE_BOTTLE; String ebn = "&e{amount}exp"; List<String> ebl = List.of();
        if (eb != null) { ebm = parseMaterial(eb.getString("material", "EXPERIENCE_BOTTLE"), org.bukkit.Material.EXPERIENCE_BOTTLE); ebn = eb.getString("name", ebn); ebl = eb.getStringList("lore"); }
        ConfigurationSection ra = ggs.getConfigurationSection("recover-all");
        int raSlot = 49; org.bukkit.Material raMat = org.bukkit.Material.NETHER_STAR; String raName = "&a&l모두 회수"; List<String> raLore = List.of();
        if (ra != null) { raSlot = ra.getInt("slot", 49); raMat = parseMaterial(ra.getString("material", "NETHER_STAR"), org.bukkit.Material.NETHER_STAR); raName = ra.getString("name", raName); raLore = ra.getStringList("lore"); }
        ConfigurationSection rs = ggs.getConfigurationSection("recovery-status");
        String rso = "&a&l[ 주인 회수 ]", rsl = "&c&l[ 도굴꾼 회수 ]", rsn = "&7[ 미회수 ]";
        if (rs != null) { rso = rs.getString("owner", rso); rsl = rs.getString("looter", rsl); rsn = rs.getString("none", rsn); }
        graveGuiConfig = new GraveGuiConfig(ggs.getString("title", "{player} 의 무덤"), ebm, ebn, ebl, raSlot, raMat, raName, raLore, rso, rsl, rsn);
    }

    private FileConfiguration loadYml(String fileName) {
        File f = new File(plugin.getDataFolder(), fileName);
        if (!f.exists()) plugin.saveResource(fileName, false);
        return YamlConfiguration.loadConfiguration(f);
    }

    public static GraveExpireRule resolveBestRule(org.bukkit.entity.Player player, List<GraveExpireRule> rules) {
        GraveExpireRule best = null; int bestP = Integer.MIN_VALUE;
        for (GraveExpireRule r : rules) {
            if (player.hasPermission(r.getPermission()) && r.getPriority() > bestP) { best = r; bestP = r.getPriority(); }
        }
        return best;
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

    /** 값이 없거나 숫자가 아니면 null. */
    private static Double parseOptionalDoubleObject(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble(((String) value).trim()); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
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