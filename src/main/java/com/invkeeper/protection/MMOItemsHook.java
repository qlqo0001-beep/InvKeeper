package com.invkeeper.protection;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Logger;

public class MMOItemsHook {
    private final boolean available;
    private final Logger logger;
    private Class<?> nbtItemClass;
    private Method getMethod;
    private Method getTypeMethod;
    private Method getIdMethod;

    public MMOItemsHook(Plugin plugin) {
        this.logger = plugin.getLogger();
        Plugin mmoPlugin = plugin.getServer().getPluginManager().getPlugin("MMOItems");
        if (mmoPlugin == null) {
            this.available = false;
            return;
        }

        boolean ok = true;
        try {
            nbtItemClass = Class.forName("net.Indyuce.mmoitems.api.item.NBTItem");
            getMethod = nbtItemClass.getMethod("get", ItemStack.class);
            getTypeMethod = findMethod(nbtItemClass, "getType");
            getIdMethod = findMethod(nbtItemClass, "getId");
        } catch (Exception e) {
            logger.warning("[InvKeeper] MMOItems API 로딩 중 오류가 발생했습니다. MMOItems 인식 기능이 비활성화됩니다.");
            logger.warning(e.toString());
            ok = false;
        }
        this.available = ok;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean matches(ItemStack item, String type, String id) {
        if (!available || item == null || item.getType().isAir()) {
            return false;
        }
        if (type == null || type.isBlank() || id == null || id.isBlank()) {
            return false;
        }
        try {
            Object nbtItem = getMethod.invoke(null, item);
            if (nbtItem == null) {
                return false;
            }
            String foundType = extractStringProperty(nbtItem, getTypeMethod);
            String foundId = extractStringProperty(nbtItem, getIdMethod);
            return type.equalsIgnoreCase(foundType) && id.equalsIgnoreCase(foundId);
        } catch (Exception e) {
            logger.warning("[InvKeeper] MMOItems 아이템 판별 중 오류가 발생했습니다. 이 아이템은 무시됩니다.");
            logger.warning(e.toString());
            return false;
        }
    }

    private static Method findMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private String extractStringProperty(Object target, Method method) {
        if (method == null || target == null) {
            return "";
        }
        try {
            Object result = method.invoke(target);
            if (result == null) {
                return "";
            }
            if (result instanceof String) {
                return (String) result;
            }
            Method idMethod = findMethod(result.getClass(), "getId");
            if (idMethod != null) {
                Object idResult = idMethod.invoke(result);
                if (idResult != null) {
                    return idResult.toString();
                }
            }
            Method typeIdMethod = findMethod(result.getClass(), "getTypeId");
            if (typeIdMethod != null) {
                Object typeIdResult = typeIdMethod.invoke(result);
                if (typeIdResult != null) {
                    return typeIdResult.toString();
                }
            }
            return result.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
