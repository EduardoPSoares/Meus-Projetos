package com.midgard.fooddecay;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class WeightSettingsLoader {

    private WeightSettingsLoader() {
    }

    static LoadedWeightSettings load(FileConfiguration cfg) {
        boolean enabled = cfg.getBoolean("weight-size.enabled", false);
        boolean containerRestrictionsEnabled = cfg.getBoolean("weight-size.container-restrictions", true);
        double maxKgPerStack = Math.max(0.01, loadWeightSettingKg(
                cfg,
                "weight-size.max-kg-per-stack",
                "weight-size.max-oz-per-stack",
                64.0 * FoodDecayConfig.ouncesToKg(1.0)
        ));
        String defaultSize = cfg.getString("weight-size.default-size", "MEDIUM").toUpperCase();
        double defaultKg = Math.max(0.001, loadWeightSettingKg(
                cfg,
                "weight-size.default-kg",
                "weight-size.default-oz",
                4.0 * FoodDecayConfig.ouncesToKg(1.0)
        ));

        Map<Material, Double> weightPerFood = new EnumMap<>(Material.class);
        Map<Material, String> sizePerFood = new EnumMap<>(Material.class);
        ConfigurationSection weightFoodsSec = cfg.getConfigurationSection("weight-size.foods");
        if (weightFoodsSec != null) {
            for (String key : weightFoodsSec.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    ConfigurationSection foodSec = weightFoodsSec.getConfigurationSection(key);
                    if (foodSec == null) {
                        continue;
                    }

                    double weightKg = loadFoodWeightKg(foodSec, defaultKg);
                    weightPerFood.put(mat, Math.max(0.001, weightKg));
                    sizePerFood.put(mat, foodSec.getString("size", defaultSize).toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        Map<String, String> sizeDisplayNames = new HashMap<>();
        Map<String, String> sizeColors = new HashMap<>();
        ConfigurationSection sizeCatsSec = cfg.getConfigurationSection("weight-size.size-categories");
        if (sizeCatsSec != null) {
            for (String key : sizeCatsSec.getKeys(false)) {
                String upper = key.toUpperCase();
                sizeDisplayNames.put(upper, sizeCatsSec.getString(key + ".name", key));
                sizeColors.put(upper, sizeCatsSec.getString(key + ".color", "&7"));
            }
        }

        Map<Material, Set<String>> containerRestrictions = new EnumMap<>(Material.class);
        ConfigurationSection containerRestSec = cfg.getConfigurationSection("weight-size.container-restrictions-map");
        if (containerRestSec != null) {
            for (String key : containerRestSec.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    List<String> blocked = containerRestSec.getStringList(key);
                    Set<String> normalized = new HashSet<>();
                    for (String value : blocked) {
                        normalized.add(value.toUpperCase());
                    }
                    containerRestrictions.put(mat, normalized);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        return new LoadedWeightSettings(
                enabled,
                containerRestrictionsEnabled,
                maxKgPerStack,
                defaultSize,
                defaultKg,
                weightPerFood,
                sizePerFood,
                sizeDisplayNames,
                sizeColors,
                containerRestrictions
        );
    }

    private static double loadWeightSettingKg(FileConfiguration cfg, String kgPath, String legacyOzPath, double defaultKg) {
        if (cfg.contains(kgPath, true)) {
            return cfg.getDouble(kgPath, defaultKg);
        }
        if (cfg.contains(legacyOzPath, true)) {
            return FoodDecayConfig.ouncesToKg(cfg.getDouble(legacyOzPath));
        }
        return defaultKg;
    }

    private static double loadFoodWeightKg(ConfigurationSection foodSec, double defaultKg) {
        if (foodSec.contains("weight-kg", true)) {
            return foodSec.getDouble("weight-kg", defaultKg);
        }
        if (foodSec.contains("weight", true)) {
            return FoodDecayConfig.ouncesToKg(foodSec.getDouble("weight"));
        }
        return defaultKg;
    }

    record LoadedWeightSettings(
            boolean enabled,
            boolean containerRestrictionsEnabled,
            double maxKgPerStack,
            String defaultSize,
            double defaultKg,
            Map<Material, Double> weightPerFood,
            Map<Material, String> sizePerFood,
            Map<String, String> sizeDisplayNames,
            Map<String, String> sizeColors,
            Map<Material, Set<String>> containerRestrictions
    ) {
    }
}
