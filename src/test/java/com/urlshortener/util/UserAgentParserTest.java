package com.urlshortener.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserAgentParserTest {

    private static final String CHROME_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String SAFARI_IPHONE = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
    private static final String FIREFOX_LINUX = "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/119.0";
    private static final String GOOGLEBOT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";

    @Test
    @DisplayName("Device detection from User-Agent")
    void testDeviceDetection() {
        assertEquals("Desktop", UserAgentParserUtil.getDeviceType(CHROME_WINDOWS));
        assertEquals("Mobile", UserAgentParserUtil.getDeviceType(SAFARI_IPHONE));
        assertEquals("Desktop", UserAgentParserUtil.getDeviceType(FIREFOX_LINUX));
        assertEquals("Bot", UserAgentParserUtil.getDeviceType(GOOGLEBOT));
        assertEquals("Unknown", UserAgentParserUtil.getDeviceType(null));
    }

    @Test
    @DisplayName("Browser detection from User-Agent")
    void testBrowserDetection() {
        assertEquals("Chrome", UserAgentParserUtil.getBrowser(CHROME_WINDOWS));
        assertEquals("Safari", UserAgentParserUtil.getBrowser(SAFARI_IPHONE));
        assertEquals("Firefox", UserAgentParserUtil.getBrowser(FIREFOX_LINUX));
        assertEquals("Unknown", UserAgentParserUtil.getBrowser(null));
    }

    @Test
    @DisplayName("Operating System detection from User-Agent")
    void testOsDetection() {
        assertEquals("Windows", UserAgentParserUtil.getOperatingSystem(CHROME_WINDOWS));
        assertEquals("iOS", UserAgentParserUtil.getOperatingSystem(SAFARI_IPHONE));
        assertEquals("Linux", UserAgentParserUtil.getOperatingSystem(FIREFOX_LINUX));
        assertEquals("Unknown", UserAgentParserUtil.getOperatingSystem(null));
    }

    @Test
    @DisplayName("Referrer domain parsing")
    void testReferrerParsing() {
        assertEquals("google.com", UserAgentParserUtil.parseReferrerDomain("https://www.google.com/search?q=spring"));
        assertEquals("twitter.com", UserAgentParserUtil.parseReferrerDomain("https://twitter.com/dev/status/123"));
        assertEquals("Direct", UserAgentParserUtil.parseReferrerDomain(null));
        assertEquals("Direct", UserAgentParserUtil.parseReferrerDomain(""));
    }
}
