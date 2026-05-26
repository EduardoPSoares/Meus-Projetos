package me.ray.midgard.modules.mythicmobs.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.CombatData;
import org.bukkit.entity.Player;

public class MidgardGiveExpMechanic implements ITargetedEntitySkill {

    private final double amount;

    public MidgardGiveExpMechanic(MythicLineConfig config) {
        this.amount = config.getDouble(new String[]{"amount", "a"}, 0);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        if (!target.isPlayer()) {
            return SkillResult.INVALID_TARGET;
        }
        
        Player player = (Player) BukkitAdapter.adapt(target);
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        
        if (profile != null) {
            CombatData combatData = profile.getOrCreateData(CombatData.class);
            combatData.addExperience(amount);
            return SkillResult.SUCCESS;
        }
        
        return SkillResult.CONDITION_FAILED;
    }
}
