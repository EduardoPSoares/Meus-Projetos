package me.ray.midgard.bot.core.interaction;

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SelectMenuHandler {

    private static final Logger logger = LoggerFactory.getLogger(SelectMenuHandler.class);

    private final Map<String, Consumer<StringSelectInteractionEvent>> handlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<StringSelectInteractionEvent>> prefixHandlers = new ConcurrentHashMap<>();

    public void register(String id, Consumer<StringSelectInteractionEvent> handler) {
        handlers.put(id, handler);
    }

    public void registerPrefix(String prefix, Consumer<StringSelectInteractionEvent> handler) {
        prefixHandlers.put(prefix, handler);
    }

    public boolean handle(StringSelectInteractionEvent event) {
        String id = event.getComponentId();

        Consumer<StringSelectInteractionEvent> handler = handlers.get(id);
        if (handler != null) {
            try {
                handler.accept(event);
            } catch (Exception e) {
                logger.error("Error handling select menu: {}", id, e);
            }
            return true;
        }

        for (var entry : prefixHandlers.entrySet()) {
            if (id.startsWith(entry.getKey())) {
                try {
                    entry.getValue().accept(event);
                } catch (Exception e) {
                    logger.error("Error handling select menu with prefix: {}", id, e);
                }
                return true;
            }
        }

        return false;
    }

    public void unregister(String id) {
        handlers.remove(id);
    }
}
