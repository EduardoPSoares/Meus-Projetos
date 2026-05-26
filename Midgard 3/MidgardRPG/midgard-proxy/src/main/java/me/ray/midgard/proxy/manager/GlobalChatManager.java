package me.ray.midgard.proxy.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.ProxyServer;
import me.ray.midgard.proxy.config.ConfigManager;
import me.ray.midgard.proxy.redis.ProxyRedisManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import redis.clients.jedis.JedisPubSub;
import org.slf4j.Logger;

public class GlobalChatManager {

    private final ProxyServer server;
    private final ProxyRedisManager redisManager;
    private final ConfigManager configManager;
    private final Logger logger;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public GlobalChatManager(ProxyServer server, ProxyRedisManager redisManager, ConfigManager configManager, Logger logger) {
        this.server = server;
        this.redisManager = redisManager;
        this.configManager = configManager;
        this.logger = logger;
    }

    public void init() {
        if (!redisManager.isEnabled()) {
            logger.warn("Chat Global desativado pois o Redis não está conectado.");
            return;
        }

        redisManager.subscribe("midgard:global_chat", new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                try {
                    // Handles Gson deprecation if needed, or uses static method
                    JsonElement element = JsonParser.parseString(message);
                    if (!element.isJsonObject()) return;
                    
                    JsonObject json = element.getAsJsonObject();
                    
                    String sender = json.has("sender") ? json.get("sender").getAsString() : "Unknown";
                    String msg = json.has("msg") ? json.get("msg").getAsString() : "";
                    
                    String format = configManager.getGlobalChatFormat();
                    
                    Component output = miniMessage.deserialize(format,
                            Placeholder.unparsed("sender", sender),
                            Placeholder.unparsed("message", msg)
                    );
                    
                    server.sendMessage(output);
                    
                } catch (Exception e) {
                    logger.error("Erro ao processar mensagem de chat global: " + message, e);
                }
            }
        });
        
        logger.info("GlobalChatManager inicializado e inscrito no Redis.");
    }
}
