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
 * World-based sharpening mini-game.
 *
 * The player physically right-clicks the grindstone in the world.
 * A BossBar shows a pressure gauge. Each click increases pressure,
 * but it naturally decays. The player must keep the pressure within
 * a target zone for as long as possible across multiple passes.
 *
 * Visual feedback: particles on the grindstone, ActionBar pressure readout,
 * BossBar color changes, and world sounds.
 */
public class SharpeningMinigame implements ForgeMinigame {

    private static String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final int TICKS_PER_PASS = 100; // 5 seconds per pass

    private final ForgeSession session;
    private final int requiredPasses;

    private boolean active;
    private BukkitTask tickTask;
    private BossBar bossBar;

    // Pressure gauge state
    private double pressure;       // 0.0 to 1.0
    private double targetMin;
    private double targetMax;
    private double pressureVelocity;
    private boolean playerPressing;
    private int ticksInZone;
    private int ticksTotal;

    // Pass tracking
    private int currentPass;
    private double totalScore;

    // World grindstone location for effects
    private Location grindstoneLocation;

    private BiConsumer<Player, ForgeSession> onComplete;

    public SharpeningMinigame(ForgeSession session) {
        this.session = session;
        this.requiredPasses = session.getRecipe().getSharpeningPasses() > 0 ?
                session.getRecipe().getSharpeningPasses() : 3;
        this.pressure = 0.5;
        this.currentPass = 0;
        this.totalScore = 0;
    }

    /**
     * Sets the grindstone location for world-based particle effects.
     */
    public void setGrindstoneLocation(Location grindstoneLocation) {
        this.grindstoneLocation = grindstoneLocation;
    }

    @Override
    public void start(Player player, ForgeSession session) {
        this.active = true;

        setupNewPass();

        this.bossBar = BossBar.bossBar(
                mm.deserialize(buildBarText()),
                (float) pressure,
                BossBar.Color.WHITE,
                BossBar.Overlay.NOTCHED_20
        );
        player.showBossBar(bossBar);

        player.sendMessage(mm.deserialize(msg("forge.sharpening.instruction")));
        player.sendMessage(mm.deserialize(msg("forge.sharpening.required_passes").replace("%count%", String.valueOf(requiredPasses))));

        tickTask = Task.syncTimer(() -> tick(player, session), 1L, 2L);
    }

    @Override
    public void onAction(Player player, ForgeSession session, int slot) {
        if (!active) { return; }

        playerPressing = true;
        player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.3f, 1.2f);

