package com.urlshortener.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Bypass static assets, Swagger, Actuator, and H2 console
        if (shouldBypass(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimiterService.Policy policy = determinePolicy(path, method);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        RateLimiterService.RateLimitResult result = rateLimiterService.tryConsume(clientIp, policy);

        // Standard rate-limiting headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetSeconds()));

        if (!result.isAllowed()) {
            log.warn("Rate limit exceeded for IP {} on path {} (policy: {})", clientIp, path, policy);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String json = String.format(
                    "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again in %d seconds.\",\"path\":\"%s\"}",
                    Instant.now(), result.getResetSeconds(), path
            );
            response.getWriter().write(json);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimiterService.Policy determinePolicy(String path, String method) {
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/v1/urls")) {
            return RateLimiterService.Policy.URL_CREATION;
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/v1/analytics")) {
            return RateLimiterService.Policy.ANALYTICS;
        }
        if ("GET".equalsIgnoreCase(method) && !path.startsWith("/api/") && path.length() > 1) {
            return RateLimiterService.Policy.REDIRECT;
        }
        return null;
    }

    private boolean shouldBypass(String path) {
        return path.startsWith("/h2-console")
                || path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/")
                || path.equals("/index.html")
                || path.equals("/favicon.ico")
                || path.endsWith(".js")
                || path.endsWith(".css")
                || path.endsWith(".png")
                || path.endsWith(".svg");
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
