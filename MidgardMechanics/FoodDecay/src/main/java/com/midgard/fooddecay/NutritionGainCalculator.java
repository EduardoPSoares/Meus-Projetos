package com.midgard.fooddecay;

/**
 * Calculates nutrition gain based on the strength of the consumed food.
 * Strong meals reach the configured cap, while lighter foods give less.
 */
final class NutritionGainCalculator {

    private static final int REFERENCE_NUTRITION = 8;
    private static final float REFERENCE_SATURATION = 12.8f;
    private static final double NUTRITION_WEIGHT = 0.7;
    private static final double SATURATION_WEIGHT = 0.3;
    private static final double MIN_GAIN_FACTOR = 0.2;

    private NutritionGainCalculator() {
    }

    static double calculate(double maxGain, int nutrition, float saturation, int totalPortions) {
        if (maxGain <= 0) {
            return 0;
        }

        int safeNutrition = Math.max(0, nutrition);
        float safeSaturation = Math.max(0f, saturation);
        int safePortions = Math.max(1, totalPortions);

        double nutritionFactor = Math.min(1.0, safeNutrition / (double) REFERENCE_NUTRITION);
        double saturationFactor = Math.min(1.0, safeSaturation / REFERENCE_SATURATION);
        double weightedFactor = nutritionFactor * NUTRITION_WEIGHT + saturationFactor * SATURATION_WEIGHT;
        double finalFactor = Math.max(MIN_GAIN_FACTOR, weightedFactor);

        return maxGain * finalFactor / safePortions;
    }
}
