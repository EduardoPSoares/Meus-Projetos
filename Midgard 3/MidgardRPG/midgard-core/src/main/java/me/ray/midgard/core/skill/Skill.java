package me.ray.midgard.core.skill;

import me.ray.midgard.core.profile.MidgardProfile;

import java.util.List;

public abstract class Skill {

    private final String id;
    private final String name;
    private final SkillType type;
    private final long cooldown; // in milliseconds
    private final int maxLevel;

    public Skill(String id, String name, SkillType type, long cooldown, int maxLevel) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.cooldown = cooldown;
        this.maxLevel = maxLevel;
    }

    /**
     * Called when the skill is cast or triggered.
     * @param caster The player casting the skill.
     * @param level The current level of the skill for the player.
     * @param context Contextual data (target, location, etc).
     * @return The result of the cast.
     */
    public abstract SkillResult cast(MidgardProfile caster, int level, SkillContext context);

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SkillType getType() {
        return type;
    }

    public long getCooldown() {
        return cooldown;
    }

    public int getMaxLevel() {
        return maxLevel;
    }
    
    public abstract List<String> getDescription(int level);
}
