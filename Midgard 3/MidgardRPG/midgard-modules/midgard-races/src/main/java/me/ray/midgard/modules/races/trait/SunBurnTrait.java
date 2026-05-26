package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;

public class SunBurnTrait implements RaceTrait {

    @Override
    public String getId() {
        return "sun_burn";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger != TraitTrigger.PASSIVE_TICK) { return; }
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) { return; }

        long time = player.getWorld().getTime();
        boolean isDay = me.ray.midgard.modules.races.RacesModule.isDayTime(time);
        
        if (isDay && !player.getWorld().hasStorm()) {
            Block block = player.getLocation().getBlock();
            if (block.getLightFromSky() == 15) {
                // Check for helmet
                if (player.getInventory().getHelmet() != null) { return; }
                
                double damage = 1.0;
                if (config.get("damage") instanceof Number n) {
                    damage = n.doubleValue();
                }
                
                player.damage(damage);
                player.setFireTicks(60);
            }
        }
    }
}
