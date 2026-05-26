package com.midgard.fooddecay.multiblock;

public enum RecipeDiscoveryStage {
    UNKNOWN,
    SUSPECTED,
    TESTED,
    MASTERED;

    public boolean hasAnyClue() {
        return this != UNKNOWN;
    }

    public boolean isCatalogued() {
        return this == TESTED || this == MASTERED;
    }
}
