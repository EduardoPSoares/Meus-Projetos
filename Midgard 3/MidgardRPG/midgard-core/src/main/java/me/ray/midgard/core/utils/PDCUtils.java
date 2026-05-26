package me.ray.midgard.core.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public class PDCUtils {

    private static volatile JavaPlugin plugin;

    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Creates a NamespacedKey with the plugin instance.
     */
    public static NamespacedKey key(String key) {
        if (plugin == null) {
            throw new IllegalStateException("PDCUtils not initialized! Call PDCUtils.init(plugin) first.");
        }
        return new NamespacedKey(plugin, key);
    }

    public static void setString(PersistentDataHolder holder, String key, String value) {
        holder.getPersistentDataContainer().set(key(key), PersistentDataType.STRING, value);
    }

    public static String getString(PersistentDataHolder holder, String key) {
        return holder.getPersistentDataContainer().get(key(key), PersistentDataType.STRING);
    }

    public static boolean has(PersistentDataHolder holder, String key) {
        return holder.getPersistentDataContainer().has(key(key), PersistentDataType.STRING); // Assuming STRING as default check
    }
    
    public static void setInt(PersistentDataHolder holder, String key, int value) {
        holder.getPersistentDataContainer().set(key(key), PersistentDataType.INTEGER, value);
    }

    public static int getInt(PersistentDataHolder holder, String key) {
        return holder.getPersistentDataContainer().getOrDefault(key(key), PersistentDataType.INTEGER, 0);
    }

    public static void setDouble(PersistentDataHolder holder, String key, double value) {
        holder.getPersistentDataContainer().set(key(key), PersistentDataType.DOUBLE, value);
    }

    public static <T, Z> Z get(PersistentDataHolder holder, NamespacedKey key, PersistentDataType<T, Z> type) {
        return holder.getPersistentDataContainer().get(key, type);
    }
    
    public static <T, Z> Z get(org.bukkit.inventory.ItemStack item, NamespacedKey key, PersistentDataType<T, Z> type) {
        if (item == null || !item.hasItemMeta()) { return null; }
        return item.getItemMeta().getPersistentDataContainer().get(key, type);
    }

    public static <T, Z> void set(PersistentDataHolder holder, NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        holder.getPersistentDataContainer().set(key, type, value);
    }

    public static double getDouble(PersistentDataHolder holder, String key) {
        return holder.getPersistentDataContainer().getOrDefault(key(key), PersistentDataType.DOUBLE, 0.0);
    }

    public static void remove(PersistentDataHolder holder, String key) {
        holder.getPersistentDataContainer().remove(key(key));
    }
}
