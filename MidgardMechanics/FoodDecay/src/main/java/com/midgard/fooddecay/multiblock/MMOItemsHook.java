package com.midgard.fooddecay.multiblock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Reflection-based hook for MMOItems integration.
 * All calls are safe even if MMOItems is not installed.
 */
public final class MMOItemsHook {

    public record MMOItemReference(String type, String id, Material material, String displayName) {
    }

    private static Boolean available;
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static Object mmoItemsPlugin;
    private static Method getItemMethod;
    private static Method getTypesMethod;
    private static Method typeGetMethod;

    // NBTItem reflection
    private static Class<?> nbtItemClass;
    private static Method nbtGetMethod;
    private static Method nbtHasTypeMethod;
    private static Method nbtGetTypeMethod;
    private static Method nbtGetStringMethod;

    private MMOItemsHook() {}

    /**
     * Returns true if MMOItems is available on this server.
     */
    public static boolean isAvailable() {
        if (available == null) {
            available = init();
        }
        return available;
    }

    private static boolean init() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("MMOItems");
            if (plugin == null || !plugin.isEnabled()) return false;

            mmoItemsPlugin = plugin;

            // Resolve MMOItems.plugin.getTypes().get(typeName)
            Class<?> mmoClass = plugin.getClass();
            getTypesMethod = mmoClass.getMethod("getTypes");
            Object typeMgr = getTypesMethod.invoke(plugin);
            typeGetMethod = typeMgr.getClass().getMethod("get", String.class);

            // Resolve MMOItems.plugin.getItem(type, id)
            Class<?> typeClass = Class.forName("net.Indyuce.mmoitems.api.Type");
            getItemMethod = mmoClass.getMethod("getItem", typeClass, String.class);

            // Resolve NBTItem for reading
            nbtItemClass = Class.forName("io.lumine.mythic.lib.api.item.NBTItem");
            nbtGetMethod = nbtItemClass.getMethod("get", ItemStack.class);
            nbtHasTypeMethod = nbtItemClass.getMethod("hasType");
            nbtGetTypeMethod = nbtItemClass.getMethod("getType");
            nbtGetStringMethod = nbtItemClass.getMethod("getString", String.class);

