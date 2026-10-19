package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(
    name = "short_urls",
    indexes = {
        @Index(name = "idx_short_code", columnList = "shortCode", unique = true),
        @Index(name = "idx_expires_at", columnList = "expiresAt"),
        @Index(name = "idx_created_at", columnList = "createdAt")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String shortCode;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Long clickCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Boolean customAlias = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Checks if the URL is expired based on current instant.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Returns remaining seconds until expiration, or a default long TTL if no expiry set.
     */
    public long getRemainingTtlSeconds(long defaultTtlSeconds) {
        if (expiresAt == null) {
            return defaultTtlSeconds;
        }
        long seconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        return Math.max(0, seconds);
    }
}
