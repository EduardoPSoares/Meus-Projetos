package me.ray.midgardProxy.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.ProxyServer;
import me.ray.midgardProxy.config.ConfigManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import redis.clients.jedis.JedisPubSub;

public class RedisSubscriber extends JedisPubSub {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final RedisManager redisManager;

    public RedisSubscriber(ProxyServer server, ConfigManager configManager, RedisManager redisManager) {
        this.server = server;
        this.configManager = configManager;
        this.redisManager = redisManager;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals("midgard:global_chat")) {
            try {
                JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                String sender = json.get("sender").getAsString();
                String msg = json.get("msg").getAsString();

                String format = configManager.getPrefix() + "<yellow>" + sender + ": <white>" + msg;
                server.sendMessage(MiniMessage.miniMessage().deserialize(format));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (channel.equals("midgard:sync:saved")) {
            try {
                java.util.UUID uuid = java.util.UUID.fromString(message);
                redisManager.completeSave(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
