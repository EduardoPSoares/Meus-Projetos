package me.ray.midgard.modules.combat.mechanics;

import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.DamageIndicatorManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.concurrent.ThreadLocalRandom;

public class DodgeMechanic {

    private final DamageIndicatorManager indicatorManager;

    public DodgeMechanic(DamageIndicatorManager indicatorManager) {
        this.indicatorManager = indicatorManager;
    }

    public boolean apply(EntityDamageEvent event, LivingEntity victim, CoreAttributeData victimAttributes, CoreAttributeData attackerAttributes) {
        AttributeInstance dodgeAttr = victimAttributes.getInstance(CombatAttributes.DODGE_RATING);
        double dodgeChance = dodgeAttr != null ? dodgeAttr.getValue() : 0.0;

        if (attackerAttributes != null) {
            AttributeInstance accuracyAttr = attackerAttributes.getInstance(CombatAttributes.ACCURACY);
            if (accuracyAttr != null) {
                // Precisão reduz diretamente a chance de esquiva
                dodgeChance = Math.max(0, dodgeChance - accuracyAttr.getValue());
            }
        }

        if (dodgeChance > 0 && ThreadLocalRandom.current().nextDouble() * 100 < dodgeChance) {
            indicatorManager.spawnCustomIndicator(victim, "DODGE", "<aqua>");
            event.setCancelled(true);
            return true;
        }
        return false;
    }
}
