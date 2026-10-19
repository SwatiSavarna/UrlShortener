package com.urlshortener.service;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.ShortUrlResponse;
import com.urlshortener.entity.ShortUrl;
import com.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private UrlShortenerService urlShortenerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlShortenerService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(urlShortenerService, "defaultTtlSeconds", 604800L);
    }

    @Test
    @DisplayName("Create short URL with custom alias successfully")
    void testCreateWithCustomAlias() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com/long-page")
                .customAlias("my-custom")
                .ttlDuration("1h")
                .build();

        when(shortUrlRepository.existsByShortCode("my-custom")).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(i -> {
            ShortUrl s = i.getArgument(0);
            s.setId(1L);
            return s;
        });

        ShortUrlResponse response = urlShortenerService.createShortUrl(request);

        assertNotNull(response);
        assertEquals("my-custom", response.getShortCode());
        assertEquals("https://example.com/long-page", response.getOriginalUrl());
        assertTrue(response.getCustomAlias());
        assertNotNull(response.getExpiresAt());

        // Verify pre-warming cache in Redis
        verify(redisCacheService, times(1)).putUrl(eq("my-custom"), eq("https://example.com/long-page"), anyLong());
    }

    @Test
    @DisplayName("Duplicate custom alias must throw IllegalArgumentException")
    void testDuplicateCustomAliasThrows() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .customAlias("taken-alias")
                .build();

        when(shortUrlRepository.existsByShortCode("taken-alias")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> urlShortenerService.createShortUrl(request));
        verify(shortUrlRepository, never()).save(any());
    }

    @Test
    @DisplayName("Resolve URL via Redis cache hit")
    void testResolveCacheHit() {
        when(redisCacheService.getUrl("fastCode")).thenReturn("https://fast-site.com");

        String original = urlShortenerService.resolveOriginalUrl("fastCode");

        assertEquals("https://fast-site.com", original);
        // Verify DB was NOT touched!
        verify(shortUrlRepository, never()).findByShortCode(anyString());
    }

    @Test
    @DisplayName("Resolve URL via DB on cache miss and populate cache")
    void testResolveCacheMiss() {
        when(redisCacheService.getUrl("dbCode")).thenReturn(null);

        ShortUrl entity = ShortUrl.builder()
                .id(10L)
                .shortCode("dbCode")
                .originalUrl("https://destination.com")
                .createdAt(Instant.now())
                .isActive(true)
                .build();

        when(shortUrlRepository.findByShortCode("dbCode")).thenReturn(Optional.of(entity));

        String original = urlShortenerService.resolveOriginalUrl("dbCode");

        assertEquals("https://destination.com", original);
        // Verify cache is populated on miss
        verify(redisCacheService, times(1)).putUrl(eq("dbCode"), eq("https://destination.com"), anyLong());
    }

    @Test
    @DisplayName("Expired URL must throw IllegalStateException and evict cache")
    void testResolveExpiredUrl() {
        when(redisCacheService.getUrl("expiredCode")).thenReturn(null);

        ShortUrl expiredEntity = ShortUrl.builder()
                .id(20L)
                .shortCode("expiredCode")
                .originalUrl("https://destination.com")
                .createdAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600)) // 1 hour ago
                .isActive(true)
                .build();

        when(shortUrlRepository.findByShortCode("expiredCode")).thenReturn(Optional.of(expiredEntity));

        assertThrows(IllegalStateException.class, () -> urlShortenerService.resolveOriginalUrl("expiredCode"));
        verify(redisCacheService, times(1)).evict("expiredCode");
    }

    @Test
    @DisplayName("Non-existent URL must cache negative sentinel")
    void testResolveNotFoundSetsNegativeCache() {
        when(redisCacheService.getUrl("missingCode")).thenReturn(null);
        when(shortUrlRepository.findByShortCode("missingCode")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> urlShortenerService.resolveOriginalUrl("missingCode"));
        verify(redisCacheService, times(1)).putNegativeCache("missingCode");
    }
}
