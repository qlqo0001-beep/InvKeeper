package com.invkeeper.protection;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class MMOItemsHook {
    private final boolean available;
    private final Logger logger;
    private enum ApiMode { LIVE_MMO_ITEM, NBT_ITEM }
    private ApiMode apiMode;
    private Class<?> mmoItemClass;
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

        boolean ok = false;
        try {
            ClassLoader mmoClassLoader = mmoPlugin.getClass().getClassLoader();
            try {
                mmoItemClass = mmoClassLoader.loadClass("net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem");
                mmoItemConstructor = mmoItemClass.getConstructor(ItemStack.class);
                getTypeMethod = mmoItemClass.getMethod("getType");
                getIdMethod = mmoItemClass.getMethod("getId");
                apiMode = ApiMode.LIVE_MMO_ITEM;
                ok = true;
            } catch (Exception liveException) {
                // fallback to older MMOItems API
                try {
                    mmoItemClass = mmoClassLoader.loadClass("net.Indyuce.mmoitems.api.item.NBTItem");
                    mmoItemGetter = mmoItemClass.getMethod("get", ItemStack.class);
                    getTypeMethod = mmoItemClass.getMethod("getType");
                    getIdMethod = mmoItemClass.getMethod("getId");
                    apiMode = ApiMode.NBT_ITEM;
                    ok = true;
                } catch (Exception nbtException) {
                    logger.warning("[InvKeeper] MMOItems 플러그인은 설치되어 있지만 MMOItems API 로딩에 실패했습니다. MMOItems 인식 기능이 비활성화됩니다.");
                    logger.warning(liveException.toString());
                    logger.warning(nbtException.toString());
                }
            }
        } catch (Exception e) {
            logger.warning("[InvKeeper] MMOItems 플러그인은 설치되어 있지만 MMOItems API 로딩 중 예기치 않은 오류가 발생했습니다. MMOItems 인식 기능이 비활성화됩니다.");
            logger.warning(e.toString());
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
            String foundTypeString = foundType == null ? "" : foundType.toString();
            String foundIdString = foundId == null ? "" : foundId.toString();
            return type.equalsIgnoreCase(foundTypeString) && id.equalsIgnoreCase(foundIdString);
        } catch (Exception e) {
            logger.warning("[InvKeeper] MMOItems 아이템 판별 중 오류가 발생했습니다. 이 아이템은 무시됩니다.");
            logger.warning(e.toString());
            return false;
        }
    }
}
