package me.ray.midgard.modules.combat.mechanics;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.CombatData;
import me.ray.midgard.modules.combat.CombatManager;
import me.ray.midgard.modules.combat.RPGDamageCategory;
import me.ray.midgard.modules.combat.RPGDamageContext;
import org.bukkit.entity.Player;

public class ThornsMechanic {

    public void apply(Player attacker, double damage, double elementalDamage, CoreAttributeData victimAttributes, RPGDamageContext context) {
        if (attacker == null) {
            return;
        }

        AttributeInstance thornsAttr = victimAttributes.getInstance(CombatAttributes.THORNS);
        double thorns = thornsAttr != null ? thornsAttr.getValue() : 0.0;

        if (thorns > 0 && (context.hasCategory(RPGDamageCategory.PHYSICAL) || context.hasCategory(RPGDamageCategory.PROJECTILE))) {
            double reflected = (damage + elementalDamage) * (thorns / 100.0);
            if (reflected > 0) {
                // Apply thorns as true damage directly to RPG health
                MidgardProfile attackerProfile = MidgardCore.getProfileManager().getProfile(attacker.getUniqueId());
                if (attackerProfile == null) {
                    return;
                }
                CombatData attackerData = attackerProfile.getOrCreateData(CombatData.class);
                CoreAttributeData attackerAttributes = attackerProfile.getOrCreateData(CoreAttributeData.class);
                AttributeInstance maxHpAttr = attackerAttributes.getInstance(CombatAttributes.MAX_HEALTH);
                double maxHp = maxHpAttr != null ? maxHpAttr.getValue() : 100;

                double newHealth = Math.max(0, attackerData.getCurrentHealth() - reflected);
                attackerData.setCurrentHealth(newHealth);
                CombatManager.getInstance().syncHealth(attacker, newHealth, maxHp);

                if (newHealth <= 0) {
                    attacker.setHealth(0);
                }
            }
        }
    }
}
