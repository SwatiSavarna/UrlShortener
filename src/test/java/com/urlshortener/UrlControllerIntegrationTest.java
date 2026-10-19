package com.urlshortener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("End-to-End: Shorten URL -> Redirect -> Check Analytics -> Check Rate Limit Headers")
    void testEndToEndUrlShorteningAndAnalytics() throws Exception {
        // 1. Create Short URL
        String jsonPayload = """
                {
                  "originalUrl": "https://spring.io/projects/spring-boot",
                  "customAlias": "spring-boot-guide",
                  "ttlDuration": "24h"
                }
                """;

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(jsonPath("$.shortCode", is("spring-boot-guide")))
                .andExpect(jsonPath("$.originalUrl", is("https://spring.io/projects/spring-boot")))
                .andExpect(jsonPath("$.customAlias", is(true)))
                .andExpect(jsonPath("$.expiresAt", notNullValue()));

        // 2. Perform Redirection (HTTP 302 Found)
        mockMvc.perform(get("/spring-boot-guide")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                        .header("Referer", "https://news.ycombinator.com"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://spring.io/projects/spring-boot"))
                .andExpect(header().exists("X-RateLimit-Limit"));

        // Wait brief moment for async analytics thread pool to log event
        Thread.sleep(300);

        // 3. Inspect Analytics
        mockMvc.perform(get("/api/v1/analytics/spring-boot-guide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode", is("spring-boot-guide")))
                .andExpect(jsonPath("$.totalClicks", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.deviceBreakdown.Desktop", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.browserBreakdown.Chrome", greaterThanOrEqualTo(1)));

        // 4. Test Non-existent Short URL (HTTP 404)
        mockMvc.perform(get("/non-existent-code"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("Validation Error: Invalid URL format should return 400 Bad Request")
    void testInvalidUrlFormat() throws Exception {
        String badRequestJson = """
                {
                  "originalUrl": "not-a-valid-http-url"
                }
                """;

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badRequestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", notNullValue()));
    }
}
