package me.ray.midgard.modules.professions.blacksmith.forge.quality;

import me.ray.midgard.modules.professions.ProfessionsModule;

/**
 * Quality tier of a forged item, determined by the forging process.
 */
public enum QualityTier {

    DEFECTIVE(0, "Defeituoso", "<dark_gray>", 0.00, 0.14, 0.50),
    INFERIOR(1, "Inferior", "<gray>", 0.15, 0.34, 0.75),
    COMMON(2, "Comum", "<white>", 0.35, 0.54, 1.00),
    SUPERIOR(3, "Superior", "<green>", 0.55, 0.74, 1.15),
    EXCEPTIONAL(4, "Excepcional", "<blue>", 0.75, 0.89, 1.30),
    MASTERPIECE(5, "Obra-Prima", "<gold>", 0.90, 0.97, 1.50),
    LEGENDARY(6, "Lendário", "<light_purple>", 0.98, 1.00, 1.75);

    private final int level;
    private final String name;
    private final String colorTag;
    private final double minScore;
    private final double maxScore;
    private final double statMultiplier;

    QualityTier(int level, String name, String colorTag, double minScore, double maxScore, double statMultiplier) {
        this.level = level;
        this.name = name;
        this.colorTag = colorTag;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.statMultiplier = statMultiplier;
    }

    public int getLevel() { return level; }
    public String getName() {
        var m = ProfessionsModule.getInstance();
        return m != null ? m.getMessage("quality_tier." + name().toLowerCase()) : name;
    }
    public String getColorTag() { return colorTag; }
    public double getMinScore() { return minScore; }
    public double getMaxScore() { return maxScore; }
    public double getStatMultiplier() { return statMultiplier; }

    public String getFormattedName() {
        return colorTag + getName();
    }

    /**
     * Returns a visual bar representing the quality score.
     */
    public String getQualityBar() {
        int filled = (int) (minScore * 10);
        int empty = 10 - filled;
        return "<green>" + "█".repeat(Math.max(0, filled)) + "<gray>" + "░".repeat(Math.max(0, empty));
    }

    /**
     * Determines the quality tier from a given score (0.0 to 1.0).
     */
    public static QualityTier fromScore(double score) {
        if (score >= 0.98) { return LEGENDARY; }
        if (score >= 0.90) { return MASTERPIECE; }
        if (score >= 0.75) { return EXCEPTIONAL; }
        if (score >= 0.55) { return SUPERIOR; }
        if (score >= 0.35) { return COMMON; }
        if (score >= 0.15) { return INFERIOR; }
        return DEFECTIVE;
    }
}
