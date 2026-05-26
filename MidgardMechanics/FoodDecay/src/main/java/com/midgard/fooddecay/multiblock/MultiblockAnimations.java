package com.midgard.fooddecay.multiblock;

import com.midgard.fooddecay.FoodDecayConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Handles all particle and sound animations for active multiblock structures.
 * Each machine type has unique visual effects that evolve with processing progress.
 */
public final class MultiblockAnimations {

    private MultiblockAnimations() {}

    // =========================================================================
    //  Display Placement (per-machine food item positioning)
    // =========================================================================

    record DisplayPlacement(double offX, double offY, double offZ,
                            float scale, Display.Billboard billboard) {}

    static DisplayPlacement getDisplayPlacement(MultiblockType type) {
        return switch (type) {
            case DRYING_RACK       -> new DisplayPlacement(0.5, -0.35, 0.5, 0.45f, Display.Billboard.CENTER);
            case SMOKEHOUSE        -> new DisplayPlacement(0.5, 1.0,  0.5, 0.45f, Display.Billboard.FIXED);
            case SALT_BARREL       -> new DisplayPlacement(0.5, 0.55, 0.5, 0.35f, Display.Billboard.FIXED);
            case PICKLING_CAULDRON -> new DisplayPlacement(0.5, 0.5,  0.5, 0.4f,  Display.Billboard.FIXED);
            case SEALING_PRESS     -> new DisplayPlacement(0.5, 1.05, 0.5, 0.5f,  Display.Billboard.FIXED);
        };
    }

    // =========================================================================
    //  Food Display Idle Animation (called every tick for smooth item movement)
    // =========================================================================

    /**
     * Animates the food display entity with per-machine idle motion.
     * Uses interpolation for smooth transitions between ticks.
     */
    static void tickFoodDisplay(ProcessingMultiblock mb) {
        if (mb.foodDisplay == null || !mb.foodDisplay.isValid()) return;
        if (!(mb.foodDisplay instanceof ItemDisplay display)) return;

        int t = mb.animTick;
        switch (mb.type) {
            case DRYING_RACK       -> tickDryingRackDisplay(display, t);
            case SMOKEHOUSE        -> tickSmokehouseDisplay(display, t);
            case SALT_BARREL       -> tickSaltBarrelDisplay(display, t);
            case PICKLING_CAULDRON -> tickPicklingDisplay(display, t);
            case SEALING_PRESS     -> tickSealingPressDisplay(display, t);
        }
    }

    /** Drying rack: gentle pendulum swing as if hanging from the slab edge. */
    private static void tickDryingRackDisplay(ItemDisplay display, int t) {
        float swing = (float) Math.sin(t * 0.12) * 0.25f;
        float sway  = (float) Math.sin(t * 0.08) * 0.04f;
        float s = 0.45f, hs = s / 2f;
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(20);
        display.setTransformation(new Transformation(
                new Vector3f(-hs + sway, 0, 0),
                new AxisAngle4f(swing, 0, 0, 1),
                new Vector3f(s, s, s),
                new AxisAngle4f(0, 0, 0, 1)
        ));
    }

    /** Smokehouse: slow rotation inside the smoke chamber. */
    private static void tickSmokehouseDisplay(ItemDisplay display, int t) {
        float rotY = (t % 80) * (float)(Math.PI * 2.0 / 80.0);
        float s = 0.45f, hs = s / 2f;
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(20);
        display.setTransformation(new Transformation(
                new Vector3f(-hs, 0, -hs),
                new AxisAngle4f(rotY, 0, 1, 0),
                new Vector3f(s, s, s),
                new AxisAngle4f(0, 0, 0, 1)
        ));
    }

    /** Salt barrel: slow rotation with subtle vertical bobbing. */
    private static void tickSaltBarrelDisplay(ItemDisplay display, int t) {
        float rotY = (t % 120) * (float)(Math.PI * 2.0 / 120.0);
        float bob = (float) Math.sin(t * 0.1) * 0.03f;
        float s = 0.35f, hs = s / 2f;
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(20);
        display.setTransformation(new Transformation(
                new Vector3f(-hs, bob, -hs),
                new AxisAngle4f(rotY, 0, 1, 0),
                new Vector3f(s, s, s),
                new AxisAngle4f(0, 0, 0, 1)
        ));
    }

