package me.ray.midgard.modules.professions.blacksmith.forge.recipe;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.item.model.ItemStat;
import me.ray.midgard.modules.item.utils.StatRange;

import java.util.*;

/**
 * Represents a forge crafting recipe — an item that can be forged at the anvil.
 */
public class ForgeRecipe {

    private final String id;
    private String displayName;
    private String resultItemId;          // MidgardItem ID to produce
    private int requiredLevel;
    private ForgeTier requiredForgeTier;
    private int chapter;                   // Recipe book chapter

    // Primary metal  
    private String primaryMetal;           // Item ID of the main metal
    private int primaryMetalAmount;

    // Secondary materials
    private Map<String, Integer> secondaryMaterials; // itemId → count

    // Mini-game config
    private int hammerStrikes;
    private int sharpeningPasses;
    private double difficultyMultiplier;
    private int heatingTime;               // seconds

    // Base stats for the forged item
    private Map<ItemStat, StatRange> baseStats;

    // XP and specialization
    private int baseXP;
    private String specialization;         // "weaponsmith", "armorsmith", etc.

    // Optional features
    private int maxGemSockets;
    private boolean allowsRuneEngraving;

    // Lore lines for the recipe description
    private List<String> lore;

    // Metal temperature settings (auto-derived from primaryMetal if not set)
    private double idealTempMin;
    private double idealTempMax;

    public ForgeRecipe(String id) {
        this.id = id;
        this.secondaryMaterials = new LinkedHashMap<>();
        this.baseStats = new LinkedHashMap<>();
        this.lore = new ArrayList<>();
        this.difficultyMultiplier = 1.0;
        this.hammerStrikes = 15;
        this.sharpeningPasses = 3;
        this.heatingTime = 20;
        this.chapter = 1;
        this.requiredLevel = 1;
        this.requiredForgeTier = ForgeTier.BASIC;
    }

    // === Getters ===
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getResultItemId() { return resultItemId; }
    public int getRequiredLevel() { return requiredLevel; }
    public ForgeTier getRequiredForgeTier() { return requiredForgeTier; }
    public int getChapter() { return chapter; }
    public String getPrimaryMetal() { return primaryMetal; }
    public int getPrimaryMetalAmount() { return primaryMetalAmount; }
    public Map<String, Integer> getSecondaryMaterials() { return Collections.unmodifiableMap(secondaryMaterials); }
    public int getHammerStrikes() { return hammerStrikes; }
    public int getSharpeningPasses() { return sharpeningPasses; }
    public double getDifficultyMultiplier() { return difficultyMultiplier; }
    public int getHeatingTime() { return heatingTime; }
    public Map<ItemStat, StatRange> getBaseStats() { return Collections.unmodifiableMap(baseStats); }
    public int getBaseXP() { return baseXP; }
    public String getSpecialization() { return specialization; }
    public int getMaxGemSockets() { return maxGemSockets; }
    public boolean isAllowsRuneEngraving() { return allowsRuneEngraving; }
    public List<String> getLore() { return Collections.unmodifiableList(lore); }
    public double getIdealTempMin() { return idealTempMin; }
    public double getIdealTempMax() { return idealTempMax; }

    /**
     * Gets all required materials (primary + secondary) combined.
     */
    public Map<String, Integer> getAllMaterials() {
        Map<String, Integer> all = new LinkedHashMap<>();
        if (primaryMetal != null) {
            all.put(primaryMetal, primaryMetalAmount);
        }
        all.putAll(secondaryMaterials);
        return all;
    }

    /**
     * Gets effective hammer strikes (base + difficulty scaling).
     */
    public int getEffectiveHammerStrikes() {
        return (int) Math.ceil(hammerStrikes * difficultyMultiplier);
    }

    /**
     * Gets effective sharpening passes.
     */
    public int getEffectiveSharpeningPasses() {
        return (int) Math.ceil(sharpeningPasses * difficultyMultiplier);
    }

    // === Setters (builder pattern) ===
    public ForgeRecipe setDisplayName(String displayName) { this.displayName = displayName; return this; }
    public ForgeRecipe setResultItemId(String resultItemId) { this.resultItemId = resultItemId; return this; }
    public ForgeRecipe setRequiredLevel(int requiredLevel) { this.requiredLevel = requiredLevel; return this; }
    public ForgeRecipe setRequiredForgeTier(ForgeTier requiredForgeTier) { this.requiredForgeTier = requiredForgeTier; return this; }
    public ForgeRecipe setChapter(int chapter) { this.chapter = chapter; return this; }
    public ForgeRecipe setPrimaryMetal(String primaryMetal) { this.primaryMetal = primaryMetal; return this; }
    public ForgeRecipe setPrimaryMetalAmount(int primaryMetalAmount) { this.primaryMetalAmount = primaryMetalAmount; return this; }
    public ForgeRecipe setSecondaryMaterials(Map<String, Integer> materials) { this.secondaryMaterials = new LinkedHashMap<>(materials); return this; }
    public ForgeRecipe addSecondaryMaterial(String itemId, int amount) { this.secondaryMaterials.put(itemId, amount); return this; }
    public ForgeRecipe setHammerStrikes(int hammerStrikes) { this.hammerStrikes = hammerStrikes; return this; }
    public ForgeRecipe setSharpeningPasses(int sharpeningPasses) { this.sharpeningPasses = sharpeningPasses; return this; }
    public ForgeRecipe setDifficultyMultiplier(double difficulty) { this.difficultyMultiplier = difficulty; return this; }
    public ForgeRecipe setHeatingTime(int heatingTime) { this.heatingTime = heatingTime; return this; }
    public ForgeRecipe setBaseStats(Map<ItemStat, StatRange> baseStats) { this.baseStats = new LinkedHashMap<>(baseStats); return this; }
    public ForgeRecipe addBaseStat(ItemStat stat, StatRange range) { this.baseStats.put(stat, range); return this; }
    public ForgeRecipe setBaseXP(int baseXP) { this.baseXP = baseXP; return this; }
    public ForgeRecipe setSpecialization(String specialization) { this.specialization = specialization; return this; }
    public ForgeRecipe setMaxGemSockets(int maxGemSockets) { this.maxGemSockets = maxGemSockets; return this; }
    public ForgeRecipe setAllowsRuneEngraving(boolean allows) { this.allowsRuneEngraving = allows; return this; }
    public ForgeRecipe setLore(List<String> lore) { this.lore = new ArrayList<>(lore); return this; }
    public ForgeRecipe setIdealTempMin(double min) { this.idealTempMin = min; return this; }
    public ForgeRecipe setIdealTempMax(double max) { this.idealTempMax = max; return this; }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ForgeRecipe that = (ForgeRecipe) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
