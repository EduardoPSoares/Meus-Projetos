package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;

/**
 * Trait que reduz ou anula dano de queda.
 * Config:
 *   reduction: 0.5 (50% redução) ou 1.0 (imunidade total)
 */
public class FallResistanceTrait implements RaceTrait {

    @Override
    public String getId() {
        return "fall_resistance";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger != TraitTrigger.ON_DAMAGE) { return; }

        if (!(context.get("event") instanceof EntityDamageEvent event)) { return; }
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) { return; }

        double reduction = 1.0;
        if (config.get("reduction") instanceof Number n) {
            reduction = Math.clamp(n.doubleValue(), 0.0, 1.0);
        }

        double newDamage = event.getDamage() * (1.0 - reduction);
        event.setDamage(newDamage);
    }
}
