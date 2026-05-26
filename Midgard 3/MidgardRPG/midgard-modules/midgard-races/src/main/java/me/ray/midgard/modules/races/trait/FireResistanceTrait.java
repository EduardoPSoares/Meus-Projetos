package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;

/**
 * Trait que reduz ou anula danos de fogo/lava.
 * Config:
 *   reduction: 0.5 (50% redução) ou 1.0 (imunidade total)
 *   remove_fire: true (remove ticks de fogo)
 */
public class FireResistanceTrait implements RaceTrait {

    @Override
    public String getId() {
        return "fire_resistance";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger != TraitTrigger.ON_DAMAGE) { return; }

        if (!(context.get("event") instanceof EntityDamageEvent event)) { return; }

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.FIRE
                && cause != EntityDamageEvent.DamageCause.FIRE_TICK
                && cause != EntityDamageEvent.DamageCause.LAVA
                && cause != EntityDamageEvent.DamageCause.HOT_FLOOR) {
            return;
        }

        double reduction = 1.0;
        if (config.get("reduction") instanceof Number n) {
            reduction = Math.clamp(n.doubleValue(), 0.0, 1.0);
        }

        double newDamage = event.getDamage() * (1.0 - reduction);
        event.setDamage(newDamage);

        boolean removeFire = true;
        if (config.get("remove_fire") instanceof Boolean b) {
            removeFire = b;
        }

        if (removeFire && reduction >= 1.0) {
            player.setFireTicks(0);
        }
    }
}
