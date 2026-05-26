package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/**
 * Trait que permite respirar debaixo d'água e aumenta velocidade de nado.
 * Config:
 *   swim_speed: 0.04 (bonus de velocidade ao nadar, default attribute é ~0.02)
 *   water_breathing: true (dar water breathing permanente)
 */
public class WaterBreathingTrait implements RaceTrait {

    private static final NamespacedKey SWIM_SPEED_KEY = new NamespacedKey("midgard", "race_swim_speed");

    @Override
    public String getId() {
        return "water_breathing";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger == TraitTrigger.ON_REMOVE || trigger == TraitTrigger.ON_QUIT) {
            player.removePotionEffect(PotionEffectType.WATER_BREATHING);
            removeSwimSpeed(player);
            return;
        }

        if (trigger != TraitTrigger.PASSIVE_TICK) { return; }

        boolean waterBreathing = true;
        if (config.get("water_breathing") instanceof Boolean b) {
            waterBreathing = b;
        }

        if (waterBreathing) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 60, 0, false, false, true));
        }

        if (player.isInWater()) {
            double swimSpeed = 0.04;
            if (config.get("swim_speed") instanceof Number n) {
                swimSpeed = n.doubleValue();
            }
            applySwimSpeed(player, swimSpeed);
        } else {
            removeSwimSpeed(player);
        }
    }

    private void applySwimSpeed(Player player, double bonus) {
        AttributeInstance attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) { return; }

        // Remove old modifier if exists, then re-add
        attr.getModifiers().stream()
                .filter(m -> m.getKey().equals(SWIM_SPEED_KEY))
                .forEach(attr::removeModifier);

        attr.addModifier(new AttributeModifier(SWIM_SPEED_KEY, bonus, AttributeModifier.Operation.ADD_NUMBER));
    }

    private void removeSwimSpeed(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) { return; }

        attr.getModifiers().stream()
                .filter(m -> m.getKey().equals(SWIM_SPEED_KEY))
                .forEach(attr::removeModifier);
    }
}
