package me.ray.midgard.bot.core.cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Cache<K, V> {

    private final Map<K, CacheEntry<V>> entries = new ConcurrentHashMap<>();
    private final long defaultTtlMillis;
    private final int maxSize;

    public Cache(long defaultTtlMillis, int maxSize) {
        this.defaultTtlMillis = defaultTtlMillis;
        this.maxSize = maxSize;
    }

    public Cache(long defaultTtlMillis) {
        this(defaultTtlMillis, Integer.MAX_VALUE);
    }

    public Cache() {
        this(300_000, Integer.MAX_VALUE); // 5 min default
    }

    public void put(K key, V value) {
        put(key, value, defaultTtlMillis);
    }

    public void put(K key, V value, long ttlMillis) {
        evictExpired();
        if (entries.size() >= maxSize) {
            evictOldest();
        }
        entries.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMillis));
    }

    public V get(K key) {
        CacheEntry<V> entry = entries.get(key);
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expiresAt) {
            entries.remove(key);
            return null;
        }
        return entry.value;
    }

    public V getOrCompute(K key, Function<K, V> loader) {
        V value = get(key);
        if (value == null) {
            value = loader.apply(key);
            if (value != null) {
                put(key, value);
            }
        }
        return value;
    }

    public V getOrCompute(K key, Function<K, V> loader, long ttlMillis) {
        V value = get(key);
        if (value == null) {
            value = loader.apply(key);
            if (value != null) {
                put(key, value, ttlMillis);
            }
        }
        return value;
    }

    public boolean has(K key) {
        return get(key) != null;
    }

    public void invalidate(K key) {
        entries.remove(key);
    }

    public void invalidateAll() {
        entries.clear();
    }

    public int size() {
        evictExpired();
        return entries.size();
    }

    public Set<K> keys() {
        evictExpired();
        return Collections.unmodifiableSet(entries.keySet());
    }

    public Collection<V> values() {
        evictExpired();
        List<V> values = new ArrayList<>();
        for (CacheEntry<V> entry : entries.values()) {
            if (System.currentTimeMillis() <= entry.expiresAt) {
                values.add(entry.value);
            }
        }
        return values;
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        entries.entrySet().removeIf(e -> now > e.getValue().expiresAt);
    }

    private void evictOldest() {
        K oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        for (var entry : entries.entrySet()) {
            long created = entry.getValue().expiresAt - defaultTtlMillis;
            if (created < oldestTime) {
                oldestTime = created;
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) {
            entries.remove(oldestKey);
        }
    }

    private static class CacheEntry<V> {
        final V value;
        final long expiresAt;

        CacheEntry(V value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }
}
