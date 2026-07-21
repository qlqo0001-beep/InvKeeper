package com.invkeeper.protection;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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

    private Object mmoItemsPluginInstance;
    private Method getItemByTypeAndIdMethod;
    private Method getMMOItemMethod;

    public MMOItemsHook(Plugin plugin) {
        this.logger = plugin.getLogger();
        Plugin mmoPlugin = plugin.getServer().getPluginManager().getPlugin("MMOItems");
        if (mmoPlugin == null) {
            this.available = false;
            return;
        }
        this.mmoItemsPluginInstance = mmoPlugin;
 
        ClassLoader mmoClassLoader = mmoPlugin.getClass().getClassLoader();
        boolean matchSupported = false;
        Exception matchException = null;

        try {
            matchSupported = tryLoadLiveApi(mmoClassLoader);
        } catch (Exception e) {
            matchException = e;
        }

        if (!matchSupported) {
            try {
                matchSupported = tryLoadNbtApi(mmoClassLoader, "io.lumine.mythic.lib.api.item.NBTItem");
            } catch (Exception e) {
                if (matchException == null) {
                    matchException = e;
                }
            }
        }

        if (!matchSupported) {
            try {
                matchSupported = tryLoadNbtApi(mmoClassLoader, "net.Indyuce.mmoitems.api.item.NBTItem");
            } catch (Exception e) {
                if (matchException == null) {
                    matchException = e;
                }
            }
        }

        boolean itemRetrievalSupported = false;
        Exception retrievalException = null;
        try {
            itemRetrievalSupported = tryInitializeItemCreation(mmoClassLoader);
        } catch (Exception e) {
            retrievalException = e;
        }

        if (!matchSupported) {
            logger.warning("[InvKeeper] MMOItems 플러그인은 설치되어 있지만 MMOItems API 로딩에 실패했습니다. MMOItems 인식 기능이 비활성화됩니다.");
            if (matchException != null) {
                logger.warning(matchException.toString());
            }
        }

        if (!itemRetrievalSupported) {
            logger.info("[InvKeeper] MMOItems 아이템 조회 API를 사용할 수 없습니다. MMOItems 보호 아이템 지급이 제한될 수 있습니다.");
            if (retrievalException != null) {
                logger.info(retrievalException.toString());
            }
        }

        this.available = matchSupported && itemRetrievalSupported;
    }

    private boolean tryLoadLiveApi(ClassLoader loader) throws Exception {
        Class<?> liveClass = loader.loadClass("net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem");
        mmoItemConstructor = liveClass.getConstructor(ItemStack.class);
        getTypeMethod = liveClass.getMethod("getType");
        getIdMethod = liveClass.getMethod("getId");
        apiMode = ApiMode.LIVE_MMO_ITEM;
        logger.info("[InvKeeper] MMOItems API 로딩 성공: LiveMMOItem 경로 사용");
        return true;
    }

    private boolean tryLoadNbtApi(ClassLoader loader, String className) throws Exception {
        Class<?> nbtClass = loader.loadClass(className);
        mmoItemGetter = nbtClass.getMethod("get", ItemStack.class);
        getTypeMethod = nbtClass.getMethod("getType");
        getIdMethod = nbtClass.getMethod("getId");
        apiMode = ApiMode.NBT_ITEM;
        logger.info("[InvKeeper] MMOItems API 로딩 성공: " + className + " 경로 사용");
        return true;
    }

    private boolean tryInitializeItemCreation(ClassLoader loader) throws Exception {
        Class<?> mmoitemsClass = loader.loadClass("net.Indyuce.mmoitems.MMOItems");
        try {
            Field pluginField = mmoitemsClass.getField("plugin");
            Object staticPlugin = pluginField.get(null);
            if (staticPlugin != null) {
                this.mmoItemsPluginInstance = staticPlugin;
            }
        } catch (Throwable ignored) {
        }

        // Verify that mmoItemsPluginInstance is actually an instance of the MMOItems class
        // If not, the reflection methods won't work correctly
        if (this.mmoItemsPluginInstance == null || !mmoitemsClass.isInstance(this.mmoItemsPluginInstance)) {
            logger.warning("[InvKeeper] MMOItems 인스턴스를 확인할 수 없습니다. 아이템 조회 API를 비활성화합니다.");
            getItemByTypeAndIdMethod = null;
            getMMOItemMethod = null;
            return false;
        }

        try {
            getItemByTypeAndIdMethod = mmoitemsClass.getMethod("getItem", String.class, String.class);
        } catch (NoSuchMethodException ignored) {
            getItemByTypeAndIdMethod = null;
        }
        try {
            getMMOItemMethod = mmoitemsClass.getMethod("getMMOItem", String.class, String.class);
        } catch (NoSuchMethodException ignored) {
            getMMOItemMethod = null;
        }

        if (getItemByTypeAndIdMethod != null) {
            logger.info("[InvKeeper] MMOItems direct item retrieval API 지원: getItem(String,String)");
            return true;
        }
        if (getMMOItemMethod != null) {
            logger.info("[InvKeeper] MMOItems direct MMOItem retrieval API 지원: getMMOItem(String,String)");
            return true;
        }

        return false;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean itemExists(String type, String id) {
        if (!available || type == null || type.isBlank() || id == null || id.isBlank()) {
            return false;
        }
        try {
            if (getItemByTypeAndIdMethod != null) {
                Object itemCandidate = getItemByTypeAndIdMethod.invoke(mmoItemsPluginInstance, type, id);
                ItemStack result = resolveItemStack(itemCandidate);
                return result != null && !result.getType().isAir();
            }
            if (getMMOItemMethod != null) {
                Object itemCandidate = getMMOItemMethod.invoke(mmoItemsPluginInstance, type, id);
                ItemStack result = resolveItemStack(itemCandidate);
                return result != null && !result.getType().isAir();
            }
        } catch (Exception e) {
            logger.warning("[InvKeeper] MMOItems 아이템 존재 여부 확인 중 오류가 발생했습니다.");
            logger.warning(e.toString());
        }
        return false;
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
            Throwable t = e.getTargetException();
            // Suppress noisy NullPointerExceptions originating inside MMOItems internals (they occur
            // when the MMOItem implementation isn't fully initialized for this ItemStack). Treat as
            // non-matching without logging to avoid spamming the console.
            if (t instanceof NullPointerException) {
                // Fine-level logging for NPE to avoid console spam; only shown when FINE logging is enabled
                String itemType = item != null ? item.getType().toString() : "null";
                logger.fine("[InvKeeper] MMOItems 아이템 판별 중 NPE 발생 (무시됨): item=" + itemType + ", type=" + type + ", id=" + id);
                return false;
            }
            logger.warning("[InvKeeper] MMOItems 아이템 판별 중 내부 오류가 발생했습니다. 이 아이템은 무시됩니다.");
            logger.warning(t.toString());
            return false;
        } catch (Exception e) {
            // For other exceptions, log at warning level but avoid printing huge stack traces.
            logger.warning("[InvKeeper] MMOItems 아이템 판별 중 오류가 발생했습니다. 이 아이템은 무시됩니다.");
            logger.warning(e.toString());
            return false;
        }
    }


    public boolean giveItemByCommand(org.bukkit.entity.Player target, String type, String id, int amount) {
        if (!available || target == null || type == null || type.isBlank() || id == null || id.isBlank()) {
            return false;
        }
        try {
            String command = "mi give " + type + " " + id + " " + target.getName();
            if (amount > 1) {
                command += " " + amount;
            }
            if (Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                return true;
            }
            command = "mmoitems give " + type + " " + id + " " + target.getName();
            if (amount > 1) {
                command += " " + amount;
            }
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            logger.warning("[InvKeeper] MMOItems 지급 명령 실행 중 오류가 발생했습니다.");
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

    private static ItemStack resolveItemStack(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof ItemStack) {
            return (ItemStack) raw;
        }

        ItemStack item = extractItemStackFromMethods(raw);
        if (item != null) {
            return item;
        }

        // Some MMOItems APIs return wrapper objects that can still supply an ItemStack.
        try {
            Method buildMethod = findMethod(raw.getClass(), "build");
            if (buildMethod != null) {
                Object result = buildMethod.invoke(raw);
                return resolveItemStack(result);
            }
        } catch (Exception ignored) {
        }

        try {
            Method getItemMethod = findMethod(raw.getClass(), "getItem");
            if (getItemMethod != null) {
                Object result = getItemMethod.invoke(raw);
                return resolveItemStack(result);
            }
        } catch (Exception ignored) {
        }

        return null;
    }


    private static ItemStack extractItemStackFromMethods(Object raw) {
        for (String methodName : new String[]{"getItemStack", "toItemStack", "asItemStack", "getItem", "toItem", "asItem", "toBukkitItem", "asBukkitItem", "getBukkitItem"}) {
            try {
                Method method = findMethod(raw.getClass(), methodName);
                if (method == null) {
                    continue;
                }
                Object result = method.invoke(raw);
                if (result instanceof ItemStack) {
                    return (ItemStack) result;
                }
                if (result != null && result != raw) {
                    ItemStack nested = resolveItemStack(result);
                    if (nested != null) {
                        return nested;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
