package com.invkeeper.command.handler;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.config.ProtectionItemConfig;
import com.invkeeper.protection.ProtectionManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GiveHandler {
    private final ConfigManager configManager;
    private final ProtectionManager protectionManager;

    public GiveHandler(ConfigManager configManager, ProtectionManager protectionManager) {
        this.configManager = configManager;
        this.protectionManager = protectionManager;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("invkeeper.admin")) {
            MessageUtil.send(sender, "&c권한이 없습니다.");
            return true;
        }
        if (args.length < 3) {
            MessageUtil.send(sender, "&c사용법: /invkeeper give <player> <item-key> [amount]");
            return true;
        }

        ItemStack item = null;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.send(sender, "&c대상 플레이어가 온라인 상태가 아닙니다.");
            return true;
        }

        String itemKey = args[2];
        ProtectionItemConfig itemConfig = configManager.getProtectionItemConfig(itemKey);
        if (itemConfig == null) {
            MessageUtil.send(sender, "&c존재하지 않는 보호 아이템 키입니다. 사용 가능한 키: " + String.join(", ", configManager.getProtectionItemKeys()));
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                MessageUtil.send(sender, "&c유효한 개수를 입력해야 합니다.");
                return true;
            }
        }

        if (itemConfig.isUseMmo()) {
            if (!protectionManager.getMmoItemsHook().isAvailable()) {
                MessageUtil.send(sender, "&cMMOItems가 서버에 설치되어 있지 않거나 사용할 수 없습니다. MMOItems 설치를 확인하세요.");
                return true;
            }
            boolean gaveViaCommand = protectionManager.getMmoItemsHook().giveItemByCommand(target, itemConfig.getMmoItemsType(), itemConfig.getMmoItemsId(), amount);
            if (gaveViaCommand) {
                MessageUtil.send(sender, "&a" + target.getName() + "님에게 MMOItems 보호 아이템을 지급했습니다.");
                MessageUtil.send(target, "&a보호 아이템을 지급받았습니다.");
                // Schedule a short delayed scan to bind MMOItems that were just given
                int giveAmount = amount;
                Bukkit.getScheduler().runTaskLater(protectionManager.getPlugin(), () -> {
                    try {
                        int bound = protectionManager.bindMatchingItemsInInventory(target, itemConfig, giveAmount);
                        if (bound > 0 && itemConfig.isSoulbindEnabled()) {
                            String remaining;
                            if (itemConfig.isSoulbindInfinite()) {
                                remaining = "무한";
                            } else {
                                long millis = (long) itemConfig.getSoulbindDurationMinutes() * 60L * 1000L;
                                remaining = com.invkeeper.util.MessageUtil.formatDuration(millis, configManager.getTimeFormat());
                            }
                            MessageUtil.send(target, configManager.getSoulboundAppliedMessage().replace("{owner}", target.getName()).replace("{remaining}", remaining));
                        }
                    } catch (Exception e) {
                        protectionManager.getPlugin().getLogger().warning("[InvKeeper] MMOItems 지급 후 각인 처리 중 오류: " + e.getMessage());
                    }
                }, 2L);
            } else {
                MessageUtil.send(sender, "&cMMOItems 지급 명령을 실행할 수 없습니다. 아이템이 존재하지 않거나 명령 형식/권한 문제가 있을 수 있습니다. type=" + itemConfig.getMmoItemsType() + " id=" + itemConfig.getMmoItemsId());
            }
            return true;
        } else if (itemConfig.isUseVanilla()) {
            // Prepare soulbind lore placeholders if soulbind is enabled
            String ownerName = null;
            String remainingTime = null;
            if (itemConfig.isSoulbindEnabled()) {
                ownerName = target.getName();
                if (itemConfig.isSoulbindInfinite()) {
                    remainingTime = "무한";
                } else {
                    long millis = (long) itemConfig.getSoulbindDurationMinutes() * 60L * 1000L;
                    remainingTime = com.invkeeper.util.MessageUtil.formatDuration(millis, configManager.getTimeFormat());
                }
            }
            item = protectionManager.getVanillaProtectionItems().create(itemConfig, ownerName, remainingTime);
            if (item != null && amount > 1) {
                item.setAmount(amount);
            }
            // Apply soulbind on give if configured
            if (item != null && protectionManager.getConfigManager().getProtectionItemConfig(itemConfig.getKey()) != null) {
                var cfg = protectionManager.getConfigManager().getProtectionItemConfig(itemConfig.getKey());
                if (cfg.isSoulbindEnabled()) {
                    long expiry = cfg.isSoulbindInfinite() ? -1L : (System.currentTimeMillis() + (long)cfg.getSoulbindDurationMinutes() * 60L * 1000L);
                    protectionManager.getSoulbindManager().applySoulbind(item, target.getUniqueId(), expiry);
                }
            }
        }

        if (item == null || item.getType().isAir()) {
            MessageUtil.send(sender, "&c지급할 수 있는 보호 아이템이 없습니다. 설정을 확인해주세요.");
            return true;
        }
        var remainder = target.getInventory().addItem(item);
        if (!remainder.isEmpty()) {
            for (ItemStack leftover : remainder.values()) {
                if (leftover != null && !leftover.getType().isAir()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), leftover);
                }
            }
            MessageUtil.send(target, "&e인벤토리가 가득 차서 일부 아이템이 바닥에 떨어졌습니다.");
        }
        MessageUtil.send(sender, "&a" + target.getName() + "님에게 보호 아이템을 지급했습니다.");
        MessageUtil.send(target, "&a보호 아이템을 지급받았습니다.");
        return true;
    }
}