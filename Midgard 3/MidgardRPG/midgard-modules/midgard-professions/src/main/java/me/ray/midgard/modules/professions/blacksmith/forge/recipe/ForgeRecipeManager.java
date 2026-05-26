package me.ray.midgard.modules.professions.blacksmith.forge.recipe;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.item.model.ItemStat;
import me.ray.midgard.modules.item.utils.StatRange;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import me.ray.midgard.core.debug.MidgardLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all forge recipes. Loads from configuration/DB and provides lookups.
 */
public class ForgeRecipeManager {

    private final Map<String, ForgeRecipe> recipes = new ConcurrentHashMap<>();

    public ForgeRecipeManager() {
    }

    /**
     * Gets a recipe by ID.
     */
    public ForgeRecipe getRecipe(String id) {
        return recipes.get(id.toLowerCase());
    }

    /**
     * Gets all registered recipes.
     */
    public Collection<ForgeRecipe> getAllRecipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    /**
     * Gets recipes available for a given profession level and forge tier.
     */
    public List<ForgeRecipe> getAvailableRecipes(int professionLevel, ForgeTier forgeTier) {
        return recipes.values().stream()
                .filter(r -> r.getRequiredLevel() <= professionLevel)
                .filter(r -> r.getRequiredForgeTier().getLevel() <= forgeTier.getLevel())
                .sorted(Comparator.comparingInt(ForgeRecipe::getRequiredLevel))
                .collect(Collectors.toList());
    }

    /**
     * Gets recipes organized by chapter.
     */
    public Map<Integer, List<ForgeRecipe>> getRecipesByChapter() {
        Map<Integer, List<ForgeRecipe>> byChapter = new TreeMap<>();
        for (ForgeRecipe recipe : recipes.values()) {
            byChapter.computeIfAbsent(recipe.getChapter(), k -> new ArrayList<>()).add(recipe);
        }
        // Sort each chapter's recipes by level
        for (List<ForgeRecipe> list : byChapter.values()) {
            list.sort(Comparator.comparingInt(ForgeRecipe::getRequiredLevel));
        }
        return byChapter;
    }

    /**
     * Gets recipes for a specific chapter.
     */
    public List<ForgeRecipe> getRecipesForChapter(int chapter) {
        return recipes.values().stream()
                .filter(r -> r.getChapter() == chapter)
                .sorted(Comparator.comparingInt(ForgeRecipe::getRequiredLevel))
                .collect(Collectors.toList());
    }

    /**
     * Registers a recipe.
     */
    public void registerRecipe(ForgeRecipe recipe) {
        recipes.put(recipe.getId().toLowerCase(), recipe);
    }

    /**
     * Unregisters a recipe.
     */
    public void unregisterRecipe(String id) {
        recipes.remove(id.toLowerCase());
    }

    /**
     * Clears all recipes. Used during reload.
     */
    public void clear() {
        recipes.clear();
    }

    /**
     * Loads recipes from a YAML configuration section.
     */
    public void loadFromConfig(ConfigurationSection section) {
        if (section == null) { return; }

        for (String key : section.getKeys(false)) {
            try {
                ConfigurationSection recipeSection = section.getConfigurationSection(key);
                if (recipeSection == null) { continue; }

                ForgeRecipe recipe = parseRecipe(key, recipeSection);
                registerRecipe(recipe);
            } catch (Exception e) {
                MidgardLogger.warn("Failed to load forge recipe '" + key + "': " + e.getMessage());
            }
        }
        MidgardLogger.info("Loaded " + recipes.size() + " forge recipes.");
    }

