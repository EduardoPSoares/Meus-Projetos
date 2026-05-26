package com.midgard.fooddecay.multiblock;

import com.midgard.fooddecay.FoodDecayConfig;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents an additional processing slot within a multiblock machine.
 * Each slot holds one food item being processed with its own timing and quality.
 * Used for multi-slot support when a player's level allows more simultaneous items.
 */
public class ProcessingSlot {

    ItemStack food;
    MultiblockRecipe recipe;
    long startTime;
    long pausedMs;
    long completedTime;
    float qualityBonus;
    UUID ownerId;
    Entity foodDisplay;
    int animTick;

    public boolean hasFood() { return food != null; }

    public ItemStack getFood() { return food; }
    public MultiblockRecipe getRecipe() { return recipe; }
    public long getStartTime() { return startTime; }
    public long getCompletedTime() { return completedTime; }
    public float getQualityBonus() { return qualityBonus; }
    public UUID getOwnerId() { return ownerId; }

    long getEffectiveElapsed() {
        return System.currentTimeMillis() - startTime - pausedMs;
    }

    int getProcessingMinutes(FoodDecayConfig config, MultiblockType type) {
        if (recipe != null) return recipe.getTimeMinutes();
        return config.getMultiblockProcessingMinutes(type);
    }

    boolean isComplete(FoodDecayConfig config, MultiblockType type) {
        if (food == null) return false;
        long required = getProcessingMinutes(config, type) * 60_000L;
        return getEffectiveElapsed() >= required;
    }

    void reset() {
        food = null;
        recipe = null;
        startTime = 0;
        pausedMs = 0;
        completedTime = 0;
        qualityBonus = 0;
        animTick = 0;
    }
}
