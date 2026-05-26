package me.ray.midgard.proxy.manager;

import com.velocitypowered.api.proxy.ProxyServer;
import me.ray.midgard.proxy.redis.ProxyRedisManager;
import org.slf4j.Logger;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SessionManager {

    private final Object plugin;
    private final ProxyServer server;
    private final ProxyRedisManager redisManager;
    private final Logger logger;
    
    private final Map<UUID, CompletableFuture<Boolean>> pendingSaves = new ConcurrentHashMap<>();
    private final Set<UUID> safeToConnect = ConcurrentHashMap.newKeySet();

    public SessionManager(Object plugin, ProxyServer server, ProxyRedisManager redisManager, Logger logger) {
        this.plugin = plugin;
        this.server = server;
        this.redisManager = redisManager;
        this.logger = logger;
    }

    public void init() {
        if (!redisManager.isEnabled()) return;
        
        redisManager.psubscribe("sync:saved:*", new JedisPubSub() {
            @Override
            public void onPMessage(String pattern, String channel, String message) {
                try {
                    String[] parts = channel.split(":");
                    if (parts.length < 3) return;
                    
                    String uuidStr = parts[2];
                    UUID uuid = UUID.fromString(uuidStr);
                    
                    completeSave(uuid, true);
                   // logger.info("Confirmado salvamento para " + uuid);
                    
                } catch (Exception e) {
                    logger.error("Error processing sync confirmation: " + channel, e);
                }
            }
        });
    }

    public CompletableFuture<Boolean> requestSave(UUID uuid) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingSaves.put(uuid, future);
        
        redisManager.publish("midgard:server_switch", uuid.toString());
        
        // Timeout 2 seconds
        server.getScheduler().buildTask(plugin, () -> {
            if (!future.isDone()) {
                completeSave(uuid, false);
            }
        }).delay(2, TimeUnit.SECONDS).schedule();
        
        return future;
    }
    
    private void completeSave(UUID uuid, boolean success) {
        CompletableFuture<Boolean> future = pendingSaves.remove(uuid);
        if (future != null && !future.isDone()) {
            future.complete(success);
        }
    }
    
    public void setSafe(UUID uuid, boolean safe) {
        if (safe) safeToConnect.add(uuid);
        else safeToConnect.remove(uuid);
    }
    
    public boolean isSafe(UUID uuid) {
        return safeToConnect.contains(uuid);
    }
}
