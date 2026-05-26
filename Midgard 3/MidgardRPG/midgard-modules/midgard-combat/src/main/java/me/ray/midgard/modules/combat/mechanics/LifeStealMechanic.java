package me.ray.midgard.modules.combat.mechanics;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.CombatData;
import me.ray.midgard.modules.combat.CombatManager;
import org.bukkit.entity.Player;

public class LifeStealMechanic {

    private final CombatManager combatManager;

    public LifeStealMechanic(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    public void apply(Player attacker, double damage, double lifeSteal) {
        if (attacker == null || lifeSteal <= 0 || damage <= 0) {
            return;
        }

        double heal = damage * (lifeSteal / 100.0);
        MidgardProfile attackerProfile = MidgardCore.getProfileManager().getProfile(attacker.getUniqueId());
        if (attackerProfile != null) {
            CombatData attackerCombat = attackerProfile.getOrCreateData(CombatData.class);
            CoreAttributeData attackerAttrs = attackerProfile.getOrCreateData(CoreAttributeData.class);
            AttributeInstance attMaxHealthAttr = attackerAttrs.getInstance(CombatAttributes.MAX_HEALTH);
            double attMaxHealth = attMaxHealthAttr != null ? attMaxHealthAttr.getValue() : 100;

            double attNewHealth = Math.min(attMaxHealth, attackerCombat.getCurrentHealth() + heal);
            attackerCombat.setCurrentHealth(attNewHealth);
            combatManager.syncHealth(attacker, attNewHealth, attMaxHealth);
        }
    }
}
