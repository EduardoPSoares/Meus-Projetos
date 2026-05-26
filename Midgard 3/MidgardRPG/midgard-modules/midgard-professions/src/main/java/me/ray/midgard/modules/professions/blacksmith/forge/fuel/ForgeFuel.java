package me.ray.midgard.modules.professions.blacksmith.forge.fuel;

import org.bukkit.Material;

/**
 * Represents a type of fuel that can be used in the forge.
 * Each fuel has different heating power, burn time, and quality bonuses.
 */
public class ForgeFuel {

    private final Material material;
    private final String displayName;
    private final double heatingPower;     // Multiplier for heating speed (1.0 = normal)
    private final int burnTime;            // Ticks the fuel lasts
    private final double qualityBonus;     // Bonus to final quality (0.0 - 0.15)
    private final int minForgeLevel;       // Minimum profession level to use

    public ForgeFuel(Material material, String displayName, double heatingPower,
                     int burnTime, double qualityBonus, int minForgeLevel) {
        this.material = material;
        this.displayName = displayName;
        this.heatingPower = heatingPower;
        this.burnTime = burnTime;
        this.qualityBonus = qualityBonus;
        this.minForgeLevel = minForgeLevel;
    }

    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public double getHeatingPower() { return heatingPower; }
    public int getBurnTime() { return burnTime; }
    public double getQualityBonus() { return qualityBonus; }
    public int getMinForgeLevel() { return minForgeLevel; }
}
