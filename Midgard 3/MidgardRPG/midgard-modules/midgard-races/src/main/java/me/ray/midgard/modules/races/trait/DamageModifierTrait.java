package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class DamageModifierTrait implements RaceTrait {

    @Override
    public String getId() {
        return "damage_modifier";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger != TraitTrigger.ON_DAMAGE && trigger != TraitTrigger.ON_DEFEND && trigger != TraitTrigger.ON_ATTACK) { return; }

        Object eventObj = context.get("event");
        if (!(eventObj instanceof EntityDamageEvent event)) { return; }

        // 1. Check Causes Filter (FALL, FIRE, etc.)
        if (config.containsKey("causes")) {
            Object causesObj = config.get("causes");
            if (causesObj instanceof List) {
                List<?> causes = (List<?>) causesObj;
                boolean match = false;
                for (Object causeObj : causes) {
                    if (event.getCause().name().equalsIgnoreCase(causeObj.toString())) {
                        match = true;
                        break;
                    }
                }
                if (!match) { return; }
            }
        }

        // Contextual checks for Attack/Defend
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageByEntityEvent = (EntityDamageByEntityEvent) event;
            
            // 2. Weapon Filter (Only for ON_ATTACK)
            if (trigger == TraitTrigger.ON_ATTACK && config.containsKey("weapons")) {
                Object weaponsObj = config.get("weapons");
                if (weaponsObj instanceof List) {
                    List<?> weapons = (List<?>) weaponsObj;
                    ItemStack item = player.getInventory().getItemInMainHand();
                    String itemType = item.getType().name();
                    
                    boolean match = false;
                    for (Object weaponObj : weapons) {
                        String w = weaponObj.toString().toUpperCase();
                        // Support partial match like "_SWORD" or exact "DIAMOND_AXE"
                        if (itemType.contains(w) || itemType.equals(w)) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) { return; }
                }
            }

            // 3. Entity Type Filter (Target for Attack, Attacker for Defend)
            if (config.containsKey("entities")) {
                Object entitiesObj = config.get("entities");
                if (entitiesObj instanceof List) {
                    List<?> entities = (List<?>) entitiesObj;
                    Entity targetEntity = null;

                    if (trigger == TraitTrigger.ON_ATTACK) {
                        targetEntity = damageByEntityEvent.getEntity(); // We are attacking this
                    } else if (trigger == TraitTrigger.ON_DEFEND) {
                        targetEntity = damageByEntityEvent.getDamager(); // We are defending against this
                    }

                    if (targetEntity != null) {
                        String typeName = targetEntity.getType().name();
                        boolean match = false;
                        for (Object entityObj : entities) {
                            if (typeName.equalsIgnoreCase(entityObj.toString())) {
                                match = true;
                                break;
                            }
                        }
                        if (!match) { return; }
                    }
                }
            }
        } else {
            // If it's not EntityDamageByEntityEvent, we cannot check weapons or entities
            // So if config requires them, we must abort
            if (config.containsKey("weapons") || config.containsKey("entities")) {
                return;
            }
        }

        double value = 0.0;
        if (config.containsKey("value")) {
            Object val = config.get("value");
            if (val instanceof Number) {
                value = ((Number) val).doubleValue();
            }
        }

        Object opObj = config.getOrDefault("operation", "ADD");
        String operation = opObj instanceof String s ? s : "ADD";

        double currentDamage = event.getDamage();
        double newDamage = currentDamage;

        if ("MULTIPLY".equalsIgnoreCase(operation)) {
            newDamage = currentDamage * value;
        } else { // ADD
            newDamage = currentDamage + value;
        }

        if (newDamage < 0) { newDamage = 0; }
        event.setDamage(newDamage);
    }
}
