package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;

import java.util.Map;

public class ExpBoostTrait implements RaceTrait {

    @Override
    public String getId() {
        return "exp_boost";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger != TraitTrigger.ON_EXP_GAIN) { return; }

        Object eventObj = context.get("event");
        if (!(eventObj instanceof PlayerExpChangeEvent event)) { return; }

        double value = 1.0;
        if (config.containsKey("multiplier")) {
            Object val = config.get("multiplier");
            if (val instanceof Number) {
                value = ((Number) val).doubleValue();
            }
        } else if (config.containsKey("value")) {
            Object val = config.get("value");
            if (val instanceof Number) {
                value = 1.0 + (((Number) val).doubleValue() / 100.0);
            }
        }

        double multiplier = value;
        
        int currentExp = event.getAmount();
        int newExp = (int) Math.round(currentExp * multiplier);
        
        event.setAmount(newExp);
    }
}
