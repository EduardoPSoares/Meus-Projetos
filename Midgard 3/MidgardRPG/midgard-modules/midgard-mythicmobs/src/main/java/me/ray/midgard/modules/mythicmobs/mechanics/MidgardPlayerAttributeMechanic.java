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
import me.ray.midgard.core.attribute.AttributeModifier;
import me.ray.midgard.core.attribute.AttributeOperation;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.item.manager.AttributeUpdater;
import org.bukkit.entity.Player;
import me.ray.midgard.core.utils.Task;

public class MidgardPlayerAttributeMechanic implements ITargetedEntitySkill {

    private final String attribute;
    private final PlaceholderDouble amount;
    private final String mode; // SET, ADD, SUBTRACT
    private final PlaceholderDouble duration;

    public MidgardPlayerAttributeMechanic(MythicLineConfig config) {
        this.attribute = config.getString(new String[]{"attribute", "attr", "a", "id"}, "defense").toLowerCase();
        this.amount = config.getPlaceholderDouble(new String[]{"amount", "value", "v", "val"}, 0.0);
        this.mode = config.getString(new String[]{"mode", "m"}, "SET").toUpperCase();
        this.duration = config.getPlaceholderDouble(new String[]{"duration", "d", "time"}, 0.0);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        if (!target.isPlayer()) {
            return SkillResult.INVALID_TARGET;
        }

        Player player = (Player) BukkitAdapter.adapt(target);
        
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) {
            return SkillResult.CONDITION_FAILED;
        }

        CoreAttributeData attrData = profile.getOrCreateData(CoreAttributeData.class);
        AttributeInstance instance = attrData.getInstance(attribute);
        
        if (instance == null) {
             return SkillResult.CONDITION_FAILED;
        }

        double value = amount.get(data, target);
        int durationTicks = (int) duration.get(data, target);

        if (durationTicks > 0) {
            double modValue = 0;
            switch (mode) {
                case "SET":
                    modValue = value - instance.getValue();
                    break;
                case "ADD":
                case "GIVE":
                    modValue = value;
                    break;
                case "SUBTRACT":
                case "TAKE":
                case "REMOVE":
                    modValue = -value;
                    break;
            }

            if (modValue != 0) {
                String modId = "MythicTemp_" + attribute + "_" + System.nanoTime();
                AttributeModifier modifier = new AttributeModifier(modId, modValue, AttributeOperation.ADD_NUMBER);
                
                instance.addModifier(modifier);
                AttributeUpdater.updateAttributes(player);

                // Schedule Removal
                Task.syncLater(player, () -> {
                    if (player.isOnline()) {
                        instance.removeModifier(modifier);
                        AttributeUpdater.updateAttributes(player);
                    }
                }, durationTicks);
            }
        } else {
            switch (mode) {
                case "SET":
                    instance.setBaseValue(value);
                    break;
                case "ADD":
                case "GIVE":
                    instance.setBaseValue(instance.getBaseValue() + value);
                    break;
                case "SUBTRACT":
                case "TAKE":
                case "REMOVE":
                    instance.setBaseValue(instance.getBaseValue() - value);
                    break;
            }
            AttributeUpdater.updateAttributes(player);
        }
        
        return SkillResult.SUCCESS;
    }
}
