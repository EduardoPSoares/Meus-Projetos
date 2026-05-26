package me.ray.midgard.modules.spells.requirement;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Collections;

public class ClassRequirement implements SpellRequirement {

    private final List<String> requiredClasses;

    public ClassRequirement(List<String> requiredClasses) {
        this.requiredClasses = requiredClasses;
    }

    public ClassRequirement(String singleClass) {
        this.requiredClasses = Collections.singletonList(singleClass);
    }

    @Override
    public boolean check(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) { return false; }

        // Dynamic check for ClassData to avoid hard dependency on CharacterModule if not loaded
        try {
            Class<?> classDataClass = Class.forName("me.ray.midgard.modules.classes.ClassData");
            @SuppressWarnings("unchecked")
            Object data = profile.getData((Class<me.ray.midgard.core.profile.ModuleData>) classDataClass);
            
            if (data == null) {
                 // Try legacy/fallback location if module structure changed
                 classDataClass = Class.forName("me.ray.midgard.modules.character.ClassData");
                 @SuppressWarnings("unchecked")
                 Object fallbackData = profile.getData((Class<me.ray.midgard.core.profile.ModuleData>) classDataClass);
                 data = fallbackData;
                 if (data == null) { return false; }
            }
            
            java.lang.reflect.Method getClassName = data.getClass().getMethod("getClassName");
            String className = (String) getClassName.invoke(data);
            
            if (className == null) { return false; }

            for (String req : requiredClasses) {
                if (req.equalsIgnoreCase(className)) { return true; }
            }
            return false;
        } catch (Exception e) {
            // Module not present or class not found
            return false;
        }
    }

    @Override
    public String getFailureMessage() {
        String raw = me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("spells.requirements.class_needed");
        return raw.replace("%class%", String.join("/", requiredClasses));
    }
}
