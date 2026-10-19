package com.urlshortener.util;

/**
 * High-performance Base62 encoder/decoder.
 * Base62 uses [0-9, a-z, A-Z] (62 alphanumeric characters), making it URL-safe
 * and compact.
 * 
 * Scalability Note:
 * With a 7-character Base62 string, the addressable space is 62^7 = 3,521,614,606,208 (3.5+ trillion).
 * Using a bijective mapping from an autoincrement sequence or distributed Snowflake ID
 * guarantees 0% collision rate and eliminates expensive collision detection roundtrips.
 */
public final class Base62Encoder {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();
    
    // Starting offset so generated codes start at a clean length (e.g., 6-7 characters)
    // 62^3 = 238,328; 62^4 = 14,776,336
    private static final long DEFAULT_OFFSET = 10000000L;

    private Base62Encoder() {
        // utility class
    }

    /**
     * Encodes a positive long ID into a Base62 string with an offset to ensure minimum length.
     */
    public static String encode(long id) {
        long value = id + DEFAULT_OFFSET;
        if (value == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            int remainder = (int) (value % BASE);
            sb.append(ALPHABET.charAt(remainder));
            value /= BASE;
        }

        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back to the original long ID.
     */
    public static long decode(String str) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("Short code cannot be null or empty");
        }

        long id = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int digit = ALPHABET.indexOf(c);
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            id = id * BASE + digit;
        }

        return id - DEFAULT_OFFSET;
    }

    /**
     * Validates custom vanity aliases: alphanumeric, hyphens, and underscores, between 3 and 30 characters.
     */
    public static boolean isValidCustomAlias(String alias) {
        if (alias == null) {
            return false;
        }
        return alias.matches("^[a-zA-Z0-9_-]{3,30}$");
    }
}
