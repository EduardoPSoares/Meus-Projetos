package com.midgard.core.scoreboard;

import com.midgard.core.MidgardCore;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Manages per-player scoreboards with automatic refresh.
 */
public class ScoreboardManager {

    private final Map<UUID, ScoreboardBuilder> boards = new ConcurrentHashMap<>();
    private BukkitTask updateTask;
    private Function<Player, ScoreboardBuilder> boardProvider;

    public void setProvider(Function<Player, ScoreboardBuilder> provider) {
        this.boardProvider = provider;
    }

    public void show(Player player) {
        if (boardProvider == null) return;
        ScoreboardBuilder board = boardProvider.apply(player);
        boards.put(player.getUniqueId(), board);
        board.apply(player);
    }

    public void hide(Player player) {
        ScoreboardBuilder board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.remove(player);
        }
    }

    public void startAutoUpdate(long intervalTicks) {
        stopAutoUpdate();
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : boards.keySet()) {
                    Player player = org.bukkit.Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        show(player);
                    } else {
                        boards.remove(uuid);
                    }
                }
            }
        }.runTaskTimer(MidgardCore.getInstance(), intervalTicks, intervalTicks);
    }

    public void stopAutoUpdate() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }
    }

    public void clear() {
        boards.clear();
        stopAutoUpdate();
    }
}
