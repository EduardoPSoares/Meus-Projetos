package me.ray.midgard.modules.character.gui;

import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.classes.ClassData;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Métodos utilitários compartilhados entre CharacterMenu e AttributeMenu.
 */
final class CharacterMenuHelper {

    private CharacterMenuHelper() {}

    static void addAttributePlaceholder(Map<String, String> map, MidgardProfile profile, String placeholder, String attribute) {
        double val = getAttributeValue(profile, attribute);
        String color = (val >= 0) ? "<green>+" : "<red>";
        String numStr = (val % 1 == 0) ? String.valueOf((int) val) : String.format("%.1f", val);
        map.put(placeholder, color + numStr);
    }

    static void addStatsPlaceholders(Map<String, String> map, ClassData data, String attrId, String keyPrefix) {
        int pts = (data != null) ? data.getSpentPoints(attrId) : 0;
        map.put("%" + keyPrefix + "_pts%", String.valueOf(pts));
        map.put("%" + keyPrefix + "_pts_next%", String.valueOf(pts + 1));
    }

    static double getAttributeValue(MidgardProfile profile, String attr) {
        if (profile == null) {
            return 0;
        }
        CoreAttributeData data = profile.getData(CoreAttributeData.class);
        if (data == null) {
            return 0;
        }
        AttributeInstance instance = data.getInstance(attr);
        return instance != null ? instance.getValue() : 0;
    }

    static ItemStack buildItemFromConfig(FileConfiguration config, String path, Map<String, String> placeholders) {
        String matName = config.getString(path + ".material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) {
            mat = Material.STONE;
        }

        ItemBuilder builder = new ItemBuilder(mat);

        String name = config.getString(path + ".name");
        if (name != null) {
            builder.setName(applyPlaceholders(name, placeholders));
        }

        if (config.contains(path + ".lore")) {
            List<String> lore = config.getStringList(path + ".lore");
            for (String line : lore) {
                builder.addLore(applyPlaceholders(line, placeholders));
            }
        }

        if (config.contains(path + ".skull_owner")) {
            String owner = applyPlaceholders(config.getString(path + ".skull_owner"), placeholders);
            builder.skullOwner(org.bukkit.Bukkit.getOfflinePlayer(owner));
        }

        if (config.contains(path + ".flags")) {
            List<String> flags = config.getStringList(path + ".flags");
            for (String flagName : flags) {
                try {
                    builder.flags(ItemFlag.valueOf(flagName));
                } catch (IllegalArgumentException e) {
                    MidgardLogger.warn("Flag de item inválida na config: %s", flagName);
                }
            }
        }

        return builder.build();
    }

    static String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null) {
            return null;
        }
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
