package com.urlshortener.service;

import com.urlshortener.dto.AnalyticsOverviewResponse;
import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.ClickDetailDto;
import com.urlshortener.dto.ShortUrlResponse;
import com.urlshortener.entity.ClickAnalytics;
import com.urlshortener.entity.ShortUrl;
import com.urlshortener.event.UrlClickEvent;
import com.urlshortener.repository.ClickAnalyticsRepository;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.util.UserAgentParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ClickAnalyticsRepository analyticsRepository;
    private final ShortUrlRepository shortUrlRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Records a click event asynchronously.
     * Persists granular tracking info and increments the aggregate counter.
     */
    @Transactional
    public void recordClick(UrlClickEvent event) {
        try {
            String deviceType = UserAgentParserUtil.getDeviceType(event.getUserAgent());
            String browser = UserAgentParserUtil.getBrowser(event.getUserAgent());
            String os = UserAgentParserUtil.getOperatingSystem(event.getUserAgent());
            String referrerDomain = UserAgentParserUtil.parseReferrerDomain(event.getReferrer());

            // Anonymize IP address for privacy/GDPR (mask last octet)
            String anonymizedIp = maskIp(event.getIpAddress());

            ClickAnalytics analytics = ClickAnalytics.builder()
                    .shortCode(event.getShortCode())
                    .clickedAt(event.getClickedAt() != null ? event.getClickedAt() : Instant.now())
                    .ipAddress(anonymizedIp)
                    .referrer(referrerDomain)
                    .userAgent(truncate(event.getUserAgent(), 500))
                    .deviceType(deviceType)
                    .browser(browser)
                    .operatingSystem(os)
                    .country(event.getCountry() != null ? event.getCountry() : "Unknown")
                    .build();

            analyticsRepository.save(analytics);
            shortUrlRepository.incrementClickCount(event.getShortCode());

            log.debug("Recorded click event for {}: Device={}, Browser={}, OS={}",
                    event.getShortCode(), deviceType, browser, os);
        } catch (Exception e) {
            log.error("Failed to record click analytics for {}: {}", event.getShortCode(), e.getMessage(), e);
        }
    }

    /**
     * Retrieves aggregated analytics metrics for a specific short code.
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String shortCode) {
        ShortUrl url = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new NoSuchElementException("Short URL not found: " + shortCode));

        long totalClicks = url.getClickCount();
        long uniqueVisitors = analyticsRepository.countDistinctVisitorsByShortCode(shortCode);

        Map<String, Long> devices = convertTuplesToMap(analyticsRepository.countByDeviceType(shortCode));
        Map<String, Long> browsers = convertTuplesToMap(analyticsRepository.countByBrowser(shortCode));
        Map<String, Long> osMap = convertTuplesToMap(analyticsRepository.countByOperatingSystem(shortCode));
        Map<String, Long> referrers = convertTuplesToMap(analyticsRepository.countByReferrer(shortCode));
        Map<String, Long> countries = convertTuplesToMap(analyticsRepository.countByCountry(shortCode));

        List<ClickDetailDto> recentClicks = analyticsRepository.findTop50ByShortCodeOrderByClickedAtDesc(shortCode)
                .stream()
                .map(c -> ClickDetailDto.builder()
                        .clickedAt(c.getClickedAt())
                        .referrer(c.getReferrer())
                        .deviceType(c.getDeviceType())
                        .browser(c.getBrowser())
                        .operatingSystem(c.getOperatingSystem())
                        .country(c.getCountry())
                        .build())
                .collect(Collectors.toList());

        return AnalyticsResponse.builder()
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .isExpired(url.isExpired())
                .totalClicks(totalClicks)
                .uniqueVisitors(uniqueVisitors)
                .deviceBreakdown(devices)
                .browserBreakdown(browsers)
                .osBreakdown(osMap)
                .topReferrers(referrers)
                .countryBreakdown(countries)
                .recentClicks(recentClicks)
                .build();
    }

    /**
     * Returns platform-wide analytics overview.
     */
    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse getOverview() {
        long totalActive = shortUrlRepository.countByIsActiveTrue();
        List<ShortUrl> topUrls = shortUrlRepository.findTop10ByOrderByClickCountDesc();
        long totalClicks = topUrls.stream().mapToLong(ShortUrl::getClickCount).sum();

        List<ShortUrlResponse> topResponses = topUrls.stream()
                .map(u -> ShortUrlResponse.builder()
                        .shortCode(u.getShortCode())
                        .shortUrl(baseUrl + "/" + u.getShortCode())
                        .originalUrl(u.getOriginalUrl())
                        .createdAt(u.getCreatedAt())
                        .expiresAt(u.getExpiresAt())
                        .clickCount(u.getClickCount())
                        .customAlias(u.getCustomAlias())
                        .isExpired(u.isExpired())
                        .build())
                .collect(Collectors.toList());

        return AnalyticsOverviewResponse.builder()
                .totalActiveUrls(totalActive)
                .totalClicks(totalClicks)
                .topUrls(topResponses)
                .build();
    }

    private Map<String, Long> convertTuplesToMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] != null ? row[0].toString() : "Unknown";
            Long count = (Long) row[1];
            map.put(key, count);
        }
        return map;
    }

    private String maskIp(String ip) {
        if (ip == null || ip.isBlank()) return "0.0.0.***";
        int lastDot = ip.lastIndexOf('.');
        if (lastDot != -1) {
            return ip.substring(0, lastDot) + ".***";
        }
        int lastColon = ip.lastIndexOf(':');
        if (lastColon != -1) {
            return ip.substring(0, lastColon) + ":****";
        }
        return "***";
    }

    private String truncate(String val, int maxLen) {
        if (val == null) return null;
        return val.length() <= maxLen ? val : val.substring(0, maxLen);
    }
}
