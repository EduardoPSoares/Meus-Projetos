package me.ray.midgard.modules.mythicmobs.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import org.bukkit.entity.Player;

public class MidgardAttributeCondition implements IEntityCondition {

    private final String attribute;
    private final String comparator;
    private final double value;

    public MidgardAttributeCondition(MythicLineConfig config) {
        this.attribute = config.getString(new String[]{"attribute", "attr", "a"}, "strength");
        String valStr = config.getString(new String[]{"amount", "value", "v", "val"}, ">0");
        
        // Parse da string (ex: ">10", "<=50", "==20")
        try {
            if (valStr.startsWith(">=")) {
                comparator = ">=";
                value = Double.parseDouble(valStr.substring(2));
            } else if (valStr.startsWith("<=")) {
                comparator = "<=";
                value = Double.parseDouble(valStr.substring(2));
            } else if (valStr.startsWith("!=")) {
                comparator = "!=";
                value = Double.parseDouble(valStr.substring(2));
            } else if (valStr.startsWith(">")) {
                comparator = ">";
                value = Double.parseDouble(valStr.substring(1));
            } else if (valStr.startsWith("<")) {
                comparator = "<";
                value = Double.parseDouble(valStr.substring(1));
            } else if (valStr.startsWith("==")) {
                comparator = "==";
                value = Double.parseDouble(valStr.substring(2));
            } else if (valStr.startsWith("=")) {
                comparator = "==";
                value = Double.parseDouble(valStr.substring(1));
            } else {
                comparator = "==";
                value = Double.parseDouble(valStr);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato inválido na condição de atributo: " + valStr);
        }
    }

    @Override
    public boolean check(AbstractEntity entity) {
        if (!entity.isPlayer()) {
            return false;
        }
        Player player = (Player) BukkitAdapter.adapt(entity);
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) {
            return false;
        }
        
        CoreAttributeData data = profile.getOrCreateData(CoreAttributeData.class);
        AttributeInstance instance = data.getInstance(attribute);
        if (instance == null) {
            return false;
        }
        
        double attrValue = instance.getValue(); // Valor Total

        boolean result = false;
        switch (comparator) {
            case ">": result = attrValue > value; break;
            case "<": result = attrValue < value; break;
            case ">=": result = attrValue >= value; break;
            case "<=": result = attrValue <= value; break;
            case "==": result = attrValue == value; break;
            case "!=": result = attrValue != value; break;
            default: result = false;
        }

        return result;
    }
}
