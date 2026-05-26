package com.midgard.fooddecay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NutritionGainCalculatorTest {

    @Test
    void lightFoodsStayWellBelowConfiguredCap() {
        double gain = NutritionGainCalculator.calculate(20.0, 2, 0.4f, 1);

        assertEquals(4.0, gain, 0.0001);
    }

    @Test
    void strongMealsReachConfiguredCap() {
        double gain = NutritionGainCalculator.calculate(20.0, 8, 12.8f, 1);

        assertEquals(20.0, gain, 0.0001);
    }

    @Test
    void mediumFoodsLandBetweenSnackAndFullMeal() {
        double gain = NutritionGainCalculator.calculate(20.0, 5, 6.0f, 1);

        assertTrue(gain > 4.0);
        assertTrue(gain < 20.0);
        assertEquals(11.5625, gain, 0.0001);
    }

    @Test
    void portionsSplitTheFinalGain() {
        double gain = NutritionGainCalculator.calculate(20.0, 8, 12.8f, 4);

        assertEquals(5.0, gain, 0.0001);
    }
}
