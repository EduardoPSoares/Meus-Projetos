package me.ray.midgard.core.leaderboard;

import me.ray.midgard.core.redis.RedisManager;
import redis.clients.jedis.resps.Tuple;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LeaderboardManager {

    private final RedisManager redisManager;

    public LeaderboardManager(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    public void updateScore(String leaderboard, String member, double score) {
        redisManager.execute(jedis -> {
            jedis.zadd("lb:" + leaderboard, score, member);
        });
    }
    
    public void incrementScore(String leaderboard, String member, double score) {
        redisManager.execute(jedis -> {
            jedis.zincrby("lb:" + leaderboard, score, member);
        });
    }

    public CompletableFuture<List<Tuple>> getTop(String leaderboard, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            return redisManager.execute(jedis -> {
                return jedis.zrevrangeWithScores("lb:" + leaderboard, 0, limit - 1);
            });
        });
    }
    
    public CompletableFuture<Long> getRank(String leaderboard, String member) {
        return CompletableFuture.supplyAsync(() -> {
            return redisManager.execute(jedis -> {
                return jedis.zrevrank("lb:" + leaderboard, member);
            });
        });
    }
    
    public CompletableFuture<Double> getScore(String leaderboard, String member) {
        return CompletableFuture.supplyAsync(() -> {
            return redisManager.execute(jedis -> {
                return jedis.zscore("lb:" + leaderboard, member);
            });
        });
    }
}
