package me.ray.midgard.modules.races.trait;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.CooldownManager;
import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.Map;

/**
 * Trait ativa de teleporte curto (Enderman-like).
 * Config:
 *   range: 15.0 (distância máxima em blocos)
 *   cooldown: 10 (segundos)
 *   particle: PORTAL (partícula no local de origem)
 */
public class TeleportTrait implements RaceTrait {

    private final CooldownManager cooldownManager = new CooldownManager();

    @Override
    public String getId() {
        return "teleport";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger != TraitTrigger.ON_ACTIVE) { return; }

        String abilityId = "teleport";
        if (config.get("trait_id") instanceof String s) { abilityId = s; }

        int cooldownSeconds = 10;
        if (config.get("cooldown") instanceof Number n) { cooldownSeconds = n.intValue(); }

        if (cooldownManager.isOnCooldown(player.getUniqueId(), abilityId)) {
            String remaining = cooldownManager.getRemainingFormatted(player.getUniqueId(), abilityId);
            MessageUtils.send(player, me.ray.midgard.modules.races.RacesModule.getInstance()
                    .getMessage("gui.errors.ability_cooldown")
                    .replace("%remaining%", remaining));
            return;
        }

        double range = 15.0;
        if (config.get("range") instanceof Number n) { range = n.doubleValue(); }

        // Raycast para encontrar onde o jogador está olhando
        RayTraceResult result = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(), player.getEyeLocation().getDirection(), range);

        Location target;
        if (result != null && result.getHitBlock() != null) {
            // Teleportar para a face do bloco atingido
            Block hitBlock = result.getHitBlock();
            target = hitBlock.getRelative(result.getHitBlockFace()).getLocation().add(0.5, 0, 0.5);
            target.setYaw(player.getLocation().getYaw());
            target.setPitch(player.getLocation().getPitch());
        } else {
            // Sem bloco, teleportar para o alcance máximo
            Vector dir = player.getEyeLocation().getDirection().normalize().multiply(range);
            target = player.getEyeLocation().add(dir);
            target.setYaw(player.getLocation().getYaw());
            target.setPitch(player.getLocation().getPitch());
        }

        // Verificar se o destino é seguro (não dentro de bloco sólido)
        if (target.getBlock().getType().isSolid()) { return; }

        // Efeitos na origem
        Location origin = player.getLocation();
        player.getWorld().spawnParticle(Particle.PORTAL, origin, 40, 0.5, 1.0, 0.5);
        player.getWorld().playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // Teleportar
        player.teleport(target);

        // Efeitos no destino
        player.getWorld().spawnParticle(Particle.PORTAL, target, 40, 0.5, 1.0, 0.5);
        player.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);

        cooldownManager.setCooldown(player.getUniqueId(), abilityId, Duration.ofSeconds(cooldownSeconds));
    }
}
