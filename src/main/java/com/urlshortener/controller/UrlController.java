package com.urlshortener.controller;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.ShortUrlResponse;
import com.urlshortener.event.UrlClickEvent;
import com.urlshortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UrlController {

    private final UrlShortenerService urlShortenerService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a new shortened URL with optional custom alias and TTL expiry.
     */
    @PostMapping("/api/v1/urls")
    public ResponseEntity<ShortUrlResponse> createShortUrl(@Valid @RequestBody CreateUrlRequest request) {
        ShortUrlResponse response = urlShortenerService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fast-path redirect endpoint.
     * Served with sub-millisecond latency via Redis cache-aside.
     * Asynchronously dispatches click analytics event without blocking redirection.
     */
    @GetMapping("/{shortCode:[a-zA-Z0-9_-]{3,64}}")
    public ResponseEntity<Void> redirect(
            @PathVariable("shortCode") String shortCode,
            HttpServletRequest request
    ) {
        // Fast Cache-Aside resolution
        String originalUrl = urlShortenerService.resolveOriginalUrl(shortCode);

        // Asynchronous non-blocking analytics event dispatch
        String clientIp = extractClientIp(request);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        String referrer = request.getHeader(HttpHeaders.REFERER);
        String cfCountry = request.getHeader("CF-IPCountry"); // Cloudflare GeoIP header if available

        eventPublisher.publishEvent(UrlClickEvent.builder()
                .shortCode(shortCode)
                .ipAddress(clientIp)
                .referrer(referrer)
                .userAgent(userAgent)
                .country(cfCountry != null ? cfCountry : "Unknown")
                .clickedAt(Instant.now())
                .build());

        // HTTP 302 Found redirect
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate")
                .build();
    }

    /**
     * Deactivates a short URL.
     */
    @DeleteMapping("/api/v1/urls/{shortCode}")
    public ResponseEntity<Void> deleteShortUrl(@PathVariable("shortCode") String shortCode) {
        urlShortenerService.deactivateUrl(shortCode);
        return ResponseEntity.noContent().build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
