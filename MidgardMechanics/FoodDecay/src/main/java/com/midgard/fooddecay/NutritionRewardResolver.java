package com.midgard.fooddecay;

import org.bukkit.attribute.Attribute;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class NutritionRewardResolver {

    record ResolvedRewards(
            double healthBonus,
            Map<Attribute, Double> attributeBonuses,
            List<String> effects,
            Map<String, Double> mmocoreStats,
            Set<NutritionManager.FoodGroup> activeGroups
    ) {
    }

    private NutritionRewardResolver() {
    }

    static ResolvedRewards resolve(double[] nutrition,
                                   double defaultThreshold,
                                   double defaultHealthBonus,
                                   Map<String, FoodDecayConfig.GroupBonus> configuredBonuses) {
        double totalHealthBonus = 0D;
        Map<Attribute, Double> attributeBonuses = new LinkedHashMap<>();
        List<String> effects = new ArrayList<>();
        Map<String, Double> mmocoreStats = new LinkedHashMap<>();
        Set<NutritionManager.FoodGroup> activeGroups = EnumSet.noneOf(NutritionManager.FoodGroup.class);

        for (NutritionManager.FoodGroup group : NutritionManager.FoodGroup.values()) {
            FoodDecayConfig.GroupBonus bonus = configuredBonuses.get(group.name());
            double threshold = bonus != null && bonus.activationThreshold() != null
                    ? bonus.activationThreshold()
                    : defaultThreshold;
            double value = group.ordinal() < nutrition.length ? nutrition[group.ordinal()] : 0D;
            if (value < threshold) {
                continue;
            }

            activeGroups.add(group);
            if (bonus == null) {
                totalHealthBonus += defaultHealthBonus;
                continue;
            }

            totalHealthBonus += bonus.healthBonus();
            effects.addAll(bonus.effects());
            bonus.attributes().forEach((attribute, amount) ->
                    attributeBonuses.merge(attribute, amount, Double::sum));
            bonus.mmocoreStats().forEach((stat, amount) ->
                    mmocoreStats.merge(stat, amount, Double::sum));
        }

        return new ResolvedRewards(
                totalHealthBonus,
                Map.copyOf(attributeBonuses),
                List.copyOf(effects),
                Map.copyOf(mmocoreStats),
                Set.copyOf(activeGroups)
        );
    }
}
