package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class PotionEffectTrait implements RaceTrait {

    @Override
    public String getId() {
        return "potion_effect";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        Object effectObj = config.get("effect");
        if (!(effectObj instanceof String effectName)) { return; }

        PotionEffectType type = org.bukkit.Registry.POTION_EFFECT_TYPE.get(org.bukkit.NamespacedKey.minecraft(effectName.toLowerCase()));
        if (type == null) { return; }

        int amplifier = 0;
        if (config.containsKey("amplifier")) {
            Object ampObj = config.get("amplifier");
            if (ampObj instanceof Number) {
                amplifier = ((Number) ampObj).intValue();
            }
        }

        int duration = 20 * 10; // Default 10 seconds if not specified (for passive renewal)
        if (config.containsKey("duration")) {
            Object durObj = config.get("duration");
            if (durObj instanceof Number) {
                duration = ((Number) durObj).intValue();
            }
        }
        
        // Handle chance if present
        if (config.containsKey("chance")) {
             Object chanceObj = config.get("chance");
             double chance = 1.0;
             if (chanceObj instanceof Number) {
                 chance = ((Number) chanceObj).doubleValue();
             }
             if (ThreadLocalRandom.current().nextDouble() > chance) { return; }
        }

        if (trigger == TraitTrigger.ON_REMOVE || trigger == TraitTrigger.ON_QUIT) {
            player.removePotionEffect(type);
            return;
        }

        player.addPotionEffect(new PotionEffect(type, duration, amplifier, false, false, true));
    }
}
