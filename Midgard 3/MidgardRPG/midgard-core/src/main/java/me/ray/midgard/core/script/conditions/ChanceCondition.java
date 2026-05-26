package me.ray.midgard.core.script.conditions;

import me.ray.midgard.core.script.Condition;
import me.ray.midgard.core.utils.MathUtils;
import org.bukkit.entity.Player;

public class ChanceCondition implements Condition {

    private final double chance;

    public ChanceCondition(double chance) {
        this.chance = chance;
    }

    @Override
    public boolean check(Player player) {
        return MathUtils.chance(chance);
    }
}
