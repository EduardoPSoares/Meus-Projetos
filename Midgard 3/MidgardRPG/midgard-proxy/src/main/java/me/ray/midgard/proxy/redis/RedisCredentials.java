package me.ray.midgard.proxy.redis;

public record RedisCredentials(
    String host,
    int port,
    String password,
    boolean useSsl
) {}