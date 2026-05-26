package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import org.bukkit.configuration.ConfigurationSection;

import me.ray.midgard.core.debug.MidgardLogger;

import java.util.*;

/**
 * Manages alloy recipes loaded from YAML configuration.
 * Alloys combine two or more molten metals to produce a new metal.
 */
public class AlloyRecipeManager {

    private final List<AlloyRecipe> recipes = new ArrayList<>();

    public AlloyRecipeManager() {
    }

    public List<AlloyRecipe> getAllRecipes() {
        return Collections.unmodifiableList(recipes);
    }

    public void register(AlloyRecipe recipe) {
        recipes.add(recipe);
    }

    public void clear() {
        recipes.clear();
    }

    public int size() {
        return recipes.size();
    }

    /**
     * Finds all alloys that can be formed with the given tank contents and temperature.
     */
    public List<AlloyRecipe> findFormableAlloys(Map<MoltenMetal, Integer> tankContents, int temperature) {
        List<AlloyRecipe> result = new ArrayList<>();
        for (AlloyRecipe recipe : recipes) {
            if (recipe.canForm(tankContents, temperature)) {
                result.add(recipe);
            }
        }
        return result;
    }

    /**
     * Loads alloy recipes from a YAML configuration section.
     * Expected format:
     * <pre>
     *   bronze:
     *     result: BRONZE
     *     result_amount: 576
     *     min_temperature: 700
     *     ingredients:
     *       COPPER: 432
     *       GOLD: 144
     * </pre>
     */
    public void loadFromConfig(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        clear();

        for (String key : section.getKeys(false)) {
            try {
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }

                String resultName = entry.getString("result");
                if (resultName == null) {
                    MidgardLogger.warn("Alloy recipe '" + key + "' missing 'result' field");
                    continue;
                }

                MoltenMetal result;
                try {
                    result = MoltenMetal.valueOf(resultName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    MidgardLogger.warn("Alloy recipe '" + key + "' has invalid result metal: " + resultName);
                    continue;
                }

                int resultAmount = entry.getInt("result_amount", 288);
                int minTemp = entry.getInt("min_temperature", 800);

                ConfigurationSection ingredientsSection = entry.getConfigurationSection("ingredients");
                if (ingredientsSection == null || ingredientsSection.getKeys(false).isEmpty()) {
                    MidgardLogger.warn("Alloy recipe '" + key + "' missing or empty 'ingredients'");
                    continue;
                }

                Map<MoltenMetal, Integer> ingredients = new LinkedHashMap<>();
                boolean valid = true;
                for (String metalKey : ingredientsSection.getKeys(false)) {
                    MoltenMetal metal;
                    try {
                        metal = MoltenMetal.valueOf(metalKey.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        MidgardLogger.warn("Alloy recipe '" + key + "' has invalid ingredient: " + metalKey);
                        valid = false;
                        break;
                    }
                    ingredients.put(metal, ingredientsSection.getInt(metalKey));
                }
                if (!valid) {
                    continue;
                }

                register(new AlloyRecipe(key, result, resultAmount, ingredients, minTemp));
            } catch (Exception e) {
                MidgardLogger.warn("Failed to load alloy recipe '" + key + "': " + e.getMessage());
            }
        }
        MidgardLogger.info("Loaded " + recipes.size() + " alloy recipes.");
    }
}
