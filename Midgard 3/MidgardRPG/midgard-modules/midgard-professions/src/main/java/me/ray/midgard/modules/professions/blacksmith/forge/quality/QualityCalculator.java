package me.ray.midgard.modules.professions.blacksmith.forge.quality;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Calculates the final quality of a forged item based on all contributing factors.
 * Uses a diminishing returns curve so high quality is progressively harder to achieve.
 */
public class QualityCalculator {

    // Weight of each factor in the final quality score
    private double materialWeight = 0.25;
    private double heatingWeight = 0.15;
    private double hammeringWeight = 0.30;
    private double quenchingWeight = 0.15;
    private double sharpeningWeight = 0.15;

    // Additive bonuses (small, not multiplicative)
    private double professionLevelBonus = 0.0005;  // per level (max +0.05 at lv100)
    private double forgeTierBonus = 0.005;          // per tier above 1 (max +0.02 at tier 5)

    // Diminishing returns exponent (higher = harder to reach top quality)
    private double qualityCurve = 1.5;

    // Random variance range (±half this value)
    private double randomVariance = 0.06;

    public QualityCalculator() {}

    public QualityCalculator(double materialWeight, double heatingWeight, double hammeringWeight,
                             double quenchingWeight, double sharpeningWeight, double professionLevelBonus) {
        this.materialWeight = materialWeight;
        this.heatingWeight = heatingWeight;
        this.hammeringWeight = hammeringWeight;
        this.quenchingWeight = quenchingWeight;
        this.sharpeningWeight = sharpeningWeight;
        this.professionLevelBonus = professionLevelBonus;
    }

    /**
     * Calculates the final quality score (0.0 to 1.0).
     * Applies a diminishing returns curve so that high scores require
     * truly excellent performance across all phases.
     *
     * @param materialQuality  Quality of the materials used (0.0 - 1.0)
     * @param heatingScore     Precision during heating phase (0.0 - 1.0)
     * @param hammeringScore   Performance in hammering mini-game (0.0 - 1.0)
     * @param quenchingScore   Precision in quenching mini-game (0.0 - 1.0)
     * @param sharpeningScore  Precision in sharpening mini-game (0.0 - 1.0)
     * @param professionLevel  Player's blacksmith profession level
     * @param forgeTierLevel   The tier level of the forge being used
     * @return Final quality score clamped to [0.0, 1.0]
     */
    public double calculate(double materialQuality, double heatingScore, double hammeringScore,
                            double quenchingScore, double sharpeningScore, int professionLevel, int forgeTierLevel) {

        double baseScore = materialQuality * materialWeight
                + heatingScore * heatingWeight
                + hammeringScore * hammeringWeight
                + quenchingScore * quenchingWeight
                + sharpeningScore * sharpeningWeight;

        // Diminishing returns: makes high scores progressively harder
        // 0.95 → 0.925, 0.85 → 0.784, 0.70 → 0.586, 0.50 → 0.354
        double curvedScore = Math.pow(baseScore, qualityCurve);

        // Small additive profession level bonus (max +0.05 at level 100)
        double levelAdd = professionLevel * professionLevelBonus;

        // Small additive forge tier bonus (max +0.02 at tier 5)
        double tierAdd = (forgeTierLevel - 1) * forgeTierBonus;

        // Small random variance for natural-feeling results
        double variance = (ThreadLocalRandom.current().nextDouble() - 0.5) * randomVariance;

        double finalScore = curvedScore + levelAdd + tierAdd + variance;

        return Math.min(1.0, Math.max(0.0, finalScore));
    }

    /**
     * Gets the quality tier from a calculated score.
     */
    public QualityTier getTier(double score) {
        return QualityTier.fromScore(score);
    }

    /**
     * Calculates XP gained from forging based on recipe base XP and quality.
     */
    public double calculateXP(int baseRecipeXP, QualityTier qualityTier, boolean firstCraft) {
        double qualityMultiplier = switch (qualityTier) {
            case DEFECTIVE -> 0.25;
            case INFERIOR -> 0.50;
            case COMMON -> 1.00;
            case SUPERIOR -> 1.50;
            case EXCEPTIONAL -> 2.00;
            case MASTERPIECE -> 3.00;
            case LEGENDARY -> 5.00;
        };

        double firstCraftBonus = firstCraft ? 2.0 : 1.0;
        return baseRecipeXP * qualityMultiplier * firstCraftBonus;
    }

    // Getters for configuration
    public double getMaterialWeight() { return materialWeight; }
    public double getHeatingWeight() { return heatingWeight; }
    public double getHammeringWeight() { return hammeringWeight; }
    public double getQuenchingWeight() { return quenchingWeight; }
    public double getSharpeningWeight() { return sharpeningWeight; }
    public double getQualityCurve() { return qualityCurve; }
    public double getRandomVariance() { return randomVariance; }

    public void setQualityCurve(double curve) { this.qualityCurve = curve; }
    public void setRandomVariance(double variance) { this.randomVariance = variance; }
}
