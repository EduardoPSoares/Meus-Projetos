package me.ray.midgard.modules.professions.blacksmith.forge.display;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.core.utils.TeleportUtils;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeStage;
import me.ray.midgard.modules.professions.blacksmith.forge.quality.QualityTier;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeStructure;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages contextual TextDisplay holograms above forge blocks.
 *
 * Design rules to avoid visual pollution:
 * - Only ONE hologram per player at a time (at the currently active block)
 * - Small scale, subtle background, auto-fade
 * - Positioned precisely above the relevant block
 * - Each hologram has a purpose and lifespan tied to the current stage
 */
public class ForgeHologram {

    private static String msg(String key) { return ProfessionsModule.getInstance().getMessage("display.hologram." + key); }

    // Active hologram entity per player (only one at a time)
    private final Map<UUID, TextDisplay> activeDisplays = new ConcurrentHashMap<>();
    // Animation tasks per player
    private final Map<UUID, BukkitTask> animationTasks = new ConcurrentHashMap<>();

    /**
     * Shows a stage-contextual hologram above the relevant forge block.
     * Automatically removes any previous hologram for this player.
     */
    public void showStageHologram(UUID playerId, ForgeStructure forge, ForgeStage stage) {
        removeHologram(playerId);

        Location loc = getLocationForStage(forge, stage);
        if (loc == null) { return; }

        String text = getStageText(stage);
        if (text == null) { return; }

        spawnHologram(playerId, loc, text, 0.8f, false, -1);
    }

    /**
     * Shows a dynamic temperature hologram above the furnace during heating.
     * Updated externally by the heating phase timer.
     */
    public void showTemperature(UUID playerId, ForgeStructure forge, double temp, double idealMin, double idealMax) {
        Location loc = getBlockLocation(forge, ForgeBlock.ForgeBlockType.FURNACE);
        if (loc == null) { return; }

        String color;
        if (temp >= idealMin && temp <= idealMax) {
            color = "<green>";
        } else if (temp < idealMin) {
            color = "<yellow>";
        } else {
            color = "<red>";
        }

        String text = color + String.format("%.0f°C", temp);

        TextDisplay existing = activeDisplays.get(playerId);
        if (existing != null && existing.isValid()) {
            // Update on the entity's owning region thread (Folia requirement)
            Task.sync(existing, () -> {
                if (!existing.isValid()) { return; }
                existing.text(MessageUtils.parse(text));
            });
        } else {
            removeHologram(playerId);
            spawnHologram(playerId, loc, text, 0.7f, false, -1);
        }
    }

    /**
     * Shows a brief strike result hologram above the anvil (auto-fading ~1s).
     */
    public void showStrikeResult(UUID playerId, ForgeStructure forge, String result) {
        removeHologram(playerId);

        Location loc = getBlockLocation(forge, ForgeBlock.ForgeBlockType.ANVIL);
        if (loc == null) { return; }

        spawnHologram(playerId, loc, result, 1.0f, true, 25);
    }

