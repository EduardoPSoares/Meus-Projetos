package com.midgard.fooddecay.multiblock;

public record RecipeDiscoveryProgress(boolean attempted, int successfulCollections) {

    public static final int MASTERY_COLLECTIONS_REQUIRED = 3;
    public static final RecipeDiscoveryProgress UNKNOWN = new RecipeDiscoveryProgress(false, 0);

    public RecipeDiscoveryProgress {
        if (successfulCollections < 0) {
            throw new IllegalArgumentException("successfulCollections cannot be negative");
        }
    }

    public RecipeDiscoveryStage stage() {
        if (!attempted && successfulCollections <= 0) {
            return RecipeDiscoveryStage.UNKNOWN;
        }
        if (successfulCollections >= MASTERY_COLLECTIONS_REQUIRED) {
            return RecipeDiscoveryStage.MASTERED;
        }
        if (successfulCollections > 0) {
            return RecipeDiscoveryStage.TESTED;
        }
        return RecipeDiscoveryStage.SUSPECTED;
    }

    public RecipeDiscoveryProgress withAttempted() {
        if (attempted) {
            return this;
        }
        return new RecipeDiscoveryProgress(true, successfulCollections);
    }

    public RecipeDiscoveryProgress withSuccessfulCollection() {
        int cappedCollections = Math.min(MASTERY_COLLECTIONS_REQUIRED,
                Math.max(successfulCollections + 1, 1));
        if (attempted && cappedCollections == successfulCollections) {
            return this;
        }
        return new RecipeDiscoveryProgress(true, cappedCollections);
    }

    public int collectionsRemainingForMastery() {
        return Math.max(0, MASTERY_COLLECTIONS_REQUIRED - successfulCollections);
    }
}
