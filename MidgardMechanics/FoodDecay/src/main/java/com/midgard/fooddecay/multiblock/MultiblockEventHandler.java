package com.midgard.fooddecay.multiblock;

import com.midgard.core.utils.MessageUtils;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayConfig;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.time.Duration;
import java.util.List;

/**
 * Handles QTE minigame events during multiblock processing.
 * Each machine type has a themed visual minigame with animated display entities,
 * BossBar countdown, and Title notifications.
 */
public final class MultiblockEventHandler {

    private MultiblockEventHandler() {}

    // =========================================================================
    //  Per-Machine Minigame Display Items
    // =========================================================================

    private static Material getMinigameItem(MultiblockType type, int eventType) {
        return switch (type) {
            case DRYING_RACK       -> eventType == 0 ? Material.FEATHER : Material.SPIDER_EYE;
            case SMOKEHOUSE        -> eventType == 0 ? Material.BLAZE_ROD : Material.COAL;
            case SALT_BARREL       -> eventType == 0 ? Material.QUARTZ : Material.PRISMARINE_CRYSTALS;
            case PICKLING_CAULDRON -> eventType == 0 ? Material.MAGMA_CREAM : Material.HEART_OF_THE_SEA;
            case SEALING_PRESS     -> eventType == 0 ? Material.HONEYCOMB : Material.SLIME_BALL;
        };
    }

    // =========================================================================
    //  Tick — called once per second during processing
    // =========================================================================

    public static void tickEvent(ProcessingMultiblock mb, Location anchor,
                                  FoodDecayConfig config) {
        if (mb.eventActive) {
            long elapsed = System.currentTimeMillis() - mb.eventStartTime;
            long eventDurationMs = config.getQteDurationSeconds() * 1000L;
            double progress = Math.max(0, 1.0 - (double) elapsed / eventDurationMs);

            // Update BossBar
            if (mb.eventBossBar != null) {
                mb.eventBossBar.setProgress(progress);
                if (progress < 0.25) {
                    mb.eventBossBar.setColor(BarColor.RED);
                } else if (progress < 0.55) {
                    mb.eventBossBar.setColor(BarColor.YELLOW);
                }
                updateBossBarPlayers(mb, anchor, config);
            }

            if (elapsed >= eventDurationMs) {
                expireMinigame(mb, anchor, config);
            } else {
                tickMinigameAnimation(mb, anchor, elapsed, eventDurationMs);
            }
            return;
        }

        // Check if enough time has passed to potentially spawn a new event
        long effectiveElapsed = mb.getEffectiveElapsed();
        long total = mb.getProcessingMinutes(config) * 60_000L;
        if (effectiveElapsed < 15_000 || (total - effectiveElapsed) < 15_000) return;
        if (mb.animTick % config.getQteIntervalSeconds() != 0) return;
        if (mb.eventsHandled + mb.eventsMissed >= config.getQteMaxPerCycle()) return;

        if (Math.random() < config.getQteChance()) {
            startMinigame(mb, anchor, config);
        }
    }

    // =========================================================================
    //  Start Minigame — spawn display entities + BossBar + notify
    // =========================================================================

