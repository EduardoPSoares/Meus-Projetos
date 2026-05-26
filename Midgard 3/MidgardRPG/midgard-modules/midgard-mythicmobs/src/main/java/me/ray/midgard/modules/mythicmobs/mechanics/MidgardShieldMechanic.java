package me.ray.midgard.modules.mythicmobs.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.bukkit.BukkitAdapter;
import org.bukkit.entity.LivingEntity;

public class MidgardShieldMechanic implements ITargetedEntitySkill {

    private final PlaceholderDouble amount;
    private final boolean add;

    public MidgardShieldMechanic(MythicLineConfig config) {
        this.amount = config.getPlaceholderDouble(new String[]{"amount", "a", "value", "v"}, 0.0);
        this.add = config.getBoolean(new String[]{"add", "stack"}, false);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        if (!target.isLiving()) {
            return SkillResult.INVALID_TARGET;
        }
        
        LivingEntity entity = (LivingEntity) BukkitAdapter.adapt(target);
        double value = amount.get(data, target);
        
        // Absorption in Bukkit is handled via setAbsorptionAmount
        // 2 Absorption = 1 Heart (Shield)
        
        double current = entity.getAbsorptionAmount();
        
        double newAmount = add ? (current + value) : value;
        if (newAmount < 0) {
            newAmount = 0;
        }
        entity.setAbsorptionAmount(newAmount);
        
        return SkillResult.SUCCESS;
    }
}
