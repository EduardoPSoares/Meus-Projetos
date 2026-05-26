package me.ray.midgardProxy.redis;

import redis.clients.jedis.JedisPubSub;
import java.util.UUID;

public class SyncSubscriber extends JedisPubSub {

    private final RedisManager redisManager;

    public SyncSubscriber(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals("midgard:sync:saved")) {
            try {
                UUID uuid = UUID.fromString(message);
                redisManager.completeSave(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