    private static void startMinigame(ProcessingMultiblock mb, Location anchor,
                                       FoodDecayConfig config) {
        mb.eventActive = true;
        mb.eventStartTime = System.currentTimeMillis();
        mb.eventType = (int) (Math.random() * 2);

        World world = anchor.getWorld();
        Location center = anchor.clone().add(0.5, 1.5, 0.5);

        // Alert sound
        world.playSound(anchor, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 0.5f);
        world.playSound(anchor, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.2f);

        // Spawn themed display entities
        spawnMinigameDisplays(mb, anchor, world);

        // Create BossBar
        String eventMsg = getEventMessage(mb.type, mb.eventType, config);
        String bossBarTitle = MessageUtils.colorize(sc(config.msg("qte-bossbar-title")
                .replace("{machine}", config.getMultiblockDisplayName(mb.type))
                .replace("{message}", eventMsg)));
        mb.eventBossBar = Bukkit.createBossBar(bossBarTitle, BarColor.GREEN, BarStyle.SEGMENTED_10);
        mb.eventBossBar.setProgress(1.0);

        // Notify nearby players
        int radius = config.getNotificationRadius();
        if (radius > 0) {
            double rSq = radius * radius;
            for (Player p : world.getPlayers()) {
                if (p.getLocation().distanceSquared(anchor) <= rSq) {
                    mb.eventBossBar.addPlayer(p);

                    p.sendActionBar(MessageUtils.toComponent(
                            sc(config.msg("qte-alert-actionbar")
                                    .replace("{message}", eventMsg))));
                    p.sendMessage(MessageUtils.toComponent(
                            sc(config.msg("qte-alert-chat")
                                    .replace("{machine}", config.getMultiblockDisplayName(mb.type))
                                    .replace("{message}", eventMsg))));
                    p.showTitle(Title.title(
                            MessageUtils.toComponent(sc(config.msg("qte-title"))),
                            MessageUtils.toComponent(sc(eventMsg)),
                            Title.Times.times(
                                    Duration.ofMillis(200),
                                    Duration.ofSeconds(3),
                                    Duration.ofMillis(500))));
                }
            }
        }

        // Initial particle burst
        world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION,
                center, 20, 0.6, 0.6, 0.6, 0.02);
        world.spawnParticle(Particle.END_ROD,
                center, 10, 0.3, 0.3, 0.3, 0.05);
    }

    private static void spawnMinigameDisplays(ProcessingMultiblock mb,
                                               Location anchor, World world) {
        double cx = anchor.getBlockX() + 0.5;
        double cy = anchor.getBlockY() + 1.5;
        double cz = anchor.getBlockZ() + 0.5;

        Material item = getMinigameItem(mb.type, mb.eventType);

        switch (mb.type) {
            case DRYING_RACK -> {
                // 3 items orbiting — wind blowing / insects swarming
                for (int i = 0; i < 3; i++) {
                    double angle = (Math.PI * 2 / 3) * i;
                    double ox = Math.cos(angle) * 0.8;
                    double oz = Math.sin(angle) * 0.8;
                    mb.eventDisplays.add(spawnEventItem(world,
                            new Location(world, cx + ox, cy + 0.3, cz + oz), item, 0.3f));
                }
            }
            case SMOKEHOUSE -> {
                // 1 large fire item pulsing + 2 smaller smoke indicators
                mb.eventDisplays.add(spawnEventItem(world,
                        new Location(world, cx, cy + 0.8, cz), item, 0.5f));
                mb.eventDisplays.add(spawnEventItem(world,
                        new Location(world, cx + 0.5, cy + 1.3, cz), Material.GRAY_DYE, 0.2f));
                mb.eventDisplays.add(spawnEventItem(world,
                        new Location(world, cx - 0.5, cy + 1.3, cz), Material.GRAY_DYE, 0.2f));
            }
            case SALT_BARREL -> {
                // Crystal forming — 4 items rising from barrel edges
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI * 2 / 4) * i + Math.PI / 4;
                    double ox = Math.cos(angle) * 0.5;
                    double oz = Math.sin(angle) * 0.5;
                    mb.eventDisplays.add(spawnEventItem(world,
                            new Location(world, cx + ox, cy - 0.3, cz + oz), item, 0.2f));
                }
            }
            case PICKLING_CAULDRON -> {
                // Bubbling item rising + 2 side splash items
                mb.eventDisplays.add(spawnEventItem(world,
                        new Location(world, cx, cy + 0.2, cz), item, 0.4f));
                mb.eventDisplays.add(spawnEventItem(world,
                        new Location(world, cx + 0.4, cy, cz + 0.4), Material.KELP, 0.2f));
                mb.eventDisplays.add(spawnEventItem(world,
                        new Location(world, cx - 0.4, cy, cz - 0.4), Material.KELP, 0.2f));
            }
            case SEALING_PRESS -> {
                // Wax/pressure item dropping from piston + side indicators
                mb.eventDisplays.add(spawnEventItem(world,
                        new Location(world, cx, cy + 1.5, cz), item, 0.45f));
                mb.eventDisplays.add(spawnEventItem(world,
                        new Location(world, cx + 0.6, cy + 0.5, cz), Material.IRON_NUGGET, 0.15f));
                mb.eventDisplays.add(spawnEventItem(world,
                        new Location(world, cx - 0.6, cy + 0.5, cz), Material.IRON_NUGGET, 0.15f));
            }
        }

        // Warning text display above
        TextDisplay txt = (TextDisplay) world.spawnEntity(
                new Location(world, cx, cy + 1.8, cz), org.bukkit.entity.EntityType.TEXT_DISPLAY);
        txt.setText("⚠");
        txt.setBillboard(Display.Billboard.CENTER);
        txt.setBrightness(new Display.Brightness(15, 15));
        txt.setShadowed(true);
        txt.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        float ts = 1.5f;
        txt.setTransformation(new Transformation(
                new Vector3f(-ts / 2f, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(ts, ts, ts),
                new AxisAngle4f(0, 0, 0, 1)
        ));
        mb.eventDisplays.add(txt);
    }

    private static ItemDisplay spawnEventItem(World world, Location loc,
                                               Material mat, float scale) {
        ItemDisplay display = (ItemDisplay) world.spawnEntity(loc, org.bukkit.entity.EntityType.ITEM_DISPLAY);
        display.setItemStack(new ItemStack(mat));
        float hs = scale / 2f;
        display.setTransformation(new Transformation(
                new Vector3f(-hs, 0, -hs),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0, 0, 0, 1)
        ));
        display.setBillboard(Display.Billboard.CENTER);
        display.setBrightness(new Display.Brightness(15, 15));
        display.setInterpolationDuration(10);
        return display;
    }

    // =========================================================================
    //  Tick Minigame Animation — per-machine display entity movement
    // =========================================================================

    private static void tickMinigameAnimation(ProcessingMultiblock mb, Location anchor,
                                               long elapsedMs, long durationMs) {
        int t = mb.animTick;
        double urgency = (double) elapsedMs / durationMs; // 0→1 as time runs out
        double cx = anchor.getBlockX() + 0.5;
        double cy = anchor.getBlockY() + 1.5;
        double cz = anchor.getBlockZ() + 0.5;
        World world = anchor.getWorld();

        List<Entity> displays = mb.eventDisplays;
        if (displays.isEmpty()) return;

        switch (mb.type) {
            case DRYING_RACK -> tickDryingMinigame(displays, t, urgency, cx, cy, cz, world);
            case SMOKEHOUSE -> tickSmokehouseMinigame(displays, t, urgency, cx, cy, cz, world);
            case SALT_BARREL -> tickSaltMinigame(displays, t, urgency, cx, cy, cz, world);
            case PICKLING_CAULDRON -> tickPicklingMinigame(displays, t, urgency, cx, cy, cz, world);
            case SEALING_PRESS -> tickSealingMinigame(displays, t, urgency, cx, cy, cz, world);
        }

        // Animate warning text (last entity) — pulse scale
        Entity last = displays.getLast();
        if (last instanceof TextDisplay txt && txt.isValid()) {
            float pulse = 1.5f + (float) Math.sin(t * 0.4) * 0.3f * (1f + (float) urgency);
            txt.setInterpolationDelay(0);
            txt.setInterpolationDuration(10);
            // Bob the text up and down
            float textY = (float) (cy + 1.8 + Math.sin(t * 0.2) * 0.15) - (float) cy;
            txt.setTransformation(new Transformation(
                    new Vector3f(-pulse / 2f, textY - 1.8f, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(pulse, pulse, pulse),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
            // Change text color as urgency increases
            if (urgency > 0.75) {
                txt.setText("§c⚠");
            } else if (urgency > 0.45) {
                txt.setText("§e⚠");
            }
        }

        // Ambient particles based on urgency
        if (t % 3 == 0) {
            int count = urgency > 0.6 ? 5 : 2;
            world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION,
                    cx, cy + 0.5, cz, count, 0.5, 0.4, 0.5, 0.01);
        }
        if (urgency > 0.5 && t % 2 == 0) {
            world.spawnParticle(Particle.WAX_OFF,
                    cx, cy + 1.0, cz, 2, 0.3, 0.2, 0.3, 0.02);
        }
        // Urgency sound ticks
        if (urgency > 0.7 && t % 3 == 0) {
            world.playSound(anchor, Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.5f + (float) urgency);
        }
    }

    /**
     * Drying rack: items orbit faster as wind/insects intensify,
     * wobble increases with urgency, height oscillates.
     */
    private static void tickDryingMinigame(List<Entity> displays, int t, double urgency,
                                            double cx, double cy, double cz, World world) {
        double speed = 0.08 + urgency * 0.15;
        double radius = 0.8 + Math.sin(t * 0.1) * 0.2 * urgency;
        double wobble = urgency * 0.3;

        for (int i = 0; i < Math.min(3, displays.size()); i++) {
            Entity e = displays.get(i);
            if (!(e instanceof ItemDisplay d) || !d.isValid()) continue;

            double angle = (Math.PI * 2 / 3) * i + t * speed;
            float ox = (float) (Math.cos(angle) * radius);
            float oy = (float) (0.3 + Math.sin(t * 0.2 + i) * 0.2
                    + Math.random() * wobble * 0.1);
            float oz = (float) (Math.sin(angle) * radius);
            float tilt = (float) (Math.sin(t * 0.3 + i * 2) * (0.3 + urgency * 0.5));

            d.setInterpolationDelay(0);
            d.setInterpolationDuration(10);
            float s = 0.3f;
            d.setTransformation(new Transformation(
                    new Vector3f(ox - s / 2f, oy, oz - s / 2f),
                    new AxisAngle4f(tilt, 0, 0, 1),
                    new Vector3f(s, s, s),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        }
        // Wind / insect particles
        if (t % 2 == 0) {
            double windAngle = t * 0.15;
            world.spawnParticle(Particle.CLOUD,
                    cx + Math.cos(windAngle) * 1.2, cy + 0.5, cz + Math.sin(windAngle) * 1.2,
                    1, 0.1, 0.05, 0.1, 0.01);
        }
    }

    /**
     * Smokehouse: fire item pulses in size, smoke items drift upward,
     * flame particles intensify with urgency.
     */
    private static void tickSmokehouseMinigame(List<Entity> displays, int t, double urgency,
                                                double cx, double cy, double cz, World world) {
        // Main fire item — pulsing
        if (!displays.isEmpty() && displays.getFirst() instanceof ItemDisplay fire && fire.isValid()) {
            float pulse = (float) (0.5 + Math.sin(t * 0.3) * 0.15 * (1 + urgency));
            float glow = (float) (Math.sin(t * 0.5) * 0.1 * urgency);
            fire.setInterpolationDelay(0);
            fire.setInterpolationDuration(10);
            fire.setTransformation(new Transformation(
                    new Vector3f(-pulse / 2f, 0.8f + glow, -pulse / 2f),
                    new AxisAngle4f((float) (t * 0.15), 0, 1, 0),
                    new Vector3f(pulse, pulse, pulse),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        }

        // Smoke indicators — drift upward and sway
        for (int i = 1; i <= 2 && i < displays.size(); i++) {
            Entity e = displays.get(i);
            if (!(e instanceof ItemDisplay smoke) || !smoke.isValid()) continue;
            float side = i == 1 ? 0.5f : -0.5f;
            float drift = (float) (1.3 + (t % 40) * 0.02 + Math.sin(t * 0.2) * 0.1);
            float sway = (float) (Math.sin(t * 0.15 + i) * 0.15 * (1 + urgency));
            smoke.setInterpolationDelay(0);
            smoke.setInterpolationDuration(10);
            float s = (float) (0.2 + urgency * 0.1);
            smoke.setTransformation(new Transformation(
                    new Vector3f(side + sway - s / 2f, drift, -s / 2f),
                    new AxisAngle4f((float) (t * 0.1), 0, 1, 0),
                    new Vector3f(s, s, s),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        }

        // Smoke & fire particles
        if (t % 2 == 0) {
            int smokeCount = (int) (2 + urgency * 5);
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                    cx, cy + 0.5, cz, smokeCount, 0.2, 0.3, 0.2, 0.01);
        }
        if (urgency > 0.3 && t % 3 == 0) {
            world.spawnParticle(Particle.FLAME,
                    cx, cy + 0.3, cz, 3, 0.15, 0.1, 0.15, 0.01);
        }
    }

    /**
     * Salt barrel: crystals spin and rise from the barrel, wobble erratically,
     * sparkle particles increase with urgency.
     */
    private static void tickSaltMinigame(List<Entity> displays, int t, double urgency,
                                          double cx, double cy, double cz, World world) {
        for (int i = 0; i < Math.min(4, displays.size()); i++) {
            Entity e = displays.get(i);
            if (!(e instanceof ItemDisplay d) || !d.isValid()) continue;

            double baseAngle = (Math.PI * 2 / 4) * i + Math.PI / 4;
            double spin = t * (0.05 + urgency * 0.1);
            double r = 0.5 + Math.sin(t * 0.08 + i) * 0.15;
            float ox = (float) (Math.cos(baseAngle + spin) * r);
            float oz = (float) (Math.sin(baseAngle + spin) * r);
            float rise = (float) (-0.3 + (t % 60) * 0.01 * (1 + urgency * 0.5));
            float wobbleX = (float) (Math.sin(t * 0.2 + i * 1.5) * 0.1 * urgency);

            d.setInterpolationDelay(0);
            d.setInterpolationDuration(10);
            float s = (float) (0.2 + Math.sin(t * 0.15 + i) * 0.05);
            d.setTransformation(new Transformation(
                    new Vector3f(ox + wobbleX - s / 2f, rise, oz - s / 2f),
                    new AxisAngle4f((float) (t * 0.2 + i), 1, 0.5f, 0),
                    new Vector3f(s, s, s),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        }

        // Crystal sparkle particles
        if (t % 3 == 0) {
            world.spawnParticle(Particle.END_ROD,
                    cx, cy + 0.3, cz, 2, 0.4, 0.3, 0.4, 0.01);
        }
        if (urgency > 0.4) {
            world.spawnParticle(Particle.ENCHANT,
                    cx, cy, cz, 3, 0.3, 0.4, 0.3, 0.3);
        }
    }

    /**
     * Pickling cauldron: main item bobs and grows as temperature rises,
     * side items splash outward, bubble particles intensify.
     */
    private static void tickPicklingMinigame(List<Entity> displays, int t, double urgency,
                                              double cx, double cy, double cz, World world) {
        // Main temperature item — bobs and grows with heat
        if (!displays.isEmpty() && displays.getFirst() instanceof ItemDisplay main && main.isValid()) {
            float bob = (float) (0.2 + Math.sin(t * 0.3) * 0.3 * (1 + urgency));
            float scale = (float) (0.4 + urgency * 0.2 + Math.sin(t * 0.2) * 0.05);
            float spin = (float) (t * 0.12);
            main.setInterpolationDelay(0);
            main.setInterpolationDuration(10);
            main.setTransformation(new Transformation(
                    new Vector3f(-scale / 2f, bob, -scale / 2f),
                    new AxisAngle4f(spin, 0, 1, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        }

        // Side splash items — orbit outward
        for (int i = 1; i <= 2 && i < displays.size(); i++) {
            Entity e = displays.get(i);
            if (!(e instanceof ItemDisplay splash) || !splash.isValid()) continue;
            float side = i == 1 ? 1 : -1;
            double orbSpeed = 0.1 + urgency * 0.08;
            float ox = (float) (side * (0.4 + Math.sin(t * orbSpeed + i) * 0.2));
            float oy = (float) (Math.sin(t * 0.15 + i * 2) * 0.15);
            float oz = (float) (side * Math.cos(t * orbSpeed) * 0.3);
            splash.setInterpolationDelay(0);
            splash.setInterpolationDuration(10);
            float s = 0.2f;
            splash.setTransformation(new Transformation(
                    new Vector3f(ox - s / 2f, oy, oz - s / 2f),
                    new AxisAngle4f((float) (t * 0.2), 0, 1, 0),
                    new Vector3f(s, s, s),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        }

        // Bubble and steam particles
        int bubbles = (int) (3 + urgency * 8);
        world.spawnParticle(Particle.BUBBLE_POP,
                cx, cy - 0.2, cz, bubbles, 0.2, 0.05, 0.2, 0.01);
        if (t % 2 == 0 && urgency > 0.3) {
            world.spawnParticle(Particle.CLOUD,
                    cx, cy + 0.5, cz, 2, 0.15, 0.1, 0.15, 0.02);
        }
        if (urgency > 0.6 && t % 3 == 0) {
            world.spawnParticle(Particle.SPLASH,
                    cx, cy, cz, 5, 0.3, 0.1, 0.3, 0.03);
        }
    }

    /**
     * Sealing press: wax/pressure item drops slowly from above, wobbles under pressure,
     * side indicators vibrate with increasing intensity.
     */
    private static void tickSealingMinigame(List<Entity> displays, int t, double urgency,
                                             double cx, double cy, double cz, World world) {
        // Main item — descends from piston height, wobbles
        if (!displays.isEmpty() && displays.getFirst() instanceof ItemDisplay main && main.isValid()) {
            float drop = (float) (1.5 - urgency * 0.8);
            float wobbleX = (float) (Math.sin(t * 0.25) * 0.1 * (1 + urgency));
            float wobbleZ = (float) (Math.cos(t * 0.3) * 0.1 * (1 + urgency));
            float scale = (float) (0.45 + Math.sin(t * 0.2) * 0.05);
            main.setInterpolationDelay(0);
            main.setInterpolationDuration(10);
            main.setTransformation(new Transformation(
                    new Vector3f(wobbleX - scale / 2f, drop, wobbleZ - scale / 2f),
                    new AxisAngle4f((float) (t * 0.08), 0, 1, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        }

        // Side pressure indicators — vibrate
        for (int i = 1; i <= 2 && i < displays.size(); i++) {
            Entity e = displays.get(i);
            if (!(e instanceof ItemDisplay ind) || !ind.isValid()) continue;
            float side = i == 1 ? 0.6f : -0.6f;
            float vibX = (float) (Math.random() * 0.06 * urgency - 0.03 * urgency);
            float vibY = (float) (0.5 + Math.sin(t * 0.3 + i) * 0.1);
            ind.setInterpolationDelay(0);
            ind.setInterpolationDuration(10);
            float s = (float) (0.15 + urgency * 0.08);
            ind.setTransformation(new Transformation(
                    new Vector3f(side + vibX - s / 2f, vibY, -s / 2f),
                    new AxisAngle4f((float) (t * 0.3), 1, 0, 0),
                    new Vector3f(s, s, s),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        }

        // Pressure particles
        if (t % 2 == 0) {
            world.spawnParticle(Particle.CRIT,
                    cx, cy + 1.0, cz, 2, 0.2, 0.3, 0.2, 0.05);
        }
        if (urgency > 0.5 && t % 3 == 0) {
            world.spawnParticle(Particle.WAX_ON,
                    cx, cy + 0.5, cz, 3, 0.3, 0.1, 0.3, 0.02);
        }
        // Piston creak sounds as urgency grows
        if (urgency > 0.6 && t % 5 == 0) {
            world.playSound(new Location(world, cx, cy, cz),
                    Sound.BLOCK_PISTON_EXTEND, 0.2f, 0.5f + (float) urgency);
        }
    }

    // =========================================================================
    //  Resolve Minigame — success animation
    // =========================================================================

    public static boolean handleInteraction(ProcessingMultiblock mb,
                                             Player player, Location anchor,
                                             FoodDecayConfig config) {
        if (!mb.eventActive) return false;

        mb.eventActive = false;
        mb.eventsHandled++;
        mb.qualityBonus += config.getQteBonusPerEvent();
        cleanupBossBar(mb);

        Location center = anchor.clone().add(0.5, 1.5, 0.5);
        World world = anchor.getWorld();

        // Success animation: spiral entities upward then remove
        animateSuccessAndCleanup(mb, center, world);

        // Success particles
        world.spawnParticle(Particle.HAPPY_VILLAGER, center, 25, 0.6, 0.6, 0.6, 0.05);
        world.spawnParticle(Particle.WAX_ON, center, 15, 0.5, 0.5, 0.5, 0.08);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 8, 0.2, 0.3, 0.2, 0.1);

        // Success sounds
        world.playSound(anchor, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.8f);
        world.playSound(anchor, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.5f);
        world.playSound(anchor, Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 2.0f);

        // Player feedback
        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("qte-resolved-prefix")
                        + getResolvedMessage(mb.type, mb.eventType, config))));
        player.showTitle(Title.title(
                MessageUtils.toComponent(sc("&a✔")),
                MessageUtils.toComponent(sc(config.msg("qte-resolved-subtitle"))),
                Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(1), Duration.ofMillis(300))));

        return true;
    }

    /**
     * Animates display entities spiraling upward then removes them.
     */
    private static void animateSuccessAndCleanup(ProcessingMultiblock mb,
                                                  Location center, World world) {
        int i = 0;
        for (Entity e : mb.eventDisplays) {
            if (e == null || !e.isValid()) continue;

            if (e instanceof ItemDisplay d) {
                // Spiral upward animation
                float angle = (float) (i * Math.PI * 2 / 3);
                d.setInterpolationDelay(0);
                d.setInterpolationDuration(15);
                float s = 0.15f;
                d.setTransformation(new Transformation(
                        new Vector3f(
                                (float) Math.cos(angle) * 0.3f,
                                2.5f,
                                (float) Math.sin(angle) * 0.3f),
                        new AxisAngle4f((float) (Math.PI * 2), 0, 1, 0),
                        new Vector3f(s, s, s),
                        new AxisAngle4f(0, 0, 0, 1)
                ));
                i++;
            } else if (e instanceof TextDisplay txt) {
                txt.setInterpolationDelay(0);
                txt.setInterpolationDuration(10);
                txt.setText("§a✔");
                txt.setTransformation(new Transformation(
                        new Vector3f(-1, 1, 0),
                        new AxisAngle4f(0, 0, 0, 1),
                        new Vector3f(2, 2, 2),
                        new AxisAngle4f(0, 0, 0, 1)
                ));
            }
        }
        // Schedule removal after animation plays out
        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("FoodDecay"), () -> {
                    for (Entity e : mb.eventDisplays) {
                        if (e != null && e.isValid()) e.remove();
                    }
                    mb.eventDisplays.clear();
                }, 20L); // 1 second delay
    }

    // =========================================================================
    //  Expire Minigame — failure animation
    // =========================================================================

    private static void expireMinigame(ProcessingMultiblock mb, Location anchor,
                                        FoodDecayConfig config) {
        mb.eventActive = false;
        mb.eventsMissed++;
        mb.qualityBonus -= config.getQteMissPenalty();
        cleanupBossBar(mb);

        World world = anchor.getWorld();
        Location center = anchor.clone().add(0.5, 1.5, 0.5);

        // Failure animation: items fall down with smoke
        animateFailureAndCleanup(mb, center, world);

        // Failure particles
        world.spawnParticle(Particle.SMOKE, center, 20, 0.5, 0.4, 0.5, 0.03);
        world.spawnParticle(Particle.LARGE_SMOKE, center, 5, 0.3, 0.3, 0.3, 0.02);

        // Failure sound
        world.playSound(anchor, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
        world.playSound(anchor, Sound.BLOCK_ANVIL_LAND, 0.2f, 0.5f);

        // Notify nearby players
        int radius = config.getNotificationRadius();
        if (radius > 0) {
            double rSq = radius * radius;
            for (Player p : world.getPlayers()) {
                if (p.getLocation().distanceSquared(anchor) <= rSq) {
                    String missedMsg = getMissedMessage(mb.type, mb.eventType, config);
                    p.sendActionBar(MessageUtils.toComponent(
                            sc(config.msg("qte-alert-actionbar")
                                    .replace("{message}", missedMsg))));
                    p.showTitle(Title.title(
                            MessageUtils.toComponent(sc("&c✘")),
                            MessageUtils.toComponent(sc(missedMsg)),
                            Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofMillis(300))));
                }
            }
        }
    }

    /**
     * Animates display entities falling/shrinking then removes them.
     */
    private static void animateFailureAndCleanup(ProcessingMultiblock mb,
                                                  Location center, World world) {
        for (Entity e : mb.eventDisplays) {
            if (e == null || !e.isValid()) continue;

            if (e instanceof ItemDisplay d) {
                d.setInterpolationDelay(0);
                d.setInterpolationDuration(15);
                float s = 0.05f;
                d.setTransformation(new Transformation(
                        new Vector3f(-s / 2f, -0.5f, -s / 2f),
                        new AxisAngle4f((float) (Math.random() * Math.PI), 1, 0, 0),
                        new Vector3f(s, s, s),
                        new AxisAngle4f(0, 0, 0, 1)
                ));
            } else if (e instanceof TextDisplay txt) {
                txt.setInterpolationDelay(0);
                txt.setInterpolationDuration(10);
                txt.setText("§c✘");
                txt.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, 0),
                        new AxisAngle4f(0, 0, 0, 1),
                        new Vector3f(1, 1, 1),
                        new AxisAngle4f(0, 0, 0, 1)
                ));
            }
        }
        // Schedule removal
        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("FoodDecay"), () -> {
                    for (Entity e : mb.eventDisplays) {
                        if (e != null && e.isValid()) e.remove();
                    }
                    mb.eventDisplays.clear();
                }, 20L);
    }

    // =========================================================================
    //  BossBar Management
    // =========================================================================

    private static void cleanupBossBar(ProcessingMultiblock mb) {
        if (mb.eventBossBar != null) {
            mb.eventBossBar.removeAll();
            mb.eventBossBar = null;
        }
    }

    private static void updateBossBarPlayers(ProcessingMultiblock mb, Location anchor,
                                              FoodDecayConfig config) {
        if (mb.eventBossBar == null) return;
        int radius = config.getNotificationRadius();
        if (radius <= 0) return;
        double rSq = radius * radius;
        World world = anchor.getWorld();
        for (Player p : world.getPlayers()) {
            double distSq = p.getLocation().distanceSquared(anchor);
            if (distSq <= rSq) {
                if (!mb.eventBossBar.getPlayers().contains(p)) {
                    mb.eventBossBar.addPlayer(p);
                }
            } else {
                mb.eventBossBar.removePlayer(p);
            }
        }
    }

    // =========================================================================
    //  Cleanup — called when processing resets or machine breaks
    // =========================================================================

    /**
     * Force-removes all minigame display entities and BossBar.
     * Call this when a machine is broken or processing is cancelled.
     */
    public static void forceCleanup(ProcessingMultiblock mb) {
        cleanupBossBar(mb);
        for (Entity e : mb.eventDisplays) {
            if (e != null && e.isValid()) e.remove();
        }
        mb.eventDisplays.clear();
        mb.eventActive = false;
    }

    // =========================================================================
    //  Event Messages (per machine type)
    // =========================================================================

    public static String getEventMessage(MultiblockType type, int eventId, FoodDecayConfig config) {
        return config.msg("qte-event-" + getMsgKey(type) + "-" + eventId);
    }

    public static String getMissedMessage(MultiblockType type, int eventId, FoodDecayConfig config) {
        return config.msg("qte-missed-" + getMsgKey(type) + "-" + eventId);
    }

    public static String getResolvedMessage(MultiblockType type, int eventId, FoodDecayConfig config) {
        return config.msg("qte-resolved-" + getMsgKey(type) + "-" + eventId);
    }

    private static String getMsgKey(MultiblockType type) {
        return type.getConfigKey()
                .replace("barril-de-sal", "salt")
                .replace("tina-de-conserva", "pickling")
                .replace("prensa-de-selagem", "sealing")
                .replace("defumeiro", "smokehouse")
                .replace("secador", "drying");
    }
}
