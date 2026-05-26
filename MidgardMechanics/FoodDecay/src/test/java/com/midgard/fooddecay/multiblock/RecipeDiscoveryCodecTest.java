package com.midgard.fooddecay.multiblock;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeDiscoveryCodecTest {

    private final RecipeDiscoveryCodec codec = new RecipeDiscoveryCodec();

    @Test
    void migratesLegacyDiscoveryListToTestedProgress() {
        Map<String, RecipeDiscoveryProgress> progress = codec.decode("jerky_beef, smoked_fish");

        assertEquals(RecipeDiscoveryStage.TESTED, progress.get("jerky_beef").stage());
        assertEquals(1, progress.get("jerky_beef").successfulCollections());
        assertEquals(RecipeDiscoveryStage.TESTED, progress.get("smoked_fish").stage());
    }

    @Test
    void roundTripsNewProgressFormatDeterministically() {
        Map<String, RecipeDiscoveryProgress> original = new LinkedHashMap<>();
        original.put("smoked_fish", new RecipeDiscoveryProgress(true, 0));
        original.put("jerky_beef", new RecipeDiscoveryProgress(true, 3));

        String encoded = codec.encode(original);
        Map<String, RecipeDiscoveryProgress> decoded = codec.decode(encoded);

        assertEquals("jerky_beef|1|3;smoked_fish|1|0", encoded);
        assertEquals(new RecipeDiscoveryProgress(true, 3), decoded.get("jerky_beef"));
        assertEquals(new RecipeDiscoveryProgress(true, 0), decoded.get("smoked_fish"));
    }
}
