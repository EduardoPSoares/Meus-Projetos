package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/**
 * Trait que dá visão noturna permanente.
 * Config:
 *   (nenhuma config necessária)
 */
public class NightVisionTrait implements RaceTrait {

    @Override
    public String getId() {
        return "night_vision";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger == TraitTrigger.ON_REMOVE || trigger == TraitTrigger.ON_QUIT) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            return;
        }

        if (trigger != TraitTrigger.PASSIVE_TICK) { return; }

        // Reaplica a cada tick para manter permanente (duration 400+ evita flickering do Night Vision)
        if (!player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, false, false, true));
        }
    }
}
