package me.ray.midgard.core.redis;

import me.ray.midgard.core.debug.DebugCategory;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.profile.ProfileManager;
import me.ray.midgard.core.profile.data.VanillaData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

public class SyncReqListener extends JedisPubSub {
    private final ProfileManager profileManager;
    private final RedisManager redisManager;

    public SyncReqListener(ProfileManager profileManager, RedisManager redisManager) {
        this.profileManager = profileManager;
        this.redisManager = redisManager;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals("midgard:sync:req_save")) {
            try {
                UUID uuid = UUID.fromString(message);
                
                // 1. Switch from Async (Redis) to Global (to find player)
                me.ray.midgard.core.utils.Task.sync(() -> {
                    Player player = Bukkit.getPlayer(uuid);
                    // If player is on this server
                    if (player != null && player.isOnline()) {
                        // 2. Switch from Global to Player Region (to access data)
                        me.ray.midgard.core.utils.Task.sync(player, () -> {
                            MidgardProfile profile = profileManager.getProfile(uuid);
                            if (profile != null) {
                                MidgardLogger.debug(DebugCategory.CORE, "Recebida solicitação de salvamento remoto para %s", player.getName());
                                
                                try {
                                    VanillaData data = VanillaData.fromPlayer(player);
                                    profile.setData(data);
                                    
                                    profileManager.saveProfile(profile).thenRun(() -> {
                                        // Ack to Proxy
                                        if (redisManager.isEnabled()) {
                                             redisManager.execute(jedis -> {
                                                 jedis.publish("midgard:sync:saved", uuid.toString());
                                             });
                                        }
                                    });
                                } catch (Exception e) {
                                    MidgardLogger.error("Erro ao processar sync request para " + player.getName(), e);
                                }
                            }
                        });
                    }
                });
            } catch (Exception e) {
                MidgardLogger.error("Erro ao processar mensagem de sync do Redis", e);
            }
        }
    }
}
