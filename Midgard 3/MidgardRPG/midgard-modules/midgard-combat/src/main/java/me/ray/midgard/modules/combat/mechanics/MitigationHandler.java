package me.ray.midgard.modules.combat.mechanics;

import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.CombatConfig;
import me.ray.midgard.modules.combat.RPGDamageCategory;
import me.ray.midgard.modules.combat.RPGDamageContext;
import org.bukkit.event.entity.EntityDamageEvent;

public class MitigationHandler {

    private final CombatConfig config;

    public MitigationHandler(CombatConfig config) {
        this.config = config;
    }

    public double applyMitigation(double damage, CoreAttributeData victimAttributes, CoreAttributeData attackerAttributes, int attackerLevel, RPGDamageContext context, EntityDamageEvent.DamageCause cause, boolean isAttackerPlayer) {
        // 1. Reduções Percentuais Diretas (Damage Reduction)
        // Estas reduções são aplicadas antes da defesa e não sofrem diminishing returns da fórmula de defesa.
        double totalReduction = 0.0;

        AttributeInstance dmgRedAttr = victimAttributes.getInstance(CombatAttributes.DAMAGE_REDUCTION);
        if (dmgRedAttr != null) {
            totalReduction += dmgRedAttr.getValue();
        }

        if (isAttackerPlayer) {
            AttributeInstance pvpRedAttr = victimAttributes.getInstance(CombatAttributes.PVP_DAMAGE_REDUCTION);
            if (pvpRedAttr != null) {
                totalReduction += pvpRedAttr.getValue();
            }
        } else {
            AttributeInstance pveRedAttr = victimAttributes.getInstance(CombatAttributes.PVE_DAMAGE_REDUCTION);
            if (pveRedAttr != null) {
                totalReduction += pveRedAttr.getValue();
            }
        }

        if (cause == EntityDamageEvent.DamageCause.FALL) {
            AttributeInstance fallRedAttr = victimAttributes.getInstance(CombatAttributes.FALL_DAMAGE_REDUCTION);
            if (fallRedAttr != null) {
                totalReduction += fallRedAttr.getValue();
            }
        }

        if (context.hasCategory(RPGDamageCategory.PROJECTILE)) {
            AttributeInstance projRedAttr = victimAttributes.getInstance(CombatAttributes.PROJECTILE_DAMAGE_REDUCTION);
            if (projRedAttr != null) {
                totalReduction += projRedAttr.getValue();
            }
        }

        if (context.hasCategory(RPGDamageCategory.PHYSICAL)) {
            AttributeInstance physRedAttr = victimAttributes.getInstance(CombatAttributes.PHYSICAL_DAMAGE_REDUCTION);
            if (physRedAttr != null) {
                totalReduction += physRedAttr.getValue();
            }
        }

        if (context.hasCategory(RPGDamageCategory.MAGICAL)) {
            AttributeInstance magicRedAttr = victimAttributes.getInstance(CombatAttributes.MAGIC_DAMAGE_REDUCTION);
            if (magicRedAttr != null) {
                totalReduction += magicRedAttr.getValue();
            }
        }

        // DOT Damage Reduction
        if (config.dotReductionEnabled && context.hasCategory(RPGDamageCategory.DOT)) {
            AttributeInstance dotRedAttr = victimAttributes.getInstance(CombatAttributes.DOT_DAMAGE_REDUCTION);
            if (dotRedAttr != null) {
                totalReduction += dotRedAttr.getValue();
            }
        }

        // Skill Damage Reduction
        if (config.skillReductionEnabled && context.hasCategory(RPGDamageCategory.SKILL)) {
            AttributeInstance skillRedAttr = victimAttributes.getInstance(CombatAttributes.SKILL_DAMAGE_REDUCTION);
            if (skillRedAttr != null) {
                totalReduction += skillRedAttr.getValue();
            }
        }

        // Minion Damage Reduction
        if (config.minionReductionEnabled && context.hasCategory(RPGDamageCategory.MINION)) {
            AttributeInstance minionRedAttr = victimAttributes.getInstance(CombatAttributes.MINION_DAMAGE_REDUCTION);
            if (minionRedAttr != null) {
                totalReduction += minionRedAttr.getValue();
            }
        }

        // Aplica redução percentual (Hard Cap de 100% implícito se totalReduction >= 100)
        if (totalReduction > 0) {
            damage *= Math.max(0.0, 1.0 - (totalReduction / 100.0));
        }

        // 2. Extração de Penetração
        double armorPen = 0.0;
        double armorPenFlat = 0.0;
        double magicPen = 0.0;
        double magicPenFlat = 0.0;

        if (attackerAttributes != null) {
            AttributeInstance ap = attackerAttributes.getInstance(CombatAttributes.ARMOR_PENETRATION);
            if (ap != null) {
                armorPen = ap.getValue();
            }
            
            AttributeInstance apFlat = attackerAttributes.getInstance(CombatAttributes.ARMOR_PENETRATION_FLAT);
            if (apFlat != null) {
                armorPenFlat = apFlat.getValue();
            }

            AttributeInstance mp = attackerAttributes.getInstance(CombatAttributes.MAGIC_PENETRATION);
            if (mp != null) {
                magicPen = mp.getValue();
            }

            AttributeInstance mpFlat = attackerAttributes.getInstance(CombatAttributes.MAGIC_PENETRATION_FLAT);
            if (mpFlat != null) {
                magicPenFlat = mpFlat.getValue();
            }
        }

        // 3. Cálculo do Divisor de Defesa (Escalonamento por Nível)
        double divisor = config.defenseDivisor;
        if (config.defenseScalingEnabled) {
            // Divisor aumenta com o nível do atacante, tornando a defesa fixa menos efetiva contra níveis altos.
            // Ex: Nível 1 -> Divisor 20. Nível 100 -> Divisor 2000.
            // Defesa 100 vs Nível 1 (Div 20) -> 100 / 120 = 83% Redução
            // Defesa 100 vs Nível 100 (Div 2000) -> 100 / 2100 = 4.7% Redução
            divisor = config.defenseScalingBase * Math.max(1, attackerLevel);
        }
        if (divisor <= 0) {
            divisor = 100.0; // Prevent division by zero / NaN propagation
        }

        // 4. Cálculo de Mitigação por Defesa/Resistência
        double mitigation = 0.0;

        // Físico / Projétil / Ambiental(Físico) -> Defesa
        if (context.hasCategory(RPGDamageCategory.PHYSICAL) ||
                context.hasCategory(RPGDamageCategory.PROJECTILE) ||
                (context.hasCategory(RPGDamageCategory.ENVIRONMENTAL) && !context.hasCategory(RPGDamageCategory.MAGICAL))) {

            AttributeInstance defenseAttr = victimAttributes.getInstance(CombatAttributes.DEFENSE);
            double defense = defenseAttr != null ? defenseAttr.getValue() : 0.0;

            if (defense > 0) {
                // Fórmula Híbrida: (Defesa - Flat) * (1 - %)
                double effectiveDefense = Math.max(0, (defense - armorPenFlat) * (1.0 - (armorPen / 100.0)));
                mitigation = effectiveDefense / (effectiveDefense + divisor);
            }
        }

        // Mágico / Ambiental(Mágico) -> Resistência Mágica
        if (context.hasCategory(RPGDamageCategory.MAGICAL) ||
                (context.hasCategory(RPGDamageCategory.ENVIRONMENTAL) && context.hasCategory(RPGDamageCategory.MAGICAL))) {

            AttributeInstance magicResAttr = victimAttributes.getInstance(CombatAttributes.MAGIC_RESISTANCE);
            double magicRes = magicResAttr != null ? magicResAttr.getValue() : 0.0;

            if (magicRes > 0) {
                double effectiveRes = Math.max(0, (magicRes - magicPenFlat) * (1.0 - (magicPen / 100.0)));
                // Usa o maior valor de mitigação se já tiver calculado (ex: dano híbrido) ou substitui
                mitigation = Math.max(mitigation, effectiveRes / (effectiveRes + divisor));
            }
        }

        // Dano Global / Verdadeiro -> Ignora defesa
        if (context.hasCategory(RPGDamageCategory.GLOBAL)) {
            mitigation = 0.0;
        }

        // 5. Aplica Hard Cap na Mitigação
        // Impede que jogadores fiquem imortais apenas com defesa
        if (mitigation > config.maxMitigation) {
            mitigation = config.maxMitigation;
        }

        damage *= (1.0 - mitigation);
        return damage;
    }
}
