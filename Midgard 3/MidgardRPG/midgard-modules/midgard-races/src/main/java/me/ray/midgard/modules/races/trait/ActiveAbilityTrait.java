package me.ray.midgard.modules.races.trait;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.CooldownManager;
import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.Map;

public class ActiveAbilityTrait implements RaceTrait {

    private final CooldownManager cooldownManager;

    public ActiveAbilityTrait() {
        this.cooldownManager = new CooldownManager();
    }

    @Override
    public String getId() {
        return "active_ability";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger != TraitTrigger.ON_ACTIVE) { return; }

        Object idObj = config.getOrDefault("id", "ability");
        String abilityId = idObj instanceof String s ? s : "ability";
        int cooldownSeconds = config.getOrDefault("cooldown", 10) instanceof Number n ? n.intValue() : 10;
        
        if (cooldownManager.isOnCooldown(player.getUniqueId(), abilityId)) {
            String remaining = cooldownManager.getRemainingFormatted(player.getUniqueId(), abilityId);
            MessageUtils.send(player, me.ray.midgard.modules.races.RacesModule.getInstance()
                    .getMessage("gui.errors.ability_cooldown")
                    .replace("%remaining%", remaining));
            return;
        }

        Object typeObj = config.getOrDefault("effect_type", "NONE");
        String type = typeObj instanceof String s ? s : "NONE";

        boolean executed = true;
        switch (type.toUpperCase()) {
            case "BREATH":
                doBreathAttack(player, config);
                break;
            case "DASH":
                doDash(player, config);
                break;
            case "HEAL":
                doHeal(player, config);
                break;
            default:
                executed = false;
                break;
        }

        if (executed) {
            cooldownManager.setCooldown(player.getUniqueId(), abilityId, Duration.ofSeconds(cooldownSeconds));
        }
    }

    private void doBreathAttack(Player player, Map<String, Object> config) {
        Object particleObj = config.getOrDefault("particle", "FLAME");
        String particleName = particleObj instanceof String s ? s : "FLAME";
        double range = config.getOrDefault("range", 5.0) instanceof Number n ? n.doubleValue() : 5.0;
        double damage = config.getOrDefault("damage", 5.0) instanceof Number n ? n.doubleValue() : 5.0;
        
        Location origin = player.getEyeLocation();
        Vector direction = origin.getDirection();
        
        for (double i = 0; i < range; i += 0.5) {
            Location point = origin.clone().add(direction.clone().multiply(i));
            try {
                Particle particle = Particle.valueOf(particleName.toUpperCase());
                player.getWorld().spawnParticle(particle, point, 1, 0.1, 0.1, 0.1, 0.02);
            } catch (IllegalArgumentException ignored) { /* Partícula pode não existir */ }
            
            for (Entity e : point.getWorld().getNearbyEntities(point, 0.5, 0.5, 0.5)) {
                if (e != player && e instanceof LivingEntity living) {
                    living.damage(damage, player);
                }
            }
        }
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
    }

    private void doDash(Player player, Map<String, Object> config) {
        double force = config.getOrDefault("force", 1.5) instanceof Number n ? n.doubleValue() : 1.5;
        player.setVelocity(player.getLocation().getDirection().multiply(force));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
    }

    private void doHeal(Player player, Map<String, Object> config) {
        int duration = config.getOrDefault("duration", 100) instanceof Number n ? n.intValue() : 100;
        int amplifier = config.getOrDefault("amplifier", 0) instanceof Number n ? n.intValue() : 0;
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, amplifier));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
    }
}
