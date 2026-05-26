package me.ray.midgard.modules.spells.data;

import java.util.Collections;
import java.util.Map;

public record SpellMilestone(
    int level,
    String visualEffect,
    Map<String, Double> statBonuses,
    String mechanicSkillOverride
) {
    public SpellMilestone {
        statBonuses = statBonuses != null ? Collections.unmodifiableMap(statBonuses) : Collections.emptyMap();
    }
}
