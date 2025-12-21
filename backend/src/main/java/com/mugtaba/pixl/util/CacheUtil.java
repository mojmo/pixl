package com.mugtaba.pixl.util;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple in-memory cache with TTL (Time To Live) support.
 * Thread-safe and includes automatic cleanup of expired entries.
 */
public class CacheUtil {

    private static final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);

    static {
        // Run cleanup every 5 minutes
        cleanupExecutor.scheduleAtFixedRate(CacheUtil::cleanupExpiredEntries, 5, 5, TimeUnit.MINUTES);
        LogUtil.logInfo("CacheUtil", "static", "Cache initialized with automatic cleanup");
    }

    /**
     * Puts a value in the cache with specified TTL.
     * @param key cache key
     * @param value cache value
     * @param ttlMinutes time to live in minutes
     */
    public static void put(String key, Object value, int ttlMinutes) {
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(ttlMinutes);
        cache.put(key, new CacheEntry(value, expiry));

        LogUtil.logInfo("CacheUtil", "put", String.format("Cached item: %s (TTL: %d minutes)", key, ttlMinutes));
    }

    /**
     * Gets a value from the cache
     * @param <T> type of the value
     * @param key cache key
     * @param type expected type of the value
     * @return cached value or null if not found/expired/type mismatch
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Class<T> type) {
        CacheEntry entry = cache.get(key);

        if (entry == null) { return null; }

        if (entry.isExpired()) {
            cache.remove(key);
            LogUtil.logInfo("CacheUtil", "get", "Removed expired cache entry: " + key);
            return null;
        }

        try {
            return (T) entry.value();
        } catch (ClassCastException e) {
            LogUtil.logWarning(
                "CacheUtil", "get",
                String.format("Cache type mismatch for key %s: expected %s", key, type.getSimpleName())
            );
            cache.remove(key);
            return null;
        }
    }

    /**
     * Removes a specific cache entry.
     * @param key cache key to remove
     */
    public static void remove(String key) {
        cache.remove(key);
        LogUtil.logInfo("CacheUtil", "remove", "Removed cache entry: " + key);
    }

    /**
     * Removes all cache entries matching a pattern
     * @param pattern key pattern (supports '*' wildcards)
     */
    public static void removePattern(String pattern) {
        String regex = pattern.replace("*", ".*");
        int removedCount = 0;

        for (String key : cache.keySet()) {
            if (key.matches(regex)) {
                cache.remove(key);
                removedCount++;
            }
        }

        LogUtil.logInfo(
            "CacheUtil", "removePattern",
            String.format("Removed %d cache entries matching pattern: %s", removedCount, pattern)
        );
    }

    /**
     * Clear all cache entries.
     */
    public static void clear() {
        int size = cache.size();
        cache.clear();
        LogUtil.logInfo("CacheUtil", "clear", String.format("Cleared %d cache entries", size));
    }

    /**
     * Gets cache statistics.
     * @return map with cache stats
     */
    public static Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", cache.size());

        long expiredCount = cache.values().stream().mapToLong(entry -> entry.isExpired() ? 1 : 0).sum();

        stats.put("expiredEntries", expiredCount);
        stats.put("activeEntries", cache.size() - expiredCount);

        return stats;
    }

    /**
     * Cleanup expired entries (called periodically).
     */
    private static void cleanupExpiredEntries() {
        int removedCount = 0;

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            LogUtil.logInfo(
                "CacheUtil", "cleanupExpiredEntries",
                String.format("Cleaned up %d expired cache entries", removedCount)
            );
        }
    }

    /**
     * Cache entry wrapper with value and TTL support.
     */
    private record CacheEntry(Object value, LocalDateTime expiry) {

        public boolean isExpired() {
                return LocalDateTime.now().isAfter(expiry);
            }
    }
}
