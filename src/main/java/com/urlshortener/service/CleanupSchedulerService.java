package com.urlshortener.service;

import com.urlshortener.repository.ClickAnalyticsRepository;
import com.urlshortener.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduled background tasks for maintaining database hygiene.
 * 
 * Scalability Architecture:
 * - Active TTL Cleanup: In addition to passive expiration at query time, periodic batch deletes
 *   prevent database index bloat and keep read scans compact.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupSchedulerService {

    private final ShortUrlRepository shortUrlRepository;
    private final ClickAnalyticsRepository analyticsRepository;

    /**
     * Purges expired URLs every 30 minutes.
     */
    @Scheduled(fixedRateString = "${app.cleanup.rate-ms:1800000}")
    @Transactional
    public void purgeExpiredUrls() {
        log.info("Starting scheduled cleanup of expired short URLs...");
        try {
            int deletedUrls = shortUrlRepository.deleteExpiredUrls(Instant.now());
            log.info("Expired URL cleanup completed. Deleted {} records.", deletedUrls);

            // Retain analytics history for 90 days
            Instant cutoff = Instant.now().minus(Duration.ofDays(90));
            int deletedAnalytics = analyticsRepository.deleteOldAnalytics(cutoff);
            log.info("Old analytics cleanup completed. Deleted {} records older than 90 days.", deletedAnalytics);
        } catch (Exception e) {
            log.error("Error occurred during scheduled cleanup: {}", e.getMessage(), e);
        }
    }
}
