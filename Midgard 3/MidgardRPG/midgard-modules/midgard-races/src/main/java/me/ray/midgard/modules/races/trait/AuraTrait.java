package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;

import java.util.Map;

/**
 * Trait que aplica efeitos de poção em entidades próximas (aura).
 * Modo "buff" aplica em aliados (players); modo "debuff" aplica em inimigos (mobs/jogadores hostis).
 * Config:
 *   radius: 5.0 (raio em blocos)
 *   effect: "speed" (efeito aplicado)
 *   amplifier: 0
 *   mode: "buff" ou "debuff"
 *   affect_players: true (se afeta outros jogadores)
 *   affect_mobs: false (se afeta mobs)
 */
public class AuraTrait implements RaceTrait {

    @Override
    public String getId() {
        return "aura";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger != TraitTrigger.PASSIVE_TICK) { return; }

        double radius = 5.0;
        if (config.get("radius") instanceof Number n) { radius = n.doubleValue(); }

        Object effectObj = config.get("effect");
        if (!(effectObj instanceof String effectName)) { return; }

        PotionEffectType effectType = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(effectName.toLowerCase()));
        if (effectType == null) { return; }

        int amplifier = 0;
        if (config.get("amplifier") instanceof Number n) { amplifier = n.intValue(); }

        boolean affectPlayers = true;
        if (config.get("affect_players") instanceof Boolean b) { affectPlayers = b; }

        boolean affectMobs = false;
        if (config.get("affect_mobs") instanceof Boolean b) { affectMobs = b; }

        PotionEffect effect = new PotionEffect(effectType, 60, amplifier, false, false, true);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity == player) { continue; }

            if (entity instanceof Player target && affectPlayers) {
                target.addPotionEffect(effect);
            } else if (entity instanceof LivingEntity living && affectMobs && !(entity instanceof Player)) {
                living.addPotionEffect(effect);
            }
        }
    }
}
