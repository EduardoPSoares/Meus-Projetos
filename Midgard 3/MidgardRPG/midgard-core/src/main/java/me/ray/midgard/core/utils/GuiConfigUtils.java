package me.ray.midgard.core.utils;

import me.ray.midgard.core.i18n.LanguageManager;
import me.ray.midgard.core.text.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuiConfigUtils {

    public static ItemStack getItem(FileConfiguration config, String path, LanguageManager lang) {
        if (!config.contains(path)) { return null; }
        
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) { return null; }

        ItemStack baseItem = null;
        if (section.contains("nexo") && org.bukkit.Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
            try {
                baseItem = me.ray.midgard.core.integration.NexoUtils.getCustomItem(section.getString("nexo"));
            } catch (Throwable e) {
                me.ray.midgard.core.debug.MidgardLogger.warn("Falha ao carregar item Nexo '%s'", section.getString("nexo"));
            }
        }
        
        Material material;
        if (baseItem != null) {
            material = baseItem.getType();
        } else {
            String materialName = section.getString("material", "STONE");
            material = Material.matchMaterial(materialName);
            if (material == null) { material = Material.STONE; }
            baseItem = new ItemStack(material);
        }

        ItemBuilder builder = new ItemBuilder(baseItem);

        String nameKey = section.getString("name");
        if (nameKey != null) {
            Component name;
            if (lang != null && lang.hasKey(nameKey)) {
                name = lang.getMessage(nameKey);
            } else {
                name = MessageUtils.parse(nameKey);
            }
            builder.name(name);
        }

        if (section.contains("lore")) {
            List<Component> lore = new ArrayList<>();
            if (section.isList("lore")) {
                List<String> rawLore = section.getStringList("lore");
                for (String line : rawLore) {
                    if (lang != null && lang.hasKey(line)) {
                        lore.addAll(lang.getStringList(line).stream().map(MessageUtils::parse).collect(Collectors.toList()));
                    } else {
                        lore.add(MessageUtils.parse(line));
                    }
                }
            } else {
                String loreKey = section.getString("lore");
                if (lang != null && lang.hasKey(loreKey)) {
                    lore = lang.getStringList(loreKey).stream()
                            .map(MessageUtils::parse)
                            .collect(Collectors.toList());
                } else {
                    lore.add(MessageUtils.parse(loreKey));
                }
            }
            builder.lore(lore);
        }
        
        if (section.contains("model-data")) {
            builder.customModelData(section.getInt("model-data"));
        }

        return builder.build();
    }
    
    public static int getSlot(FileConfiguration config, String path) {
        return config.getInt(path + ".slot", -1);
    }
}
