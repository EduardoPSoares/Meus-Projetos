package me.ray.midgard.core.skill;

import me.ray.midgard.core.registry.Registry;

public class SkillRegistry extends Registry<String, Skill> {

    private static final SkillRegistry INSTANCE = new SkillRegistry();

    public static SkillRegistry getInstance() {
        return INSTANCE;
    }

    private SkillRegistry() {}
    
    public java.util.Optional<Skill> getSkill(String id) {
        return get(id);
    }
}