    /** Pickling cauldron: floating bob simulating liquid submersion. */
    private static void tickPicklingDisplay(ItemDisplay display, int t) {
        float bob = (float) Math.sin(t * 0.25) * 0.06f;
        float rotY = (t % 200) * (float)(Math.PI * 2.0 / 200.0);
        float s = 0.4f, hs = s / 2f;
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(20);
        display.setTransformation(new Transformation(
                new Vector3f(-hs, bob, -hs),
                new AxisAngle4f(rotY, 0, 1, 0),
                new Vector3f(s, s, s),
                new AxisAngle4f(0, 0, 0, 1)
        ));
    }

    /** Sealing press: slow rotation on the stone surface. */
    private static void tickSealingPressDisplay(ItemDisplay display, int t) {
        float rotY = (t % 160) * (float)(Math.PI * 2.0 / 160.0);
        float s = 0.5f, hs = s / 2f;
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(20);
        display.setTransformation(new Transformation(
                new Vector3f(-hs, 0, -hs),
                new AxisAngle4f(rotY, 0, 1, 0),
                new Vector3f(s, s, s),
                new AxisAngle4f(0, 0, 0, 1)
        ));
    }

    /**
     * Spawns processing animation for the given multiblock based on its type and progress.
     */
    public static void spawnProcessingAnimation(ProcessingMultiblock mb, Location anchor,
                                                 FoodDecayConfig config) {
        World world = anchor.getWorld();
        mb.animTick++;
        double ax = anchor.getBlockX() + 0.5;
        double ay = anchor.getBlockY();
        double az = anchor.getBlockZ() + 0.5;

        long elapsed = mb.getEffectiveElapsed();
        long total = mb.getProcessingMinutes(config) * 60_000L;
        float progress = total > 0 ? Math.min(1f, (float) elapsed / total) : 0f;

        switch (mb.type) {
            case DRYING_RACK -> animateDryingRack(world, ax, ay, az, mb, progress);
            case SMOKEHOUSE -> animateSmokehouse(world, ax, ay, az, mb, progress);
            case SALT_BARREL -> animateSaltBarrel(world, ax, ay, az, mb, progress);
            case PICKLING_CAULDRON -> animatePicklingCauldron(world, ax, ay, az, mb, progress);
            case SEALING_PRESS -> animateSealingPress(world, ax, ay, az, mb, progress);
        }
    }

    /**
     * Spawns golden glow animation for completed but uncollected multiblocks.
     */
    public static void spawnCompletionGlow(ProcessingMultiblock mb, Location anchor) {
        World world = anchor.getWorld();
        mb.animTick++;
        double x = anchor.getBlockX() + 0.5;
        double y = anchor.getBlockY();
        double z = anchor.getBlockZ() + 0.5;

        double angle = (mb.animTick % 40) * (Math.PI / 20);
        world.spawnParticle(Particle.WAX_ON,
                x + Math.cos(angle) * 0.5, y + 1.4, z + Math.sin(angle) * 0.5,
                1, 0, 0.05, 0, 0.01);
        if (mb.animTick % 3 == 0) {
            world.spawnParticle(Particle.END_ROD,
                    x, y + 1.5, z, 1, 0.2, 0.1, 0.2, 0.005);
        }
    }

    // =========================================================================
    //  Per-Machine Animations
    // =========================================================================

