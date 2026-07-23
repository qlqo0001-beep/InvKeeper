package com.invkeeper.listener;

import com.invkeeper.config.ConfigManager;
import com.invkeeper.config.ProtectionItemConfig;
import com.invkeeper.protection.ProtectionManager;
import com.invkeeper.soulbind.SoulbindManager;
import com.invkeeper.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryAction;

public class SoulbindInventoryListener implements Listener {
    private final ProtectionManager protectionManager;
    private final ConfigManager configManager;
    private final SoulbindManager soulbindManager;

    public SoulbindInventoryListener(ProtectionManager protectionManager, ConfigManager configManager) {
        this.protectionManager = protectionManager;
        this.configManager = configManager;
        this.soulbindManager = protectionManager.getSoulbindManager();
    }

    private boolean canBypass(Player player) {
        return player.hasPermission("invkeeper.admin") || player.hasPermission("invkeeper.soulbind.bypass");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            
            // Only process clicks in the player's own inventory
            // This prevents soulbind tools from activating in custom GUIs (shops, etc.)
            if (event.getClickedInventory() == null || event.getClickedInventory().getType() != InventoryType.PLAYER) {
                return;
            }
            // If the top inventory is a custom GUI (not player inventory), skip processing
            if (event.getView().getTopInventory().getType() != InventoryType.PLAYER
                    && event.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
                return;
            }
            
            InventoryAction action = event.getAction();
            if (action != InventoryAction.SWAP_WITH_CURSOR && action != InventoryAction.HOTBAR_SWAP) return;
            
            ItemStack toolItem = null;
            if (action == InventoryAction.SWAP_WITH_CURSOR) {
                toolItem = event.getCursor();
            } else if (action == InventoryAction.HOTBAR_SWAP) {
                int hotbarSlot = event.getHotbarButton();
                if (hotbarSlot < 0 || hotbarSlot > 8) return;
                toolItem = player.getInventory().getItem(hotbarSlot);
            }
            
            if (toolItem == null || toolItem.getType().isAir()) return;
            
            // Check if tool has expired soulbind and remove it
            if (soulbindManager.isSoulbound(toolItem)) {
                soulbindManager.checkAndRemoveExpired(toolItem);
            }
            
            String toolKey = protectionManager.getVanillaProtectionItems().getItemKey(toolItem);
            com.invkeeper.config.ProtectionItemConfig toolConfig = null;
            if (toolKey != null) {
                toolConfig = protectionManager.getConfigManager().getProtectionItemConfig(toolKey);
            } else if (protectionManager.getMmoItemsHook().isAvailable()) {
                for (com.invkeeper.config.ProtectionItemConfig pc : protectionManager.getConfigManager().getProtectionItemConfigs().values()) {
                    if (!pc.isUseMmo()) continue;
                    try {
                        if (protectionManager.getMmoItemsHook().matches(toolItem, pc.getMmoItemsType(), pc.getMmoItemsId())) {
                            toolConfig = pc;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }
            
            if (toolConfig == null || !toolConfig.getKind().isSoulbindTool()) return;
            
            ItemStack target = event.getCurrentItem();
            if (target == null || target.getType().isAir()) return;
            
            // Clear the target's soulbind first if it has already expired.
            if (soulbindManager.isSoulbound(target)) {
                soulbindManager.checkAndRemoveExpired(target);
            }
            
            boolean isUnbindTool = toolConfig.getKind().isUnbindTool();
            String loreFormat = configManager.getSoulboundLoreFormat();
            String stackLoreFormat = configManager.getSoulboundLoreFormatStack();
            
            java.util.UUID targetOwner = soulbindManager.getOwnerUuid(target);
            boolean canModify = targetOwner == null || targetOwner.equals(player.getUniqueId()) || canBypass(player);
            if (!canModify) {
                MessageUtil.send(player, configManager.getSoulboundCantPickupMessage().replace("{owner}", soulbindManager.resolveOwnerName(targetOwner)));
                event.setCancelled(true);
                return;
            }
            
            event.setCancelled(true);
            
            if (isUnbindTool) {
                if (soulbindManager.isSoulbound(target)) {
                    soulbindManager.removeLoreLines(target);
                    soulbindManager.removeSoulbind(target);
                    event.setCurrentItem(target);
                    consumeOneTool(event, player, action);
                    MessageUtil.send(player, configManager.getSoulboundUnboundMessage());
                }
                return;
            }
            
            // Check for type conflict: TIME tool on STACK target, or STACK tool on TIME target
            SoulbindManager.SoulbindType targetType = soulbindManager.getSoulbindType(target);
            
            if (toolConfig.getKind().isTimeTool() && targetType == SoulbindManager.SoulbindType.STACK) {
                MessageUtil.send(player, configManager.getSoulboundConflictTypeMessage()
                        .replace("{type}", "스택형"));
                return;
            }
            
            if (toolConfig.getKind().isStackTool() && targetType == SoulbindManager.SoulbindType.TIME) {
                MessageUtil.send(player, configManager.getSoulboundConflictTypeMessage()
                        .replace("{type}", "시간형"));
                return;
            }
            
            if (toolConfig.getKind().isTimeTool()) {
                // Time-based soulbind application
                int applyDuration = toolConfig.getSoulbindApplyDuration();
                boolean applyingInfinite = applyDuration <= 0;
                
                if (!soulbindManager.isSoulbound(target)) {
                    // New soulbind application
                    long targetExpiry = applyingInfinite ? -1L : (System.currentTimeMillis() + (long) applyDuration * 60L * 1000L);
                    soulbindManager.applySoulbind(target, player.getUniqueId(), targetExpiry);
                    soulbindManager.updateLore(target, loreFormat, player.getName(), targetExpiry);
                    event.setCurrentItem(target);
                    consumeOneTool(event, player, action);
                    String remainingDisplay = applyingInfinite ? "무한" : MessageUtil.formatDuration((long)applyDuration*60*1000, configManager.getTimeFormat());
                    MessageUtil.send(player, configManager.getSoulboundAppliedMessage()
                            .replace("{owner}", player.getName())
                            .replace("{remaining}", remainingDisplay));
                } else {
                    // Already soulbound
                    long currentExpiry = soulbindManager.getExpiryMillis(target);
                    if (applyingInfinite) {
                        // Tool applies infinite soulbind
                        if (currentExpiry == -1L) {
                            // Already infinite
                            MessageUtil.send(player, configManager.getSoulboundAlreadyInfiniteMessage());
                            return;
                        }
                        // Upgrade to infinite
                        soulbindManager.applySoulbind(target, soulbindManager.getOwnerUuid(target), -1L);
                        String ownerName = soulbindManager.getOwnerUuid(target) != null
                                ? soulbindManager.resolveOwnerName(soulbindManager.getOwnerUuid(target))
                                : player.getName();
                        soulbindManager.updateLore(target, loreFormat, ownerName, -1L);
                        event.setCurrentItem(target);
                        consumeOneTool(event, player, action);
                        MessageUtil.send(player, configManager.getSoulboundExtendedMessage()
                                .replace("{owner}", player.getName())
                                .replace("{remaining}", "무한"));
                    } else {
                        // Tool applies timed soulbind
                        if (currentExpiry == -1L) {
                            // Target is already infinite, cannot apply timed
                            MessageUtil.send(player, configManager.getSoulboundAlreadyInfiniteMessage());
                            return;
                        }
                        // Extend the existing timed soulbind
                        long newExpiry = currentExpiry + (long) applyDuration * 60L * 1000L;
                        soulbindManager.applySoulbind(target, soulbindManager.getOwnerUuid(target), newExpiry);
                        String ownerName = soulbindManager.getOwnerUuid(target) != null
                                ? soulbindManager.resolveOwnerName(soulbindManager.getOwnerUuid(target))
                                : player.getName();
                        soulbindManager.updateLore(target, loreFormat, ownerName, newExpiry);
                        event.setCurrentItem(target);
                        consumeOneTool(event, player, action);
                        String remainingDisplay = MessageUtil.formatDuration((long)applyDuration*60*1000, configManager.getTimeFormat());
                        MessageUtil.send(player, configManager.getSoulboundExtendedMessage()
                                .replace("{owner}", player.getName())
                                .replace("{remaining}", remainingDisplay));
                    }
                }
            } else if (toolConfig.getKind().isStackTool()) {
                // Stack-based soulbind application
                int stacks = toolConfig.getSoulbindStacks();
                boolean applyingInfinite = stacks < 0;
                int maxStack = configManager.getMaxSoulbindStack();
                
                if (!soulbindManager.isSoulbound(target)) {
                    // New soulbind application
                    if (!applyingInfinite && maxStack >= 0 && stacks > maxStack) {
                        MessageUtil.send(player, configManager.getSoulboundMaxStackMessage()
                                .replace("{max}", String.valueOf(maxStack)));
                        return;
                    }
                    soulbindManager.applyStackSoulbind(target, player.getUniqueId(), stacks);
                    soulbindManager.updateLore(target, loreFormat, stackLoreFormat, player.getName(), -1L, stacks);
                    event.setCurrentItem(target);
                    consumeOneTool(event, player, action);
                    String stacksDisplay = applyingInfinite ? "무한" : String.valueOf(stacks);
                    MessageUtil.send(player, configManager.getSoulboundAppliedMessage()
                            .replace("{owner}", player.getName())
                            .replace("{remaining}", stacksDisplay + "회"));
                } else {
                    // Already soulbound (stack type, since we checked conflict above)
                    int currentStacks = soulbindManager.getStacks(target);
                    if (applyingInfinite) {
                        // Tool applies infinite stacks
                        if (currentStacks < 0) {
                            // Already infinite
                            MessageUtil.send(player, configManager.getSoulboundAlreadyInfiniteMessage());
                            return;
                        }
                        // Check max stack limit for infinite upgrade
                        if (maxStack >= 0) {
                            MessageUtil.send(player, configManager.getSoulboundMaxStackMessage()
                                    .replace("{max}", String.valueOf(maxStack)));
                            return;
                        }
                        soulbindManager.applyStackSoulbind(target, soulbindManager.getOwnerUuid(target), -1);
                        String ownerName = soulbindManager.getOwnerUuid(target) != null
                                ? soulbindManager.resolveOwnerName(soulbindManager.getOwnerUuid(target))
                                : player.getName();
                        soulbindManager.updateLore(target, loreFormat, stackLoreFormat, ownerName, -1L, -1);
                        event.setCurrentItem(target);
                        consumeOneTool(event, player, action);
                        MessageUtil.send(player, configManager.getSoulboundExtendedMessage()
                                .replace("{owner}", player.getName())
                                .replace("{remaining}", "무한"));
                    } else {
                        // Tool adds stacks
                        if (currentStacks < 0) {
                            // Already infinite, cannot add stacks
                            MessageUtil.send(player, configManager.getSoulboundAlreadyInfiniteMessage());
                            return;
                        }
                        int newStacks = currentStacks + stacks;
                        // Check max stack limit
                        if (maxStack >= 0 && newStacks > maxStack) {
                            MessageUtil.send(player, configManager.getSoulboundMaxStackMessage()
                                    .replace("{max}", String.valueOf(maxStack)));
                            return;
                        }
                        soulbindManager.applyStackSoulbind(target, soulbindManager.getOwnerUuid(target), newStacks);
                        String ownerName = soulbindManager.getOwnerUuid(target) != null
                                ? soulbindManager.resolveOwnerName(soulbindManager.getOwnerUuid(target))
                                : player.getName();
                        soulbindManager.updateLore(target, loreFormat, stackLoreFormat, ownerName, -1L, newStacks);
                        event.setCurrentItem(target);
                        consumeOneTool(event, player, action);
                        MessageUtil.send(player, configManager.getSoulboundExtendedMessage()
                                .replace("{owner}", player.getName())
                                .replace("{remaining}", String.valueOf(stacks) + "회"));
                    }
                }
            }
        } catch (Exception e) {
            protectionManager.getPlugin().getLogger().warning("[InvKeeper] 인벤토리 클릭 처리 중 오류: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
    
    private void consumeOneTool(InventoryClickEvent event, Player player, InventoryAction action) {
        if (action == InventoryAction.SWAP_WITH_CURSOR) {
            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType().isAir()) return;
            int amount = cursor.getAmount() - 1;
            if (amount <= 0) {
                event.setCursor(null);
            } else {
                cursor.setAmount(amount);
                event.setCursor(cursor);
            }
        } else if (action == InventoryAction.HOTBAR_SWAP) {
            int hotbarSlot = event.getHotbarButton();
            if (hotbarSlot < 0 || hotbarSlot > 8) return;
            ItemStack item = player.getInventory().getItem(hotbarSlot);
            if (item == null || item.getType().isAir()) return;
            int amount = item.getAmount() - 1;
            if (amount <= 0) {
                player.getInventory().setItem(hotbarSlot, null);
            } else {
                item.setAmount(amount);
                player.getInventory().setItem(hotbarSlot, item);
            }
        }
    }
}