            return true;
        } catch (Exception e) {
            Logger logger = Bukkit.getServer() != null
                    ? Bukkit.getLogger()
                    : Logger.getLogger(MMOItemsHook.class.getName());
            logger.log(Level.INFO,
                    "[FoodDecay] MMOItems not found or incompatible, MMOItems recipes disabled.");
            return false;
        }
    }

    /**
     * Checks if the given item is an MMOItems item of the specified type and id.
     */
    public static boolean matchesItem(ItemStack item, String typeName, String itemId) {
        if (!isAvailable() || item == null) return false;
        try {
            Object nbtItem = nbtGetMethod.invoke(null, item);
            boolean hasType = (boolean) nbtHasTypeMethod.invoke(nbtItem);
            if (!hasType) return false;

            String type = (String) nbtGetTypeMethod.invoke(nbtItem);
            String id = (String) nbtGetStringMethod.invoke(nbtItem, "MMOITEMS_ITEM_ID");

            return typeName.equalsIgnoreCase(type) && itemId.equalsIgnoreCase(id);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts MMOItems reference data from an ItemStack.
     * Returns null if the item is not an MMOItems item or the hook is unavailable.
     */
    public static MMOItemReference identifyItem(ItemStack item) {
        if (!isAvailable() || item == null || item.getType().isAir()) return null;
        try {
            Object nbtItem = nbtGetMethod.invoke(null, item);
            boolean hasType = (boolean) nbtHasTypeMethod.invoke(nbtItem);
            if (!hasType) return null;

            String type = (String) nbtGetTypeMethod.invoke(nbtItem);
            String id = (String) nbtGetStringMethod.invoke(nbtItem, "MMOITEMS_ITEM_ID");
            if (type == null || type.isBlank() || id == null || id.isBlank()) {
                return null;
            }

            return new MMOItemReference(
                    type,
                    id,
                    item.getType(),
                    extractDisplayName(item, formatIdentifier(id))
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates an MMOItems item by type name and item id.
     * Returns null if MMOItems is not available or the item doesn't exist.
     */
    public static ItemStack createItem(String typeName, String itemId) {
        if (!isAvailable()) return null;
        try {
            Object typeMgr = getTypesMethod.invoke(mmoItemsPlugin);
            Object type = typeGetMethod.invoke(typeMgr, typeName);
            if (type == null) return null;
            return (ItemStack) getItemMethod.invoke(mmoItemsPlugin, type, itemId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns a player-facing display name for an MMOItems reference.
     */
    public static String getItemDisplayName(String typeName, String itemId) {
        if (typeName == null || typeName.isBlank() || itemId == null || itemId.isBlank()) {
            return "Item especial";
        }

        ItemStack item = createItem(typeName, itemId);
        if (item != null) {
            String fallback = formatIdentifier(itemId);
            return extractDisplayName(item, fallback);
        }

        return formatIdentifier(itemId);
    }

    /**
     * Returns an admin-facing label with both display name and technical reference.
     */
    public static String getItemReferenceLabel(String typeName, String itemId) {
        String technical = typeName + ":" + itemId;
        String display = getItemDisplayName(typeName, itemId);
        if (display.equalsIgnoreCase(technical)) {
            return technical;
        }
        return display + " (" + technical + ")";
    }

    /**
     * Returns all registered MMOItems type names (e.g. "SWORD", "CONSUMABLE").
     * Returns empty list if MMOItems is not available.
     */
    @SuppressWarnings("unchecked")
    public static List<String> getTypeNames() {
        if (!isAvailable()) return List.of();
        try {
            Object typeMgr = getTypesMethod.invoke(mmoItemsPlugin);
            Method getAllMethod = typeMgr.getClass().getMethod("getAll");
            Collection<?> types = (Collection<?>) getAllMethod.invoke(typeMgr);
            List<String> names = new ArrayList<>();
            for (Object type : types) {
                Method getIdMethod = type.getClass().getMethod("getId");
                names.add((String) getIdMethod.invoke(type));
            }
            return names;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Returns all item IDs registered under a specific MMOItems type.
     * Returns empty list if the type doesn't exist or MMOItems is unavailable.
     */
    @SuppressWarnings("unchecked")
    public static List<String> getItemIds(String typeName) {
        if (!isAvailable()) return List.of();
        try {
            Object typeMgr = getTypesMethod.invoke(mmoItemsPlugin);
            Object type = typeGetMethod.invoke(typeMgr, typeName);
            if (type == null) return List.of();

            // MMOItems.plugin.getTemplates().getTemplates(type)
            Method getTemplatesManagerMethod = mmoItemsPlugin.getClass().getMethod("getTemplates");
            Object templateMgr = getTemplatesManagerMethod.invoke(mmoItemsPlugin);
            Method getTemplatesMethod = templateMgr.getClass().getMethod("getTemplates",
                    Class.forName("net.Indyuce.mmoitems.api.Type"));
            Collection<?> templates = (Collection<?>) getTemplatesMethod.invoke(templateMgr, type);

            List<String> ids = new ArrayList<>();
            for (Object template : templates) {
                Method getIdMethod = template.getClass().getMethod("getId");
                ids.add((String) getIdMethod.invoke(template));
            }
            return ids;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String extractDisplayName(ItemStack item, String fallback) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            String plain = PLAIN_TEXT.serialize(meta.displayName()).trim();
            if (!plain.isBlank()) {
                return plain;
            }
        }
        return fallback;
    }

    private static String formatIdentifier(String value) {
        String normalized = value.replace('-', ' ').replace('_', ' ').trim();
        if (normalized.isEmpty()) {
            return "Item especial";
        }

        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            String lower = part.toLowerCase(Locale.ROOT);
            builder.append(Character.toUpperCase(lower.charAt(0)))
                    .append(lower.substring(1));
        }
        return builder.toString();
    }
}
