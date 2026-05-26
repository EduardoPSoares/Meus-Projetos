package me.ray.midgard.modules.mythicmobs.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.CombatData;
import org.bukkit.entity.Player;

public class MidgardLevelCondition implements IEntityCondition {

    private final String levelRange;

    public MidgardLevelCondition(MythicLineConfig config) {
        this.levelRange = config.getString(new String[]{"level", "l"}, ">0");
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
        
        CombatData combatData = profile.getOrCreateData(CombatData.class);
        int level = combatData.getLevel();
        
        // Parser robusto para >X, >=X, <X, <=X, =X, X-Y
        try {
            if (levelRange.startsWith(">=")) {
                int val = Integer.parseInt(levelRange.substring(2).trim());
                return level >= val;
            } else if (levelRange.startsWith("<=")) {
                int val = Integer.parseInt(levelRange.substring(2).trim());
                return level <= val;
            } else if (levelRange.startsWith(">")) {
                int val = Integer.parseInt(levelRange.substring(1).trim());
                return level > val;
            } else if (levelRange.startsWith("<")) {
                int val = Integer.parseInt(levelRange.substring(1).trim());
                return level < val;
            } else if (levelRange.contains("-")) {
                String[] parts = levelRange.split("-", 2);
                if (parts.length < 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                    return false;
                }
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                return level >= min && level <= max;
            } else {
                int val = Integer.parseInt(levelRange.replace("=", "").trim());
                return level == val;
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }
}
