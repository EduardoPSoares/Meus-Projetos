package me.ray.midgard.modules.professions.blacksmith.forge.quality;

/**
 * Represents the grade/purity of a material used in forging.
 * Higher grades produce better quality items.
 */
public enum MaterialGrade {

    IMPURE(0.4, "<dark_gray>Impuro</dark_gray>"),
    CRUDE(0.6, "<gray>Bruto</gray>"),
    REFINED(0.8, "<white>Refinado</white>"),
    PURE(0.95, "<aqua>Puro</aqua>"),
    PRISTINE(1.0, "<gold>Prístino</gold>");

    private final double qualityMultiplier;
    private final String displayName;

    MaterialGrade(double qualityMultiplier, String displayName) {
        this.qualityMultiplier = qualityMultiplier;
        this.displayName = displayName;
    }

    public double getQualityMultiplier() { return qualityMultiplier; }
    public String getDisplayName() { return displayName; }

    /**
     * Gets the default grade for a material (used when no grade is specified).
     */
    public static MaterialGrade getDefault() {
        return REFINED;
    }

    public static MaterialGrade fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return REFINED;
        }
    }
}
