package me.ray.midgard.modules.spells.requirement;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import org.bukkit.entity.Player;

public class AttributeRequirement implements SpellRequirement {

    private final String attributeId;
    private final double requiredValue;

    public AttributeRequirement(String attributeId, double requiredValue) {
        this.attributeId = attributeId;
        this.requiredValue = requiredValue;
    }

    @Override
    public boolean check(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) { return false; }

        CoreAttributeData data = profile.getData(CoreAttributeData.class);
        if (data == null) { return false; }

        AttributeInstance instance = data.getInstance(attributeId);
        return instance != null && instance.getValue() >= requiredValue;
    }

    @Override
    public String getFailureMessage() {
        String raw = me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("spells.requirements.attribute_needed");
        raw = raw.replace("%attribute%", attributeId);
        raw = raw.replace("%value%", String.valueOf(requiredValue));
        return raw;
    }
}
