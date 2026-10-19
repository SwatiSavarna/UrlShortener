package com.urlshortener.service;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.ShortUrlResponse;
import com.urlshortener.entity.ShortUrl;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core business service for shortening, caching, and resolving URLs.
 * 
 * Scalability Architecture:
 * - Cache-Aside: Reads check Redis first. On cache miss, DB is queried and cache is populated.
 * - Negative Caching: Non-existent keys are cached briefly to prevent database cache-penetration.
 * - Dynamic TTL: Redis TTL matches the exact remaining lifetime of the short link.
 * - Distributed Bijective Base62: Generates collision-free short codes without retry loops.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private final ShortUrlRepository shortUrlRepository;
    private final RedisCacheService redisCacheService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.default-ttl-seconds:604800}") // 7 days default
    private long defaultTtlSeconds;

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)([smhd])$", Pattern.CASE_INSENSITIVE);

    /**
     * Creates a new shortened URL with optional custom alias and TTL expiry.
     */
    @Transactional
    public ShortUrlResponse createShortUrl(CreateUrlRequest request) {
        String originalUrl = request.getOriginalUrl().trim();
        Instant expiresAt = calculateExpiration(request.getTtlDuration(), request.getExpiresAt());

        ShortUrl entity;

        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            String alias = request.getCustomAlias().trim();
            if (!Base62Encoder.isValidCustomAlias(alias)) {
                throw new IllegalArgumentException("Invalid custom alias: '" + alias + "'. Must be 3-30 characters [a-zA-Z0-9_-].");
            }
            if (shortUrlRepository.existsByShortCode(alias)) {
                throw new IllegalArgumentException("Custom alias '" + alias + "' is already taken.");
            }

            entity = ShortUrl.builder()
                    .shortCode(alias)
                    .originalUrl(originalUrl)
                    .createdAt(Instant.now())
                    .expiresAt(expiresAt)
                    .customAlias(true)
                    .isActive(true)
                    .clickCount(0L)
                    .build();

            entity = shortUrlRepository.save(entity);
        } else {
            // Reserve entity to obtain database auto-increment ID for bijective Base62 encoding
            entity = ShortUrl.builder()
                    .shortCode("temp-" + System.nanoTime())
                    .originalUrl(originalUrl)
                    .createdAt(Instant.now())
                    .expiresAt(expiresAt)
                    .customAlias(false)
                    .isActive(true)
                    .clickCount(0L)
                    .build();

            entity = shortUrlRepository.save(entity);
            String shortCode = Base62Encoder.encode(entity.getId());
            entity.setShortCode(shortCode);
            entity = shortUrlRepository.save(entity);
        }

        // Cache pre-warming: immediately store in Redis with matching TTL
        long remainingTtl = entity.getRemainingTtlSeconds(defaultTtlSeconds);
        redisCacheService.putUrl(entity.getShortCode(), entity.getOriginalUrl(), remainingTtl);

        log.info("Shortened URL created: {} -> {} (expires: {})", entity.getShortCode(), originalUrl, expiresAt);
        return mapToResponse(entity);
    }

    /**
     * Resolves the original URL for redirection using Redis Cache-Aside.
     */
    public String resolveOriginalUrl(String shortCode) {
        // 1. Check Redis Cache
        String cached = redisCacheService.getUrl(shortCode);
        if (cached != null) {
            if (redisCacheService.isNotFoundSentinel(cached)) {
                throw new NoSuchElementException("Short URL not found or expired: " + shortCode);
            }
            return cached;
        }

        // 2. Cache Miss: Query Database
        Optional<ShortUrl> opt = shortUrlRepository.findByShortCode(shortCode);
        if (opt.isEmpty() || !opt.get().getIsActive()) {
            // Negative caching to prevent cache penetration
            redisCacheService.putNegativeCache(shortCode);
            throw new NoSuchElementException("Short URL not found: " + shortCode);
        }

        ShortUrl shortUrl = opt.get();

        // 3. Check Expiry
        if (shortUrl.isExpired()) {
            redisCacheService.evict(shortCode);
            throw new IllegalStateException("Short URL '" + shortCode + "' has expired.");
        }

        // 4. Populate Redis Cache
        long remainingTtl = shortUrl.getRemainingTtlSeconds(defaultTtlSeconds);
        redisCacheService.putUrl(shortCode, shortUrl.getOriginalUrl(), remainingTtl);

        return shortUrl.getOriginalUrl();
    }

    /**
     * Deactivates a URL and purges it from cache.
     */
    @Transactional
    public void deactivateUrl(String shortCode) {
        ShortUrl url = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new NoSuchElementException("Short URL not found: " + shortCode));
        url.setIsActive(false);
        shortUrlRepository.save(url);
        redisCacheService.evict(shortCode);
        log.info("Deactivated short URL: {}", shortCode);
    }

    private Instant calculateExpiration(String ttlDuration, Instant explicitExpiry) {
        if (explicitExpiry != null) {
            if (explicitExpiry.isBefore(Instant.now())) {
                throw new IllegalArgumentException("Expiration date must be in the future.");
            }
            return explicitExpiry;
        }

        if (ttlDuration == null || ttlDuration.isBlank()) {
            return null; // Permanent or default expiration
        }

        Matcher matcher = DURATION_PATTERN.matcher(ttlDuration.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid TTL duration format: '" + ttlDuration + "'. Use format like '30m', '2h', '7d'.");
        }

        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2).toLowerCase();

        Duration duration = switch (unit) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> throw new IllegalArgumentException("Unsupported duration unit: " + unit);
        };

        return Instant.now().plus(duration);
    }

    private ShortUrlResponse mapToResponse(ShortUrl entity) {
        return ShortUrlResponse.builder()
                .shortCode(entity.getShortCode())
                .shortUrl(baseUrl + "/" + entity.getShortCode())
                .originalUrl(entity.getOriginalUrl())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .clickCount(entity.getClickCount())
                .customAlias(entity.getCustomAlias())
                .isExpired(entity.isExpired())
                .build();
    }
}
