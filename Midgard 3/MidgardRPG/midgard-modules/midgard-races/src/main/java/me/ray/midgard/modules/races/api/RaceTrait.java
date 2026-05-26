package me.ray.midgard.modules.races.api;

import org.bukkit.entity.Player;
import java.util.Map;

public interface RaceTrait {
    /**
     * Unique identifier for the trait (e.g., "night_vision", "strength_boost").
     */
    String getId();

    /**
     * Called when a trigger activates this trait.
     * @param player The player invoking the trait.
     * @param trigger The trigger type (e.g., ON_ATTACK).
     * @param context Context data (e.g., damage amount, target entity) - mutable if needed.
     * @param config Configuration specific to this usage of the trait (e.g., power=2, chance=0.5).
     */
    void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config);
}