    /**
     * Shows the final quality result with a pop-in animation above the forge.
     */
    public void showCompletionResult(UUID playerId, ForgeStructure forge, QualityTier tier, double score) {
        removeHologram(playerId);

        Location loc = forge.getAnchorLocation();
        if (loc == null) { return; }

        Location spawnLoc = loc.clone().add(0.5, 2.5, 0.5);

        String text = tier.getColorTag() + "<bold>" + tier.getName() + "</bold>\n" +
                "<gray>" + String.format("%.0f%%", score * 100);

        Task.sync(spawnLoc, () -> {
            try {
                spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, display -> {
                    display.text(MessageUtils.parse(text));
                    display.setBillboard(Display.Billboard.CENTER);
                    display.setSeeThrough(false);
                    display.setShadowed(true);
                    display.setBackgroundColor(Color.fromARGB(160, 20, 20, 20));
                    display.setAlignment(TextDisplay.TextAlignment.CENTER);
                    display.setLineWidth(200);

                    // Start tiny, will animate up
                    display.setTransformation(new Transformation(
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f(),
                            new Vector3f(0f, 0f, 0f),
                            new AxisAngle4f()
                    ));
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(5);

                    activeDisplays.put(playerId, display);

                    // Phase 1: Pop in (5 ticks)
                    Task.syncLater(display, () -> {
                        if (!display.isValid()) { return; }
                        display.setTransformation(new Transformation(
                                new Vector3f(0, 0.4f, 0),
                                new AxisAngle4f(),
                                new Vector3f(1.5f, 1.5f, 1.5f),
                                new AxisAngle4f()
                        ));
                    }, 1L);

                    // Phase 2: Settle (after pop)
                    Task.syncLater(display, () -> {
                        if (!display.isValid()) { return; }
                        display.setInterpolationDelay(0);
                        display.setInterpolationDuration(10);
                        display.setTransformation(new Transformation(
                                new Vector3f(0, 0.5f, 0),
                                new AxisAngle4f(),
                                new Vector3f(1.2f, 1.2f, 1.2f),
                                new AxisAngle4f()
                        ));
                    }, 6L);

                    // Phase 3: Fade out after 4 seconds
                    BukkitTask fadeTask = Task.syncTimer(display, new Runnable() {
                        int tick = 0;
                        @Override
                        public void run() {
                            if (!display.isValid()) { return; }
                            tick++;
                            if (tick > 80) { // 4 seconds
                                int fadeStep = tick - 80;
                                int alpha = Math.max(0, 255 - (fadeStep * 25));
                                display.setTextOpacity((byte) alpha);
                                if (alpha <= 0) {
                                    display.remove();
                                    activeDisplays.remove(playerId);
                                    BukkitTask self = animationTasks.remove(playerId);
                                    if (self != null) { self.cancel(); }
                                }
                            }
                        }
                    }, 1L, 1L);
                    animationTasks.put(playerId, fadeTask);
                });
            } catch (Exception ignored) { /* animation best-effort */ }
        });
    }

    /**
     * Removes the active hologram for a player.
     */
    public void removeHologram(UUID playerId) {
        BukkitTask task = animationTasks.remove(playerId);
        if (task != null) { task.cancel(); }

        TextDisplay display = activeDisplays.remove(playerId);
        if (display != null && display.isValid()) {
            Task.sync(display, () -> {
                if (display.isValid()) { display.remove(); }
            });
        }
    }

    /**
     * Removes all holograms — called on shutdown.
     */
    public void removeAll() {
        for (UUID id : activeDisplays.keySet()) {
            removeHologram(id);
        }
        activeDisplays.clear();
        animationTasks.clear();
    }

    // === Private helpers ===

    private void spawnHologram(UUID playerId, Location blockLoc, String text, float scale, boolean autoFade, int lifeTicks) {
        Location spawnLoc = blockLoc.clone().add(0.5, 3.4, 0.5);

        Task.sync(spawnLoc, () -> {
            try {
                spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, display -> {
                    display.text(MessageUtils.parse(text));
                    display.setBillboard(Display.Billboard.CENTER);
                    display.setSeeThrough(false);
                    display.setShadowed(true);
                    display.setBackgroundColor(Color.fromARGB(140, 15, 15, 15));
                    display.setAlignment(TextDisplay.TextAlignment.CENTER);
                    display.setLineWidth(200);

                    Transformation transform = new Transformation(
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f(),
                            new Vector3f(scale, scale, scale),
                            new AxisAngle4f()
                    );
                    display.setTransformation(transform);

                    activeDisplays.put(playerId, display);

                    if (autoFade && lifeTicks > 0) {
                        BukkitTask fadeTask = Task.syncTimer(display, new Runnable() {
                            int tick = 0;
                            @Override
                            public void run() {
                                if (!display.isValid()) {
                                    BukkitTask self = animationTasks.remove(playerId);
                                    if (self != null) { self.cancel(); }
                                    return;
                                }
                                tick++;
                                if (tick >= lifeTicks) {
                                    // Fade over 5 ticks
                                    int fadeStep = tick - lifeTicks;
                                    int alpha = Math.max(0, 255 - (fadeStep * 50));
                                    display.setTextOpacity((byte) alpha);
                                    if (alpha <= 0) {
                                        display.remove();
                                        activeDisplays.remove(playerId);
                                        BukkitTask self = animationTasks.remove(playerId);
                                        if (self != null) { self.cancel(); }
                                    }
                                }
                            }
                        }, 1L, 1L);
                        animationTasks.put(playerId, fadeTask);
                    }
                });
            } catch (Exception ignored) { /* animation best-effort */ }
        });
    }

    private Location getLocationForStage(ForgeStructure forge, ForgeStage stage) {
        ForgeBlock.ForgeBlockType type = switch (stage) {
            case HEATING -> ForgeBlock.ForgeBlockType.FURNACE;
            case HAMMERING -> ForgeBlock.ForgeBlockType.ANVIL;
            case QUENCHING -> ForgeBlock.ForgeBlockType.CAULDRON;
            case SHARPENING -> ForgeBlock.ForgeBlockType.GRINDSTONE;
            default -> ForgeBlock.ForgeBlockType.SMITHING_TABLE;
        };
        return getBlockLocation(forge, type);
    }

    private Location getBlockLocation(ForgeStructure forge, ForgeBlock.ForgeBlockType type) {
        if (forge.getInteractiveLocations() == null) { return null; }
        return forge.getInteractiveLocations().get(type);
    }

    private String getStageText(ForgeStage stage) {
        return switch (stage) {
            case HEATING -> "<red>🔥 <gray>" + msg("heating");
            case HAMMERING -> "<gold>⚒ <gray>" + msg("hammering");
            case QUENCHING -> "<aqua>💧 <gray>" + msg("quenching");
            case SHARPENING -> "<yellow>🔪 <gray>" + msg("sharpening");
            case FINALIZING -> "<light_purple>✨ <gray>" + msg("finalizing");
            default -> null;
        };
    }
}
