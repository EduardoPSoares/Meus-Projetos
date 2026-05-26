package me.ray.midgard.modules.mythicmobs.targeters;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.targeters.IEntityTargeter;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.classes.ClassData;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MidgardClassTargeter implements IEntityTargeter {

    private final String className;
    private final double radius;

    public MidgardClassTargeter(MythicLineConfig config) {
        this.className = config.getString(new String[]{"class", "c"}, "");
        this.radius = config.getDouble(new String[]{"radius", "r"}, 30);
    }

    @Override
    public Collection<AbstractEntity> getEntities(SkillMetadata data) {
        Set<AbstractEntity> targets = new HashSet<>();
        Entity caster = BukkitAdapter.adapt(data.getCaster().getEntity());
        
        for (Entity entity : caster.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player player) {
                MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
                if (profile != null) {
                    ClassData classData = profile.getOrCreateData(ClassData.class);
                    if (className.equalsIgnoreCase(classData.getClassName())) {
                        // Explicit cast to help compiler if needed
                        targets.add(BukkitAdapter.adapt((Entity) player));
                    }
                }
            }
        }
        return targets;
    }
}
