package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import me.ray.midgard.core.debug.MidgardLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages smelting recipes loaded from YAML configuration.
 * Maps solid items (Material) to their molten metal output.
 */
public class SmeltingRecipeManager {

    private final Map<Material, SmeltingRecipe> recipes = new ConcurrentHashMap<>();

    public SmeltingRecipeManager() {
    }

    public SmeltingRecipe getRecipe(Material input) {
        return recipes.get(input);
    }

    public boolean canSmelt(Material input) {
        return recipes.containsKey(input);
    }

    public Collection<SmeltingRecipe> getAllRecipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    public void register(SmeltingRecipe recipe) {
        recipes.put(recipe.getInput(), recipe);
    }

    public void clear() {
        recipes.clear();
    }

    public int size() {
        return recipes.size();
    }

    /**
     * Loads smelting recipes from a YAML configuration section.
     * Expected format:
     * <pre>
     *   iron_ingot:
     *     input: IRON_INGOT
     *     output: IRON
     *     output_amount: 144
     *     smelt_time: 100
     *     min_temperature: 800
     * </pre>
     */
    public void loadFromConfig(ConfigurationSection section) {
        if (section == null) { return; }
        clear();

        for (String key : section.getKeys(false)) {
            try {
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (entry == null) { continue; }

                String inputName = entry.getString("input");
                if (inputName == null) {
                    MidgardLogger.warn("Smelting recipe '" + key + "' missing 'input' field");
                    continue;
                }

                Material input;
                try {
                    input = Material.valueOf(inputName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    MidgardLogger.warn("Smelting recipe '" + key + "' has invalid input material: " + inputName);
                    continue;
                }

                String outputName = entry.getString("output");
                if (outputName == null) {
                    MidgardLogger.warn("Smelting recipe '" + key + "' missing 'output' field");
                    continue;
                }

                MoltenMetal output;
                try {
                    output = MoltenMetal.valueOf(outputName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    MidgardLogger.warn("Smelting recipe '" + key + "' has invalid output metal: " + outputName);
                    continue;
                }

                int outputAmount = entry.getInt("output_amount", 144);
                int smeltTime = entry.getInt("smelt_time", 100);
                int minTemp = entry.getInt("min_temperature", 800);

                register(new SmeltingRecipe(key, input, output, outputAmount, smeltTime, minTemp));
            } catch (Exception e) {
                MidgardLogger.warn("Failed to load smelting recipe '" + key + "': " + e.getMessage());
            }
        }
        MidgardLogger.info("Loaded " + recipes.size() + " smelting recipes.");
    }
}
