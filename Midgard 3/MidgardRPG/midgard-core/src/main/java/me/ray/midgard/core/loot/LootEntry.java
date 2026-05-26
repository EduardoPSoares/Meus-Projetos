package me.ray.midgard.core.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LootEntry {

    private final LootType type;
    private final String value; // Item ID, Command, or Amount
    private final double chance; // 0-100
    private final int minAmount;
    private final int maxAmount;
    private final List<String> conditions = new ArrayList<>();
    
    // Cache for resolved item/material to avoid repeated lookups
    private transient Object cachedObject;

    public LootEntry(LootType type, String value, double chance, int minAmount, int maxAmount) {
        this.type = type;
        this.value = value;
        this.chance = chance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public boolean canDrop(LootContext context) {
        // Check conditions
        if (context.getPlayer().isPresent()) {
            // Player p = context.getPlayer().get();
            // if (!ScriptEngine.checkConditions(p, conditions)) return false;
        }

        // Check chance with luck
        double roll = ThreadLocalRandom.current().nextDouble(100);
        double effectiveChance = chance + context.getLuck();
        return roll < effectiveChance;
    }

    public int rollAmount() {
        if (minAmount == maxAmount) {
            return minAmount;
        }
        return ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
    }

    public LootType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public double getChance() {
        return chance;
    }

    public List<String> getConditions() {
        return conditions;
    }

    public Object getCachedObject() {
        return cachedObject;
    }

    public void setCachedObject(Object cachedObject) {
        this.cachedObject = cachedObject;
    }
}
