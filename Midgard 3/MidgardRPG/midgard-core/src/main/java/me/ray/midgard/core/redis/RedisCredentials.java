package me.ray.midgard.core.redis;

public record RedisCredentials(
    String host,
    int port,
    String password,
    boolean useSsl
) {}
