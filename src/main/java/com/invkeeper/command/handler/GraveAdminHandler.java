package com.invkeeper.command.handler;

import com.invkeeper.grave.GraveHistoryGui;
import com.invkeeper.grave.GraveListGui;
import com.invkeeper.grave.GraveManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GraveAdminHandler {

    private final GraveManager graveManager;
    private final GraveListGui listGui;
    private final GraveHistoryGui historyGui;

    public GraveAdminHandler(GraveManager graveManager) {
        this.graveManager = graveManager;
        this.listGui = new GraveListGui(graveManager);
        this.historyGui = new GraveHistoryGui(graveManager);
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("invkeeper.grave.admin")) {
            MessageUtil.send(sender, "&c권한이 없습니다.");
            return true;
        }
        if (args.length < 2) return false;

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "list":
                if (sender instanceof Player p) listGui.open(p, 0);
                else MessageUtil.send(sender, "&c플레이어만 사용 가능합니다.");
                return true;
            case "history":
                if (args.length < 3) { MessageUtil.send(sender, "&c사용법: /invkeeper grave history <player>"); return true; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                if (target == null || target.getName() == null) {
                    MessageUtil.send(sender, "&c플레이어를 찾을 수 없습니다."); return true;
                }
                if (sender instanceof Player p) {
                    historyGui.open(p, target.getUniqueId());
                } else MessageUtil.send(sender, "&c플레이어만 사용 가능합니다.");
                return true;
            case "reload":
                graveManager.reloadConfig();
                graveManager.resyncHolograms();
                MessageUtil.send(sender, "&a무덤 설정이 리로드되었습니다. (홀로그램 재동기화 포함)");
                return true;
            default:
                return false;
        }
    }

    public GraveListGui getListGui() { return listGui; }
    public GraveHistoryGui getHistoryGui() { return historyGui; }
}
