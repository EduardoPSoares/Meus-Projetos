package com.midgard.core.nms;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection utilities for NMS/CraftBukkit access.
 * Caches lookups for performance.
 */
public final class ReflectionUtils {

    private static final String VERSION;
    private static final Map<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    private ReflectionUtils() {
    }

    public static String getVersion() {
        return VERSION;
    }

    // --- Class Resolution ---

    public static Class<?> getNMSClass(String name) throws ClassNotFoundException {
        // 1.17+ uses direct mapping
        return getClass("net.minecraft.server." + VERSION + "." + name);
    }

    public static Class<?> getCraftBukkitClass(String name) throws ClassNotFoundException {
        return getClass("org.bukkit.craftbukkit." + VERSION + "." + name);
    }

    public static Class<?> getClass(String fullName) throws ClassNotFoundException {
        Class<?> cached = CLASS_CACHE.get(fullName);
        if (cached != null) return cached;
        Class<?> clazz = Class.forName(fullName);
        CLASS_CACHE.put(fullName, clazz);
        return clazz;
    }

    // --- Method Access ---

    public static Method getMethod(Class<?> clazz, String name, Class<?>... params) throws NoSuchMethodException {
        String key = clazz.getName() + "#" + name + "(" + paramKey(params) + ")";
        Method cached = METHOD_CACHE.get(key);
        if (cached != null) return cached;

        Method method = clazz.getDeclaredMethod(name, params);
        method.setAccessible(true);
        METHOD_CACHE.put(key, method);
        return method;
    }

    private static String paramKey(Class<?>[] params) {
        if (params == null || params.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(params[i].getName());
        }
        return sb.toString();
    }

    public static Object invokeMethod(Object obj, String name, Object... args) throws ReflectiveOperationException {
        Class<?>[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
        }
        Method method = getMethod(obj.getClass(), name, paramTypes);
        return method.invoke(obj, args);
    }

    public static Object invokeStaticMethod(Class<?> clazz, String name, Class<?>[] paramTypes, Object... args) throws ReflectiveOperationException {
        Method method = getMethod(clazz, name, paramTypes);
        return method.invoke(null, args);
    }

    // --- Field Access ---

    public static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
        String key = clazz.getName() + "#" + name;
        Field cached = FIELD_CACHE.get(key);
        if (cached != null) return cached;

        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        FIELD_CACHE.put(key, field);
        return field;
    }

    public static Object getFieldValue(Object obj, String name) throws ReflectiveOperationException {
        Field field = getField(obj.getClass(), name);
        return field.get(obj);
    }

    public static void setFieldValue(Object obj, String name, Object value) throws ReflectiveOperationException {
        Field field = getField(obj.getClass(), name);
        field.set(obj, value);
    }

    // --- Constructor Access ---

    public static <T> T newInstance(Class<T> clazz, Class<?>[] paramTypes, Object... args) throws ReflectiveOperationException {
        Constructor<T> constructor = clazz.getDeclaredConstructor(paramTypes);
        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }

    // --- Player Handle ---

    public static Object getHandle(Object craftObject) throws ReflectiveOperationException {
        return invokeMethod(craftObject, "getHandle");
    }

    public static Object getPlayerConnection(Object entityPlayer) throws ReflectiveOperationException {
        // Works for 1.17+ (field name is 'b' or 'connection')
        try {
            return getFieldValue(entityPlayer, "b");
        } catch (Exception e) {
            return getFieldValue(entityPlayer, "playerConnection");
        }
    }

    public static void sendPacket(Object playerConnection, Object packet) throws ReflectiveOperationException {
        try {
            invokeMethod(playerConnection, "a", packet);
        } catch (Exception e) {
            invokeMethod(playerConnection, "sendPacket", packet);
        }
    }
}
