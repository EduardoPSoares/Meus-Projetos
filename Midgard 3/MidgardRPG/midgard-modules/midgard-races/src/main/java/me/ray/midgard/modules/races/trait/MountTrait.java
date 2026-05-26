package me.ray.midgard.modules.races.trait;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.CooldownManager;
import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trait ativa que invoca uma montaria invisível (boost de velocidade temporário).
 * Config:
 *   speed_boost: 0.08 (bonus de velocidade ao "montar")
 *   duration: 200 (ticks, 10 segundos)
 *   cooldown: 30 (segundos)
 */
public class MountTrait implements RaceTrait {

    private static final NamespacedKey MOUNT_SPEED_KEY = new NamespacedKey("midgard", "race_mount_speed");

    private final CooldownManager cooldownManager = new CooldownManager();
    private final Set<java.util.UUID> mounted = ConcurrentHashMap.newKeySet();

    @Override
    public String getId() {
        return "mount";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger == TraitTrigger.ON_REMOVE || trigger == TraitTrigger.ON_QUIT) {
            dismount(player);
            return;
        }

        if (trigger != TraitTrigger.ON_ACTIVE) { return; }

        String abilityId = "mount";
        if (config.get("trait_id") instanceof String s) { abilityId = s; }

        int cooldownSeconds = 30;
        if (config.get("cooldown") instanceof Number n) { cooldownSeconds = n.intValue(); }

        if (cooldownManager.isOnCooldown(player.getUniqueId(), abilityId)) {
            String remaining = cooldownManager.getRemainingFormatted(player.getUniqueId(), abilityId);
            MessageUtils.send(player, me.ray.midgard.modules.races.RacesModule.getInstance()
                    .getMessage("gui.errors.ability_cooldown")
                    .replace("%remaining%", remaining));
            return;
        }

        // Toggle mount
        if (mounted.contains(player.getUniqueId())) {
            dismount(player);
            cooldownManager.setCooldown(player.getUniqueId(), abilityId, Duration.ofSeconds(cooldownSeconds));
            return;
        }

        double speedBoost = 0.08;
        if (config.get("speed_boost") instanceof Number n) { speedBoost = n.doubleValue(); }

        int duration = 200;
        if (config.get("duration") instanceof Number n) { duration = n.intValue(); }

        // Montar
        mounted.add(player.getUniqueId());

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.addModifier(new AttributeModifier(MOUNT_SPEED_KEY, speedBoost, AttributeModifier.Operation.ADD_NUMBER));
        }

        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_HORSE_GALLOP, 0.8f, 1.0f);

        // Agendar fim da montaria
        final String finalAbilityId = abilityId;
        final int finalCooldown = cooldownSeconds;
        me.ray.midgard.core.utils.Task.syncLater(player, () -> {
            try {
                if (player.isOnline()) {
                    dismount(player);
                    cooldownManager.setCooldown(player.getUniqueId(), finalAbilityId, Duration.ofSeconds(finalCooldown));
                }
            } catch (Exception e) {
                me.ray.midgard.core.debug.MidgardLogger.error("Erro ao desmontar para %s", player.getName(), e);
            }
        }, duration);
    }

    private void dismount(Player player) {
        if (!mounted.remove(player.getUniqueId())) { return; }

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.getModifiers().stream()
                    .filter(m -> m.getKey().equals(MOUNT_SPEED_KEY))
                    .forEach(speedAttr::removeModifier);
        }

        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_HORSE_SADDLE, 0.6f, 1.0f);
    }
}
