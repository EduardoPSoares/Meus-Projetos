package me.ray.midgard.core.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.entity.Player;

public class MenuLoader {

    public static void loadItems(Player player, Inventory inventory, ConfigurationSection section, Map<Integer, String> actions, Function<String, String> placeholderReplacer) {
        if (section == null) { return; }

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) { continue; }

            try {
                ItemStack baseItem = null;

                if (itemSection.contains("nexo") && org.bukkit.Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
                    try {
                        baseItem = me.ray.midgard.core.integration.NexoUtils.getCustomItem(itemSection.getString("nexo"));
                    } catch (Throwable e) {
                        MidgardLogger.warn("Falha ao carregar item Nexo '%s': %s", itemSection.getString("nexo"), e.getMessage());
                    }
                }

                if (baseItem == null) {
                    String matName = itemSection.getString("material", "STONE").toUpperCase();
                    Material material = Material.matchMaterial(matName);
                    if (material == null) {
                        material = Material.STONE;
                    }
                    baseItem = new ItemStack(material);
                }

                ItemBuilder builder = new ItemBuilder(baseItem);

                if (itemSection.contains("name")) {
                    String name = itemSection.getString("name");
                    if (name != null) {
                        if (name.startsWith("key:")) {
                            name = MidgardCore.getLanguageManager().getRawMessage(name.substring(4));
                        }
                        if (placeholderReplacer != null) { name = placeholderReplacer.apply(name); }
                        builder.name(MessageUtils.parse(name));
                    }
                }

                if (itemSection.contains("lore")) {
                    List<String> lore = itemSection.getStringList("lore");
                    List<Component> loreComponents = new ArrayList<>();
                    for (String line : lore) {
                        if (line.startsWith("list:")) {
                            List<String> lines = MidgardCore.getLanguageManager().getStringList(line.substring(5));
                            for (String l : lines) {
                                if (placeholderReplacer != null) { l = placeholderReplacer.apply(l); }
                                loreComponents.add(MessageUtils.parse(l));
                            }
                        } else {
                            if (line.startsWith("key:")) {
                                line = MidgardCore.getLanguageManager().getRawMessage(line.substring(4));
                            }
                            if (placeholderReplacer != null) { line = placeholderReplacer.apply(line); }
                            loreComponents.add(MessageUtils.parse(line));
                        }
                    }
                    builder.lore(loreComponents);
                }

                if (itemSection.contains("model-data")) {
                    builder.customModelData(itemSection.getInt("model-data"));
                }
                
                if (itemSection.getBoolean("hide-attributes", false)) {
                    builder.flags(ItemFlag.HIDE_ATTRIBUTES);
                }
                
                if (itemSection.getBoolean("glow", false)) {
                    builder.glow();
                }

                ItemStack item = builder.build();
                String action = itemSection.getString("action");

                List<Integer> slots = new ArrayList<>();
                if (itemSection.contains("slots")) {
                    slots.addAll(itemSection.getIntegerList("slots"));
                } else if (itemSection.contains("slot")) {
                    slots.add(itemSection.getInt("slot"));
                }

                for (int slot : slots) {
                    if (slot >= 0 && slot < inventory.getSize()) {
                        inventory.setItem(slot, item);
                        if (action != null && actions != null) {
                            actions.put(slot, action);
                        }
                    }
                }
            } catch (Exception e) {
                MidgardLogger.error("Erro ao carregar item de menu", e);
            }
        }
    }
}
