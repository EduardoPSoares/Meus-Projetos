package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.util.Map;

public class RegenTrait implements RaceTrait {

    @Override
    public String getId() {
        return "regen";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger != TraitTrigger.ON_REGEN) { return; }

        Object eventObj = context.get("event");
        if (!(eventObj instanceof EntityRegainHealthEvent event)) { return; }

        double value = 0.0;
        if (config.containsKey("value")) {
            Object val = config.get("value");
            if (val instanceof Number) {
                value = ((Number) val).doubleValue();
            }
        }

        Object opObj = config.getOrDefault("operation", "ADD");
        String operation = opObj instanceof String s ? s : "ADD";

        double currentAmount = event.getAmount();
        double newAmount = currentAmount;

        if ("MULTIPLY".equalsIgnoreCase(operation)) {
            newAmount = currentAmount * value;
        } else { // ADD
            newAmount = currentAmount + value;
        }
        
        // Prevent negative regen (damage?) unless intentional, but EntityRegainHealthEvent usually handles positive.
        // If amount is negative, it might be weird. Let's clamp to 0 minimum unless we want to block regen.
        if (newAmount < 0) { newAmount = 0; }
        
        event.setAmount(newAmount);
    }
}
