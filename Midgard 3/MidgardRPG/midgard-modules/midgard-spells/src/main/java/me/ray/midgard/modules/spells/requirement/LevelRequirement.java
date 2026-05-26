package me.ray.midgard.modules.spells.requirement;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import org.bukkit.entity.Player;

public class LevelRequirement implements SpellRequirement {

    private final int requiredLevel;

    public LevelRequirement(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    @Override
    public boolean check(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) { return false; }

        // Check class level from ClassData (primary - for class skills)
        try {
            Class<?> classDataClass = Class.forName("me.ray.midgard.modules.classes.ClassData");
            @SuppressWarnings("unchecked")
            Object data = profile.getData((Class<me.ray.midgard.core.profile.ModuleData>) classDataClass);
            
            if (data != null) {
                java.lang.reflect.Method getLevel = data.getClass().getMethod("getLevel");
                int classLevel = (Integer) getLevel.invoke(data);
                return classLevel >= requiredLevel;
            }
        } catch (Exception ignored) {
            // ClassData not available, fall through to combat level
        }

        // Fallback to combat level if ClassData not available
        try {
            Class<?> combatDataClass = Class.forName("me.ray.midgard.modules.combat.CombatData");
            @SuppressWarnings("unchecked")
            Object data = profile.getData((Class<me.ray.midgard.core.profile.ModuleData>) combatDataClass);
            if (data != null) {
                java.lang.reflect.Method getLevel = data.getClass().getMethod("getLevel");
                int combatLevel = (Integer) getLevel.invoke(data);
                return combatLevel >= requiredLevel;
            }
        } catch (Exception ignored) {
            // CombatData not available
        }
        
        return false;
    }

    @Override
    public String getFailureMessage() {
        String raw = me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("spells.requirements.level_needed");
        return raw.replace("%level%", String.valueOf(requiredLevel));
    }
}
