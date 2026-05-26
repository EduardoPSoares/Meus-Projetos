package com.midgard.fooddecay.multiblock;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents an additional ingredient required by a multiblock recipe.
 * Supports vanilla items, MMOItems and ItemsAdder with optional custom model data.
 */
public final class RecipeIngredient {

    private final Material material;
    private final String mmoType;
    private final String mmoId;
    private final String itemsAdderId;
    private final int customModelData;
    private final int amount;

    public RecipeIngredient(Material material,
                            String mmoType,
                            String mmoId,
                            String itemsAdderId,
                            int customModelData,
                            int amount) {
        this.material = material;
        this.mmoType = blankToNull(mmoType);
        this.mmoId = blankToNull(mmoId);
        this.itemsAdderId = blankToNull(itemsAdderId);
        this.customModelData = Math.max(0, customModelData);
        this.amount = Math.max(1, amount);
    }

    public Material getMaterial() {
        return material;
    }

    public String getMmoType() {
        return mmoType;
    }

    public String getMmoId() {
        return mmoId;
    }

    public String getItemsAdderId() {
        return itemsAdderId;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public int getAmount() {
        return amount;
    }

    public boolean matches(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        if (itemsAdderId != null) {
            return ItemsAdderHook.matchesItem(item, itemsAdderId);
        }

        if (mmoType != null && mmoId != null) {
            return MMOItemsHook.matchesItem(item, mmoType, mmoId);
        }

        if (material == null || item.getType() != material) {
            return false;
        }

        if (customModelData > 0) {
            ItemMeta meta = item.getItemMeta();
            return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == customModelData;
        }

        return true;
    }

    public ItemStack createPreview() {
        if (itemsAdderId != null) {
            ItemStack preview = ItemsAdderHook.createItem(itemsAdderId);
            if (preview != null) {
                preview.setAmount(Math.min(amount, preview.getMaxStackSize()));
                return preview;
            }
        }

        if (mmoType != null && mmoId != null) {
            ItemStack preview = MMOItemsHook.createItem(mmoType, mmoId);
            if (preview != null) {
                preview.setAmount(Math.min(amount, preview.getMaxStackSize()));
                return preview;
            }
        }

        ItemStack preview = new ItemStack(material != null ? material : Material.CHEST);
        preview.setAmount(Math.min(amount, preview.getMaxStackSize()));
        if (customModelData > 0) {
            ItemMeta meta = preview.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                preview.setItemMeta(meta);
            }
        }
        return preview;
    }

    public String getReferenceLabel() {
        String base;
        if (itemsAdderId != null) {
            base = ItemsAdderHook.getItemReferenceLabel(itemsAdderId);
        } else if (mmoType != null && mmoId != null) {
            base = MMOItemsHook.getItemReferenceLabel(mmoType, mmoId);
        } else if (material != null) {
            base = formatMaterial(material.name());
            if (customModelData > 0) {
                base += " (CMD " + customModelData + ")";
            }
        } else {
            base = "Ingrediente invalido";
        }

        return amount > 1 ? base + " x" + amount : base;
    }

    public Map<String, Object> toConfigMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (itemsAdderId != null) {
            map.put("itemsadder", itemsAdderId);
        } else if (mmoType != null && mmoId != null) {
            map.put("mmoitems", mmoType + ":" + mmoId);
        } else if (material != null) {
            map.put("material", material.name());
        }
        if (customModelData > 0) {
            map.put("custom-model-data", customModelData);
        }
        if (amount > 1) {
            map.put("amount", amount);
        }
        return map;
    }

    public String toShorthand() {
        String base;
        if (itemsAdderId != null) {
            base = "ia:" + itemsAdderId;
        } else if (mmoType != null && mmoId != null) {
            base = "mmo:" + mmoType + ":" + mmoId;
        } else if (material != null) {
            base = material.name();
            if (customModelData > 0) {
                base += "#" + customModelData;
            }
        } else {
            base = "INVALID";
        }

        return amount > 1 ? base + "*" + amount : base;
    }

    public static RecipeIngredient fromConfigMap(Map<?, ?> rawMap) {
        if (rawMap == null || rawMap.isEmpty()) {
            return null;
        }

        Object itemsAdderValue = rawMap.get("itemsadder");
        Object mmoItemsValue = rawMap.get("mmoitems");
        Object materialValue = rawMap.get("material");
        String itemsAdderId = itemsAdderValue instanceof String value && !value.isBlank()
                ? value.trim()
                : null;
        String mmoItems = mmoItemsValue instanceof String value && !value.isBlank()
                ? value.trim()
                : null;
        Material material = null;
        String mmoType = null;
        String mmoId = null;

        if (itemsAdderId == null && mmoItems != null && mmoItems.contains(":")) {
            String[] parts = mmoItems.split(":", 2);
            mmoType = blankToNull(parts[0]);
            mmoId = blankToNull(parts[1]);
        }

        if (itemsAdderId == null && mmoType == null && materialValue instanceof String materialName) {
            try {
                material = Material.valueOf(materialName.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        if (itemsAdderId == null && mmoType == null && material == null) {
            return null;
        }

        int customModelData = parseInt(rawMap.get("custom-model-data"), 0);
        int amount = Math.max(1, parseInt(rawMap.get("amount"), 1));
        return new RecipeIngredient(material, mmoType, mmoId, itemsAdderId, customModelData, amount);
    }

    public static RecipeIngredient parseShorthand(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String token = rawValue.trim();
        int amount = 1;
        int amountSep = token.lastIndexOf('*');
        if (amountSep > 0 && amountSep < token.length() - 1) {
            try {
                amount = Math.max(1, Integer.parseInt(token.substring(amountSep + 1).trim()));
                token = token.substring(0, amountSep).trim();
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        if (token.regionMatches(true, 0, "mmo:", 0, 4)) {
            String payload = token.substring(4).trim();
            String[] parts = payload.split(":", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                return null;
            }
            return new RecipeIngredient(null, parts[0].trim(), parts[1].trim(), null, 0, amount);
        }

        if (token.regionMatches(true, 0, "ia:", 0, 3)) {
            String payload = token.substring(3).trim();
            if (!payload.contains(":")) {
                return null;
            }
            return new RecipeIngredient(null, null, null, payload, 0, amount);
        }

        int customModelData = 0;
        int cmdSep = token.lastIndexOf('#');
        if (cmdSep > 0 && cmdSep < token.length() - 1) {
            try {
                customModelData = Math.max(0, Integer.parseInt(token.substring(cmdSep + 1).trim()));
                token = token.substring(0, cmdSep).trim();
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        try {
            Material material = Material.valueOf(token.toUpperCase(Locale.ROOT));
            return new RecipeIngredient(material, null, null, null, customModelData, amount);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String formatMaterial(String name) {
        String[] parts = name.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.charAt(0))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static int parseInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
