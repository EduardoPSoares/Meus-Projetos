package com.midgard.core.event;

import com.midgard.core.MidgardCore;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.util.function.Consumer;

/**
 * Utility for registering event listeners with lambda syntax.
 */
public final class Events {

    private Events() {
    }

    /**
     * Register an event listener with a lambda.
     * @return the Listener instance, useful for later unregistration
     */
    public static <T extends Event> Listener listen(Class<T> eventClass, Consumer<T> handler) {
        return listen(eventClass, EventPriority.NORMAL, false, handler);
    }

    public static <T extends Event> Listener listen(Class<T> eventClass, EventPriority priority, Consumer<T> handler) {
        return listen(eventClass, priority, false, handler);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Event> Listener listen(Class<T> eventClass, EventPriority priority,
                                                 boolean ignoreCancelled, Consumer<T> handler) {
        Listener listener = new Listener() {};
        EventExecutor executor = (l, event) -> {
            if (eventClass.isInstance(event)) {
                handler.accept((T) event);
            }
        };

        MidgardCore.getInstance().getServer().getPluginManager()
                .registerEvent(eventClass, listener, priority, executor, MidgardCore.getInstance(), ignoreCancelled);
        return listener;
    }

    /**
     * Unregister a specific listener.
     */
    public static void unregister(Listener listener) {
        HandlerList.unregisterAll(listener);
    }

    /**
     * Unregister all listeners for the core plugin.
     */
    public static void unregisterAll() {
        HandlerList.unregisterAll(MidgardCore.getInstance());
    }
}
