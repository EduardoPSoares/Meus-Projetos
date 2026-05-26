package me.ray.midgard.modules.professions.blacksmith.forge.event;

import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryStructure;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Evento disparado quando uma nova smeltery é detectada e registrada.
 */
public class SmelteryActivateEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final SmelteryStructure smeltery;

    public SmelteryActivateEvent(Player player, SmelteryStructure smeltery) {
        this.player = player;
        this.smeltery = smeltery;
    }

    public Player getPlayer() { return player; }
    public SmelteryStructure getSmeltery() { return smeltery; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }
}
