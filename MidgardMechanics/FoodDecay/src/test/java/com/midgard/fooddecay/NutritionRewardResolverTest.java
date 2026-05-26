package com.midgard.fooddecay;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NutritionRewardResolverTest {

    @Test
    void usesPerGroupThresholdAndAggregatesConfiguredRewards() {
        Map<String, FoodDecayConfig.GroupBonus> bonuses = Map.of(
                "PROTEIN", new FoodDecayConfig.GroupBonus(
                        3D,
                        40D,
                        List.of("STRENGTH:0:200"),
                        Map.of(),
                        Map.of("MAX_MANA", 25D)
                ),
                "DAIRY", new FoodDecayConfig.GroupBonus(
                        1D,
                        null,
                        List.of("SPEED:0:200"),
                        Map.of(),
                        Map.of("MAX_MANA", 10D)
                )
        );

        double[] nutrition = new double[NutritionManager.FoodGroup.values().length];
        nutrition[NutritionManager.FoodGroup.PROTEIN.ordinal()] = 45D;
        nutrition[NutritionManager.FoodGroup.DAIRY.ordinal()] = 30D;

        NutritionRewardResolver.ResolvedRewards resolved = NutritionRewardResolver.resolve(
                nutrition,
                25D,
                2D,
                bonuses
        );

        assertEquals(4D, resolved.healthBonus(), 0.0001);
        assertTrue(resolved.attributeBonuses().isEmpty());
        assertEquals(35D, resolved.mmocoreStats().get("MAX_MANA"), 0.0001);
        assertEquals(List.of("STRENGTH:0:200", "SPEED:0:200"), resolved.effects());
        assertTrue(resolved.activeGroups().contains(NutritionManager.FoodGroup.PROTEIN));
        assertTrue(resolved.activeGroups().contains(NutritionManager.FoodGroup.DAIRY));
    }

    @Test
    void fallsBackToDefaultHealthBonusWhenGroupHasNoCustomConfig() {
        double[] nutrition = new double[NutritionManager.FoodGroup.values().length];
        nutrition[NutritionManager.FoodGroup.FRUIT.ordinal()] = 30D;

        NutritionRewardResolver.ResolvedRewards resolved = NutritionRewardResolver.resolve(
                nutrition,
                25D,
                2.5D,
                Map.of()
        );

        assertEquals(2.5D, resolved.healthBonus(), 0.0001);
        assertTrue(resolved.attributeBonuses().isEmpty());
        assertTrue(resolved.mmocoreStats().isEmpty());
        assertEquals(List.of(), resolved.effects());
    }
}
