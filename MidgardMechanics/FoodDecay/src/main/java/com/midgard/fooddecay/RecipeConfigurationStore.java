package com.midgard.fooddecay;

import com.midgard.fooddecay.multiblock.MultiblockRecipe;
import com.midgard.fooddecay.multiblock.MultiblockType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class RecipeConfigurationStore {

    private RecipeConfigurationStore() {
    }

    static Map<MultiblockType, List<MultiblockRecipe>> loadAll(File recipesFile, RecipeLoader loader) {
        YamlConfiguration recipesCfg = YamlConfiguration.loadConfiguration(recipesFile);
        Map<MultiblockType, List<MultiblockRecipe>> recipesByType = new EnumMap<>(MultiblockType.class);

        for (MultiblockType type : MultiblockType.values()) {
            List<MultiblockRecipe> recipes = new ArrayList<>();
            ConfigurationSection recipesSection = recipesCfg.getConfigurationSection(type.getConfigKey());
            if (recipesSection != null) {
                for (String recipeId : recipesSection.getKeys(false)) {
                    MultiblockRecipe recipe = loader.load(
                            recipeId,
                            type,
                            recipesSection.getConfigurationSection(recipeId)
                    );
                    if (recipe != null) {
                        recipes.add(recipe);
                    }
                }
            }
            recipesByType.put(type, recipes);
        }

        return recipesByType;
    }

    static void saveRecipe(File recipesFile, MultiblockRecipe recipe) throws IOException {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(recipesFile);
        String path = recipe.getMachineType().getConfigKey() + "." + recipe.getId();

        cfg.set(path, null);

        if (recipe.getInputItemsAdderId() != null) {
            cfg.set(path + ".input.itemsadder", recipe.getInputItemsAdderId());
        } else if (recipe.getInputMmoType() != null && recipe.getInputMmoId() != null) {
            cfg.set(path + ".input.mmoitems", recipe.getInputMmoType() + ":" + recipe.getInputMmoId());
        } else if (recipe.getInputMaterial() != null) {
            cfg.set(path + ".input.material", recipe.getInputMaterial().name());
        }
        if (recipe.getInputCustomModelData() > 0) {
            cfg.set(path + ".input.custom-model-data", recipe.getInputCustomModelData());
        }

        if (recipe.getOutputItemsAdderId() != null) {
            cfg.set(path + ".output.itemsadder", recipe.getOutputItemsAdderId());
        } else if (recipe.getOutputMmoType() != null && recipe.getOutputMmoId() != null) {
            cfg.set(path + ".output.mmoitems", recipe.getOutputMmoType() + ":" + recipe.getOutputMmoId());
        } else if (recipe.getOutputMaterial() != null) {
            cfg.set(path + ".output.material", recipe.getOutputMaterial().name());
        }
        if (recipe.getOutputName() != null && !recipe.getOutputName().isEmpty()) {
            cfg.set(path + ".output.name", recipe.getOutputName());
        }
        if (recipe.getOutputLore() != null && !recipe.getOutputLore().isEmpty()) {
            cfg.set(path + ".output.lore", recipe.getOutputLore());
        }
        if (recipe.getOutputCustomModelData() > 0) {
            cfg.set(path + ".output.custom-model-data", recipe.getOutputCustomModelData());
        }

        if (recipe.getSpoiledCustomModelData() > 0 || recipe.getSpoiledName() != null) {
            cfg.set(path + ".spoiled.custom-model-data", recipe.getSpoiledCustomModelData());
            if (recipe.getSpoiledName() != null) {
                cfg.set(path + ".spoiled.name", recipe.getSpoiledName());
            }
        }

        cfg.set(path + ".time-minutes", recipe.getTimeMinutes());
        if (recipe.getTrait() != null) {
            cfg.set(path + ".trait", recipe.getTrait().name());
        }
        if (recipe.getRequiresTrait() != null) {
            cfg.set(path + ".requires-trait", recipe.getRequiresTrait().name());
        }
        if (recipe.getRequiresRecipe() != null) {
            cfg.set(path + ".requires-recipe", recipe.getRequiresRecipe());
        }
        if (!recipe.getExtraIngredients().isEmpty()) {
            cfg.set(path + ".extra-inputs", recipe.getExtraIngredients().stream()
                    .map(com.midgard.fooddecay.multiblock.RecipeIngredient::toConfigMap)
                    .toList());
        }
        if (recipe.getNutritionGroups() != null && !recipe.getNutritionGroups().isEmpty()) {
            cfg.set(path + ".nutrition-groups", recipe.getNutritionGroups());
        }

        if (recipe.getProfession() != null) {
            cfg.set(path + ".profession", recipe.getProfession());
            cfg.set(path + ".profession-level", recipe.getProfessionLevel());
        }
        if (recipe.getExperienceProfession() != null && recipe.getExperienceReward() > 0) {
            cfg.set(path + ".experience-profession", recipe.getExperienceProfession());
            cfg.set(path + ".experience-reward", recipe.getExperienceReward());
        }

        File parent = recipesFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        cfg.save(recipesFile);
    }

    static void deleteRecipe(File recipesFile, MultiblockType machineType, String recipeId) throws IOException {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(recipesFile);
        cfg.set(machineType.getConfigKey() + "." + recipeId, null);

        File parent = recipesFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        cfg.save(recipesFile);
    }

    @FunctionalInterface
    interface RecipeLoader {
        MultiblockRecipe load(String recipeId, MultiblockType type, ConfigurationSection section);
    }
}
