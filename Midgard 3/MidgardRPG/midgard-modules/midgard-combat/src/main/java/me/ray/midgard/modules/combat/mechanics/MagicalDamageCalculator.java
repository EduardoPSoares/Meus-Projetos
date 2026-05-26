package me.ray.midgard.modules.combat.mechanics;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.RPGDamageCategory;
import me.ray.midgard.modules.combat.RPGDamageContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MagicalDamageCalculator implements DamageCalculator {

    @Override
    public DamageResult calculate(Player attacker, LivingEntity victim, CoreAttributeData attackerAttributes, RPGDamageContext context, double baseDamage) {
        me.ray.midgard.modules.combat.CombatConfig config = me.ray.midgard.modules.combat.CombatManager.getInstance().getConfig();
        double damage = config.baseHandDamage; // Base damage for magic too? Usually scaled by spell/wand.
        
        // Se for um ataque mágico, geralmente vem de uma Skill ou Varinha
        // Se vier de evento de dano (baseDamage > 0), usamos como "Weapon Damage" equivalente
        double spellBaseDamage = baseDamage;
        if (spellBaseDamage <= 1.0) {
            spellBaseDamage = 0; // Ignore vanilla punch damage as spell base
        }
        
        AttributeInstance magicDmgAttr = attackerAttributes.getInstance(CombatAttributes.MAGIC_DAMAGE);
        double flatMagicDamage = magicDmgAttr != null ? magicDmgAttr.getValue() : 0.0;
        
        // Weapon Damage also applies to magic? Usually "Magic Damage" stats are separate
        // But some systems use Weapon Damage for everything. Let's keep separate for now.

        if (config.damageFormulaMode == me.ray.midgard.modules.combat.CombatConfig.ScalingMode.MULTIPLICATIVE) {
            // Modo RPG Moderno
            // Dano = (SpellBase) * (1 + (Int * Multiplier)) + FlatBonuses
            
            AttributeInstance intAttr = attackerAttributes.getInstance(CombatAttributes.INTELLIGENCE);
            double intelligence = intAttr != null ? intAttr.getValue() : 0.0;
            
            double multiplier = 1.0 + (intelligence * config.intelligenceMultiplier);
            
            // Se spellBaseDamage for 0 (ex: ataque de varinha customizada lida via atributo), busque atributo "Dano Mágico" base
            // Mas aqui 'flatMagicDamage' pode ser o "Multiplier" se a varinha der +10 Dano Mágico.
            // Para simplificar: No modo multiplicativo, assumimos que 'flatMagicDamage' é o dano BASE vindo de equipamentos.
            
            damage = (spellBaseDamage + flatMagicDamage) * multiplier;
            
        } else {
            // Modo Aditivo
            damage = spellBaseDamage + flatMagicDamage;
        }

        List<String> types = new ArrayList<>();

        // Simplificação, a lógica de tipped arrow pode ser passada no contexto ou verificada aqui se tivermos o evento
        // Mas como o contexto já processa categorias, podemos confiar nele ou passar o evento se necessário.
        // Para manter a interface limpa, vamos assumir que o contexto já sabe se é projétil.
        
        if (context.hasCategory(RPGDamageCategory.PROJECTILE)) {
            types.add("Projectile");
        }

        types.add("Magical");

        // Skill Critical Hit (gated por config.criticalEnabled)
        boolean isCritical = false;
        if (config.criticalEnabled) {
            AttributeInstance skillCritChanceAttr = attackerAttributes.getInstance(CombatAttributes.SKILL_CRITICAL_CHANCE);
            double skillCritChance = skillCritChanceAttr != null ? skillCritChanceAttr.getValue() : 0.0;

            // Critical Resistance from victim
            if (victim instanceof Player p) {
                MidgardProfile victimProfile = MidgardCore.getProfileManager().getProfile(p.getUniqueId());
                if (victimProfile != null) {
                    CoreAttributeData victimData = victimProfile.getOrCreateData(CoreAttributeData.class);
                    AttributeInstance critResAttr = victimData.getInstance(CombatAttributes.CRITICAL_RESISTANCE);
                    if (critResAttr != null) {
                        skillCritChance = Math.max(0, skillCritChance - critResAttr.getValue());
                    }
                }
            }

            AttributeInstance skillCritDmgAttr = attackerAttributes.getInstance(CombatAttributes.SKILL_CRITICAL_DAMAGE);
            double skillCritDamage = skillCritDmgAttr != null ? skillCritDmgAttr.getValue() : 150.0;

            if (skillCritChance > 0 && ThreadLocalRandom.current().nextDouble() * 100 < skillCritChance) {
                damage *= (skillCritDamage / 100.0);
                isCritical = true;
            }
        }

        String key = String.join("+", types);
        return new DamageResult(damage, isCritical, key);
    }
}
