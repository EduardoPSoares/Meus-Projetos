package me.ray.midgard.modules.professions.blacksmith.forge.session;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeStage;
import me.ray.midgard.modules.professions.blacksmith.forge.quality.MaterialGrade;
import me.ray.midgard.modules.professions.blacksmith.forge.quality.QualityTier;
import me.ray.midgard.modules.professions.blacksmith.forge.recipe.ForgeRecipe;

import java.util.UUID;

/**
 * Represents an active forging session for a player.
 * Tracks progress through all stages of the forging process.
 */
public class ForgeSession {

    private static final long DEFAULT_TIMEOUT = 10 * 60 * 1000L; // 10 minutes

    private final UUID sessionId;
    private final UUID playerId;
    private final UUID forgeId;
    private final ForgeRecipe recipe;
    private final long startTime;
    private final long timeout;

    private ForgeStage currentStage;
    private boolean materialsConsumed;

    // Material quality (determined by input materials)
    private MaterialGrade materialGrade = MaterialGrade.REFINED;
    private double materialQuality = 0.8;

    // Scores from each stage (0.0 – 1.0)
    private double heatingScore;
    private double hammeringScore;
    private double quenchingScore;
    private double sharpeningScore;

    // Heating state
    private double currentTemperature;
    private boolean metalHeated;

    // Hammering state
    private int totalStrikes;
    private int perfectStrikes;
    private int goodStrikes;
    private int missedStrikes;
    private int hammeringProgress;

    // Sharpening state
    private int completedPasses;

    // Final result
    private double finalQualityScore;
    private QualityTier qualityTier;
    private double xpGained;

    public ForgeSession(UUID playerId, UUID forgeId, ForgeRecipe recipe) {
        this.sessionId = UUID.randomUUID();
        this.playerId = playerId;
        this.forgeId = forgeId;
        this.recipe = recipe;
        this.startTime = System.currentTimeMillis();
        this.timeout = DEFAULT_TIMEOUT;
        this.currentStage = ForgeStage.SELECTING;
    }

    // === Core getters ===
    public UUID getSessionId() { return sessionId; }
    public UUID getPlayerId() { return playerId; }
    public UUID getForgeId() { return forgeId; }
    public ForgeRecipe getRecipe() { return recipe; }
    public long getStartTime() { return startTime; }
    public ForgeStage getCurrentStage() { return currentStage; }

    // === Stage management ===
    public void setCurrentStage(ForgeStage stage) {
        this.currentStage = stage;
    }

    public void advanceToNextStage() {
        currentStage = switch (currentStage) {
            case SELECTING -> ForgeStage.PREPARING;
            case PREPARING -> ForgeStage.HEATING;
            case HEATING -> ForgeStage.HAMMERING;
            case HAMMERING -> ForgeStage.QUENCHING;
            case QUENCHING -> ForgeStage.SHARPENING;
            case SHARPENING -> ForgeStage.FINALIZING;
            case FINALIZING -> ForgeStage.COMPLETED;
            default -> currentStage;
        };
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > timeout;
    }

    public boolean isActive() {
        return currentStage.isActive() && !isExpired();
    }

    public long getRemainingTime() {
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, timeout - elapsed);
    }

    // === Material quality ===
    public MaterialGrade getMaterialGrade() { return materialGrade; }
    public void setMaterialGrade(MaterialGrade grade) {
        this.materialGrade = grade;
        this.materialQuality = grade.getQualityMultiplier();
    }
    public double getMaterialQuality() { return materialQuality; }
    public boolean isMaterialsConsumed() { return materialsConsumed; }
    public void setMaterialsConsumed(boolean consumed) { this.materialsConsumed = consumed; }

    // === Heating ===
    public double getCurrentTemperature() { return currentTemperature; }
    public void setCurrentTemperature(double temp) { this.currentTemperature = temp; }
    public boolean isMetalHeated() { return metalHeated; }
    public void setMetalHeated(boolean heated) { this.metalHeated = heated; }
    public double getHeatingScore() { return heatingScore; }

    /**
     * Calculates heating score based on when the metal was removed.
     * Closer to the center of the ideal range = higher score.
     */
    public void calculateHeatingScore(double removeTemp) {
        double idealMin = recipe.getIdealTempMin();
        double idealMax = recipe.getIdealTempMax();
        double idealCenter = (idealMin + idealMax) / 2.0;
        double idealHalfRange = (idealMax - idealMin) / 2.0;

        if (removeTemp >= idealMin && removeTemp <= idealMax) {
            // Within ideal range
            double distFromCenter = Math.abs(removeTemp - idealCenter);
            this.heatingScore = 1.0 - (distFromCenter / idealHalfRange) * 0.3;
        } else if (removeTemp < idealMin) {
            // Too cold
            double deficit = idealMin - removeTemp;
            this.heatingScore = Math.max(0.1, 0.7 - (deficit / 200.0));
        } else {
            // Too hot
            double excess = removeTemp - idealMax;
            if (excess > 300) {
                this.heatingScore = 0.0; // Melted
                this.currentStage = ForgeStage.FAILED;
            } else {
                this.heatingScore = Math.max(0.1, 0.5 - (excess / 400.0));
            }
        }
    }

    // === Hammering ===
    public int getTotalStrikes() { return totalStrikes; }
    public int getPerfectStrikes() { return perfectStrikes; }
    public int getGoodStrikes() { return goodStrikes; }
    public int getMissedStrikes() { return missedStrikes; }
    public int getHammeringProgress() { return hammeringProgress; }
    public double getHammeringScore() { return hammeringScore; }

    public void recordHammerStrike(StrikeResult result) {
        totalStrikes++;
        switch (result) {
            case PERFECT -> { perfectStrikes++; hammeringProgress += 3; }
            case GOOD -> { goodStrikes++; hammeringProgress += 2; }
            case MISS -> { missedStrikes++; hammeringProgress += 1; }
        }
    }

    /**
     * Calculates the final hammering score once all strikes are done.
     */
    public void calculateHammeringScore() {
        if (totalStrikes == 0) {
            this.hammeringScore = 0;
            return;
        }
        double score = (perfectStrikes * 3.0 + goodStrikes * 1.5) / (totalStrikes * 3.0);

        // Small bonus for consistent perfect strikes (5+ perfects: +3%, all perfect: +5%)
        if (perfectStrikes >= 5) { score += 0.03; }
        if (perfectStrikes == totalStrikes) { score += 0.05; }

        this.hammeringScore = Math.min(1.0, score);
    }

    // === Quenching ===
    public double getQuenchingScore() { return quenchingScore; }
    public void setQuenchingScore(double score) {
        this.quenchingScore = Math.min(1.0, Math.max(0.0, score));
    }

    // === Sharpening ===
    public int getCompletedPasses() { return completedPasses; }
    public double getSharpeningScore() { return sharpeningScore; }
    public void setSharpeningScore(double score) {
        this.sharpeningScore = Math.min(1.0, Math.max(0.0, score));
    }
    public void incrementCompletedPasses() { completedPasses++; }

    // === Final results ===
    public double getFinalQualityScore() { return finalQualityScore; }
    public void setFinalQualityScore(double score) { this.finalQualityScore = score; }
    public QualityTier getQualityTier() { return qualityTier; }
    public void setQualityTier(QualityTier tier) { this.qualityTier = tier; }
    public double getXpGained() { return xpGained; }
    public void setXpGained(double xp) { this.xpGained = xp; }

    public enum StrikeResult {
        PERFECT,
        GOOD,
        MISS
    }
}
