package me.ray.midgard.modules.character.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.character.CharacterModule;
import me.ray.midgard.modules.classes.ClassData;
import me.ray.midgard.modules.classes.ClassesModule;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.CombatData;
import me.ray.midgard.modules.combat.CombatModule;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CharacterMenu extends BaseGui {

    private static int getMenuRows() {
        CharacterModule module = CharacterModule.getInstance();
        if (module == null || module.getCharacterConfig() == null) {
            return 6;
        }
        return module.getCharacterConfig().getInt("menu.rows", 6);
    }

    private static String getMenuTitle() {
        CharacterModule module = CharacterModule.getInstance();
        String fallback = module.getMessage("menu.title");
        if (fallback == null || fallback.equals("menu.title")) {
            fallback = "ᴘᴀɪɴᴇʟ ᴅᴏ ʜᴇʀᴏɪ";
        }
        if (module.getCharacterConfig() == null) {
            return fallback;
        }
        return module.getCharacterConfig().getString("menu.title", fallback);
    }

    public CharacterMenu(Player player) {
        super(player, getMenuRows(), getMenuTitle());

        String openMsg = CharacterModule.getInstance().getMessage("menu.opening");
        if (openMsg != null && !openMsg.isEmpty() && !openMsg.equals("menu.opening")) {
            me.ray.midgard.core.text.MessageUtils.send(player, openMsg);
        }
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

            Map<String, String> placeholders = buildPlaceholders(profile);

            // Equipment (Paper Doll)
            renderEquipment(config, placeholders);

            // Config-driven items
            if (config.isConfigurationSection("menu.items")) {
                for (String key : config.getConfigurationSection("menu.items").getKeys(false)) {
                    String path = "menu.items." + key;
                    int slot = config.getInt(path + ".slot");
                    if (slot >= 0 && slot < inventory.getSize()) {
                        try {
                            inventory.setItem(slot, CharacterMenuHelper.buildItemFromConfig(config, path, placeholders));
                        } catch (Exception e) {
                            MidgardLogger.warn("Erro ao construir item do menu '%s': %s", key, e.getMessage());
                        }
                    }
                }
            }

            fillEmpty();
        } catch (Exception e) {
            MidgardLogger.error("Erro crítico ao inicializar o CharacterMenu", e);
        }
    }

    // ========================================================
    // Placeholder Building
    // ========================================================

    private Map<String, String> buildPlaceholders(MidgardProfile profile) {
        Map<String, String> placeholders = new HashMap<>();

        // Player basics
        placeholders.put("%player_name%", player.getName() != null ? player.getName() : "Unknown");

        // Class data
        ClassData classData = profile.getData(ClassData.class);
        String noClassName = CharacterModule.getInstance().getMessage("menu.no_class");
        if (noClassName == null || noClassName.equals("menu.no_class")) {
            noClassName = "Nenhuma";
        }
        placeholders.put("%class_name%", (classData != null && classData.getClassName() != null) ? classData.getClassName() : noClassName);
        int level = classData != null ? classData.getLevel() : 1;
        int availablePoints = classData != null ? classData.getAttributePoints() : 0;
        int usedPoints = classData != null ? classData.getSpentPoints().values().stream().mapToInt(Integer::intValue).sum() : 0;
        placeholders.put("%class_level%", String.valueOf(level));
        placeholders.put("%available_points%", String.valueOf(availablePoints));
        placeholders.put("%max_points%", String.valueOf(availablePoints + usedPoints));

        // Skill tree points
        int skillPoints = classData != null ? classData.getSkillPoints() : 0;
        int skillNodes = classData != null ? classData.getUnlockedSkillNodes().size() : 0;
        placeholders.put("%skill_points%", String.valueOf(skillPoints));
        placeholders.put("%skill_nodes_unlocked%", String.valueOf(skillNodes));

        // Health
        placeholders.put("%health%", String.valueOf((int) Math.max(0, player.getHealth())));
        org.bukkit.attribute.AttributeInstance maxHealthAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        placeholders.put("%max_health%", String.valueOf(maxHealthAttr != null ? (int) maxHealthAttr.getValue() : 20));

        // Mana
        addManaPlaceholders(placeholders, profile);

        // Attribute points spent
        CharacterMenuHelper.addStatsPlaceholders(placeholders, classData, CombatAttributes.STRENGTH, "strength");
        CharacterMenuHelper.addStatsPlaceholders(placeholders, classData, CombatAttributes.DEXTERITY, "dexterity");
        CharacterMenuHelper.addStatsPlaceholders(placeholders, classData, CombatAttributes.INTELLIGENCE, "intelligence");
        CharacterMenuHelper.addStatsPlaceholders(placeholders, classData, CombatAttributes.DEFENSE, "defense");
        CharacterMenuHelper.addStatsPlaceholders(placeholders, classData, CombatAttributes.AGILITY, "agility");

        // Attribute totals
        CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%strength%", CombatAttributes.STRENGTH);
        CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%dexterity%", CombatAttributes.DEXTERITY);
        CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%intelligence%", CombatAttributes.INTELLIGENCE);
        CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%defense%", CombatAttributes.DEFENSE);
        CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%agility%", CombatAttributes.AGILITY);

        // Defenses
        CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%defense_earth%", CombatAttributes.EARTH_DEFENSE);
        CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%defense_thunder%", CombatAttributes.THUNDER_DEFENSE);
        CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%defense_water%", CombatAttributes.WATER_DEFENSE);
        CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%defense_fire%", CombatAttributes.FIRE_DEFENSE);
        CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%defense_air%", CombatAttributes.AIR_DEFENSE);

        // Damage & Stats
        CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%damage%", CombatAttributes.PHYSICAL_DAMAGE);
        CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%spell_damage%", CombatAttributes.SKILL_DAMAGE);
        CharacterMenuHelper.addAttributePlaceholder(placeholders, profile, "%health_regen%", CombatAttributes.HEALTH_REGEN);

        // XP Percent
        placeholders.put("%xp_percent%", calculateXpPercent(profile));

        // Race (soft-dependency)
        addRacePlaceholders(placeholders, profile);

        // Spells (soft-dependency)
        addSpellPlaceholders(placeholders, profile);

        // Professions (soft-dependency)
        addProfessionPlaceholders(placeholders, profile);

        // Economy (soft-dependency)
        addEconomyPlaceholders(placeholders);

        return placeholders;
    }

    // ========================================================
    // Soft-Dependency Placeholder Methods
    // ========================================================

    private void addManaPlaceholders(Map<String, String> placeholders, MidgardProfile profile) {
        try {
            CombatData combatData = profile.getData(CombatData.class);
            if (combatData != null) {
                placeholders.put("%mana%", String.valueOf((int) combatData.getCurrentMana()));
            } else {
                placeholders.put("%mana%", "0");
            }
            double maxMana = CharacterMenuHelper.getAttributeValue(profile, CombatAttributes.MAX_MANA);
            placeholders.put("%max_mana%", String.valueOf((int) maxMana));
        } catch (Exception | NoClassDefFoundError e) {
            placeholders.put("%mana%", "0");
            placeholders.put("%max_mana%", "0");
        }
    }

    private void addRacePlaceholders(Map<String, String> placeholders, MidgardProfile profile) {
        try {
            me.ray.midgard.modules.races.data.RaceData raceData = profile.getData(me.ray.midgard.modules.races.data.RaceData.class);
            if (raceData != null && raceData.hasRace()) {
                me.ray.midgard.modules.races.RacesModule racesModule = me.ray.midgard.modules.races.RacesModule.getInstance();
                if (racesModule != null && racesModule.getRaceManager() != null) {
                    me.ray.midgard.modules.races.model.Race race = racesModule.getRaceManager().getRace(raceData.getRaceId());
                    placeholders.put("%race_name%", race != null ? race.getDisplayName() : raceData.getRaceId());
                } else {
                    placeholders.put("%race_name%", raceData.getRaceId());
                }
                placeholders.put("%race_level%", String.valueOf(raceData.getLevel()));
            } else {
                placeholders.put("%race_name%", "Nenhuma");
                placeholders.put("%race_level%", "0");
            }
        } catch (Exception | NoClassDefFoundError e) {
            placeholders.put("%race_name%", "—");
            placeholders.put("%race_level%", "—");
        }
    }

    @SuppressWarnings("unused")
    private void addSpellPlaceholders(Map<String, String> placeholders, MidgardProfile profile) {
        try {
            me.ray.midgard.modules.spells.data.SpellProfile spellProfile = profile.getData(me.ray.midgard.modules.spells.data.SpellProfile.class);
            if (spellProfile != null) {
                placeholders.put("%spells_learned%", String.valueOf(spellProfile.getUnlockedSpells().size()));
                placeholders.put("%spells_mastered%", String.valueOf(spellProfile.getMasteredSpells().size()));
                String ult = spellProfile.getEquippedUltimate();
                placeholders.put("%ultimate_name%", ult != null ? ult : "Nenhuma");
            } else {
                placeholders.put("%spells_learned%", "0");
                placeholders.put("%spells_mastered%", "0");
                placeholders.put("%ultimate_name%", "Nenhuma");
            }
        } catch (Exception | NoClassDefFoundError e) {
            placeholders.put("%spells_learned%", "—");
            placeholders.put("%spells_mastered%", "—");
            placeholders.put("%ultimate_name%", "—");
        }
    }

    private void addProfessionPlaceholders(Map<String, String> placeholders, MidgardProfile profile) {
        try {
            @SuppressWarnings("unchecked")
            me.ray.midgard.modules.professions.blacksmith.forge.data.ForgeData forgeData =
                    (me.ray.midgard.modules.professions.blacksmith.forge.data.ForgeData)
                            profile.getData((Class) me.ray.midgard.modules.professions.blacksmith.forge.data.ForgeData.class);
            if (forgeData != null) {
                placeholders.put("%forge_level%", String.valueOf(forgeData.getLevel()));
                placeholders.put("%forge_spec%", forgeData.getSpecialization() != null ? forgeData.getSpecialization() : "Nenhuma");
                placeholders.put("%recipes_unlocked%", String.valueOf(forgeData.getUnlockedRecipes().size()));
            } else {
                placeholders.put("%forge_level%", "0");
                placeholders.put("%forge_spec%", "Nenhuma");
                placeholders.put("%recipes_unlocked%", "0");
            }
        } catch (Exception | NoClassDefFoundError e) {
            placeholders.put("%forge_level%", "—");
            placeholders.put("%forge_spec%", "—");
            placeholders.put("%recipes_unlocked%", "—");
        }
    }

    private void addEconomyPlaceholders(Map<String, String> placeholders) {
        try {
            me.ray.midgard.modules.economy.EconomyModule ecoModule = me.ray.midgard.modules.economy.EconomyModule.getInstance();
            if (ecoModule != null && ecoModule.getCurrencyManager() != null) {
                int balance = ecoModule.getCurrencyManager().getPhysicalBalance(player);
                placeholders.put("%balance%", String.valueOf(balance));
            } else {
                placeholders.put("%balance%", "0");
            }
        } catch (Exception | NoClassDefFoundError e) {
            placeholders.put("%balance%", "—");
        }
    }

    private String calculateXpPercent(MidgardProfile profile) {
        try {
            CombatModule combatModule = CombatModule.getInstance();
            if (combatModule != null && combatModule.getLevelManager() != null) {
                CombatData combatData = profile.getData(CombatData.class);
                if (combatData != null) {
                    double currentXp = combatData.getExperience();
                    double requiredXp = combatModule.getLevelManager().getRequiredXp(combatData.getLevel());
                    if (requiredXp > 0) {
                        double percent = Math.min(100, Math.max(0, (currentXp / requiredXp) * 100));
                        if (Double.isNaN(percent) || Double.isInfinite(percent)) {
                            percent = 0;
                        }
                        return String.format("%.1f%%", percent);
                    }
                    String maxLevelStr = CharacterModule.getInstance().getMessage("menu.xp_max_level");
                    return (maxLevelStr != null && !maxLevelStr.equals("menu.xp_max_level")) ? maxLevelStr : "MAX";
                }
            }
        } catch (Exception e) {
            MidgardLogger.debug("Erro ao calcular XP percent: %s", e.getMessage());
        }
        return "0%";
    }

    // ========================================================
    // Equipment Rendering
    // ========================================================

    private void renderEquipment(FileConfiguration config, Map<String, String> placeholders) {
        if (!config.isConfigurationSection("menu.equipment")) {
            return;
        }

        for (String key : config.getConfigurationSection("menu.equipment").getKeys(false)) {
            String path = "menu.equipment." + key;
            int slot = config.getInt(path + ".slot");
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }

            ItemStack item = switch (key.toLowerCase()) {
                case "helmet" -> player.getInventory().getHelmet();
                case "chestplate" -> player.getInventory().getChestplate();
                case "leggings" -> player.getInventory().getLeggings();
                case "boots" -> player.getInventory().getBoots();
                case "mainhand" -> player.getInventory().getItemInMainHand();
                case "offhand" -> player.getInventory().getItemInOffHand();
                default -> null;
            };

            if (item == null || item.getType() == Material.AIR) {
                if (config.contains(path + ".placeholder")) {
                    inventory.setItem(slot, CharacterMenuHelper.buildItemFromConfig(config, path + ".placeholder", placeholders));
                }
            } else {
                inventory.setItem(slot, item.clone());
            }
        }
    }

    // ========================================================
    // Click Handling
    // ========================================================

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        try {
            int slot = event.getSlot();
            FileConfiguration config = CharacterModule.getInstance().getCharacterConfig();
            if (config == null) {
                return;
            }

            // Find which item was clicked and its action
            String action = findAction(config, slot);
            if (action != null && !action.isEmpty()) {
                handleAction(action);
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar clique no CharacterMenu", e);
            me.ray.midgard.core.text.MessageUtils.send(player, CharacterModule.getInstance().getMessage("menu.menu_error"));
        }
    }

    private String findAction(FileConfiguration config, int slot) {
        if (!config.isConfigurationSection("menu.items")) {
            return null;
        }
        for (String key : config.getConfigurationSection("menu.items").getKeys(false)) {
            String path = "menu.items." + key;
            if (config.getInt(path + ".slot") == slot) {
                return config.getString(path + ".action", null);
            }
        }
        return null;
    }

    private void handleAction(String action) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        switch (action.toLowerCase()) {
            case "open_attribute_menu" -> new AttributeMenu(player).open();

            case "open_race_menu" -> {
                try {
                    me.ray.midgard.modules.races.RacesModule racesModule = me.ray.midgard.modules.races.RacesModule.getInstance();
                    if (racesModule != null) {
                        new me.ray.midgard.modules.races.gui.RaceMainMenuGui(player).open();
                    }
                } catch (Exception | NoClassDefFoundError e) {
                    MidgardLogger.debug("Módulo de raças não disponível");
                }
            }

            case "open_spell_menu" -> {
                try {
                    me.ray.midgard.modules.spells.SpellsModule spellsModule = (me.ray.midgard.modules.spells.SpellsModule)
                            MidgardCore.getModuleManager().getModule("Spells");
                    if (spellsModule != null) {
                        new me.ray.midgard.modules.spells.gui.MainSpellGUI(player, spellsModule).open();
                    }
                } catch (Exception | NoClassDefFoundError e) {
                    MidgardLogger.debug("Módulo de magias não disponível");
                }
            }

            case "open_skill_tree" -> {
                try {
                    ClassesModule classesModule = ClassesModule.getInstance();
                    if (classesModule != null && classesModule.getSkillTreeManager() != null) {
                        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
                        ClassData data = profile != null ? profile.getData(ClassData.class) : null;
                        if (data != null && data.getClassName() != null) {
                            var tree = classesModule.getSkillTreeManager().getSkillTree(data.getClassName().toLowerCase());
                            if (tree != null) {
                                new me.ray.midgard.modules.classes.skilltree.gui.SkillTreeGui(player, tree).open();
                            }
                        }
                    }
                } catch (Exception | NoClassDefFoundError e) {
                    MidgardLogger.debug("Skill tree não disponível");
                }
            }

            case "open_forge_menu" -> {
                try {
                    me.ray.midgard.modules.professions.ProfessionsModule profModule = me.ray.midgard.modules.professions.ProfessionsModule.getInstance();
                    if (profModule != null) {
                        // ForgeMainGui needs a forge instance - just send player to command
                        player.performCommand("forge");
                    }
                } catch (Exception | NoClassDefFoundError e) {
                    MidgardLogger.debug("Módulo de profissões não disponível");
                }
            }

            default -> { /* no action */ }
        }
    }

    // ========================================================
    // Fill Empty Slots
    // ========================================================

    private void fillEmpty() {
        FileConfiguration config = CharacterModule.getInstance().getCharacterConfig();
        if (!config.getBoolean("menu.filler.enabled", true)) {
            return;
        }

        ItemStack filler = CharacterMenuHelper.buildItemFromConfig(config, "menu.filler", Map.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, filler);
            }
        }
    }
}
