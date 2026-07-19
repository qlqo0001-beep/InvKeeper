package com.invkeeper.protection;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class MMOItemsHook {
    private final boolean available;
    private final Logger logger;
    private Class<?> liveMMOItemClass;
    private Constructor<?> liveMMOItemConstructor;
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
            ClassLoader mmoClassLoader = mmoPlugin.getClass().getClassLoader();
            liveMMOItemClass = mmoClassLoader.loadClass("net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem");
            liveMMOItemConstructor = liveMMOItemClass.getConstructor(ItemStack.class);
            getTypeMethod = liveMMOItemClass.getMethod("getType");
            getIdMethod = liveMMOItemClass.getMethod("getId");
        } catch (Exception e) {
            logger.warning("[InvKeeper] MMOItems 플러그인은 설치되어 있지만 MMOItems API 로딩에 실패했습니다. MMOItems 인식 기능이 비활성화됩니다.");
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
            Object liveMMOItem = liveMMOItemConstructor.newInstance(item);
            Object foundType = getTypeMethod.invoke(liveMMOItem);
            Object foundId = getIdMethod.invoke(liveMMOItem);
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
