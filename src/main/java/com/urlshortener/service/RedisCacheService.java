package com.urlshortener.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache-Aside service backed by Redis.
 * 
 * Scalability Architecture:
 * - Ultra-fast read path (< 2ms) serving high read-to-write ratios.
 * - Negative Caching: Caches non-existent short codes with a short TTL to prevent Cache Penetration.
 * - Cache Synchronization: Synchronizes key TTL in Redis with the custom expiration time of the URL.
 * - Fault Tolerance: If Redis becomes unreachable, methods gracefully fail open to DB without throwing exceptions.
 */
@Service
@Slf4j
public class RedisCacheService {

    private static final String KEY_PREFIX = "shorturl:";
    private static final String NOT_FOUND_PREFIX = "shorturl:notfound:";
    private static final String NOT_FOUND_SENTINEL = "__NOT_FOUND__";
    private static final Duration NEGATIVE_CACHE_TTL = Duration.ofSeconds(60);
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public RedisCacheService(@Autowired(required = false) RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if Redis caching is actively enabled and reachable.
     */
    public boolean isAvailable() {
        if (redisTemplate == null) {
            return false;
        }
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retrieves the original URL from cache.
     * Returns:
     * - Optional.of(url) if found
     * - Optional.empty() with sentinel if known NOT_FOUND (negative cache hit)
     * - null if cache miss or Redis error
     */
    public String getUrl(String shortCode) {
        if (redisTemplate == null) return null;
        try {
            // Check negative cache first
            String negativeHit = redisTemplate.opsForValue().get(NOT_FOUND_PREFIX + shortCode);
            if (NOT_FOUND_SENTINEL.equals(negativeHit)) {
                log.debug("Negative cache hit for short code: {}", shortCode);
                return NOT_FOUND_SENTINEL;
            }

            // Check standard cache
            String cachedUrl = redisTemplate.opsForValue().get(KEY_PREFIX + shortCode);
            if (cachedUrl != null) {
                log.debug("Redis cache hit for short code {}: {}", shortCode, cachedUrl);
                return cachedUrl;
            }
        } catch (Exception e) {
            log.warn("Redis lookup failed for short code {}. Falling back to DB. Error: {}", shortCode, e.getMessage());
        }
        return null;
    }

    /**
     * Caches the original URL with a TTL.
     */
    public void putUrl(String shortCode, String originalUrl, Long ttlSeconds) {
        if (redisTemplate == null) return;
        try {
            Duration ttl = (ttlSeconds != null && ttlSeconds > 0)
                    ? Duration.ofSeconds(ttlSeconds)
                    : DEFAULT_CACHE_TTL;

            redisTemplate.opsForValue().set(KEY_PREFIX + shortCode, originalUrl, ttl);
            log.debug("Cached short code {} with TTL {}s", shortCode, ttl.getSeconds());
        } catch (Exception e) {
            log.warn("Failed to cache short code {} in Redis. Error: {}", shortCode, e.getMessage());
        }
    }

    /**
     * Negative caching: Marks this code as non-existent for 60 seconds.
     * Prevents cache penetration attacks from repeatedly hitting the database for invalid keys.
     */
    public void putNegativeCache(String shortCode) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.opsForValue().set(NOT_FOUND_PREFIX + shortCode, NOT_FOUND_SENTINEL, NEGATIVE_CACHE_TTL);
            log.debug("Cached negative lookup for short code {}", shortCode);
        } catch (Exception e) {
            log.warn("Failed to set negative cache for short code {}. Error: {}", shortCode, e.getMessage());
        }
    }

    /**
     * Evicts the URL and negative cache entries from Redis.
     */
    public void evict(String shortCode) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(KEY_PREFIX + shortCode);
            redisTemplate.delete(NOT_FOUND_PREFIX + shortCode);
            log.debug("Evicted short code {} from Redis", shortCode);
        } catch (Exception e) {
            log.warn("Failed to evict short code {} from Redis: {}", shortCode, e.getMessage());
        }
    }

    public boolean isNotFoundSentinel(String value) {
        return NOT_FOUND_SENTINEL.equals(value);
    }
}
