package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {

    private String shortCode;
    private String originalUrl;
    private String shortUrl;
    private Instant createdAt;
    private Instant expiresAt;
    private Boolean isExpired;
    private Long totalClicks;
    private Long uniqueVisitors;

    private Map<String, Long> deviceBreakdown;
    private Map<String, Long> browserBreakdown;
    private Map<String, Long> osBreakdown;
    private Map<String, Long> topReferrers;
    private Map<String, Long> countryBreakdown;

    private List<ClickDetailDto> recentClicks;
}
