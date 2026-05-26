package me.ray.midgard.modules.combat.mechanics;

import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.modules.combat.RPGDamageContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface DamageCalculator {
    DamageResult calculate(Player attacker, LivingEntity victim, CoreAttributeData attackerAttributes, RPGDamageContext context, double baseDamage);
}
