package me.ray.midgard.modules.professions.blacksmith.forge.event;

import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.MoltenMetal;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryStructure;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Evento disparado quando uma liga (alloy) é formada automaticamente na smeltery.
 */
public class AlloyFormedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final SmelteryStructure smeltery;
    private final MoltenMetal alloy;
    private final int amountProduced;

    public AlloyFormedEvent(SmelteryStructure smeltery, MoltenMetal alloy, int amountProduced) {
        this.smeltery = smeltery;
        this.alloy = alloy;
        this.amountProduced = amountProduced;
    }

    public SmelteryStructure getSmeltery() { return smeltery; }
    public MoltenMetal getAlloy() { return alloy; }
    public int getAmountProduced() { return amountProduced; }
    public UUID getOwnerUuid() { return smeltery.getOwnerUuid(); }

    @NotNull
    @Override
    public HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }
}
