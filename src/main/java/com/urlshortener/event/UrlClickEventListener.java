package com.urlshortener.event;

import com.urlshortener.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UrlClickEventListener {

    private final AnalyticsService analyticsService;

    @Async("analyticsExecutor")
    @EventListener
    public void handleUrlClickEvent(UrlClickEvent event) {
        log.debug("Async click event received for code: {}", event.getShortCode());
        analyticsService.recordClick(event);
    }
}
