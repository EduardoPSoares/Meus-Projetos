package me.ray.midgard.modules.professions.blacksmith.forge.minigame;

import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.session.ForgeSession;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

/**
 * World-based hammering mini-game.
 *
 * The player physically right-clicks the anvil in the world.
 * A BossBar shows a timing indicator that cycles left-to-right.
 * The player must click when the indicator is in the target zone.
 *
 * Visual feedback: particles on the anvil, ActionBar messages,
 * BossBar color changes, and world sounds.
 *
 * - PERFECT zone (green): center of the bar, narrow window
 * - GOOD zone (yellow): around the perfect zone
 * - MISS: outside zones entirely
 */
public class HammeringMinigame implements ForgeMinigame {

    private static String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }

    private static final MiniMessage mm = MiniMessage.miniMessage();

    // Timing zones (as fractions of the 0-1 BossBar)
    private static final float PERFECT_HALF_WIDTH = 0.06f;
    private static final float GOOD_HALF_WIDTH = 0.14f;

    // Cursor speed (fraction per tick — higher = faster = harder)
    private static final float BASE_SPEED = 0.025f;

    private final ForgeSession session;
    private final int requiredStrikes;
    private final double difficultyMultiplier;

    private boolean active;
    private BukkitTask tickTask;
    private BossBar bossBar;

    // Cursor state: oscillates 0→1→0→1...
    private float cursor;
    private int direction; // +1 or -1
    private float speed;

    // Target zone center (randomized each strike)
    private float targetCenter;

    // Stats
    private int strikesCompleted;

    // World anvil location for effects
    private Location anvilLocation;

    private BiConsumer<Player, ForgeSession> onComplete;

    public HammeringMinigame(ForgeSession session) {
        this.session = session;
        this.requiredStrikes = session.getRecipe().getEffectiveHammerStrikes();
        this.difficultyMultiplier = session.getRecipe().getDifficultyMultiplier();
        this.speed = (float) (BASE_SPEED * difficultyMultiplier);
        this.cursor = 0f;
        this.direction = 1;
        this.strikesCompleted = 0;
    }

    /**
     * Sets the anvil location for world-based particle effects.
     */
    public void setAnvilLocation(Location anvilLocation) {
        this.anvilLocation = anvilLocation;
    }

    @Override
    public void start(Player player, ForgeSession session) {
        this.active = true;

        // Randomize first target
        randomizeTarget();

        this.bossBar = BossBar.bossBar(
                mm.deserialize(buildBarText()),
                cursor,
                BossBar.Color.WHITE,
                BossBar.Overlay.NOTCHED_20
        );
        player.showBossBar(bossBar);

        player.sendMessage(mm.deserialize(msg("forge.hammering.instruction")));
        player.sendMessage(mm.deserialize(msg("forge.hammering.required_strikes").replace("%count%", String.valueOf(requiredStrikes))));

        // Start tick
        tickTask = Task.syncTimer(() -> tick(player, session), 1L, 1L);
    }

    @Override
    public void onAction(Player player, ForgeSession session, int slot) {
        if (!active) { return; }

        float dist = Math.abs(cursor - targetCenter);

        ForgeSession.StrikeResult result;
        if (dist <= PERFECT_HALF_WIDTH) {
            // Perfect hit
            result = ForgeSession.StrikeResult.PERFECT;
            session.recordHammerStrike(result);
            strikesCompleted++;

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.5f);
            player.sendActionBar(mm.deserialize(msg("forge.hammering.perfect_actionbar")
                    .replace("%done%", String.valueOf(strikesCompleted))
                    .replace("%total%", String.valueOf(requiredStrikes))));

            if (anvilLocation != null) {
                World world = anvilLocation.getWorld();
                if (world != null) {
                    Location effectLoc = anvilLocation.clone().add(0.5, 1.2, 0.5);
                    world.spawnParticle(Particle.ENCHANT, effectLoc, 20, 0.3, 0.4, 0.3, 0.5);
                    world.spawnParticle(Particle.FLAME, effectLoc, 10, 0.2, 0.1, 0.2, 0.05);
                }
            }
        } else if (dist <= GOOD_HALF_WIDTH) {
            // Good hit
            result = ForgeSession.StrikeResult.GOOD;
            session.recordHammerStrike(result);
            strikesCompleted++;

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
            player.sendActionBar(mm.deserialize(msg("forge.hammering.good_actionbar")
                    .replace("%done%", String.valueOf(strikesCompleted))
                    .replace("%total%", String.valueOf(requiredStrikes))));

            if (anvilLocation != null) {
                World world = anvilLocation.getWorld();
                if (world != null) {
                    Location effectLoc = anvilLocation.clone().add(0.5, 1.0, 0.5);
                    world.spawnParticle(Particle.CRIT, effectLoc, 12, 0.2, 0.2, 0.2, 0.1);
                }
            }
        } else {
            // Miss
            result = ForgeSession.StrikeResult.MISS;
            session.recordHammerStrike(result);
            strikesCompleted++;

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.8f);
            player.sendActionBar(mm.deserialize(msg("forge.hammering.miss_actionbar")
                    .replace("%done%", String.valueOf(strikesCompleted))
                    .replace("%total%", String.valueOf(requiredStrikes))));

            if (anvilLocation != null) {
                World world = anvilLocation.getWorld();
                if (world != null) {
                    Location effectLoc = anvilLocation.clone().add(0.5, 1.0, 0.5);
                    world.spawnParticle(Particle.SMOKE, effectLoc, 8, 0.3, 0.2, 0.3, 0.02);
                }
            }
        }

        // Check completion
        if (strikesCompleted >= requiredStrikes) {
            finish(player);
        } else {
            // Randomize target for next strike
            randomizeTarget();
            // Slightly increase speed after each strike
            speed = Math.min(0.06f, speed + 0.001f);
        }
    }

    @Override
    public void tick(Player player, ForgeSession session) {
        if (!active) { return; }

        // Move cursor
        cursor += speed * direction;
        if (cursor >= 1.0f) {
            cursor = 1.0f;
            direction = -1;
        } else if (cursor <= 0.0f) {
            cursor = 0.0f;
            direction = 1;
        }

        // Update BossBar
        bossBar.progress(Math.max(0f, Math.min(1f, cursor)));

        float dist = Math.abs(cursor - targetCenter);
        if (dist <= PERFECT_HALF_WIDTH) {
            bossBar.color(BossBar.Color.GREEN);
        } else if (dist <= GOOD_HALF_WIDTH) {
            bossBar.color(BossBar.Color.YELLOW);
        } else {
            bossBar.color(BossBar.Color.WHITE);
        }
        bossBar.name(mm.deserialize(buildBarText()));

        // Ambient anvil particles while active
        if (anvilLocation != null && ThreadLocalRandom.current().nextInt(10) == 0) {
            World world = anvilLocation.getWorld();
            if (world != null) {
                Location effectLoc = anvilLocation.clone().add(0.5, 0.8, 0.5);
                world.spawnParticle(Particle.FLAME, effectLoc, 2, 0.1, 0.05, 0.1, 0.01);
            }
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
        session.calculateHammeringScore();
        return session.getHammeringScore();
    }

    @Override
    public boolean isActive() { return active; }

    @Override
    public String getType() { return "hammering"; }

    public void setOnComplete(BiConsumer<Player, ForgeSession> callback) {
        this.onComplete = callback;
    }

    // === Private ===

    private void finish(Player player) {
        double score = stop(player, session);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.sendMessage(mm.deserialize(msg("forge.hammering.completed")
                .replace("%score%", String.format("%.0f%%", score * 100))));
        player.sendMessage(mm.deserialize(msg("forge.hammering.summary")
                .replace("%perfect%", String.valueOf(session.getPerfectStrikes()))
                .replace("%good%", String.valueOf(session.getGoodStrikes()))
                .replace("%miss%", String.valueOf(session.getMissedStrikes()))));

        if (onComplete != null) {
            Task.syncLater(() -> onComplete.accept(player, session), 30L);
        }
    }

    private void randomizeTarget() {
        // Keep target away from edges
        targetCenter = 0.2f + ThreadLocalRandom.current().nextFloat() * 0.6f;
    }

    private String buildBarText() {
        float dist = Math.abs(cursor - targetCenter);
        String zoneLabel;
        if (dist <= PERFECT_HALF_WIDTH) {
            zoneLabel = msg("minigame.hammering.bar_perfect");
        } else if (dist <= GOOD_HALF_WIDTH) {
            zoneLabel = msg("minigame.hammering.bar_good");
        } else {
            zoneLabel = msg("minigame.hammering.bar_miss");
        }

        return msg("minigame.hammering.bar_title") + " " + zoneLabel + " <gray>| <white>" +
                strikesCompleted + "/" + requiredStrikes;
    }
}
