package com.midgard.fooddecay.multiblock;

import java.util.Collection;
import java.util.Map;

final class RecipeDiscoveryEngine {

    RecipeDiscoveryChange registerAttempt(Map<String, RecipeDiscoveryProgress> progressByRecipe,
                                          String recipeId) {
        if (recipeId == null || recipeId.isBlank()) {
            return RecipeDiscoveryChange.NO_CHANGE;
        }

        RecipeDiscoveryProgress current = progressByRecipe.getOrDefault(recipeId, RecipeDiscoveryProgress.UNKNOWN);
        RecipeDiscoveryStage previousStage = current.stage();
        if (previousStage != RecipeDiscoveryStage.UNKNOWN) {
            return new RecipeDiscoveryChange(previousStage, previousStage, 0);
        }

        RecipeDiscoveryProgress updated = current.withAttempted();
        progressByRecipe.put(recipeId, updated);
        return new RecipeDiscoveryChange(previousStage, updated.stage(), 0);
    }

    RecipeDiscoveryChange registerCollection(Map<String, RecipeDiscoveryProgress> progressByRecipe,
                                             MultiblockRecipe recipe,
                                             Collection<MultiblockRecipe> machineRecipes) {
        if (recipe == null || recipe.getId() == null || recipe.getId().isBlank()) {
            return RecipeDiscoveryChange.NO_CHANGE;
        }

        RecipeDiscoveryProgress current = progressByRecipe
                .getOrDefault(recipe.getId(), RecipeDiscoveryProgress.UNKNOWN)
                .withAttempted();
        RecipeDiscoveryStage previousStage = current.stage();

        RecipeDiscoveryProgress updated = current.withSuccessfulCollection();
        RecipeDiscoveryStage currentStage = updated.stage();
        int familyHintsUnlocked = 0;

        if (updated != current) {
            progressByRecipe.put(recipe.getId(), updated);
        }

        if (previousStage != RecipeDiscoveryStage.MASTERED
                && currentStage == RecipeDiscoveryStage.MASTERED
                && machineRecipes != null) {
            familyHintsUnlocked = unlockMachineHints(progressByRecipe, recipe.getId(), machineRecipes);
        }

        return new RecipeDiscoveryChange(previousStage, currentStage, familyHintsUnlocked);
    }

    private int unlockMachineHints(Map<String, RecipeDiscoveryProgress> progressByRecipe,
                                   String masteredRecipeId,
                                   Collection<MultiblockRecipe> machineRecipes) {
        int unlocked = 0;
        for (MultiblockRecipe machineRecipe : machineRecipes) {
            if (machineRecipe == null
                    || machineRecipe.getId() == null
                    || machineRecipe.getId().isBlank()
                    || machineRecipe.getId().equals(masteredRecipeId)) {
                continue;
            }

            RecipeDiscoveryProgress current = progressByRecipe.getOrDefault(
                    machineRecipe.getId(),
                    RecipeDiscoveryProgress.UNKNOWN
            );
            if (current.stage() != RecipeDiscoveryStage.UNKNOWN) {
                continue;
            }

            progressByRecipe.put(machineRecipe.getId(), current.withAttempted());
            unlocked++;
        }
        return unlocked;
    }
}
