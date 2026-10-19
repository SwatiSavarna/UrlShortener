package com.urlshortener.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token Bucket Rate Limiter supporting both distributed Redis execution
 * and high-concurrency in-memory fallback.
 * 
 * Scalability Architecture:
 * - Token Bucket allows bursting while maintaining steady-state rate limits.
 * - Multi-instance readiness: When Redis is present, state is shared across nodes.
 * - Standalone readiness: Seamless in-memory fallback ensures zero hard dependencies in dev.
 */
@Service
@Slf4j
public class RateLimiterService {

    public enum Policy {
        URL_CREATION(15, 15.0 / 60.0), // 15 requests per minute (refill ~0.25 tokens/sec)
        REDIRECT(120, 120.0 / 60.0),   // 120 requests per minute (refill 2 tokens/sec)
        ANALYTICS(60, 60.0 / 60.0);    // 60 requests per minute

        @Getter
        private final long capacity;
        @Getter
        private final double refillRatePerSecond;

        Policy(long capacity, double refillRatePerSecond) {
            this.capacity = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class RateLimitResult {
        private final boolean allowed;
        private final long limit;
        private final long remaining;
        private final long resetSeconds;
    }

    private static class TokenBucket {
        double tokens;
        long lastRefillTimestamp;

        TokenBucket(double initialTokens, long timestamp) {
            this.tokens = initialTokens;
            this.lastRefillTimestamp = timestamp;
        }
    }

    private final RedisTemplate<String, String> redisTemplate;
    private final Map<String, TokenBucket> inMemoryBuckets = new ConcurrentHashMap<>();

    @Autowired
    public RateLimiterService(@Autowired(required = false) RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to consume 1 token for a given client IP and policy.
     */
    public RateLimitResult tryConsume(String clientIp, Policy policy) {
        String key = "ratelimit:" + policy.name().toLowerCase() + ":" + clientIp;
        long capacity = policy.getCapacity();
        double refillRate = policy.getRefillRatePerSecond();

        // 1. Try Redis Rate Limiting
        if (redisTemplate != null) {
            try {
                return executeRedisTokenBucket(key, capacity, refillRate);
            } catch (Exception e) {
                log.warn("Redis rate limiter failed for key {}. Falling back to in-memory: {}", key, e.getMessage());
            }
        }

        // 2. In-Memory Token Bucket Fallback
        return executeInMemoryTokenBucket(key, capacity, refillRate);
    }

    private RateLimitResult executeRedisTokenBucket(String key, long capacity, double refillRate) {
        long now = System.currentTimeMillis();
        String tokensKey = key + ":tokens";
        String timestampKey = key + ":timestamp";

        String tokensStr = redisTemplate.opsForValue().get(tokensKey);
        String lastTimeStr = redisTemplate.opsForValue().get(timestampKey);

        double tokens = capacity;
        long lastTimestamp = now;

        if (tokensStr != null && lastTimeStr != null) {
            try {
                double storedTokens = Double.parseDouble(tokensStr);
                long storedTime = Long.parseLong(lastTimeStr);
                double elapsedSec = Math.max(0, (now - storedTime) / 1000.0);
                tokens = Math.min(capacity, storedTokens + (elapsedSec * refillRate));
                lastTimestamp = now;
            } catch (NumberFormatException ignored) {
            }
        }

        boolean allowed = tokens >= 1.0;
        long remaining = 0;
        if (allowed) {
            tokens -= 1.0;
            remaining = (long) Math.floor(tokens);
        }

        // Save back with 2-minute TTL for inactive keys
        redisTemplate.opsForValue().set(tokensKey, String.valueOf(tokens), Duration.ofMinutes(2));
        redisTemplate.opsForValue().set(timestampKey, String.valueOf(lastTimestamp), Duration.ofMinutes(2));

        long resetSeconds = (long) Math.ceil((capacity - tokens) / refillRate);
        return new RateLimitResult(allowed, capacity, remaining, Math.max(1, resetSeconds));
    }

    private RateLimitResult executeInMemoryTokenBucket(String key, long capacity, double refillRate) {
        long now = System.currentTimeMillis();
        
        TokenBucket bucket = inMemoryBuckets.compute(key, (k, existing) -> {
            if (existing == null) {
                return new TokenBucket(capacity, now);
            }
            double elapsedSec = Math.max(0, (now - existing.lastRefillTimestamp) / 1000.0);
            double newTokens = Math.min(capacity, existing.tokens + (elapsedSec * refillRate));
            existing.tokens = newTokens;
            existing.lastRefillTimestamp = now;
            return existing;
        });

        synchronized (bucket) {
            boolean allowed = bucket.tokens >= 1.0;
            long remaining = 0;
            if (allowed) {
                bucket.tokens -= 1.0;
                remaining = (long) Math.floor(bucket.tokens);
            }
            long resetSeconds = (long) Math.ceil((capacity - bucket.tokens) / refillRate);
            return new RateLimitResult(allowed, capacity, remaining, Math.max(1, resetSeconds));
        }
    }
}
