package com.urlshortener.util;

/**
 * High-performance, lightweight User-Agent parser.
 * Avoids heavy third-party regex-based libraries to keep memory footprint low
 * and latency minimal (< 0.1ms).
 */
public final class UserAgentParserUtil {

    private UserAgentParserUtil() {
    }

    public static String getDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("bot") || ua.contains("crawl") || ua.contains("spider") || ua.contains("slurp")) {
            return "Bot";
        }
        if (ua.contains("tablet") || ua.contains("ipad") || (ua.contains("android") && !ua.contains("mobile"))) {
            return "Tablet";
        }
        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("android") || ua.contains("phone")) {
            return "Mobile";
        }
        return "Desktop";
    }

    public static String getOperatingSystem(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) {
            return "Windows";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) {
            return "iOS";
        }
        if (ua.contains("macintosh") || ua.contains("mac os x")) {
            return "macOS";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        if (ua.contains("cros")) {
            return "Chrome OS";
        }
        return "Other";
    }

    public static String getBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/") || ua.contains("edge/")) {
            return "Edge";
        }
        if (ua.contains("opr/") || ua.contains("opera")) {
            return "Opera";
        }
        if (ua.contains("chrome") && !ua.contains("edg") && !ua.contains("opr")) {
            return "Chrome";
        }
        if (ua.contains("safari") && !ua.contains("chrome")) {
            return "Safari";
        }
        if (ua.contains("firefox")) {
            return "Firefox";
        }
        return "Other";
    }

    public static String parseReferrerDomain(String referrer) {
        if (referrer == null || referrer.isBlank() || referrer.equalsIgnoreCase("null") || referrer.equalsIgnoreCase("undefined")) {
            return "Direct";
        }
        try {
            String clean = referrer.trim();
            if (clean.startsWith("http://") || clean.startsWith("https://")) {
                java.net.URI uri = java.net.URI.create(clean);
                String host = uri.getHost();
                if (host != null) {
                    return host.startsWith("www.") ? host.substring(4) : host;
                }
            }
            return clean;
        } catch (Exception e) {
            return "Other";
        }
    }
}
