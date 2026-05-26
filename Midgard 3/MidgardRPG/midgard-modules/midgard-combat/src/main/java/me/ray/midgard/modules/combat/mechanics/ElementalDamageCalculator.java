package me.ray.midgard.modules.combat.mechanics;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.CombatConfig;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ElementalDamageCalculator {

    private final CombatConfig config;
    
    // Cache para evitar manipulação de strings repetitiva
    private static final Map<String, String> ELEMENT_FROM_ATTR_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> NORMALIZED_ATTR_CACHE = new ConcurrentHashMap<>();

    public ElementalDamageCalculator(CombatConfig config) {
        this.config = config;
    }

    public void calculateAndApply(CoreAttributeData attackerAttributes, LivingEntity victim, Map<String, Double> damageMap, double[] totalElementalDamage, int attackerLevel) {
        String victimElement = getVictimElement(victim);

        for (Map.Entry<String, String> entry : CombatAttributes.ELEMENTAL_MAP.entrySet()) {
            String dmgAttrId = entry.getKey();
            String defAttrId = entry.getValue();

            AttributeInstance dmgInstance = attackerAttributes.getInstance(dmgAttrId);
            if (dmgInstance != null && dmgInstance.getValue() > 0) {
                double eDmg = dmgInstance.getValue();
                eDmg = applyMitigation(eDmg, dmgAttrId, defAttrId, victim, victimElement, attackerLevel);

                totalElementalDamage[0] += eDmg;
                damageMap.put(dmgAttrId, eDmg);
            }
        }
    }

    public double calculateMitigatedDamage(String element, double damage, LivingEntity victim, int attackerLevel) {
        String victimElement = getVictimElement(victim);
        
        // Usa cache para normalização
        String dmgAttrId = NORMALIZED_ATTR_CACHE.computeIfAbsent(element, k -> 
            k.toLowerCase().endsWith("_damage") ? k.toLowerCase() : k.toLowerCase() + "_damage"
        );
        
        String defAttrId = CombatAttributes.ELEMENTAL_MAP.get(dmgAttrId);
        
        if (defAttrId == null) {
            return damage;
        }

        return applyMitigation(damage, dmgAttrId, defAttrId, victim, victimElement, attackerLevel);
    }

    private double applyMitigation(double damage, String dmgAttrId, String defAttrId, LivingEntity victim, String victimElement, int attackerLevel) {
        double eDmg = damage;

        // Elemental Weakness/Resistance Multiplier
        if (config.elementalInteractionsEnabled && victimElement != null) {
            String attackerElement = getElementFromAttribute(dmgAttrId);
            if (config.elementalMultipliers.containsKey(attackerElement)) {
                Map<String, Double> targets = config.elementalMultipliers.get(attackerElement);
                if (targets.containsKey(victimElement)) {
                    eDmg *= targets.get(victimElement);
                }
            }
        }

        // Verifica defesa da vítima
        double eDef = 0.0;
        if (victim instanceof Player victimPlayer) {
            MidgardProfile victimProfile = MidgardCore.getProfileManager().getProfile(victimPlayer.getUniqueId());
            if (victimProfile != null) {
                CoreAttributeData victimAttributes = victimProfile.getOrCreateData(CoreAttributeData.class);
                AttributeInstance defInstance = victimAttributes.getInstance(defAttrId);
                if (defInstance != null) {
                    eDef = defInstance.getValue();
                }
            }
        } else {
             try {
                 eDef = me.ray.midgard.core.integration.MythicMobsIntegration.getAttributeValue(victim, defAttrId);
             } catch (Exception ignored) { /* MythicMobs pode não estar disponível */ }
        }

        // Mitigação Elemental (com escalonamento por nível do atacante)
        if (eDef > 0) {
            double divisor = config.defenseDivisor;
            if (config.defenseScalingEnabled) {
                divisor = config.defenseScalingBase * Math.max(1, attackerLevel);
            }
            if (divisor <= 0) {
                divisor = 100.0;
            }
            double reduction = eDef / (eDef + divisor);
            if (reduction > config.maxMitigation) {
                reduction = config.maxMitigation;
            }
            eDmg *= (1.0 - reduction);
        }
        
        return eDmg;
    }

    private String getElementFromAttribute(String attribute) {
        return ELEMENT_FROM_ATTR_CACHE.computeIfAbsent(attribute, k -> k.replace("_damage", "").toLowerCase());
    }

    private String getVictimElement(LivingEntity victim) {
        // Check tags first (e.g. from Debug Dummy or custom mobs)
        for (String tag : victim.getScoreboardTags()) {
            if (tag.startsWith("dummy_type_")) {
                // Cachear isso seria complexo pois depende da tag, mas é menos frequente que o loop de dano
                return tag.substring(11).replace("_damage", "").toLowerCase();
            }
            if (tag.startsWith("element_")) {
                return tag.substring(8).toLowerCase();
            }
        }
        
        return null;
    }
}
