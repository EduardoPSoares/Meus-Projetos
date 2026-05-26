package me.ray.midgard.modules.spells.task;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.spells.SpellsModule;
import me.ray.midgard.modules.spells.obj.Spell;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;

import me.ray.midgard.core.utils.Task;

public class ChannelingTask implements Runnable {

    private final SpellsModule module;
    private final Player player;
    private final Spell spell;
    private final double maxTime;
    private double currentTime;
    private boolean running = false;
    private BukkitTask task;
    private BossBar bossBar;

    public ChannelingTask(SpellsModule module, Player player, Spell spell) {
        this.module = module;
        this.player = player;
        this.spell = spell;
        this.maxTime = spell.getCastTime();
        this.currentTime = 0;
    }

    public void start() {
        if (running) { return; }
        running = true;

        // Sinaliza para o CombatOverlay não sobrescrever a action bar
        player.setMetadata("midgard_channeling", new FixedMetadataValue(module.getPlugin(), true));

        // Create Adventure BossBar
        String title = module.getMessage("channeling.bossbar_title")
                .replace("%spell%", spell.getDisplayName())
                .replace("%time%", String.format("%.1fs", maxTime));
        bossBar = BossBar.bossBar(
                MessageUtils.parse(title),
                0.0f,
                BossBar.Color.YELLOW,
                BossBar.Overlay.PROGRESS
        );
        player.showBossBar(bossBar);

        // Run every 2 ticks (0.1s) — delay must be >= 1 for Paper's entity scheduler
        try {
            this.task = Task.syncTimer(player, this, 1L, 2L);
        } catch (Exception e) {
            // Timer failed to start — clean up bossbar and state
            cancel();
            module.getSpellManager().cancelChanneling(player, "error");
        }
    }

    public void cancel() {
        running = false;

        // Remove sinalização do CombatOverlay
        player.removeMetadata("midgard_channeling", module.getPlugin());

        if (bossBar != null) {
            player.hideBossBar(bossBar);
            bossBar = null;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void run() {
        if (!running || !player.isOnline()) {
            cancel();
            return;
        }

        currentTime += 0.1; // Task runs every 2 ticks (0.1s)

        if (currentTime >= maxTime) {
            finish();
            return;
        }

        updateFeedback();
    }

    private void updateFeedback() {
        double ratio = currentTime / maxTime;

        if (bossBar != null) {
            bossBar.progress((float) Math.min(1.0, ratio));

            // Change color based on progress
            if (ratio >= 0.75) {
                bossBar.color(BossBar.Color.GREEN);
            } else if (ratio >= 0.5) {
                bossBar.color(BossBar.Color.YELLOW);
            }

            String title = module.getMessage("channeling.bossbar_title")
                    .replace("%spell%", spell.getDisplayName())
                    .replace("%time%", String.format("%.1fs", maxTime - currentTime));
            bossBar.name(MessageUtils.parse(title));
        }

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.5f + (float) ratio);
    }

    private void finish() {
        cancel();
        module.getSpellManager().finishChanneling(player, spell);
    }

    public Spell getSpell() {
        return spell;
    }
}
