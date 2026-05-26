package me.ray.midgard.core.skill;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Optional;

public class SkillContext {

    private final LivingEntity targetEntity;
    private final Location targetLocation;
    private final Entity triggerSource; // e.g. projectile

    public SkillContext(LivingEntity targetEntity, Location targetLocation, Entity triggerSource) {
        this.targetEntity = targetEntity;
        this.targetLocation = targetLocation;
        this.triggerSource = triggerSource;
    }

    public Optional<LivingEntity> getTargetEntity() {
        return Optional.ofNullable(targetEntity);
    }

    public Optional<Location> getTargetLocation() {
        return Optional.ofNullable(targetLocation);
    }
    
    public Optional<Entity> getTriggerSource() {
        return Optional.ofNullable(triggerSource);
    }
    
    public static SkillContext empty() {
        return new SkillContext(null, null, null);
    }
    
    public static SkillContext of(LivingEntity target) {
        return new SkillContext(target, target.getLocation(), null);
    }
    
    public static SkillContext of(Location location) {
        return new SkillContext(null, location, null);
    }
}
