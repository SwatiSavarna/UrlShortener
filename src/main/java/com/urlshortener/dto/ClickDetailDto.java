package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickDetailDto {

    private Instant clickedAt;
    private String referrer;
    private String deviceType;
    private String browser;
    private String operatingSystem;
    private String country;
}