        // Grindstone sparks on click
        if (grindstoneLocation != null) {
            World world = grindstoneLocation.getWorld();
            if (world != null) {
                Location effectLoc = grindstoneLocation.clone().add(0.5, 0.8, 0.5);
                world.spawnParticle(Particle.CRIT, effectLoc, 6, 0.15, 0.1, 0.15, 0.1);
            }
        }
    }

    @Override
    public void tick(Player player, ForgeSession session) {
        if (!active) { return; }

        ticksTotal++;

        // Apply pressure physics
        if (playerPressing) {
            pressure = Math.min(1.0, pressure + 0.06);
            playerPressing = false; // Must keep clicking
        } else {
            // Natural pressure drop
            pressure = Math.max(0.0, pressure - 0.03 + pressureVelocity);
        }

        // Random wobble
        pressureVelocity = ThreadLocalRandom.current().nextDouble(-0.008, 0.008);

        // Check if in target zone
        boolean inZone = pressure >= targetMin && pressure <= targetMax;
        if (inZone) {
            ticksInZone++;
        }

        // Update BossBar
        bossBar.progress((float) Math.max(0.0, Math.min(1.0, pressure)));

        if (inZone) {
            bossBar.color(BossBar.Color.GREEN);
        } else if (Math.abs(pressure - (targetMin + targetMax) / 2) < 0.2) {
            bossBar.color(BossBar.Color.YELLOW);
        } else {
            bossBar.color(BossBar.Color.RED);
        }
        bossBar.name(mm.deserialize(buildBarText()));

        // ActionBar feedback
        int progressPercent = TICKS_PER_PASS > 0 ? (ticksTotal * 100 / TICKS_PER_PASS) : 0;
        String zoneStatus = inZone ? msg("minigame.sharpening.in_zone") : msg("minigame.sharpening.out_zone");
        player.sendActionBar(mm.deserialize(msg("forge.sharpening.pass_actionbar")
                .replace("%current%", String.valueOf(currentPass + 1))
                .replace("%total%", String.valueOf(requiredPasses))
                .replace("%pressure%", progressPercent + "%")));

        // Check if pass is over
        if (ticksTotal >= TICKS_PER_PASS) {
            completePass(player);
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
        double finalScore = currentPass > 0 ? totalScore / currentPass : 0;
        session.setSharpeningScore(finalScore);
        return finalScore;
    }

    @Override
    public boolean isActive() { return active; }

    @Override
    public String getType() { return "sharpening"; }

    public void setOnComplete(BiConsumer<Player, ForgeSession> callback) {
        this.onComplete = callback;
    }

    // === Private ===

    private void completePass(Player player) {
        currentPass++;
        double passScore = TICKS_PER_PASS > 0 ? (double) ticksInZone / TICKS_PER_PASS : 0;
        totalScore += passScore;
        session.incrementCompletedPasses();

        if (passScore >= 0.8) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
            player.sendMessage(mm.deserialize(msg("forge.sharpening.pass_excellent")
                    .replace("%n%", String.valueOf(currentPass))
                    .replace("%score%", String.format("%.0f%%", passScore * 100))));
            if (grindstoneLocation != null) {
                World world = grindstoneLocation.getWorld();
                if (world != null) {
                    Location effectLoc = grindstoneLocation.clone().add(0.5, 1.2, 0.5);
                    world.spawnParticle(Particle.ENCHANT, effectLoc, 15, 0.3, 0.3, 0.3, 0.5);
                }
            }
        } else if (passScore >= 0.5) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1f);
            player.sendMessage(mm.deserialize(msg("forge.sharpening.pass_good")
                    .replace("%n%", String.valueOf(currentPass))
                    .replace("%score%", String.format("%.0f%%", passScore * 100))));
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.8f);
            player.sendMessage(mm.deserialize(msg("forge.sharpening.pass_weak")
                    .replace("%n%", String.valueOf(currentPass))
                    .replace("%score%", String.format("%.0f%%", passScore * 100))));
        }

        if (currentPass >= requiredPasses) {
            finish(player);
        } else {
            setupNewPass();
        }
    }

    private void finish(Player player) {
        double score = stop(player, session);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        player.sendMessage(mm.deserialize(msg("forge.sharpening.completed")
                .replace("%score%", String.format("%.0f%%", score * 100))));

        if (onComplete != null) {
            Task.syncLater(() -> onComplete.accept(player, session), 30L);
        }
    }

    private void setupNewPass() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double center = rng.nextDouble(0.25, 0.75);
        double halfRange = 0.12 - (currentPass * 0.02); // Gets tighter each pass
        targetMin = Math.max(0.0, center - halfRange);
        targetMax = Math.min(1.0, center + halfRange);

        pressure = 0.5;
        ticksInZone = 0;
        ticksTotal = 0;
        pressureVelocity = 0;
    }

    private String buildBarText() {
        boolean inZone = pressure >= targetMin && pressure <= targetMax;
        String zoneLabel;
        if (inZone) {
            zoneLabel = msg("minigame.sharpening.bar_in_zone");
        } else {
            zoneLabel = msg("minigame.sharpening.bar_out_zone");
        }

        double avgScore = currentPass > 0 ? (totalScore / currentPass) * 100 : 0;
        return msg("minigame.sharpening.bar_title") + " " + zoneLabel + " <gray>| " + msg("minigame.sharpening.bar_pass") + " <white>" +
                (currentPass + 1) + "/" + requiredPasses +
                " <gray>| " + msg("minigame.sharpening.bar_average") + " <white>" + String.format("%.0f%%", avgScore);
    }
}
