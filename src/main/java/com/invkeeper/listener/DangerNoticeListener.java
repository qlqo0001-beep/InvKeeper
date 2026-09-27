package com.invkeeper.listener;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.protection.ProtectionManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 위험 지역 안내: 접속 시 1회, 월드 이동 시 적용 드랍률이 바뀐 경우에만 안내합니다.
 * 서버 전체 on/off는 config.yml의 danger-notice.enabled, 개인 on/off는 /invkeeper notice 로 설정합니다.
 */
public class DangerNoticeListener implements Listener {
    private static final long JOIN_DELAY_TICKS = 40L;

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final ProtectionManager protectionManager;
    private final NamespacedKey optOutKey;
    // 플레이어별 마지막으로 안내한 드랍률. 같은 드랍률의 월드로 이동하면 다시 안내하지 않습니다.
    private final Map<UUID, String> lastNotice = new HashMap<>();

    public DangerNoticeListener(Plugin plugin, ConfigManager configManager, ProtectionManager protectionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.protectionManager = protectionManager;
        this.optOutKey = new NamespacedKey(plugin, "danger_notice_off");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        lastNotice.remove(player.getUniqueId());
        // 접속 직후 쏟아지는 다른 메시지에 묻히지 않도록 잠시 뒤에 안내
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) sendNotice(player);
        }, JOIN_DELAY_TICKS);
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        sendNotice(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastNotice.remove(event.getPlayer().getUniqueId());
    }

    /** 설정 리로드 후 다음 월드 이동 때 바뀐 드랍률이 안내되도록 기록을 비움 */
    public void clearLastNotices() {
        lastNotice.clear();
    }

    public boolean isOptedOut(Player player) {
        return player.getPersistentDataContainer().has(optOutKey, PersistentDataType.BYTE);
    }

    public void setOptedOut(Player player, boolean optedOut) {
        if (optedOut) {
            player.getPersistentDataContainer().set(optOutKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            player.getPersistentDataContainer().remove(optOutKey);
            // 다시 켜면 현재 지역 안내를 바로 보여줌
            lastNotice.remove(player.getUniqueId());
            sendNotice(player);
        }
    }

    private void sendNotice(Player player) {
        if (!configManager.isDangerNoticeEnabled() || isOptedOut(player)) return;

        String world = player.getWorld().getName();
        double[] pve = configManager.resolveDropPercents(player, world, false);
        double[] pvp = configManager.resolveDropPercents(player, world, true);
        int inv = percent(pve[0]);
        int exp = percent(pve[1]);
        int pvpInv = percent(pvp[0]);
        int pvpExp = percent(pvp[1]);

        String signature = inv + ":" + exp + ":" + pvpInv + ":" + pvpExp;
        if (signature.equals(lastNotice.put(player.getUniqueId(), signature))) return;

        if (inv == 0 && exp == 0 && pvpInv == 0 && pvpExp == 0) {
            MessageUtil.send(player, configManager.getDangerNoticeSafeMessage().replace("{world}", world));
            return;
        }

        MessageUtil.send(player, configManager.getDangerNoticeMessage()
                .replace("{world}", world)
                .replace("{inv_percent}", String.valueOf(inv))
                .replace("{exp_percent}", String.valueOf(exp)));
        if (pvpInv != inv || pvpExp != exp) {
            MessageUtil.send(player, configManager.getDangerNoticePvpMessage()
                    .replace("{pvp_inv_percent}", String.valueOf(pvpInv))
                    .replace("{pvp_exp_percent}", String.valueOf(pvpExp)));
        }
        if (protectionManager.getTimedProtectionStore().isActive(player)) {
            long remainingMillis = protectionManager.getTimedProtectionStore().getRemainingMillis(player);
            MessageUtil.send(player, configManager.getDangerNoticeProtectedMessage()
                    .replace("{remaining}", MessageUtil.formatDuration(remainingMillis, configManager.getTimeFormat())));
        }
    }

    // 사망 처리(PlayerDeathListener)와 같은 방식으로 반올림/보정
    private static int percent(double value) {
        return Math.max(0, Math.min(100, (int) Math.round(value)));
    }
}
