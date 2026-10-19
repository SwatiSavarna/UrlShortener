package com.urlshortener.repository;

import com.urlshortener.entity.ClickAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {

    long countByShortCode(String shortCode);

    @Query("SELECT COUNT(DISTINCT c.ipAddress) FROM ClickAnalytics c WHERE c.shortCode = :shortCode")
    long countDistinctVisitorsByShortCode(@Param("shortCode") String shortCode);

    @Query("SELECT c.deviceType, COUNT(c) FROM ClickAnalytics c WHERE c.shortCode = :shortCode GROUP BY c.deviceType")
    List<Object[]> countByDeviceType(@Param("shortCode") String shortCode);

    @Query("SELECT c.browser, COUNT(c) FROM ClickAnalytics c WHERE c.shortCode = :shortCode GROUP BY c.browser")
    List<Object[]> countByBrowser(@Param("shortCode") String shortCode);

    @Query("SELECT c.operatingSystem, COUNT(c) FROM ClickAnalytics c WHERE c.shortCode = :shortCode GROUP BY c.operatingSystem")
    List<Object[]> countByOperatingSystem(@Param("shortCode") String shortCode);

    @Query("SELECT c.referrer, COUNT(c) FROM ClickAnalytics c WHERE c.shortCode = :shortCode GROUP BY c.referrer ORDER BY COUNT(c) DESC")
    List<Object[]> countByReferrer(@Param("shortCode") String shortCode);

    @Query("SELECT c.country, COUNT(c) FROM ClickAnalytics c WHERE c.shortCode = :shortCode GROUP BY c.country ORDER BY COUNT(c) DESC")
    List<Object[]> countByCountry(@Param("shortCode") String shortCode);

    List<ClickAnalytics> findTop50ByShortCodeOrderByClickedAtDesc(String shortCode);

    @Modifying
    @Query("DELETE FROM ClickAnalytics c WHERE c.clickedAt < :cutoff")
    int deleteOldAnalytics(@Param("cutoff") Instant cutoff);
}
