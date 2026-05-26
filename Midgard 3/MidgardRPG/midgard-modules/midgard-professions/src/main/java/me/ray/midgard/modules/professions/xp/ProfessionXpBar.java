package me.ray.midgard.modules.professions.xp;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.professions.ProfessionManager;
import me.ray.midgard.modules.professions.ProfessionProgress;
import me.ray.midgard.modules.professions.ProfessionType;
import me.ray.midgard.modules.professions.ProfessionsModule;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BossBar temporária que aparece ao ganhar XP de profissão.
 * Mostra progresso, enche gradualmente e muda de cor conforme %.
 * Some automaticamente após um tempo configurável.
 */
public class ProfessionXpBar implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ProfessionManager manager;
    private final Map<UUID, BarState> activeBars = new ConcurrentHashMap<>();

    // Configurações (recarregadas via reloadConfig)
    private volatile int displayTicks = 60;      // 3 segundos
    private volatile boolean enabled = true;

    public ProfessionXpBar(ProfessionManager manager) {
        this.manager = Objects.requireNonNull(manager);
        reloadConfig();
    }

    public void reloadConfig() {
        ProfessionsModule module = ProfessionsModule.getInstance();
        if (module == null) { return; }

        this.enabled = module.getConfig().getBoolean("professions.xp-bar.enabled", true);
        this.displayTicks = module.getConfig().getInt("professions.xp-bar.display-ticks", 60);
    }

    /**
     * Exibe ou atualiza a BossBar de XP para o jogador.
     * Chamado pelo listener de XP após premiar XP.
     */
    public void show(Player player, ProfessionType type, double xpGained) {
        if (!enabled || !player.isOnline()) { return; }

        try {
            ProfessionProgress progress = manager.getProgress(player, type);
            if (progress == null) { return; }

            UUID uuid = player.getUniqueId();
            BarState state = activeBars.get(uuid);

            float barProgress = (float) Math.min(1.0, Math.max(0.0, progress.getProgressPercent() / 100.0));
            BossBar.Color color = colorForProgress(barProgress);
            String title = buildTitle(type, progress, xpGained);

            if (state != null && state.type == type) {
                // Atualizar bar existente da mesma profissão
                state.bar.name(MM.deserialize(title));
                state.bar.progress(barProgress);
                state.bar.color(color);
                state.resetTimer(player, displayTicks);
            } else {
                // Nova profissão ou primeira vez — criar nova bar
                if (state != null) {
                    // Remover bar antiga de outra profissão
                    state.cancel();
                    player.hideBossBar(state.bar);
                }

                BossBar bar = BossBar.bossBar(
                        MM.deserialize(title),
                        barProgress,
                        color,
                        BossBar.Overlay.NOTCHED_20
                );

                BarState newState = new BarState(bar, type);
                activeBars.put(uuid, newState);
                player.showBossBar(bar);
                newState.resetTimer(player, displayTicks);
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao exibir XP bar para %s", player.getName(), e);
        }
    }

    private String buildTitle(ProfessionType type, ProfessionProgress progress, double xpGained) {
        ProfessionsModule module = ProfessionsModule.getInstance();
        if (module == null) {
            return type.getSymbol() + " " + type.getDisplayName();
        }

        String template = module.getMessage("professions.xp_bar");
        if (template == null || template.isEmpty()) {
            template = "%symbol% <white>%profession%</white> <gray>Nv.%level%</gray> <green>+%xp% XP</green> <gray>%current%/%max%</gray>";
        }

        return template
                .replace("%symbol%", type.getSymbol())
                .replace("%profession%", type.getDisplayName())
                .replace("%level%", String.valueOf(progress.getLevel()))
                .replace("%xp%", formatNumber(xpGained))
                .replace("%current%", formatNumber(progress.getXp()))
                .replace("%max%", formatNumber(progress.getXpToNextLevel()))
                .replace("%percent%", String.format("%.1f", progress.getProgressPercent()));
    }

    private static String formatNumber(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }

    /**
     * Cor da barra baseada no progresso:
     * 0-25% = RED, 25-50% = YELLOW, 50-75% = GREEN, 75-100% = BLUE
     */
    private static BossBar.Color colorForProgress(float progress) {
        if (progress < 0.25f) { return BossBar.Color.RED; }
        if (progress < 0.50f) { return BossBar.Color.YELLOW; }
        if (progress < 0.75f) { return BossBar.Color.GREEN; }
        return BossBar.Color.BLUE;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BarState state = activeBars.remove(event.getPlayer().getUniqueId());
        if (state != null) {
            state.cancel();
            event.getPlayer().hideBossBar(state.bar);
        }
    }

    public void shutdown() {
        for (var entry : activeBars.entrySet()) {
            BarState state = entry.getValue();
            state.cancel();
            Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(state.bar);
            }
        }
        activeBars.clear();
    }

    /**
     * Estado de uma BossBar ativa para um jogador.
     */
    private final class BarState {
        final BossBar bar;
        final ProfessionType type;
        BukkitTask hideTask;

        BarState(BossBar bar, ProfessionType type) {
            this.bar = bar;
            this.type = type;
        }

        void resetTimer(Player player, int delayTicks) {
            if (hideTask != null) {
                hideTask.cancel();
            }

            hideTask = Task.syncLater(player, () -> {
                try {
                    cleanup(player);
                } catch (Exception e) {
                    cleanup(player);
                }
            }, delayTicks);
        }

        void cleanup(Player player) {
            cancel();
            if (player.isOnline()) {
                player.hideBossBar(bar);
            }
            activeBars.remove(player.getUniqueId());
        }

        void cancel() {
            if (hideTask != null) {
                hideTask.cancel();
                hideTask = null;
            }
        }
    }
}
