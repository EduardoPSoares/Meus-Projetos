package me.ray.midgard.modules.mythicmobs.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.classes.ClassData;
import org.bukkit.entity.Player;

public class MidgardClassCondition implements IEntityCondition {

    private final String className;

    public MidgardClassCondition(MythicLineConfig config) {
        this.className = config.getString(new String[]{"class", "c"}, "");
    }

    @Override
    public boolean check(AbstractEntity entity) {
        return checkEntity(entity);
    }
    
    public boolean checkEntity(AbstractEntity entity) {
        if (!entity.isPlayer()) {
            return false;
        }
        Player player = (Player) BukkitAdapter.adapt(entity);
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return false;
        }
        
        ClassData classData = profile.getOrCreateData(ClassData.class);
        return className.equalsIgnoreCase(classData.getClassName());
    }
}
