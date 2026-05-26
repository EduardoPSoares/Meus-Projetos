package com.midgard.fooddecay.multiblock;

import com.midgard.fooddecay.FoodDecayConfig;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an active multiblock structure currently processing food.
 * Holds all runtime state: food item, recipe, timing, resources, QTE events, and quality.
 */
public class ProcessingMultiblock {

    final MultiblockType type;
    final List<Location> blocks;
    final List<MultiblockType.RB> patternRotation;
    int tier = 1;

    // Processing state
    ItemStack processingFood;
    MultiblockRecipe activeRecipe;
    long startTime;
    long pausedMs;
    long completedTime; // 0 = not completed yet, >0 = epoch ms when completed

    // Resource state (per machine type)
    int fuel;
    int salt;
    boolean hasWater;
    boolean hasVinegar;
    int wax;

    // Animation
    int animTick;
    Entity foodDisplay;

    // QTE event state
    boolean eventActive;
    long eventStartTime;
    int eventType;
    int eventsHandled;
    int eventsMissed;
    BossBar eventBossBar;
    final List<Entity> eventDisplays = new ArrayList<>();

    // Quality tracking
    float qualityBonus;
    int proximityTicks;

    // Owner tracking (for distance notifications)
    UUID ownerId;

    // Extra processing slots (for multi-slot machines)
    final List<ProcessingSlot> extraSlots = new ArrayList<>();

    public ProcessingMultiblock(MultiblockType type, List<Location> blocks,
                                List<MultiblockType.RB> patternRotation) {
        this.type = type;
        this.blocks = blocks;
        this.patternRotation = patternRotation;
    }

    public MultiblockType getType() { return type; }
    public List<Location> getBlocks() { return blocks; }
    public List<MultiblockType.RB> getPatternRotation() { return patternRotation; }
    public Location getAnchor() { return blocks.isEmpty() ? null : blocks.getFirst(); }
    public int getTier() { return tier; }

    public boolean hasFood() {
        return processingFood != null;
    }

    public long getEffectiveElapsed() {
        return System.currentTimeMillis() - startTime - pausedMs;
    }

    public int getProcessingMinutes(FoodDecayConfig config) {
        if (activeRecipe != null) return activeRecipe.getTimeMinutes();
        return config.getMultiblockProcessingMinutes(type);
    }

    public boolean isComplete(FoodDecayConfig config) {
        if (processingFood == null) return false;
        long required = getProcessingMinutes(config) * 60_000L;
        return getEffectiveElapsed() >= required;
    }

    public boolean containsBlock(Location loc) {
        for (Location b : blocks) {
            if (b.getBlockX() == loc.getBlockX()
                    && b.getBlockY() == loc.getBlockY()
                    && b.getBlockZ() == loc.getBlockZ()
                    && Objects.equals(b.getWorld(), loc.getWorld())) {
                return true;
            }
        }
        return false;
    }

    // Package-private accessors for helper classes
    void setProcessingFood(ItemStack food) { this.processingFood = food; }
    void setActiveRecipe(MultiblockRecipe recipe) { this.activeRecipe = recipe; }
    void setStartTime(long time) { this.startTime = time; }
    long getPausedMs() { return pausedMs; }
    void setPausedMs(long ms) { this.pausedMs = ms; }
    void setCompletedTime(long time) { this.completedTime = time; }
    Entity getFoodDisplay() { return foodDisplay; }
    void setFoodDisplay(Entity display) { this.foodDisplay = display; }

    /**
     * Resets all transient processing state for a new cycle.
     * Call after completing, collecting, or spoiling food.
     */
    void resetProcessingState() {
        processingFood = null;
        activeRecipe = null;
        startTime = 0;
        pausedMs = 0;
        completedTime = 0;
        animTick = 0;
        eventActive = false;
        eventStartTime = 0;
        eventType = 0;
        eventsHandled = 0;
        eventsMissed = 0;
        if (eventBossBar != null) {
            eventBossBar.removeAll();
            eventBossBar = null;
        }
        for (Entity e : eventDisplays) {
            if (e != null && e.isValid()) e.remove();
        }
        eventDisplays.clear();
        qualityBonus = 0;
        proximityTicks = 0;
    }

    // Public getters for cross-package access (GUI, etc.)
    public ItemStack getProcessingFood() { return processingFood; }
    public MultiblockRecipe getActiveRecipe() { return activeRecipe; }
    public long getStartTime() { return startTime; }
    public long getCompletedTime() { return completedTime; }
    public int getFuel() { return fuel; }
    public int getSalt() { return salt; }
    public boolean hasWater() { return hasWater; }
    public boolean hasVinegar() { return hasVinegar; }
    public int getWax() { return wax; }
    public boolean isEventActive() { return eventActive; }
    public int getEventsHandled() { return eventsHandled; }
    public int getEventsMissed() { return eventsMissed; }
    public float getQualityBonus() { return qualityBonus; }
    public UUID getOwnerId() { return ownerId; }
    public List<ProcessingSlot> getExtraSlots() { return extraSlots; }

    /**
     * Returns the total number of items currently being processed (primary + extra slots).
     */
    public int getActiveSlotCount() {
        int count = processingFood != null ? 1 : 0;
        for (ProcessingSlot slot : extraSlots) {
            if (slot.hasFood()) count++;
        }
        return count;
    }
}
