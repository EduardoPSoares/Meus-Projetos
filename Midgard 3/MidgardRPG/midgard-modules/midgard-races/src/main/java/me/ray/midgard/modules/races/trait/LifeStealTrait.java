package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class LifeStealTrait implements RaceTrait {

    @Override
    public String getId() {
        return "life_steal";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger != TraitTrigger.ON_ATTACK) { return; }
        if (player.isDead()) { return; }
        
        double chance = 0.25;
        Object chanceObj = config.get("chance");
        if (chanceObj instanceof Number) { chance = ((Number) chanceObj).doubleValue(); }
        
        if (ThreadLocalRandom.current().nextDouble() > chance) { return; }
        
        double value = 1.0;
        Object valObj = config.get("value");
        if (valObj instanceof Number) { value = ((Number) valObj).doubleValue(); }
        
        org.bukkit.attribute.AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) { return; }
        double maxHealth = maxHealthAttr.getValue();
        double newHealth = player.getHealth() + value;
        if (newHealth > maxHealth) { newHealth = maxHealth; }
        
        player.setHealth(newHealth);
        
        // Optional: Particles/Sound
        // player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITCH_DRINK, 0.5f, 1.0f);
    }
}
