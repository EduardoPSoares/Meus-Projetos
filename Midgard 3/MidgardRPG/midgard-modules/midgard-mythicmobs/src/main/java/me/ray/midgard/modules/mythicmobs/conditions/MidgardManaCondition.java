package me.ray.midgard.modules.mythicmobs.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.CombatData;
import org.bukkit.entity.Player;

public class MidgardManaCondition implements IEntityCondition {

    private final String valueRange;
    private final boolean percentage;

    public MidgardManaCondition(MythicLineConfig config) {
        this.valueRange = config.getString(new String[]{"amount", "a", "value", "v"}, ">0");
        this.percentage = config.getBoolean(new String[]{"percentage", "p", "percent"}, false);
    }

    @Override
    public boolean check(AbstractEntity entity) {
        if (!entity.isPlayer()) {
            return false;
        }
        Player player = (Player) BukkitAdapter.adapt(entity);
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        
        if (profile == null) {
            return false;
        }
        
        CombatData combatData = profile.getOrCreateData(CombatData.class);
        double current = combatData.getCurrentMana();
        
        if (percentage) {
            CoreAttributeData attrData = profile.getOrCreateData(CoreAttributeData.class);
            AttributeInstance maxAttr = attrData.getInstance(CombatAttributes.MAX_MANA);
            double max = maxAttr != null ? maxAttr.getValue() : 100;
            if (max == 0) {
                return false;
            }
            current = (current / max) * 100;
        }
        
        return compare(current, valueRange);
    }
    
    private boolean compare(double value, String range) {
        try {
            if (range.startsWith(">=")) {
                double val = Double.parseDouble(range.substring(2));
                return value >= val;
            } else if (range.startsWith("<=")) {
                double val = Double.parseDouble(range.substring(2));
                return value <= val;
            } else if (range.startsWith(">")) {
                double val = Double.parseDouble(range.substring(1));
                return value > val;
            } else if (range.startsWith("<")) {
                double val = Double.parseDouble(range.substring(1));
                return value < val;
            } else if (range.startsWith("=")) {
                double val = Double.parseDouble(range.substring(1));
                return Math.abs(value - val) < 0.01;
            } else {
                double val = Double.parseDouble(range);
                return Math.abs(value - val) < 0.01;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
