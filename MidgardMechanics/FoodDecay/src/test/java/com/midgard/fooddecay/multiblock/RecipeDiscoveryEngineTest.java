package com.midgard.fooddecay.multiblock;

import com.midgard.fooddecay.FoodTrait;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeDiscoveryEngineTest {

    private final RecipeDiscoveryEngine engine = new RecipeDiscoveryEngine();

    @Test
    void progressesRecipeFromUnknownToMasteredAndUnlocksMachineHints() {
        MultiblockRecipe jerky = recipe("jerky_beef", Material.BEEF, Material.COOKED_BEEF);
        MultiblockRecipe smokedFish = recipe("smoked_fish", Material.COD, Material.COOKED_COD);
        Map<String, RecipeDiscoveryProgress> progressByRecipe = new LinkedHashMap<>();

        RecipeDiscoveryChange suspected = engine.registerAttempt(progressByRecipe, jerky.getId());
        RecipeDiscoveryChange tested = engine.registerCollection(progressByRecipe, jerky, List.of(jerky, smokedFish));
        engine.registerCollection(progressByRecipe, jerky, List.of(jerky, smokedFish));
        RecipeDiscoveryChange mastered = engine.registerCollection(progressByRecipe, jerky, List.of(jerky, smokedFish));

        assertEquals(RecipeDiscoveryStage.UNKNOWN, suspected.previousStage());
        assertEquals(RecipeDiscoveryStage.SUSPECTED, suspected.currentStage());
        assertEquals(RecipeDiscoveryStage.SUSPECTED, tested.previousStage());
        assertEquals(RecipeDiscoveryStage.TESTED, tested.currentStage());
        assertEquals(RecipeDiscoveryStage.MASTERED, progressByRecipe.get("jerky_beef").stage());
        assertEquals(1, mastered.familyHintsUnlocked());
        assertEquals(RecipeDiscoveryStage.SUSPECTED, progressByRecipe.get("smoked_fish").stage());
    }

    @Test
    void collectingWithoutPriorAttemptStillRegistersRecipe() {
        MultiblockRecipe jerky = recipe("jerky_beef", Material.BEEF, Material.COOKED_BEEF);
        Map<String, RecipeDiscoveryProgress> progressByRecipe = new LinkedHashMap<>();

        RecipeDiscoveryChange tested = engine.registerCollection(progressByRecipe, jerky, List.of(jerky));

        assertEquals(RecipeDiscoveryStage.SUSPECTED, tested.previousStage());
        assertEquals(RecipeDiscoveryStage.TESTED, tested.currentStage());
        assertEquals(1, progressByRecipe.get("jerky_beef").successfulCollections());
    }

    private MultiblockRecipe recipe(String id, Material input, Material output) {
        return new MultiblockRecipe(
                id,
                null,
                input,
                null,
                null,
                null,
                0,
                output,
                null,
                null,
                null,
                "&f" + output.name(),
                List.of(),
                0,
                0,
                null,
                8,
                FoodTrait.DRIED,
                null,
                null,
                List.of()
        );
    }
}
