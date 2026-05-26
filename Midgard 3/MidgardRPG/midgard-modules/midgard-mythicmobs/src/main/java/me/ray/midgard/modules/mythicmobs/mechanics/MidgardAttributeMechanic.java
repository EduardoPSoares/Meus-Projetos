package me.ray.midgard.modules.mythicmobs.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.integration.MythicMobsIntegration;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class MidgardAttributeMechanic implements ITargetedEntitySkill {

    private final String attribute;
    private final PlaceholderDouble amount;
    private final String mode; // SET, ADD, SUBTRACT

    public MidgardAttributeMechanic(MythicLineConfig config) {
        this.attribute = config.getString(new String[]{"attribute", "attr", "a", "id"}, "defense").toLowerCase();
        this.amount = config.getPlaceholderDouble(new String[]{"amount", "value", "v", "val"}, 0.0);
        this.mode = config.getString(new String[]{"mode", "m"}, "SET").toUpperCase();
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        if (!target.isLiving()) {
            return SkillResult.INVALID_TARGET;
        }
        
        LivingEntity entity = (LivingEntity) BukkitAdapter.adapt(target);
        String tagPrefix = "midgard.attr." + attribute + ".";
        
        // Calculate current value
        double current = 0;
        if (!mode.equals("SET")) {
            current = MythicMobsIntegration.getAttributeValue(entity, attribute);
        }
        
        double value = amount.get(data, target);
        double finalValue = current;
        
        switch (mode) {
            case "SET":
                finalValue = value;
                break;
            case "ADD":
            case "GIVE":
                finalValue += value;
                break;
            case "SUBTRACT":
            case "TAKE":
            case "REMOVE":
                finalValue -= value;
                break;
        }
        
        // Remove old tags for this attribute
        Set<String> toRemove = new HashSet<>();
        for (String tag : entity.getScoreboardTags()) {
            if (tag.toLowerCase().startsWith(tagPrefix)) {
                toRemove.add(tag);
            }
        }
        for (String tag : toRemove) {
            entity.removeScoreboardTag(tag);
        }
        
        // Add new tag
        entity.addScoreboardTag(tagPrefix + finalValue);
        
        // System.out.println("MidgardRPG: Applied attribute " + attribute + " = " + finalValue + " to " + entity.getName()); // Debug
        
        return SkillResult.SUCCESS;
    }
}
