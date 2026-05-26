package me.ray.midgard.modules.races.trait;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.CooldownManager;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trait de transformação temporária com buffs potentes e cooldown longo.
 * Config:
 *   duration: 200 (ticks, default 10s)
 *   cooldown: 120 (segundos)
 *   speed_boost: 0.05 (bonus de velocidade)
 *   damage_boost: 2.0 (bonus de dano)
 *   resistance_amplifier: 1 (amplificador de resistência)
 *   particle: FLAME (partícula durante transformação)
 */
public class TransformTrait implements RaceTrait {

    private static final NamespacedKey TRANSFORM_SPEED_KEY = new NamespacedKey("midgard", "race_transform_speed");
    private static final NamespacedKey TRANSFORM_DAMAGE_KEY = new NamespacedKey("midgard", "race_transform_damage");

    private final CooldownManager cooldownManager = new CooldownManager();
    private final Set<java.util.UUID> transformed = ConcurrentHashMap.newKeySet();

    @Override
    public String getId() {
        return "transform";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger == TraitTrigger.ON_REMOVE || trigger == TraitTrigger.ON_QUIT) {
            revert(player);
            return;
        }

        if (trigger != TraitTrigger.ON_ACTIVE) { return; }

        String abilityId = "transform";
        if (config.get("trait_id") instanceof String s) { abilityId = s; }

        int cooldownSeconds = 120;
        if (config.get("cooldown") instanceof Number n) { cooldownSeconds = n.intValue(); }

        if (cooldownManager.isOnCooldown(player.getUniqueId(), abilityId)) {
            String remaining = cooldownManager.getRemainingFormatted(player.getUniqueId(), abilityId);
            MessageUtils.send(player, me.ray.midgard.modules.races.RacesModule.getInstance()
                    .getMessage("gui.errors.ability_cooldown")
                    .replace("%remaining%", remaining));
            return;
        }

        if (transformed.contains(player.getUniqueId())) { return; }

        int duration = 200;
        if (config.get("duration") instanceof Number n) { duration = n.intValue(); }

        double speedBoost = 0.05;
        if (config.get("speed_boost") instanceof Number n) { speedBoost = n.doubleValue(); }

        double damageBoost = 2.0;
        if (config.get("damage_boost") instanceof Number n) { damageBoost = n.doubleValue(); }

        int resistanceAmp = 1;
        if (config.get("resistance_amplifier") instanceof Number n) { resistanceAmp = n.intValue(); }

        String particleName = "FLAME";
        if (config.get("particle") instanceof String s) { particleName = s; }

        // Aplicar buffs
        transformed.add(player.getUniqueId());

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.addModifier(new AttributeModifier(TRANSFORM_SPEED_KEY, speedBoost, AttributeModifier.Operation.ADD_NUMBER));
        }

        AttributeInstance damageAttr = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.addModifier(new AttributeModifier(TRANSFORM_DAMAGE_KEY, damageBoost, AttributeModifier.Operation.ADD_NUMBER));
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, resistanceAmp, false, false, true));

        // Efeitos visuais
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 50, 0.5, 1.0, 0.5, 0.05);
        } catch (IllegalArgumentException ignored) { /* Partícula inválida */ }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);

        // Agendar reversão
        final String finalAbilityId = abilityId;
        final int finalCooldown = cooldownSeconds;
        Task.syncLater(player, () -> {
            try {
                if (player.isOnline()) {
                    revert(player);
                    cooldownManager.setCooldown(player.getUniqueId(), finalAbilityId, Duration.ofSeconds(finalCooldown));
                }
            } catch (Exception e) {
                me.ray.midgard.core.debug.MidgardLogger.error("Erro ao reverter transformação para %s", player.getName(), e);
            }
        }, duration);
    }

    private void revert(Player player) {
        if (!transformed.remove(player.getUniqueId())) { return; }

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.getModifiers().stream()
                    .filter(m -> m.getKey().equals(TRANSFORM_SPEED_KEY))
                    .forEach(speedAttr::removeModifier);
        }

        AttributeInstance damageAttr = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.getModifiers().stream()
                    .filter(m -> m.getKey().equals(TRANSFORM_DAMAGE_KEY))
                    .forEach(damageAttr::removeModifier);
        }

        player.removePotionEffect(PotionEffectType.RESISTANCE);

        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 30, 0.3, 0.8, 0.3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.0f);
    }
}
