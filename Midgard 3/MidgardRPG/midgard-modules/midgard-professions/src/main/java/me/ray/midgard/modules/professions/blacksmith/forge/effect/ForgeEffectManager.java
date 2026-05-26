package me.ray.midgard.modules.professions.blacksmith.forge.effect;

import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeStage;
import me.ray.midgard.modules.professions.blacksmith.forge.quality.QualityTier;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeStructure;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages visual and audio effects for the forge system.
 * Handles particles, sounds, and ambient effects during forging.
 */
public class ForgeEffectManager {

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }

    /**
     * Plays the heating stage ambient effect — fire/lava particles and crackling sound.
     */
    public BukkitTask playHeatingEffect(ForgeStructure forge) {
        Location furnaceLoc = forge.getInteractiveLocations() != null
                ? forge.getInteractiveLocations().get(
                        me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock.ForgeBlockType.FURNACE)
                : forge.getAnchorLocation();
        if (furnaceLoc == null) { return null; }

        Location effectLoc = furnaceLoc.clone().add(0.5, 0.8, 0.5);
        return Task.syncTimer(() -> {
            World world = effectLoc.getWorld();
            if (world == null) { return; }
            world.spawnParticle(Particle.FLAME, effectLoc, 5, 0.2, 0.2, 0.2, 0.02);
            world.spawnParticle(Particle.SMOKE, effectLoc, 3, 0.1, 0.3, 0.1, 0.01);
            world.spawnParticle(Particle.LAVA, effectLoc, 1, 0.1, 0.1, 0.1, 0);
        }, 5L, 8L);
    }

    /**
     * Plays a hammer strike effect — sparks and anvil sound.
     */
    public void playHammerStrike(ForgeStructure forge, boolean perfect) {
        Location anvilLoc = forge.getInteractiveLocations() != null
                ? forge.getInteractiveLocations().get(
                        me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock.ForgeBlockType.ANVIL)
                : forge.getAnchorLocation();
        if (anvilLoc == null) { return; }

        Location effectLoc = anvilLoc.clone().add(0.5, 1.0, 0.5);
        World world = effectLoc.getWorld();
        if (world == null) { return; }

        if (perfect) {
            world.spawnParticle(Particle.ENCHANT, effectLoc, 15, 0.3, 0.3, 0.3, 0.5);
            world.spawnParticle(Particle.FLAME, effectLoc, 8, 0.2, 0.1, 0.2, 0.05);
            world.playSound(effectLoc, Sound.BLOCK_ANVIL_USE, 1.0f, 1.5f);
        } else {
            world.spawnParticle(Particle.CRIT, effectLoc, 8, 0.2, 0.1, 0.2, 0.1);
            world.playSound(effectLoc, Sound.BLOCK_ANVIL_LAND, 0.8f, 1.0f);
        }
    }

    /**
     * Plays the quenching effect — steam and extinguish sound.
     */
    public void playQuenchEffect(ForgeStructure forge) {
        Location cauldronLoc = forge.getInteractiveLocations() != null
                ? forge.getInteractiveLocations().get(
                        me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock.ForgeBlockType.CAULDRON)
                : forge.getAnchorLocation();
        if (cauldronLoc == null) { return; }

        Location effectLoc = cauldronLoc.clone().add(0.5, 1.0, 0.5);
        World world = effectLoc.getWorld();
        if (world == null) { return; }

        world.playSound(effectLoc, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
        // Steam cloud
        final BukkitTask[] quenchTask = new BukkitTask[1];
        quenchTask[0] = Task.syncTimer(new Runnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick++ > 10 || world == null) {
                    if (quenchTask[0] != null) { quenchTask[0].cancel(); }
                    return;
                }
                world.spawnParticle(Particle.CLOUD, effectLoc, 10, 0.3, 0.5, 0.3, 0.02);
                world.spawnParticle(Particle.SPLASH, effectLoc.clone().add(0, -0.3, 0), 5, 0.2, 0.1, 0.2, 0.1);
            }
        }, 1L, 3L);
    }

    /**
     * Plays the sharpening ambient effect — sparks on the grindstone.
     */
    public void playSharpeningEffect(ForgeStructure forge) {
        Location grindstoneLoc = forge.getInteractiveLocations() != null
                ? forge.getInteractiveLocations().get(
                        me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock.ForgeBlockType.GRINDSTONE)
                : forge.getAnchorLocation();
        if (grindstoneLoc == null) { return; }

        Location effectLoc = grindstoneLoc.clone().add(0.5, 0.8, 0.5);
        World world = effectLoc.getWorld();
        if (world == null) { return; }

        world.spawnParticle(Particle.CRIT, effectLoc, 8, 0.1, 0.1, 0.1, 0.1);
        world.playSound(effectLoc, Sound.BLOCK_GRINDSTONE_USE, 0.6f, 1.2f);
    }

    /**
     * Plays the completion celebration effect based on quality.
     */
    public void playCompletionEffect(ForgeStructure forge, QualityTier quality) {
        Location loc = forge.getAnchorLocation();
        if (loc == null) { return; }

        Location effectLoc = loc.clone().add(0.5, 2.0, 0.5);
        World world = effectLoc.getWorld();
        if (world == null) { return; }

        switch (quality) {
            case LEGENDARY, MASTERPIECE -> {
                world.playSound(effectLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, effectLoc, 50, 0.5, 1.0, 0.5, 0.3);
                world.spawnParticle(Particle.ENCHANT, effectLoc, 30, 1.0, 1.0, 1.0, 1.0);
                // Firework-like burst
                Task.syncLater(() -> {
                    world.spawnParticle(Particle.EXPLOSION, effectLoc.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
                    world.playSound(effectLoc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f);
                }, 20L);
            }
            case EXCEPTIONAL, SUPERIOR -> {
                world.playSound(effectLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                world.spawnParticle(Particle.ENCHANT, effectLoc, 25, 0.5, 0.5, 0.5, 0.5);
                world.spawnParticle(Particle.HAPPY_VILLAGER, effectLoc, 15, 0.5, 0.5, 0.5, 0);
            }
            case COMMON, INFERIOR -> {
                world.playSound(effectLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                world.spawnParticle(Particle.ENCHANT, effectLoc, 10, 0.3, 0.3, 0.3, 0.3);
            }
            case DEFECTIVE -> {
                world.playSound(effectLoc, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                world.spawnParticle(Particle.SMOKE, effectLoc, 15, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    /**
     * Plays the failure effect — explosion and smoke.
     */
    public void playFailureEffect(ForgeStructure forge) {
        Location loc = forge.getAnchorLocation();
        if (loc == null) { return; }

        Location effectLoc = loc.clone().add(0.5, 1.5, 0.5);
        World world = effectLoc.getWorld();
        if (world == null) { return; }

        world.playSound(effectLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
        world.spawnParticle(Particle.SMOKE, effectLoc, 30, 0.5, 0.5, 0.5, 0.05);
        world.spawnParticle(Particle.LARGE_SMOKE, effectLoc, 10, 0.3, 0.3, 0.3, 0.02);
    }

    /**
     * Plays ambient forge effect — idle smoke from chimney.
     */
    public void playIdleSmoke(ForgeStructure forge) {
        Location loc = forge.getAnchorLocation();
        if (loc == null) { return; }

        // Chimney is typically at the top of the forge
        Location chimney = loc.clone().add(0.5, forge.getTier().getHeight(), 0.5);
        World world = chimney.getWorld();
        if (world == null) { return; }

        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, chimney, 2, 0.1, 0.1, 0.1, 0.01);
    }

    /**
     * Sends a stage transition message and sound to the player.
     */
    public void playStageTransition(Player player, ForgeStage newStage) {
        var mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
        switch (newStage) {
            case HEATING -> {
                player.sendMessage(mm.deserialize(msg("forge.stage.heating_hint")));
                player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1f, 1f);
            }
            case HAMMERING -> {
                player.sendMessage(mm.deserialize(msg("forge.stage.hammering_hint")));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            }
            case QUENCHING -> {
                player.sendMessage(mm.deserialize(msg("forge.stage.quenching_hint")));
                player.playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 1f, 1f);
            }
            case SHARPENING -> {
                player.sendMessage(mm.deserialize(msg("forge.stage.sharpening_hint")));
                player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
            }
            case FINALIZING -> {
                player.sendMessage(mm.deserialize(msg("forge.stage.finishing_hint")));
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
            }
            case COMPLETED -> {
                player.sendMessage(mm.deserialize(msg("forge.stage.completed")));
            }
            case FAILED -> {
                player.sendMessage(mm.deserialize(msg("forge.stage.failed")));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.8f);
            }
        }
    }
}
