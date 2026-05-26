package me.ray.midgard.modules.races.trait;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class DietRestrictionTrait implements RaceTrait {

    @Override
    public String getId() {
        return "diet_restriction";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger != TraitTrigger.ON_EAT) { return; }

        Object eventObj = context.get("event");
        if (!(eventObj instanceof PlayerItemConsumeEvent event)) { return; }
        ItemStack item = event.getItem();
        
        List<?> allowedFoods = null;
        Object foodObj = config.get("food_types");
        if (foodObj instanceof List<?> list) {
            allowedFoods = list;
        } else {
            Object altObj = config.get("allowed-foods");
            if (altObj instanceof List<?> list) {
                allowedFoods = list;
            }
        }
        if (allowedFoods == null) { return; }
        
        boolean allowed = false;
        for (Object obj : allowedFoods) {
            if (item.getType().name().equals(obj.toString())) {
                allowed = true;
                break;
            }
        }
        
        if (!allowed) {
            event.setCancelled(true);
            MessageUtils.send(player, me.ray.midgard.modules.races.RacesModule.getInstance()
                    .getMessage("gui.errors.diet_restriction"));
        }
    }
}
