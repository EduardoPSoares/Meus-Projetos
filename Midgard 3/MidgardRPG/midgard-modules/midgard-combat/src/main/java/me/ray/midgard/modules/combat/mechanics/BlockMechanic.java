package me.ray.midgard.modules.combat.mechanics;

import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.DamageIndicatorManager;
import org.bukkit.entity.LivingEntity;

import java.util.concurrent.ThreadLocalRandom;

public class BlockMechanic {

    private final DamageIndicatorManager indicatorManager;

    public BlockMechanic(DamageIndicatorManager indicatorManager) {
        this.indicatorManager = indicatorManager;
    }

    /**
     * Tenta aplicar o bloqueio e retorna o dano modificado.
     */
    public double apply(double currentDamage, LivingEntity victim, CoreAttributeData victimAttributes, CoreAttributeData attackerAttributes) {
        AttributeInstance blockRatingAttr = victimAttributes.getInstance(CombatAttributes.BLOCK_RATING);
        double blockChance = blockRatingAttr != null ? blockRatingAttr.getValue() : 0.0;

        if (attackerAttributes != null) {
            AttributeInstance accuracyAttr = attackerAttributes.getInstance(CombatAttributes.ACCURACY);
            if (accuracyAttr != null) {
                blockChance = Math.max(0, blockChance - accuracyAttr.getValue());
            }
        }

        if (blockChance > 0 && ThreadLocalRandom.current().nextDouble() * 100 < blockChance) {
            AttributeInstance blockPowerAttr = victimAttributes.getInstance(CombatAttributes.BLOCK_POWER);
            double blockPower = blockPowerAttr != null ? Math.min(blockPowerAttr.getValue(), 100.0) : 0.0;
            
            if (blockPower > 0) {
                double reducedDamage = currentDamage * (1.0 - (blockPower / 100.0));
                indicatorManager.spawnCustomIndicator(victim, "BLOCK", "<gray>");
                return reducedDamage;
            }
        }
        return currentDamage;
    }
}
