package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUrlRequest {

    @NotBlank(message = "Original URL cannot be blank")
    @Size(max = 2048, message = "Original URL must be at most 2048 characters")
    @Pattern(
        regexp = "^(https?://).+",
        message = "URL must start with http:// or https://"
    )
    private String originalUrl;

    @Size(min = 3, max = 30, message = "Custom alias must be between 3 and 30 characters")
    private String customAlias;

    /**
     * Duration string representing TTL: e.g. "30m", "1h", "24h", "7d", "30d".
     */
    private String ttlDuration;

    /**
     * Specific future expiration timestamp (alternative to ttlDuration).
     */
    private Instant expiresAt;
}
