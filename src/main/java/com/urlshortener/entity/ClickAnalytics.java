package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "click_analytics",
    indexes = {
        @Index(name = "idx_click_short_code", columnList = "shortCode"),
        @Index(name = "idx_click_time", columnList = "clickedAt"),
        @Index(name = "idx_click_code_time", columnList = "shortCode, clickedAt")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String shortCode;

    @Column(nullable = false)
    @Builder.Default
    private Instant clickedAt = Instant.now();

    @Column(length = 64)
    private String ipAddress;

    @Column(length = 255)
    private String referrer;

    @Column(length = 512)
    private String userAgent;

    @Column(length = 32)
    private String deviceType;

    @Column(length = 32)
    private String browser;

    @Column(length = 32)
    private String operatingSystem;

    @Column(length = 64)
    private String country;
}
