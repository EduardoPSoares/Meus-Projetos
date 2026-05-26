package me.ray.midgard.modules.item.utils;

import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.ItemStat;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemPDC {

    private static final Map<String, NamespacedKey> KEY_CACHE = new ConcurrentHashMap<>();

    public static NamespacedKey key(String name) {
        return KEY_CACHE.computeIfAbsent(name, k -> new NamespacedKey(ItemModule.getInstance().getPlugin(), k));
    }

    public static void setString(ItemMeta meta, String k, String value) {
        meta.getPersistentDataContainer().set(key(k), PersistentDataType.STRING, value);
    }

    public static String getString(ItemMeta meta, String k) {
        return meta.getPersistentDataContainer().get(key(k), PersistentDataType.STRING);
    }

    public static void setDouble(ItemMeta meta, String k, double value) {
        meta.getPersistentDataContainer().set(key(k), PersistentDataType.DOUBLE, value);
    }

    public static double getDouble(ItemMeta meta, String k) {
        return meta.getPersistentDataContainer().getOrDefault(key(k), PersistentDataType.DOUBLE, 0.0);
    }

    public static void setInt(ItemMeta meta, String k, int value) {
        meta.getPersistentDataContainer().set(key(k), PersistentDataType.INTEGER, value);
    }

    public static int getInt(ItemMeta meta, String k) {
        return meta.getPersistentDataContainer().getOrDefault(key(k), PersistentDataType.INTEGER, 0);
    }
    
    public static boolean has(ItemMeta meta, String k) {
        NamespacedKey nk = key(k);
        return meta.getPersistentDataContainer().has(nk, PersistentDataType.STRING) ||
               meta.getPersistentDataContainer().has(nk, PersistentDataType.DOUBLE) ||
               meta.getPersistentDataContainer().has(nk, PersistentDataType.INTEGER);
    }

    public static void setStat(ItemMeta meta, ItemStat stat, double value) {
        setDouble(meta, "stat_" + stat.name().toLowerCase(), value);
    }

    public static double getStat(ItemMeta meta, ItemStat stat) {
        return getDouble(meta, "stat_" + stat.name().toLowerCase());
    }
    
    public static boolean hasStat(ItemMeta meta, ItemStat stat) {
        return meta.getPersistentDataContainer().has(key("stat_" + stat.name().toLowerCase()), PersistentDataType.DOUBLE);
    }

    public static Map<ItemStat, Double> getStats(ItemStack item) {
        Map<ItemStat, Double> stats = new HashMap<>();
        if (item == null || !item.hasItemMeta()) {
            return stats;
        }
        
        ItemMeta meta = item.getItemMeta();
        for (ItemStat stat : ItemStat.values()) {
            if (hasStat(meta, stat)) {
                stats.put(stat, getStat(meta, stat));
            }
        }
        return stats;
    }
    
    public static void setMidgardId(ItemMeta meta, String id) {
        meta.getPersistentDataContainer().set(key("midgard_id"), PersistentDataType.STRING, id);
    }
    
    public static String getMidgardId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key("midgard_id"), PersistentDataType.STRING);
    }
}
