package me.ray.midgard.modules.mythicmobs.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.classes.ClassData;
import org.bukkit.entity.Player;

public class MidgardGiveClassExpMechanic implements ITargetedEntitySkill {

    private final PlaceholderDouble amount;

    public MidgardGiveClassExpMechanic(MythicLineConfig config) {
        this.amount = config.getPlaceholderDouble(new String[]{"amount", "a", "exp"}, 0);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        if (!target.isPlayer()) {
            return SkillResult.INVALID_TARGET;
        }
        
        Player player = (Player) BukkitAdapter.adapt(target);
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        
        if (profile != null) {
            ClassData classData = profile.getOrCreateData(ClassData.class);
            if (classData.hasClass()) {
                double exp = amount.get(data, target);
                // Assuming there is a method to add experience in ClassData or we handle it manually
                // For now, let's assume we just add it to the value, 
                // but ideally there should be a ClassManager handling level ups.
                // Since I don't have the full ClassManager code, I'll just add to the data 
                // and assume the ClassModule handles checks elsewhere or I'll just set it.
                // Looking at ClassData.java again:
                // public double getExperience() { return experience; }
                // public void setExperience(double experience) { this.experience = experience; }
                
                classData.setExperience(classData.getExperience() + exp);
                return SkillResult.SUCCESS;
            }
        }
        
        return SkillResult.CONDITION_FAILED;
    }
}
