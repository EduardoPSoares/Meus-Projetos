package com.midgard.fooddecay.multiblock;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reflection-based hook for ItemsAdder integration.
 * All calls are safe even if ItemsAdder is not installed.
 */
public final class ItemsAdderHook {

    public record ItemsAdderItemReference(String namespacedId, Material material, String displayName) {
    }

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private static Boolean available;
    private static Method customStackByItemStackMethod;
    private static Method customStackGetInstanceMethod;
    private static Method customStackGetItemStackMethod;
    private static Method customStackGetDisplayNameMethod;
    private static Method customStackGetNamespacedIdMethod;
    private static Method customStackGetNamespacedIdsInRegistryMethod;

    private ItemsAdderHook() {
    }

    public static boolean isAvailable() {
        if (available == null) {
            available = init();
        }
        return available;
    }

    private static boolean init() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("ItemsAdder");
            if (plugin == null || !plugin.isEnabled()) {
                return false;
            }

            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            customStackByItemStackMethod = customStackClass.getMethod("byItemStack", ItemStack.class);
            customStackGetInstanceMethod = customStackClass.getMethod("getInstance", String.class);
            customStackGetItemStackMethod = customStackClass.getMethod("getItemStack");
            customStackGetDisplayNameMethod = customStackClass.getMethod("getDisplayName");
            customStackGetNamespacedIdMethod = customStackClass.getMethod("getNamespacedID");
            customStackGetNamespacedIdsInRegistryMethod = customStackClass.getMethod("getNamespacedIdsInRegistry");
            return true;
        } catch (Exception e) {
            Logger logger = Bukkit.getServer() != null
                    ? Bukkit.getLogger()
                    : Logger.getLogger(ItemsAdderHook.class.getName());
            logger.log(Level.INFO,
                    "[FoodDecay] ItemsAdder not found or incompatible, ItemsAdder recipes disabled.");
            return false;
        }
    }

    public static boolean matchesItem(ItemStack item, String namespacedId) {
        if (!isAvailable() || item == null || namespacedId == null || namespacedId.isBlank()) {
            return false;
        }
        try {
            Object customStack = customStackByItemStackMethod.invoke(null, item);
            if (customStack == null) {
                return false;
            }
            String currentId = (String) customStackGetNamespacedIdMethod.invoke(customStack);
            return namespacedId.equalsIgnoreCase(currentId);
        } catch (Exception e) {
            return false;
        }
    }

    public static ItemsAdderItemReference identifyItem(ItemStack item) {
        if (!isAvailable() || item == null || item.getType().isAir()) {
            return null;
        }
        try {
            Object customStack = customStackByItemStackMethod.invoke(null, item);
            if (customStack == null) {
                return null;
            }

            String namespacedId = (String) customStackGetNamespacedIdMethod.invoke(customStack);
            if (namespacedId == null || namespacedId.isBlank()) {
                return null;
            }

            String displayName = safeDisplayName(customStack, item, formatNamespacedId(namespacedId));
            return new ItemsAdderItemReference(namespacedId, item.getType(), displayName);
        } catch (Exception e) {
            return null;
        }
    }

    public static ItemStack createItem(String namespacedId) {
        if (!isAvailable() || namespacedId == null || namespacedId.isBlank()) {
            return null;
        }
        try {
            Object customStack = customStackGetInstanceMethod.invoke(null, namespacedId);
            if (customStack == null) {
                return null;
            }
            return (ItemStack) customStackGetItemStackMethod.invoke(customStack);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getItemDisplayName(String namespacedId) {
        if (namespacedId == null || namespacedId.isBlank()) {
            return "Item customizado";
        }

        try {
            if (isAvailable()) {
                Object customStack = customStackGetInstanceMethod.invoke(null, namespacedId);
                if (customStack != null) {
                    ItemStack item = (ItemStack) customStackGetItemStackMethod.invoke(customStack);
                    return safeDisplayName(customStack, item, formatNamespacedId(namespacedId));
                }
            }
        } catch (Exception ignored) {
        }

        return formatNamespacedId(namespacedId);
    }

    public static String getItemReferenceLabel(String namespacedId) {
        String display = getItemDisplayName(namespacedId);
        if (display.equalsIgnoreCase(namespacedId)) {
            return namespacedId;
        }
        return display + " (" + namespacedId + ")";
    }

    public static List<String> getNamespaces() {
        Set<String> namespaces = new LinkedHashSet<>();
        for (String namespacedId : getNamespacedIds()) {
            int separator = namespacedId.indexOf(':');
            if (separator > 0) {
                namespaces.add(namespacedId.substring(0, separator));
            }
        }
        return new ArrayList<>(namespaces);
    }

    public static List<String> getItemIds(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return List.of();
        }

        List<String> ids = new ArrayList<>();
        for (String namespacedId : getNamespacedIds()) {
            String prefix = namespace + ":";
            if (namespacedId.regionMatches(true, 0, prefix, 0, prefix.length())) {
                ids.add(namespacedId.substring(prefix.length()));
            }
        }
        return ids;
    }

    private static List<String> getNamespacedIds() {
        if (!isAvailable()) {
            return List.of();
        }
        try {
            Object result = customStackGetNamespacedIdsInRegistryMethod.invoke(null);
            if (!(result instanceof Collection<?> collection)) {
                return List.of();
            }

            List<String> ids = new ArrayList<>();
            for (Object value : collection) {
                if (value instanceof String id && !id.isBlank()) {
                    ids.add(id);
                }
            }
            return ids;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String safeDisplayName(Object customStack, ItemStack item, String fallback) {
        try {
            String displayName = (String) customStackGetDisplayNameMethod.invoke(customStack);
            if (displayName != null && !displayName.isBlank()) {
                return displayName;
            }
        } catch (Exception ignored) {
        }

        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
                String displayName = PLAIN_TEXT.serialize(meta.displayName()).trim();
                if (!displayName.isBlank()) {
                    return displayName;
                }
            }
        }

        return fallback;
    }

    private static String formatNamespacedId(String namespacedId) {
        String raw = namespacedId;
        int separator = raw.indexOf(':');
        if (separator >= 0 && separator + 1 < raw.length()) {
            raw = raw.substring(separator + 1);
        }

        String normalized = raw.replace('-', ' ').replace('_', ' ').trim();
        if (normalized.isEmpty()) {
            return "Item customizado";
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
