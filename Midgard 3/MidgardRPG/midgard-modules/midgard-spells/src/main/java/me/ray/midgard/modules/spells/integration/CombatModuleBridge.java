package me.ray.midgard.modules.spells.integration;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.CombatData;
import me.ray.midgard.modules.spells.api.ResourceProvider;
import org.bukkit.entity.Player;

public class CombatModuleBridge implements ResourceProvider {

    @Override
    public double getMana(Player player) {
        MidgardProfile coreProfile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (coreProfile == null) { return 0.0; }
        
        CombatData data = coreProfile.getData(CombatData.class);
        return (data != null) ? data.getCurrentMana() : 0.0;
    }

    @Override
    public double getStamina(Player player) {
        MidgardProfile coreProfile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (coreProfile == null) { return 0.0; }
        
        CombatData data = coreProfile.getData(CombatData.class);
        return (data != null) ? data.getCurrentStamina() : 0.0;
    }

    @Override
    public boolean consumeMana(Player player, double amount) {
        MidgardProfile coreProfile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (coreProfile == null) { return false; }
        
        CombatData data = coreProfile.getData(CombatData.class);
        if (data == null) { return false; }

        if (data.getCurrentMana() >= amount) {
            double newMana = data.getCurrentMana() - amount;
            // Cap at max mana to prevent overflow from refunds (negative amount)
            double maxMana = getMaxMana(coreProfile);
            if (newMana > maxMana) { newMana = maxMana; }
            data.setCurrentMana(newMana);
            return true;
        }
        return false;
    }

    @Override
    public boolean consumeStamina(Player player, double amount) {
        MidgardProfile coreProfile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (coreProfile == null) { return false; }
        
        CombatData data = coreProfile.getData(CombatData.class);
        if (data == null) { return false; }

        if (data.getCurrentStamina() >= amount) {
            double newStamina = data.getCurrentStamina() - amount;
            // Cap at max stamina to prevent overflow from refunds (negative amount)
            double maxStamina = getMaxStamina(coreProfile);
            if (newStamina > maxStamina) { newStamina = maxStamina; }
            data.setCurrentStamina(newStamina);
            return true;
        }
        return false;
    }

    private double getMaxMana(MidgardProfile profile) {
        CoreAttributeData attrData = profile.getOrCreateData(CoreAttributeData.class);
        AttributeInstance instance = attrData.getInstance(CombatAttributes.MAX_MANA);
        return instance != null ? instance.getValue() : 100.0;
    }

    private double getMaxStamina(MidgardProfile profile) {
        CoreAttributeData attrData = profile.getOrCreateData(CoreAttributeData.class);
        AttributeInstance instance = attrData.getInstance(CombatAttributes.MAX_STAMINA);
        return instance != null ? instance.getValue() : 100.0;
    }
}