    private static void animateDryingRack(World world, double x, double y, double z,
                                           ProcessingMultiblock mb, float progress) {
        int t = mb.animTick;
        int shimmerCount = progress < 0.33f ? 1 : progress < 0.66f ? 2 : 3;
        for (int i = 0; i < shimmerCount; i++) {
            double ox = (i - 1) + (Math.random() - 0.5) * 0.3;
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                    x + ox, y + 0.3, z, 0, 0, 0.02, 0, 0.005);
        }
        int glowFreq = progress < 0.33f ? 5 : progress < 0.66f ? 3 : 2;
        if (t % glowFreq == 0) {
            double ox = (Math.random() - 0.5) * 2.5;
            world.spawnParticle(Particle.WAX_OFF,
                    x + ox, y + 0.3, z, 1, 0, 0.1, 0, 0.01);
        }
        if (t % 5 == 0) {
            int sparkleCount = progress < 0.5f ? 1 : 3;
            world.spawnParticle(Particle.END_ROD,
                    x, y + 1.4, z, sparkleCount, 0.1, 0.05, 0.1, 0.005);
        }
        if (progress >= 0.75f && t % 3 == 0) {
            double angle = (t % 40) * (Math.PI / 20);
            world.spawnParticle(Particle.WAX_ON,
                    x + Math.cos(angle) * 0.4, y + 1.3, z + Math.sin(angle) * 0.4,
                    1, 0, 0.02, 0, 0.005);
        }
        if (t % 8 == 0) {
            float vol = 0.1f + progress * 0.1f;
            world.playSound(new Location(world, x, y, z),
                    Sound.BLOCK_WOOL_BREAK, vol, 1.5f + (float) (Math.random() * 0.3));
        }
    }

    private static void animateSmokehouse(World world, double x, double y, double z,
                                           ProcessingMultiblock mb, float progress) {
        int t = mb.animTick;
        int smokeCount = progress < 0.33f ? 0 : progress < 0.66f ? 1 : 2;
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                x, y + 0.5, z, smokeCount, 0.1, 0.05, 0.1, 0.01);
        double angle = (t % 20) * (Math.PI / 10);
        double wx = Math.cos(angle) * 1.0;
        double wz = Math.sin(angle) * 1.0;
        int leakCount = progress < 0.5f ? 1 : 3;
        world.spawnParticle(Particle.SMOKE,
                x + wx, y + 1.5, z + wz, leakCount, 0.1, 0.2, 0.1, 0.005);
        int emberFreq = progress < 0.33f ? 6 : progress < 0.66f ? 4 : 2;
        if (t % emberFreq == 0) {
            world.spawnParticle(Particle.LAVA,
                    x, y + 0.3, z, 1, 0.2, 0.1, 0.2, 0);
        }
        if (t % 4 == 0) {
            float vol = 0.2f + progress * 0.2f;
            world.playSound(new Location(world, x, y, z),
                    Sound.BLOCK_CAMPFIRE_CRACKLE, vol, 0.8f + (float) (Math.random() * 0.4));
        }
        if (t % 6 == 0) {
            world.spawnParticle(Particle.FALLING_DUST,
                    x, y + 2.8, z, 1, 0.2, 0, 0.2, 0,
                    Material.BLACK_CONCRETE.createBlockData());
        }
        if (progress >= 0.75f && t % 4 == 0) {
            world.spawnParticle(Particle.FLAME,
                    x, y + 1.3, z, 1, 0.15, 0.05, 0.15, 0.005);
        }
    }

    private static void animateSaltBarrel(World world, double x, double y, double z,
                                           ProcessingMultiblock mb, float progress) {
        int t = mb.animTick;
        double speed = progress < 0.33f ? 40 : progress < 0.66f ? 30 : 20;
        double angle = (t % (int) speed) * (Math.PI * 2 / speed);
        double radius = 0.6;
        double ox = Math.cos(angle) * radius;
        double oz = Math.sin(angle) * radius;
        world.spawnParticle(Particle.END_ROD,
                x + ox, y + 0.8, z + oz, 0, 0, 0.02, 0, 0.01);
        double angle2 = -(t % 30) * (Math.PI / 15);
        double ox2 = Math.cos(angle2) * 0.3;
        double oz2 = Math.sin(angle2) * 0.3;
        int enchCount = progress < 0.5f ? 1 : 3;
        world.spawnParticle(Particle.ENCHANT,
                x + ox2, y + 0.5, z + oz2, enchCount, 0, 0.2, 0, 0.2);
        int soulFreq = progress < 0.33f ? 12 : progress < 0.66f ? 8 : 4;
        if (t % soulFreq == 0) {
            int side = t / soulFreq % 4;
            double sx = side == 0 ? 1 : side == 2 ? -1 : 0;
            double sz = side == 1 ? 1 : side == 3 ? -1 : 0;
            world.spawnParticle(Particle.SOUL_FIRE_FLAME,
                    x + sx, y + 0.3, z + sz, 2, 0.1, 0.15, 0.1, 0.005);
        }
        if (progress >= 0.75f && t % 2 == 0) {
            world.spawnParticle(Particle.INSTANT_EFFECT,
                    x + (Math.random() - 0.5) * 0.8, y + 0.9,
                    z + (Math.random() - 0.5) * 0.8, 1, 0, 0.1, 0, 0.01);
        }
        if (t % 10 == 0) {
            float vol = 0.15f + progress * 0.1f;
            world.playSound(new Location(world, x, y, z),
                    Sound.BLOCK_SAND_PLACE, vol, 0.8f + (float) (Math.random() * 0.4));
        }
    }

    private static void animatePicklingCauldron(World world, double x, double y, double z,
                                                 ProcessingMultiblock mb, float progress) {
        int t = mb.animTick;
        int bubbleCount = progress < 0.33f ? 1 : progress < 0.66f ? 3 : 5;
        world.spawnParticle(Particle.BUBBLE_POP,
                x, y + 1.0, z, bubbleCount, 0.15, 0.02, 0.15, 0.01);
        int steamFreq = progress < 0.33f ? 4 : progress < 0.66f ? 2 : 1;
        if (t % steamFreq == 0) {
            int steamCount = progress < 0.66f ? 0 : 1;
            world.spawnParticle(Particle.CLOUD,
                    x, y + 1.3, z, steamCount, 0.1, 0.03, 0.1, 0.02);
        }
        if (t % 5 == 0) {
            int splashCount = progress < 0.5f ? 3 : 8;
            world.spawnParticle(Particle.SPLASH,
                    x, y + 1.1, z, splashCount, 0.2, 0.05, 0.2, 0.02);
        }
        int flameCount = progress < 0.5f ? 0 : 1;
        world.spawnParticle(Particle.FLAME,
                x, y - 0.3, z, flameCount, 0.1, 0.01, 0.1, 0.005);
        int dripFreq = progress < 0.33f ? 10 : progress < 0.66f ? 7 : 4;
        if (t % dripFreq == 0) {
            world.spawnParticle(Particle.DRIPPING_HONEY,
                    x + (Math.random() - 0.5) * 0.6,
                    y + 1.0,
                    z + (Math.random() - 0.5) * 0.6,
                    1, 0, 0, 0, 0);
        }
        if (progress >= 0.75f && t % 3 == 0) {
            world.spawnParticle(Particle.ENTITY_EFFECT,
                    x + (Math.random() - 0.5) * 0.4, y + 1.05,
                    z + (Math.random() - 0.5) * 0.4, 1, 0.2, 0.6, 0.1, 0.5);
        }
        if (t % 6 == 0) {
            float vol = 0.2f + progress * 0.2f;
            world.playSound(new Location(world, x, y, z),
                    Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, vol, 1.2f);
        }
    }

    private static void animateSealingPress(World world, double x, double y, double z,
                                             ProcessingMultiblock mb, float progress) {
        int t = mb.animTick;
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        Block pistonBlock = world.getBlockAt(bx, by + 2, bz);

        int dripFreq = progress < 0.33f ? 6 : progress < 0.66f ? 4 : 2;
        if (t % dripFreq == 0) {
            world.spawnParticle(Particle.DRIPPING_HONEY,
                    x, y + 2.2, z, 1, 0.15, 0, 0.15, 0);
        }
        int waxCount = progress < 0.5f ? 1 : 2;
        world.spawnParticle(Particle.WAX_ON,
                x, y + 1.2, z, waxCount, 0.2, 0.1, 0.2, 0.01);
        int pressFreq = progress < 0.33f ? 7 : progress < 0.66f ? 5 : 3;
        if (t % pressFreq == 0) {
            extendPiston(pistonBlock);
            int critCount = progress < 0.5f ? 10 : 20;
            world.spawnParticle(Particle.CRIT,
                    x, y + 1.0, z, critCount, 0.3, 0.1, 0.3, 0.15);
            world.spawnParticle(Particle.WAX_OFF,
                    x, y + 1.0, z, 8, 0.4, 0.1, 0.4, 0.05);
            world.playSound(new Location(world, x, y, z),
                    Sound.BLOCK_PISTON_EXTEND, 0.4f, 1.0f);
        }
        if (t % pressFreq == Math.min(2, pressFreq - 1)) {
            retractPiston(pistonBlock);
            world.playSound(new Location(world, x, y, z),
                    Sound.BLOCK_PISTON_CONTRACT, 0.3f, 1.0f);
        }
        if (t % pressFreq == 1) {
            for (int d = 0; d < 4; d++) {
                double dx = d == 0 ? 1 : d == 2 ? -1 : 0;
                double dz = d == 1 ? 1 : d == 3 ? -1 : 0;
                world.spawnParticle(Particle.BLOCK,
                        x + dx, y + 0.3, z + dz, 3, 0.1, 0.05, 0.1, 0.01,
                        Material.SMOOTH_STONE.createBlockData());
            }
        }
        if (progress >= 0.75f && t % 3 == 0) {
            double angle = (t % 20) * (Math.PI / 10);
            world.spawnParticle(Particle.WAX_ON,
                    x + Math.cos(angle) * 0.5, y + 1.1, z + Math.sin(angle) * 0.5,
                    2, 0, 0.05, 0, 0.005);
        }
        if (t % 7 == 0) {
            float vol = 0.2f + progress * 0.1f;
            world.playSound(new Location(world, x, y, z),
                    Sound.BLOCK_HONEY_BLOCK_SLIDE, vol, 1.0f + (float) (Math.random() * 0.3));
        }
    }

    // =========================================================================
    //  Piston Utilities
    // =========================================================================

    /**
     * Spawns tall ambient smoke visible from a distance above fire-based machines.
     * Only spawns for SMOKEHOUSE and PICKLING_CAULDRON (both have campfires).
     * Called every tick but throttled internally.
     */
    public static void spawnAmbientSmoke(ProcessingMultiblock mb, Location anchor,
                                          FoodDecayConfig config) {
        if (!config.isAmbientSmokeEnabled()) return;
        if (mb.type != MultiblockType.SMOKEHOUSE && mb.type != MultiblockType.PICKLING_CAULDRON) return;

        World world = anchor.getWorld();
        if (world == null) return;

        int interval = config.getAmbientSmokeInterval();
        if (interval <= 0 || mb.animTick % interval != 0) return;

        double x = anchor.getBlockX() + 0.5;
        double y = anchor.getBlockY();
        double z = anchor.getBlockZ() + 0.5;

        int count = config.getAmbientSmokeCount();
        double height = config.getAmbientSmokeHeight();

        // Signal smoke rises very high — visible from distance like TFC forges
        world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE,
                x, y + 2.0, z, count, 0.15, height * 0.3, 0.15, 0.0);

        // Secondary smaller smoke lower down for realism
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                x, y + 1.0, z, Math.max(1, count / 2), 0.2, 0.1, 0.2, 0.01);
    }

    public static void extendPiston(Block pistonBlock) {
        if (pistonBlock.getType() != Material.PISTON) return;
        var pData = (org.bukkit.block.data.type.Piston) pistonBlock.getBlockData();
        if (pData.isExtended()) return;
        BlockFace facing = pData.getFacing();
        pData.setExtended(true);
        pistonBlock.setBlockData(pData, false);
        Block headPos = pistonBlock.getRelative(facing);
        headPos.setType(Material.PISTON_HEAD, false);
        var hData = (org.bukkit.block.data.type.PistonHead) headPos.getBlockData();
        hData.setFacing(facing);
        headPos.setBlockData(hData, false);
    }

    public static void retractPiston(Block pistonBlock) {
        if (pistonBlock.getType() != Material.PISTON) return;
        var pData = (org.bukkit.block.data.type.Piston) pistonBlock.getBlockData();
        if (!pData.isExtended()) return;
        BlockFace facing = pData.getFacing();
        pData.setExtended(false);
        pistonBlock.setBlockData(pData, false);
        Block headPos = pistonBlock.getRelative(facing);
        if (headPos.getType() == Material.PISTON_HEAD) {
            headPos.setType(Material.AIR, false);
        }
    }

    /**
     * Retracts the piston of a sealing press multiblock if applicable.
     */
    public static void retractSealingPressPiston(ProcessingMultiblock mb, Location anchor) {
        if (mb.type != MultiblockType.SEALING_PRESS || anchor == null) return;
        if (!anchor.isWorldLoaded()) return;
        Block pistonBlock = anchor.getWorld().getBlockAt(
                anchor.getBlockX(), anchor.getBlockY() + 2, anchor.getBlockZ());
        retractPiston(pistonBlock);
    }
}
