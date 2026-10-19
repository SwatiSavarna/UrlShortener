package com.urlshortener.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Event published on successful URL redirection.
 * Decouples the redirect latency path from analytical persistence.
 */
@Getter
@Builder
@AllArgsConstructor
public class UrlClickEvent {
    private final String shortCode;
    private final String ipAddress;
    private final String referrer;
    private final String userAgent;
    private final String country;
    private final Instant clickedAt;
}
