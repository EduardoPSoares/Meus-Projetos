package com.midgard.fooddecay;

import org.bukkit.Material;

/**
 * Food preservation traits inspired by TerraFirmaCraft.
 * Each trait reduces the decay rate of food by a configurable multiplier.
 */
public enum FoodTrait {

    SALTED("Salgado", "§e✦ Salgado", Material.SUGAR),
    SMOKED("Defumado", "§6✦ Defumado", null),
    DRIED("Desidratado", "§c✦ Desidratado", Material.STRING),
    PICKLED("Em Conserva", "§2✦ Em Conserva", Material.GLASS_BOTTLE),
    BRINED("Em Salmoura", "§b✦ Em Salmoura", Material.WATER_BUCKET),
    PRESERVED("Selado", "§d✦ Selado", Material.HONEYCOMB);

    private final String displayName;
    private final String loreLine;
    private final Material defaultIngredient;

    FoodTrait(String displayName, String loreLine, Material defaultIngredient) {
        this.displayName = displayName;
        this.loreLine = loreLine;
        this.defaultIngredient = defaultIngredient;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLoreLine() {
        return loreLine;
    }

    /**
     * Default ingredient material for manual preservation.
     * Null means the trait is applied automatically (e.g., SMOKED via Smoker).
     */
    public Material getDefaultIngredient() {
        return defaultIngredient;
    }

    /**
     * Parse a trait from string, case-insensitive.
     */
    public static FoodTrait fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
