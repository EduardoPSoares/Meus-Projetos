package me.ray.midgard.bot.core.interaction;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ModalHandler {

    private static final Logger logger = LoggerFactory.getLogger(ModalHandler.class);

    private final Map<String, Consumer<ModalInteractionEvent>> handlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<ModalInteractionEvent>> prefixHandlers = new ConcurrentHashMap<>();

    public void register(String id, Consumer<ModalInteractionEvent> handler) {
        handlers.put(id, handler);
    }

    public void registerPrefix(String prefix, Consumer<ModalInteractionEvent> handler) {
        prefixHandlers.put(prefix, handler);
    }

    public boolean handle(ModalInteractionEvent event) {
        String id = event.getModalId();

        Consumer<ModalInteractionEvent> handler = handlers.get(id);
        if (handler != null) {
            try {
                handler.accept(event);
            } catch (Exception e) {
                logger.error("Error handling modal: {}", id, e);
            }
            return true;
        }

        for (var entry : prefixHandlers.entrySet()) {
            if (id.startsWith(entry.getKey())) {
                try {
                    entry.getValue().accept(event);
                } catch (Exception e) {
                    logger.error("Error handling modal with prefix: {}", id, e);
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
