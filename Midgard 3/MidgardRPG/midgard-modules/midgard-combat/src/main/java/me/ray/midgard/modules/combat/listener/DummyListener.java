package me.ray.midgard.modules.combat.listener;

import me.ray.midgard.core.utils.Task;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class DummyListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (!entity.getScoreboardTags().contains("midgard_dummy")) {
            return;
        }

        org.bukkit.attribute.AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) {
            return;
        }
        double max = maxHealthAttr.getValue();

        // Prevent lethal damage — zero it out so damage indicators still work
        if (entity.getHealth() - event.getFinalDamage() <= 0) {
            event.setDamage(0);
            entity.setHealth(max);
        }

        // Reset health next tick so the dummy is always full
        Task.syncLater(entity, () -> {
            if (!entity.isDead()) {
                entity.setHealth(max);
            }
        }, 1L);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
         if (event.getEntity().getScoreboardTags().contains("midgard_dummy")) {
             event.getDrops().clear();
             event.setDroppedExp(0);
         }
    }
}
