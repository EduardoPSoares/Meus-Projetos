package me.ray.midgard.modules.professions.blacksmith.forge.event;

import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.MoltenMetal;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryStructure;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Evento disparado quando um item termina de ser fundido na smeltery.
 */
public class SmeltingCompleteEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final SmelteryStructure smeltery;
    private final MoltenMetal metal;
    private final int amountProduced;

    public SmeltingCompleteEvent(SmelteryStructure smeltery, MoltenMetal metal, int amountProduced) {
        this.smeltery = smeltery;
        this.metal = metal;
        this.amountProduced = amountProduced;
    }

    public SmelteryStructure getSmeltery() { return smeltery; }
    public MoltenMetal getMetal() { return metal; }
    public int getAmountProduced() { return amountProduced; }
    public UUID getOwnerUuid() { return smeltery.getOwnerUuid(); }

    @NotNull
    @Override
    public HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }
}
