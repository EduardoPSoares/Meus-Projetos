package me.ray.midgard.modules.combat.mechanics;

import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.DamageIndicatorManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.concurrent.ThreadLocalRandom;

public class ParryMechanic {

    private final DamageIndicatorManager indicatorManager;

    public ParryMechanic(DamageIndicatorManager indicatorManager) {
        this.indicatorManager = indicatorManager;
    }

    public boolean apply(EntityDamageEvent event, LivingEntity victim, CoreAttributeData victimAttributes, CoreAttributeData attackerAttributes) {
        AttributeInstance parryAttr = victimAttributes.getInstance(CombatAttributes.PARRY_RATING);
        double parryChance = parryAttr != null ? parryAttr.getValue() : 0.0;

        if (attackerAttributes != null) {
            AttributeInstance accuracyAttr = attackerAttributes.getInstance(CombatAttributes.ACCURACY);
            if (accuracyAttr != null) {
                parryChance = Math.max(0, parryChance - accuracyAttr.getValue());
            }
        }

        if (parryChance > 0 && ThreadLocalRandom.current().nextDouble() * 100 < parryChance) {
            indicatorManager.spawnCustomIndicator(victim, "PARRY", "<yellow>");
            event.setCancelled(true);
            return true;
        }
        return false;
    }
}
