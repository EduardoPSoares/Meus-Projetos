package me.ray.midgard.modules.mythicmobs.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.CombatData;
import org.bukkit.entity.Player;

public class MidgardManaMechanic implements ITargetedEntitySkill {

    private final PlaceholderDouble amount;
    private final String mode; // GIVE, TAKE, SET

    public MidgardManaMechanic(MythicLineConfig config) {
        this.amount = config.getPlaceholderDouble(new String[]{"amount", "a"}, 10.0);
        this.mode = config.getString(new String[]{"mode", "m"}, "GIVE").toUpperCase();
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        if (!target.isPlayer()) {
            return SkillResult.INVALID_TARGET;
        }
        
        Player player = (Player) BukkitAdapter.adapt(target);
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        
        if (profile == null) {
            return SkillResult.INVALID_TARGET;
        }
        
        CombatData combatData = profile.getOrCreateData(CombatData.class);
        CoreAttributeData attributeData = profile.getOrCreateData(CoreAttributeData.class);
        
        AttributeInstance maxManaAttr = attributeData.getInstance(CombatAttributes.MAX_MANA);
        double maxMana = maxManaAttr != null ? maxManaAttr.getValue() : 100;
        double currentMana = combatData.getCurrentMana();
        double value = amount.get(data, target);
        
        double newMana = currentMana;
        
        switch (mode) {
            case "GIVE":
            case "ADD":
                newMana += value;
                break;
            case "TAKE":
            case "SUBTRACT":
            case "REMOVE":
                newMana -= value;
                break;
            case "SET":
                newMana = value;
                break;
        }
        
        // Clamp values
        if (newMana > maxMana) {
            newMana = maxMana;
        }
        if (newMana < 0) {
            newMana = 0;
        }
        
        combatData.setCurrentMana(newMana);
        
        return SkillResult.SUCCESS;
    }
}
