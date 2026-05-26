package me.ray.midgard.modules.races.event;

import me.ray.midgard.modules.races.model.Race;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerRaceLevelUpEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();
    private final Race race;
    private final int oldLevel;
    private final int newLevel;

    public PlayerRaceLevelUpEvent(Player player, Race race, int oldLevel, int newLevel) {
        super(player);
        this.race = race;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public Race getRace() {
        return race;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
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
