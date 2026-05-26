package me.ray.midgard.modules.combat.mechanics;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.RPGDamageCategory;
import me.ray.midgard.modules.combat.RPGDamageContext;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class PhysicalDamageCalculator implements DamageCalculator {

    @Override
    public DamageResult calculate(Player attacker, LivingEntity victim, CoreAttributeData attackerAttributes, RPGDamageContext context, double baseDamage) {
        me.ray.midgard.modules.combat.CombatConfig config = me.ray.midgard.modules.combat.CombatManager.getInstance().getConfig();
        double damage = config.baseHandDamage; // Começa com dano base da configuração (ex: 1.0)
        
        // Verifica se é um item Midgard para adicionar o dano da arma
        // Se a arma tiver dano vanilla 7, o baseDamage do evento vem como ~7 (dependendo do attack cooldown)
        // Aqui nós ignoramos o damage do evento (baseDamage param) para ter controle total,
        // mas precisamos somar o dano da arma se não for item customizado.
        
        boolean isCritical = false;

        // Lê o dano unificado (attack_damage) da vanilla attribute.
        // Itens novos usam apenas attack-damage. Itens legados podem ainda ter weapon_damage/physical_damage.
        double attackDamage = 0.0;
        org.bukkit.attribute.Attribute attackAttr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.attack_damage"));
        if (attackAttr != null && attacker.getAttribute(attackAttr) != null) {
            attackDamage = Math.max(0, attacker.getAttribute(attackAttr).getValue() - 1.0);
        }

        // Backward compat: lê weapon_damage e physical_damage de itens legados
        AttributeInstance weaponDmgAttr = attackerAttributes.getInstance(CombatAttributes.WEAPON_DAMAGE);
        double legacyWeaponDamage = weaponDmgAttr != null ? weaponDmgAttr.getValue() : 0.0;
        
        AttributeInstance physDmgAttr = attackerAttributes.getInstance(CombatAttributes.PHYSICAL_DAMAGE);
        double legacyPhysicalDamage = physDmgAttr != null ? physDmgAttr.getValue() : 0.0;

        // Usa o maior entre dano unificado e legado para evitar double-dipping
        double totalWeaponDamage = Math.max(attackDamage, legacyWeaponDamage) + legacyPhysicalDamage;
        
        // --- CÁLCULO DA FÓRMULA ---
        if (config.damageFormulaMode == me.ray.midgard.modules.combat.CombatConfig.ScalingMode.MULTIPLICATIVE) {
            // Modo RPG Moderno (Wynncraft/RuneScape style)
            // Dano = (BaseHand + Dano) * (1 + (Strength * Multiplier))
            
            AttributeInstance strAttr = attackerAttributes.getInstance(CombatAttributes.STRENGTH);
            double strength = strAttr != null ? strAttr.getValue() : 0.0;
            
            double baseTotal = damage + totalWeaponDamage;
            double multiplier = 1.0 + (strength * config.strengthMultiplier);
            
            damage = baseTotal * multiplier;
            
        } else {
            // Modo Clássico (Aditivo)
            // Dano = BaseHand + Dano + (Strength -> Flat via Listener)
            
            damage = damage + totalWeaponDamage;
        }

        if (context.hasCategory(RPGDamageCategory.PROJECTILE)) {
            AttributeInstance projDmgAttr = attackerAttributes.getInstance(CombatAttributes.PROJECTILE_DAMAGE);
            double projectileDamage = projDmgAttr != null ? projDmgAttr.getValue() : 0.0;
            damage += projectileDamage;
        }

        AttributeInstance undeadDmgAttr = attackerAttributes.getInstance(CombatAttributes.UNDEAD_DAMAGE);
        double undeadDamage = undeadDmgAttr != null ? undeadDmgAttr.getValue() : 0.0;

        if (victim instanceof org.bukkit.entity.Monster && (victim.getType().name().contains("ZOMBIE") || victim.getType().name().contains("SKELETON") || victim.getType().name().contains("PHANTOM") || victim.getType().name().contains("WITHER"))) {
            damage += undeadDamage;
        }

        // Acerto Crítico (gated por config.criticalEnabled)
        if (config.criticalEnabled) {
            AttributeInstance critChanceAttr = attackerAttributes.getInstance(CombatAttributes.CRITICAL_CHANCE);
            double critChance = critChanceAttr != null ? critChanceAttr.getValue() : 5.0;

            // Critical Resistance (Reduz a chance de crítico do atacante)
            if (victim instanceof Player p) {
                 MidgardProfile profile = MidgardCore.getProfileManager().getProfile(p.getUniqueId());
                 if (profile != null) {
                     CoreAttributeData victimData = profile.getOrCreateData(CoreAttributeData.class);
                     AttributeInstance critResAttr = victimData.getInstance(CombatAttributes.CRITICAL_RESISTANCE);
                     if (critResAttr != null) {
                         critChance = Math.max(0, critChance - critResAttr.getValue());
                     }
                 }
            }

            AttributeInstance critDamageAttr = attackerAttributes.getInstance(CombatAttributes.CRITICAL_DAMAGE);
            double critDamage = critDamageAttr != null ? critDamageAttr.getValue() : 150.0;

            if (ThreadLocalRandom.current().nextDouble() * 100 < critChance) {
                damage *= (critDamage / 100.0);
                isCritical = true;
            }
        }

        List<String> types = new ArrayList<>();
        boolean isProjectile = context.hasCategory(RPGDamageCategory.PROJECTILE);

        if (isProjectile) {
            types.add("Projectile");
        }

        if (context.hasCategory(RPGDamageCategory.PHYSICAL)) {
            types.add("Physical");
        }

        // Fallback se por algum motivo não tiver nenhum
        if (types.isEmpty()) {
            types.add("Physical");
        }

        String key = String.join("+", types);
        return new DamageResult(damage, isCritical, key);
    }
}
