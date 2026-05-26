package me.ray.midgard.modules.combat;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.placeholder.PlaceholderRegistry;
import me.ray.midgard.core.profile.MidgardProfile;

import java.text.DecimalFormat;

public class CombatPlaceholders {

    private static final DecimalFormat DF = new DecimalFormat("#.##");

    public static void register() {
        PlaceholderRegistry registry = MidgardCore.getPlaceholderRegistry();
        if (registry == null) {
            return;
        }

        // %midgard_mana%
        registry.register("mana", (player, params) -> {
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
            if (profile == null) {
                return "0";
            }
            CombatData data = profile.getOrCreateData(CombatData.class);
            return DF.format(data.getCurrentMana());
        });

        // %midgard_max_mana%
        registry.register("max_mana", (player, params) -> {
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
            if (profile == null) {
                return "0";
            }
            CoreAttributeData data = profile.getOrCreateData(CoreAttributeData.class);
            AttributeInstance attr = data.getInstance(CombatAttributes.MAX_MANA);
            return DF.format(attr != null ? attr.getValue() : 100);
        });

        // %midgard_stamina%
        registry.register("stamina", (player, params) -> {
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
            if (profile == null) {
                return "0";
            }
            CombatData data = profile.getOrCreateData(CombatData.class);
            return DF.format(data.getCurrentStamina());
        });

        // %midgard_max_stamina%
        registry.register("max_stamina", (player, params) -> {
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
            if (profile == null) {
                return "0";
            }
            CoreAttributeData data = profile.getOrCreateData(CoreAttributeData.class);
            AttributeInstance attr = data.getInstance(CombatAttributes.MAX_STAMINA);
            return DF.format(attr != null ? attr.getValue() : 100);
        });

        // %midgard_health%
        registry.register("health", (player, params) -> {
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
            if (profile == null) {
                return "0";
            }
            CombatData data = profile.getOrCreateData(CombatData.class);
            return DF.format(data.getCurrentHealth());
        });

        // %midgard_max_health%
        registry.register("max_health", (player, params) -> {
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
            if (profile == null) {
                return "0";
            }
            CoreAttributeData data = profile.getOrCreateData(CoreAttributeData.class);
            AttributeInstance attr = data.getInstance(CombatAttributes.MAX_HEALTH);
            return DF.format(attr != null ? attr.getValue() : 100);
        });
        
        // %midgard_level%
        registry.register("level", (player, params) -> {
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
            if (profile == null) {
                return "1";
            }
            CombatData data = profile.getOrCreateData(CombatData.class);
            return String.valueOf(data.getLevel());
        });
        
        // %midgard_exp%
        registry.register("exp", (player, params) -> {
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
            if (profile == null) {
                return "0";
            }
            CombatData data = profile.getOrCreateData(CombatData.class);
            return DF.format(data.getExperience());
        });

        // %midgard_attribute_<id>%
        registry.register("attribute_", (player, params) -> {
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
            if (profile == null) {
                return "0";
            }
            CoreAttributeData data = profile.getOrCreateData(CoreAttributeData.class);
            AttributeInstance attr = data.getInstance(params);
            return DF.format(attr != null ? attr.getValue() : 0);
        });
    }
}
