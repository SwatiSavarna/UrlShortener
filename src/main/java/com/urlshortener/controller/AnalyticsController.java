package com.urlshortener.controller;

import com.urlshortener.dto.AnalyticsOverviewResponse;
import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Retrieves detailed click analytics and demographic metrics for a short code.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<AnalyticsResponse> getUrlAnalytics(@PathVariable("shortCode") String shortCode) {
        AnalyticsResponse response = analyticsService.getAnalytics(shortCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves global platform statistics and top active short URLs.
     */
    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> getOverview() {
        AnalyticsOverviewResponse response = analyticsService.getOverview();
        return ResponseEntity.ok(response);
    }
}
