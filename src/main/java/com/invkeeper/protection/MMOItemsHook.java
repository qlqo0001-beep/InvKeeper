package com.invkeeper.protection;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class MMOItemsHook {
    private final boolean available;
    private final Logger logger;
    private enum ApiMode { LIVE_MMO_ITEM, NBT_ITEM }
    private ApiMode apiMode;
    private Constructor<?> mmoItemConstructor;
    private Method mmoItemGetter;
    private Method getTypeMethod;
    private Method getIdMethod;

    public MMOItemsHook(Plugin plugin) {
        this.logger = plugin.getLogger();
        Plugin mmoPlugin = plugin.getServer().getPluginManager().getPlugin("MMOItems");
        if (mmoPlugin == null) {
            this.available = false;
            return;
        }

        boolean success = false;
        try {
            ClassLoader mmoClassLoader = mmoPlugin.getClass().getClassLoader();
            try {
                Class<?> liveClass = mmoClassLoader.loadClass("net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem");
                mmoItemConstructor = liveClass.getConstructor(ItemStack.class);
                getTypeMethod = liveClass.getMethod("getType");
                getIdMethod = liveClass.getMethod("getId");
                apiMode = ApiMode.LIVE_MMO_ITEM;
                success = true;
                logger.info("[InvKeeper] MMOItems API 로딩 성공: LiveMMOItem 경로 사용");
            } catch (Exception liveException) {
                try {
                    Class<?> nbtClass = mmoClassLoader.loadClass("net.Indyuce.mmoitems.api.item.NBTItem");
                    mmoItemGetter = nbtClass.getMethod("get", ItemStack.class);
                    getTypeMethod = nbtClass.getMethod("getType");
                    getIdMethod = nbtClass.getMethod("getId");
                    apiMode = ApiMode.NBT_ITEM;
                    success = true;
                    logger.info("[InvKeeper] MMOItems API 로딩 성공: NBTItem 경로 사용");
                } catch (Exception nbtException) {
                    logger.warning("[InvKeeper] MMOItems 플러그인은 설치되어 있지만 MMOItems API 로딩에 실패했습니다. MMOItems 인식 기능이 비활성화됩니다.");
                    logger.warning("LiveMMOItem 경로 실패: " + liveException);
                    logger.warning("NBTItem 경로 실패: " + nbtException);
                }
            }
        } catch (Exception e) {
            logger.warning("[InvKeeper] MMOItems 플러그인은 설치되어 있지만 MMOItems API 로딩 중 예기치 않은 오류가 발생했습니다. MMOItems 인식 기능이 비활성화됩니다.");
            logger.warning(e.toString());
        }
        this.available = success;
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
            Object mmoItem;
            if (apiMode == ApiMode.LIVE_MMO_ITEM) {
                mmoItem = mmoItemConstructor.newInstance(item);
            } else if (apiMode == ApiMode.NBT_ITEM) {
                mmoItem = mmoItemGetter.invoke(null, item);
            } else {
                return false;
            }
            Object foundType = getTypeMethod.invoke(mmoItem);
            Object foundId = getIdMethod.invoke(mmoItem);
            String foundTypeString = extractTypeString(foundType);
            String foundIdString = foundId == null ? "" : foundId.toString();
            return type.equalsIgnoreCase(foundTypeString) && id.equalsIgnoreCase(foundIdString);
        } catch (InvocationTargetException e) {
            logger.warning("[InvKeeper] MMOItems 아이템 판별 중 내부 오류가 발생했습니다. 이 아이템은 무시됩니다.");
            logger.warning(e.getTargetException().toString());
            return false;
        } catch (Exception e) {
            logger.warning("[InvKeeper] MMOItems 아이템 판별 중 오류가 발생했습니다. 이 아이템은 무시됩니다.");
            logger.warning(e.toString());
            return false;
        }
    }

    private static String extractTypeString(Object typeObject) {
        if (typeObject == null) {
            return "";
        }
        if (typeObject instanceof String) {
            return (String) typeObject;
        }
        try {
            Method idMethod = findMethod(typeObject.getClass(), "getId");
            if (idMethod != null) {
                Object value = idMethod.invoke(typeObject);
                if (value instanceof String) {
                    return (String) value;
                }
            }
        } catch (Exception ignored) {
        }
        try {
            Method nameMethod = findMethod(typeObject.getClass(), "name");
            if (nameMethod != null) {
                Object value = nameMethod.invoke(typeObject);
                if (value instanceof String) {
                    return (String) value;
                }
            }
        } catch (Exception ignored) {
        }
        try {
            Method getNameMethod = findMethod(typeObject.getClass(), "getName");
            if (getNameMethod != null) {
                Object value = getNameMethod.invoke(typeObject);
                if (value instanceof String) {
                    return (String) value;
                }
            }
        } catch (Exception ignored) {
        }
        return typeObject.toString();
    }

    private static Method findMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
