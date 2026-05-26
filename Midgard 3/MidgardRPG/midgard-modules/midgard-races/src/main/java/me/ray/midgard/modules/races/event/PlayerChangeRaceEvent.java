package me.ray.midgard.modules.races.event;

import me.ray.midgard.modules.races.model.Race;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerChangeRaceEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Race oldRace;
    private final Race newRace;
    private boolean cancelled;

    public PlayerChangeRaceEvent(Player player, Race oldRace, Race newRace) {
        super(player);
        this.oldRace = oldRace;
        this.newRace = newRace;
    }

    public Race getOldRace() {
        return oldRace;
    }

    public Race getNewRace() {
        return newRace;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
