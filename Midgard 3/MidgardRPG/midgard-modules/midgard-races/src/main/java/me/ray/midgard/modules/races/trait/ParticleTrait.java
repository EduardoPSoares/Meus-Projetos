package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Trait cosmético que exibe partículas ao redor do jogador.
 * Config:
 *   particle: "FLAME" (tipo de partícula)
 *   count: 5 (quantidade por tick)
 *   offset_x: 0.3
 *   offset_y: 0.5
 *   offset_z: 0.3
 *   speed: 0.02
 */
public class ParticleTrait implements RaceTrait {

    @Override
    public String getId() {
        return "particle";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger != TraitTrigger.PASSIVE_TICK) { return; }

        Object particleObj = config.get("particle");
        if (!(particleObj instanceof String particleName)) { return; }

        Particle particle;
        try {
            particle = Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }

        int count = 5;
        if (config.get("count") instanceof Number n) { count = n.intValue(); }

        double offsetX = 0.3;
        if (config.get("offset_x") instanceof Number n) { offsetX = n.doubleValue(); }

        double offsetY = 0.5;
        if (config.get("offset_y") instanceof Number n) { offsetY = n.doubleValue(); }

        double offsetZ = 0.3;
        if (config.get("offset_z") instanceof Number n) { offsetZ = n.doubleValue(); }

        double speed = 0.02;
        if (config.get("speed") instanceof Number n) { speed = n.doubleValue(); }

        Location loc = player.getLocation().add(0, 1.0, 0);
        player.getWorld().spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, speed);
    }
}
