package me.ray.midgard.core.combat.event;

import me.ray.midgard.core.combat.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MidgardDamageEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final LivingEntity attacker;
    private final LivingEntity victim;
    private double damage;
    private final double originalDamage;
    private final DamageType type;
    private boolean isCritical;
    private boolean isCancelled;

    public MidgardDamageEvent(LivingEntity attacker, LivingEntity victim, double damage, DamageType type, boolean isCritical) {
        this.attacker = attacker;
        this.victim = victim;
        this.damage = damage;
        this.originalDamage = damage;
        this.type = type;
        this.isCritical = isCritical;
    }

    public LivingEntity getAttacker() {
        return attacker;
    }

    public LivingEntity getVictim() {
        return victim;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getOriginalDamage() {
        return originalDamage;
    }

    public DamageType getType() {
        return type;
    }

    public boolean isCritical() {
        return isCritical;
    }

    public void setCritical(boolean critical) {
        isCritical = critical;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
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
