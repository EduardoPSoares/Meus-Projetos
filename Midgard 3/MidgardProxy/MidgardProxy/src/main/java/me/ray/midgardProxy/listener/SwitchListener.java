package me.ray.midgardProxy.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.ray.midgardProxy.redis.RedisManager;
import org.slf4j.Logger;

public class SwitchListener {

    private final RedisManager redisManager;
    private final Logger logger;

    public SwitchListener(RedisManager redisManager, Logger logger) {
        this.redisManager = redisManager;
        this.logger = logger;
    }

    @Subscribe
    public void onServerSwitch(ServerPreConnectEvent event) {
        // Only trigger if player is already connected to a server (switching)
        if (event.getPreviousServer() != null) {
            Player player = event.getPlayer();
            RegisteredServer previous = event.getPreviousServer();
            RegisteredServer target = event.getResult().getServer().orElse(null);

            // If target is null or same as previous, ignore
            if (target == null || previous.getServerInfo().getName().equals(target.getServerInfo().getName())) {
                return;
            }
            
            logger.info("Player {} switching from {} to {}. Initiating save request...", player.getUsername(), previous.getServerInfo().getName(), target.getServerInfo().getName());
            
            // Send Save Request to Backend if Redis is available
            if (redisManager != null) {
                redisManager.publish("midgard:sync:req_save", player.getUniqueId().toString());

                // Wait for confirmation (timeout 2s)
                try {
                   redisManager.waitForSave(player.getUniqueId(), 2000).join();
                } catch (Exception e) {
                    logger.warn("Timeout or error waiting for save for {}", player.getUsername());
                }
            } else {
                 // Fallback for non-Redis setup: Just delay slightly to allow basic SQL save to finish naturally
                 // This is not perfect but better than nothing
                 try {
                     Thread.sleep(500);
                 } catch (InterruptedException ignored) {}
            }
        }
    }
}
