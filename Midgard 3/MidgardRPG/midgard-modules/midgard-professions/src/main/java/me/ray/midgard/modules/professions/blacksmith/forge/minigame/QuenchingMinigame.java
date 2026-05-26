package me.ray.midgard.modules.professions.blacksmith.forge.minigame;

import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.session.ForgeSession;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.BiConsumer;

/**
 * Quenching mini-game — world-based BossBar timing game.
 * 
 * A BossBar acts as a thermometer dropping from max to zero.
 * The player must right-click a cauldron when the temperature
 * is within the ideal zone to achieve the best score.
 * Multiple quench attempts (dips) can be made for the best result.
 */
public class QuenchingMinigame implements ForgeMinigame {

    private static String msg(String key) { return ProfessionsModule.getInstance().getMessage("minigame.quenching." + key); }

    private static final float IDEAL_MIN = 0.30f; // Lower bound of ideal zone
    private static final float IDEAL_MAX = 0.55f; // Upper bound  of ideal zone
    private static final float PERFECT_MIN = 0.38f;
    private static final float PERFECT_MAX = 0.47f;
    private static final float COOLING_RATE = 0.015f;  // Per tick (0.75s at 15 tick interval)
    private static final int MAX_DIPS = 3;
    private static final int TICK_INTERVAL = 3; // ticks between temperature drops

    private final ForgeSession session;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private boolean active;
    private BukkitTask tickTask;
    private BossBar bossBar;

    private float temperature;  // 0.0 to 1.0
    private int dipsUsed;
    private double bestScore;
    private boolean waitingForDip;

    // World cauldron location for effects
    private Location cauldronLocation;

    private BiConsumer<Player, ForgeSession> onComplete;

    public QuenchingMinigame(ForgeSession session) {
        this.session = session;
        this.temperature = 1.0f;
        this.dipsUsed = 0;
        this.bestScore = 0;
        this.waitingForDip = true;
    }

    /**
     * Sets the cauldron location for world-based particle effects.
     */
    public void setCauldronLocation(Location cauldronLocation) {
        this.cauldronLocation = cauldronLocation;
    }

    @Override
    public void start(Player player, ForgeSession session) {
        this.active = true;

        this.bossBar = BossBar.bossBar(
                miniMessage.deserialize(msg("start_bossbar")),
                1.0f,
                BossBar.Color.RED,
                BossBar.Overlay.NOTCHED_10
        );
        player.showBossBar(bossBar);

        player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1f, 0.5f);

        tickTask = Task.syncTimer(() -> tick(player, session), 10L, TICK_INTERVAL);
    }

    @Override
    public void onAction(Player player, ForgeSession session, int slot) {
        if (!active || !waitingForDip) { return; }

        // Player dips the metal
        dipsUsed++;
        waitingForDip = false;

        double dipScore = calculateDipScore(temperature);
        if (dipScore > bestScore) {
            bestScore = dipScore;
        }

        // Visual feedback
        if (dipScore >= 0.9) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.8f);
            player.sendActionBar(miniMessage.deserialize(msg("result_perfect").replace("%score%", String.format("%.0f%%", dipScore * 100))));
        } else if (dipScore >= 0.6) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
            player.sendActionBar(miniMessage.deserialize(msg("result_good").replace("%score%", String.format("%.0f%%", dipScore * 100))));
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.8f);
            player.sendActionBar(miniMessage.deserialize((temperature > IDEAL_MAX ? msg("result_too_hot") : msg("result_too_cold")).replace("%score%", String.format("%.0f%%", dipScore * 100))));
        }

        player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1f, 1f);

        // Steam particles at cauldron
        if (cauldronLocation != null) {
            World world = cauldronLocation.getWorld();
            if (world != null) {
                Location effectLoc = cauldronLocation.clone().add(0.5, 1.0, 0.5);
                world.spawnParticle(Particle.CLOUD, effectLoc, 15, 0.3, 0.5, 0.3, 0.03);
                world.spawnParticle(Particle.SPLASH, effectLoc.clone().add(0, -0.3, 0), 8, 0.2, 0.1, 0.2, 0.1);
            }
        }

        if (dipsUsed >= MAX_DIPS || dipScore >= 0.95) {
            // Finish
            Task.syncLater(() -> finish(player), 40L);
        } else {
            // Allow another dip after cooldown
            Task.syncLater(() -> {
                if (active) {
                    temperature = Math.min(1.0f, temperature + 0.3f); // Reheat a bit
                    waitingForDip = true;
                    player.sendActionBar(miniMessage.deserialize(msg("reheated").replace("%remaining%", String.valueOf(MAX_DIPS - dipsUsed))));
                }
            }, 40L);
        }
    }

    @Override
    public void tick(Player player, ForgeSession session) {
        if (!active) { return; }

        // Cool down
        temperature = Math.max(0.0f, temperature - COOLING_RATE);

        // Update boss bar
        bossBar.progress(temperature);
        updateBossBarAppearance();

        // If temperature reaches 0, auto-finish with current best
        if (temperature <= 0.01f) {
            if (bestScore <= 0) {
                bestScore = 0.1; // Minimum score for doing nothing
            }
            finish(player);
        }
    }

    @Override
    public double stop(Player player, ForgeSession session) {
        active = false;
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
        session.setQuenchingScore(bestScore);
        return bestScore;
    }

    @Override
    public boolean isActive() { return active; }

    @Override
    public String getType() { return "quenching"; }

    public void setOnComplete(BiConsumer<Player, ForgeSession> callback) {
        this.onComplete = callback;
    }

    // === Private ===

    private void finish(Player player) {
        double score = stop(player, session);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        player.sendActionBar(miniMessage.deserialize(msg("completed").replace("%score%", String.format("%.0f%%", score * 100))));

        if (onComplete != null) {
            Task.syncLater(() -> onComplete.accept(player, session), 20L);
        }
    }

    private double calculateDipScore(float temp) {
        if (temp >= PERFECT_MIN && temp <= PERFECT_MAX) {
            return 1.0;
        } else if (temp >= IDEAL_MIN && temp <= IDEAL_MAX) {
            // Linear interpolation within ideal zone
            float center = (IDEAL_MIN + IDEAL_MAX) / 2f;
            float dist = Math.abs(temp - center);
            float halfRange = (IDEAL_MAX - IDEAL_MIN) / 2f;
            return 0.7 + 0.3 * (1.0 - dist / halfRange);
        } else if (temp > IDEAL_MAX) {
            // Too hot
            float excess = temp - IDEAL_MAX;
            return Math.max(0.1, 0.7 - excess * 1.5);
        } else {
            // Too cold
            float deficit = IDEAL_MIN - temp;
            return Math.max(0.1, 0.5 - deficit * 1.2);
        }
    }

    private void updateBossBarAppearance() {
        BossBar.Color color;
        String label;

        if (temperature >= PERFECT_MIN && temperature <= PERFECT_MAX) {
            color = BossBar.Color.GREEN;
            label = msg("zone_perfect");
        } else if (temperature >= IDEAL_MIN && temperature <= IDEAL_MAX) {
            color = BossBar.Color.YELLOW;
            label = msg("zone_ideal");
        } else if (temperature > IDEAL_MAX) {
            color = BossBar.Color.RED;
            label = msg("zone_too_hot");
        } else {
            color = BossBar.Color.BLUE;
            label = msg("zone_cold");
        }

        bossBar.color(color);
        bossBar.name(miniMessage.deserialize(label));
    }
}
