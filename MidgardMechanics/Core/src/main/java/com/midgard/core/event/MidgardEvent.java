package com.midgard.core.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Base class for custom Midgard events.
 * <p>
 * Subclasses <b>must</b> declare their own static {@link HandlerList} and
 * override {@link #getHandlers()} plus provide a static {@code getHandlerList()}
 * method. This is required by Bukkit's event system — a shared HandlerList
 * causes all subclass events to fire on the same handler set.
 * </p>
 *
 * <pre>{@code
 * public class MyEvent extends MidgardEvent {
 *     private static final HandlerList HANDLERS = new HandlerList();
 *
 *     @Override public HandlerList getHandlers() { return HANDLERS; }
 *     public static HandlerList getHandlerList() { return HANDLERS; }
 * }
 * }</pre>
 */
public abstract class MidgardEvent extends Event {

    protected MidgardEvent() {
        super();
    }

    protected MidgardEvent(boolean async) {
        super(async);
    }

    @Override
    public abstract HandlerList getHandlers();
}
