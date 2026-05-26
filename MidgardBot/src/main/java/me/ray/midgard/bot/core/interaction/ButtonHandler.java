package me.ray.midgard.bot.core.interaction;

import me.ray.midgard.bot.MidgardBot;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ButtonHandler {

    private static final Logger logger = LoggerFactory.getLogger(ButtonHandler.class);

    private final Map<String, Consumer<ButtonInteractionEvent>> permanentHandlers = new ConcurrentHashMap<>();
    private final Map<String, TemporaryHandler> temporaryHandlers = new ConcurrentHashMap<>();

    public void register(String id, Consumer<ButtonInteractionEvent> handler) {
        permanentHandlers.put(id, handler);
    }

    public void registerPrefix(String prefix, Consumer<ButtonInteractionEvent> handler) {
        permanentHandlers.put("prefix:" + prefix, handler);
    }

    public void registerTemporary(String id, long expiresAt, Consumer<ButtonInteractionEvent> handler) {
        temporaryHandlers.put(id, new TemporaryHandler(handler, expiresAt));
    }

    public void registerTemporary(String id, Consumer<ButtonInteractionEvent> handler) {
        // Default: expires in 5 minutes
        registerTemporary(id, System.currentTimeMillis() + 300_000, handler);
    }

    public boolean handle(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        // Check temporary handlers first
        TemporaryHandler temp = temporaryHandlers.get(id);
        if (temp != null) {
            if (System.currentTimeMillis() > temp.expiresAt) {
                temporaryHandlers.remove(id);
                event.reply("⏳ Esta interação expirou.").setEphemeral(true).queue();
                return true;
            }
            try {
                temp.handler.accept(event);
            } catch (Exception e) {
                logger.error("Error handling temporary button: {}", id, e);
            }
            return true;
        }

        // Check exact match
        Consumer<ButtonInteractionEvent> handler = permanentHandlers.get(id);
        if (handler != null) {
            try {
                handler.accept(event);
            } catch (Exception e) {
                logger.error("Error handling button: {}", id, e);
            }
            return true;
        }

        // Check prefix match
        for (var entry : permanentHandlers.entrySet()) {
            if (entry.getKey().startsWith("prefix:") && id.startsWith(entry.getKey().substring(7))) {
                try {
                    entry.getValue().accept(event);
                } catch (Exception e) {
                    logger.error("Error handling button with prefix: {}", id, e);
                }
                return true;
            }
        }

        return false;
    }

    public void removeTemporary(String id) {
        temporaryHandlers.remove(id);
    }

    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        temporaryHandlers.entrySet().removeIf(entry -> now > entry.getValue().expiresAt);
    }

    private static class TemporaryHandler {
        final Consumer<ButtonInteractionEvent> handler;
        final long expiresAt;

        TemporaryHandler(Consumer<ButtonInteractionEvent> handler, long expiresAt) {
            this.handler = handler;
            this.expiresAt = expiresAt;
        }
    }
}
