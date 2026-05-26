package com.midgard.fooddecay.multiblock;

record RecipeDiscoveryChange(
        RecipeDiscoveryStage previousStage,
        RecipeDiscoveryStage currentStage,
        int familyHintsUnlocked
) {

    static final RecipeDiscoveryChange NO_CHANGE = new RecipeDiscoveryChange(
            RecipeDiscoveryStage.UNKNOWN,
            RecipeDiscoveryStage.UNKNOWN,
            0
    );

    boolean changed() {
        return previousStage != currentStage || familyHintsUnlocked > 0;
    }
}