    /**
     * Loads recipes from a YAML string (from DB).
     */
    public void loadFromYaml(String id, String yamlData) {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(yamlData);
            ForgeRecipe recipe = parseRecipe(id, yaml);
            registerRecipe(recipe);
        } catch (Exception e) {
            MidgardLogger.warn("Failed to load forge recipe '" + id + "' from DB: " + e.getMessage());
        }
    }

    /**
     * Parses a single recipe from a configuration section.
     */
    private ForgeRecipe parseRecipe(String id, ConfigurationSection section) {
        ForgeRecipe recipe = new ForgeRecipe(id);

        recipe.setDisplayName(section.getString("display_name", id));
        recipe.setResultItemId(section.getString("result_item", id));
        recipe.setRequiredLevel(section.getInt("required_level", 1));

        int tierLevel = section.getInt("required_forge_tier", 1);
        ForgeTier tier = ForgeTier.fromLevel(tierLevel);
        recipe.setRequiredForgeTier(tier != null ? tier : ForgeTier.BASIC);

        recipe.setChapter(section.getInt("chapter", 1));
        recipe.setPrimaryMetal(section.getString("primary_metal"));
        recipe.setPrimaryMetalAmount(section.getInt("primary_metal_amount", 1));

        // Secondary materials
        ConfigurationSection matSection = section.getConfigurationSection("secondary_materials");
        if (matSection != null) {
            for (String matKey : matSection.getKeys(false)) {
                recipe.addSecondaryMaterial(matKey, matSection.getInt(matKey, 1));
            }
        }

        recipe.setDifficultyMultiplier(section.getDouble("difficulty", 1.0));
        recipe.setHammerStrikes(section.getInt("hammer_strikes", 15));
        recipe.setSharpeningPasses(section.getInt("sharpening_passes", 3));
        recipe.setHeatingTime(section.getInt("heating_time", 20));

        // Base stats
        ConfigurationSection statsSection = section.getConfigurationSection("base_stats");
        if (statsSection != null) {
            for (String statKey : statsSection.getKeys(false)) {
                ItemStat stat = ItemStat.fromPath(statKey.toLowerCase().replace('_', '-'));
                if (stat != null) {
                    StatRange range = StatRange.parse(statsSection.getString(statKey, "0"));
                    recipe.addBaseStat(stat, range);
                }
            }
        }

        recipe.setBaseXP(section.getInt("base_xp", 50));
        recipe.setSpecialization(section.getString("specialization", ""));
        recipe.setMaxGemSockets(section.getInt("max_gem_sockets", 0));
        recipe.setAllowsRuneEngraving(section.getBoolean("allows_rune_engraving", false));

        recipe.setIdealTempMin(section.getDouble("ideal_temp_min", 900));
        recipe.setIdealTempMax(section.getDouble("ideal_temp_max", 1100));

        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) { recipe.setLore(lore); }

        return recipe;
    }

    /**
     * Serializes a recipe to a YAML string (for DB storage).
     */
    public String serializeToYaml(ForgeRecipe recipe) {
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("display_name", recipe.getDisplayName());
        yaml.set("result_item", recipe.getResultItemId());
        yaml.set("required_level", recipe.getRequiredLevel());
        yaml.set("required_forge_tier", recipe.getRequiredForgeTier().getLevel());
        yaml.set("chapter", recipe.getChapter());
        yaml.set("primary_metal", recipe.getPrimaryMetal());
        yaml.set("primary_metal_amount", recipe.getPrimaryMetalAmount());

        for (var entry : recipe.getSecondaryMaterials().entrySet()) {
            yaml.set("secondary_materials." + entry.getKey(), entry.getValue());
        }

        yaml.set("difficulty", recipe.getDifficultyMultiplier());
        yaml.set("hammer_strikes", recipe.getHammerStrikes());
        yaml.set("sharpening_passes", recipe.getSharpeningPasses());
        yaml.set("heating_time", recipe.getHeatingTime());

        for (var entry : recipe.getBaseStats().entrySet()) {
            yaml.set("base_stats." + entry.getKey().getPath(), entry.getValue().toString());
        }

        yaml.set("base_xp", recipe.getBaseXP());
        yaml.set("specialization", recipe.getSpecialization());
        yaml.set("max_gem_sockets", recipe.getMaxGemSockets());
        yaml.set("allows_rune_engraving", recipe.isAllowsRuneEngraving());
        yaml.set("ideal_temp_min", recipe.getIdealTempMin());
        yaml.set("ideal_temp_max", recipe.getIdealTempMax());
        yaml.set("lore", recipe.getLore());

        return yaml.saveToString();
    }

    public int size() {
        return recipes.size();
    }
}
