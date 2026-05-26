package me.ray.midgard.modules.character.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.character.CharacterModule;
import me.ray.midgard.modules.classes.ClassData;
import me.ray.midgard.modules.classes.ClassesModule;
import me.ray.midgard.modules.combat.CombatAttributes;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributeMenu extends BaseGui {

    private static int getMenuRows() {
        CharacterModule module = CharacterModule.getInstance();
        if (module == null || module.getCharacterConfig() == null) {
            return 4;
        }
        return module.getCharacterConfig().getInt("attribute-menu.rows", 4);
    }

    private static String getMenuTitle() {
        CharacterModule module = CharacterModule.getInstance();
        if (module == null || module.getCharacterConfig() == null) {
            return "ᴀᴛʀɪʙᴜᴛᴏꜱ";
        }
        return module.getCharacterConfig().getString("attribute-menu.title", "ᴀᴛʀɪʙᴜᴛᴏꜱ");
    }

    public AttributeMenu(Player player) {
        super(player, getMenuRows(), getMenuTitle());
    }

    @Override
    public void initializeItems() {
        try {
            FileConfiguration config = CharacterModule.getInstance().getCharacterConfig();
            if (config == null) {
                return;
            }

            MidgardProfile profile = MidgardCore.getProfileManager() != null
                    ? MidgardCore.getProfileManager().getProfile(player) : null;
            if (profile == null) {
                return;
            }

            ClassData classData = profile.getData(ClassData.class);
            int availablePoints = classData != null ? classData.getAttributePoints() : 0;
            int usedPoints = classData != null ? classData.getSpentPoints().values().stream().mapToInt(Integer::intValue).sum() : 0;
            int maxPoints = availablePoints + usedPoints;

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%player_name%", player.getName() != null ? player.getName() : "Unknown");
            String noClassName = CharacterModule.getInstance().getMessage("menu.no_class");
            if (noClassName == null || noClassName.equals("menu.no_class")) {
                noClassName = "Nenhuma";
            }
            placeholders.put("%class_name%", (classData != null && classData.getClassName() != null) ? classData.getClassName() : noClassName);
            placeholders.put("%available_points%", String.valueOf(availablePoints));
            placeholders.put("%max_points%", String.valueOf(maxPoints));

            CharacterMenuHelper.addStatsPlaceholders(placeholders, classData, CombatAttributes.STRENGTH, "strength");
            CharacterMenuHelper.addStatsPlaceholders(placeholders, classData, CombatAttributes.DEXTERITY, "dexterity");
            CharacterMenuHelper.addStatsPlaceholders(placeholders, classData, CombatAttributes.INTELLIGENCE, "intelligence");
            CharacterMenuHelper.addStatsPlaceholders(placeholders, classData, CombatAttributes.DEFENSE, "defense");
            CharacterMenuHelper.addStatsPlaceholders(placeholders, classData, CombatAttributes.AGILITY, "agility");

            CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%strength%", CombatAttributes.STRENGTH);
            CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%dexterity%", CombatAttributes.DEXTERITY);
            CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%intelligence%", CombatAttributes.INTELLIGENCE);
            CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%defense%", CombatAttributes.DEFENSE);
            CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%agility%", CombatAttributes.AGILITY);

            // Header
            if (config.isConfigurationSection("attribute-menu.header")) {
                int headerSlot = config.getInt("attribute-menu.header.slot", 4);
                if (headerSlot >= 0 && headerSlot < inventory.getSize()) {
                    inventory.setItem(headerSlot, CharacterMenuHelper.buildItemFromConfig(config, "attribute-menu.header", placeholders));
                }
            }

            // Attribute items
            if (config.isConfigurationSection("attribute-menu.items")) {
                for (String key : config.getConfigurationSection("attribute-menu.items").getKeys(false)) {
                    String path = "attribute-menu.items." + key;
                    int slot = config.getInt(path + ".slot");
                    if (slot >= 0 && slot < inventory.getSize()) {
                        try {
                            inventory.setItem(slot, CharacterMenuHelper.buildItemFromConfig(config, path, placeholders));
                        } catch (Exception e) {
                            MidgardLogger.warn("Erro ao construir item de atributo '%s': %s", key, e.getMessage());
                        }
                    }
                }
            }

            fillEmpty(config);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao inicializar AttributeMenu", e);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        try {
            int slot = event.getSlot();
            FileConfiguration config = CharacterModule.getInstance().getCharacterConfig();
            if (config == null) {
                return;
            }

            // Check for back button
            if (config.isConfigurationSection("attribute-menu.items")) {
                for (String key : config.getConfigurationSection("attribute-menu.items").getKeys(false)) {
                    String path = "attribute-menu.items." + key;
                    if (config.getInt(path + ".slot") == slot) {
                        String action = config.getString(path + ".action", "");
                        if ("back".equalsIgnoreCase(action)) {
                            new CharacterMenu(player).open();
                            return;
                        }
                    }
                }
            }

            // Resolve attribute from slot
            String attribute = resolveAttribute(config, slot);
            if (attribute == null) {
                return;
            }

            MidgardProfile profile = MidgardCore.getProfileManager() != null
                    ? MidgardCore.getProfileManager().getProfile(player) : null;
            if (profile == null) {
                return;
            }

            ClassData data = profile.getData(ClassData.class);
            if (data == null) {
                return;
            }

            int amount = event.isShiftClick() ? 5 : 1;
            if (data.getAttributePoints() >= amount) {
                data.setAttributePoints(data.getAttributePoints() - amount);
                data.addSpentPoints(attribute, amount);

                ClassesModule classesModule = ClassesModule.getInstance();
                if (classesModule != null && data.getClassName() != null) {
                    me.ray.midgard.modules.classes.ClassManager classManager = classesModule.getClassManager();
                    if (classManager != null) {
                        me.ray.midgard.modules.classes.RPGClass rpgClass = classManager.getClass(data.getClassName());
                        if (rpgClass != null) {
                            classesModule.applyClassAttributes(profile, rpgClass, data.getLevel());
                        }
                    }
                }

                // Persistir imediatamente
                MidgardCore.getProfileManager().saveProfile(profile);

                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                initializeItems();
            } else {
                me.ray.midgard.core.text.MessageUtils.send(player, CharacterModule.getInstance().getMessage("menu.not_enough_points"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar clique no AttributeMenu", e);
            me.ray.midgard.core.text.MessageUtils.send(player, CharacterModule.getInstance().getMessage("menu.menu_error"));
        }
    }

    private String resolveAttribute(FileConfiguration config, int slot) {
        if (!config.isConfigurationSection("attribute-menu.items")) {
            return null;
        }

        for (String key : config.getConfigurationSection("attribute-menu.items").getKeys(false)) {
            String path = "attribute-menu.items." + key;
            if (config.getInt(path + ".slot") == slot) {
                String attrKey = config.getString(path + ".attribute", null);
                if (attrKey == null) {
                    return null;
                }
                return switch (attrKey.toLowerCase()) {
                    case "strength" -> CombatAttributes.STRENGTH;
                    case "dexterity" -> CombatAttributes.DEXTERITY;
                    case "intelligence" -> CombatAttributes.INTELLIGENCE;
                    case "defense" -> CombatAttributes.DEFENSE;
                    case "agility" -> CombatAttributes.AGILITY;
                    default -> null;
                };
            }
        }
        return null;
    }

    private void fillEmpty(FileConfiguration config) {
        if (!config.getBoolean("attribute-menu.filler.enabled", true)) {
            return;
        }
        String path = "attribute-menu.filler";
        ItemStack filler = CharacterMenuHelper.buildItemFromConfig(config, path, Map.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, filler);
            }
        }
    }
}
