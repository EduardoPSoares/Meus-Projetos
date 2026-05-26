package me.ray.midgard.modules.professions.blacksmith.forge.data;

import me.ray.midgard.core.profile.ModuleData;

import java.util.*;

/**
 * Player's forge profession data — stored per player profile.
 * Tracks the blacksmithing profession level, XP, recipes, and stats.
 */
public class ForgeData implements ModuleData {

    private int level;
    private double xp;
    private double xpToNextLevel;

    // Specialization (weaponsmith, armorsmith, jewelcrafter, etc.)
    private String specialization;

    // Set of recipe IDs the player has unlocked
    private final Set<String> unlockedRecipes = new HashSet<>();

    // Lifetime stats
    private int totalItemsForged;
    private int legendaryItemsForged;
    private int totalPerfectStrikes;
    private int totalForgesBuilt;
    private double highestQualityScore;

    // Current forge ownership (UUID strings of forge structures)
    private final Set<String> ownedForgeIds = new HashSet<>();

    public ForgeData() {
        this.level = 0;
        this.xp = 0;
        this.xpToNextLevel = calculateXpNeeded(1);
    }

    // === Level & XP ===
    public int getLevel() { return level; }
    public double getXp() { return xp; }
    public double getXpToNextLevel() { return xpToNextLevel; }

    public void setLevel(int level) {
        this.level = level;
        this.xpToNextLevel = calculateXpNeeded(level + 1);
    }

    /**
     * Sets XP directly without triggering level-up logic.
     * Used when loading from database.
     */
    public void setXp(double xp) {
        this.xp = xp;
    }

    /**
     * Adds XP and handles level-ups. Returns the number of levels gained.
     */
    public int addXp(double amount) {
        if (amount <= 0 || level >= 100) { return 0; }

        xp += amount;
        int levelsGained = 0;

        while (xp >= xpToNextLevel && level < 100) {
            xp -= xpToNextLevel;
            level++;
            levelsGained++;
            xpToNextLevel = calculateXpNeeded(level + 1);
        }

        // Cap at level 100
        if (level >= 100) {
            xp = 0;
        }

        return levelsGained;
    }

    /**
     * Calculates XP needed for a given level using a quadratic curve.
     * Level 2: 100, Level 10: 900, Level 50: ~12500, Level 100: ~50000
     */
    private double calculateXpNeeded(int targetLevel) {
        return 50.0 * targetLevel * targetLevel + 50.0 * targetLevel;
    }

    public double getProgressPercent() {
        return xpToNextLevel > 0 ? (xp / xpToNextLevel) * 100.0 : 0;
    }

    // === Specialization ===
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public boolean hasSpecialization() { return specialization != null && !specialization.isEmpty(); }

    // === Recipes ===
    public Set<String> getUnlockedRecipes() { return Collections.unmodifiableSet(unlockedRecipes); }
    public void unlockRecipe(String recipeId) { unlockedRecipes.add(recipeId); }
    public boolean hasRecipe(String recipeId) { return unlockedRecipes.contains(recipeId); }
    public void setUnlockedRecipes(Collection<String> recipes) {
        unlockedRecipes.clear();
        unlockedRecipes.addAll(recipes);
    }

    // === Stats ===
    public int getTotalItemsForged() { return totalItemsForged; }
    public void incrementItemsForged() { totalItemsForged++; }
    public int getLegendaryItemsForged() { return legendaryItemsForged; }
    public void incrementLegendaryForged() { legendaryItemsForged++; }
    public int getTotalPerfectStrikes() { return totalPerfectStrikes; }
    public void addPerfectStrikes(int count) { totalPerfectStrikes += count; }
    public int getTotalForgesBuilt() { return totalForgesBuilt; }
    public void incrementForgesBuilt() { totalForgesBuilt++; }
    public double getHighestQualityScore() { return highestQualityScore; }
    public void updateHighestQuality(double score) {
        if (score > highestQualityScore) { highestQualityScore = score; }
    }

    public void setTotalItemsForged(int v) { totalItemsForged = v; }
    public void setLegendaryItemsForged(int v) { legendaryItemsForged = v; }
    public void setTotalPerfectStrikes(int v) { totalPerfectStrikes = v; }
    public void setTotalForgesBuilt(int v) { totalForgesBuilt = v; }
    public void setHighestQualityScore(double v) { highestQualityScore = v; }

    // === Forges ===
    public Set<String> getOwnedForgeIds() { return Collections.unmodifiableSet(ownedForgeIds); }
    public void addForge(String forgeId) { ownedForgeIds.add(forgeId); }
    public void removeForge(String forgeId) { ownedForgeIds.remove(forgeId); }
    public void setOwnedForgeIds(Collection<String> ids) {
        ownedForgeIds.clear();
        ownedForgeIds.addAll(ids);
    }
}
