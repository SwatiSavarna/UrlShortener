package com.urlshortener.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Test
    @DisplayName("Bijective encoding: decode(encode(id)) must equal id for various ranges")
    void testEncodeAndDecodeRoundtrip() {
        long[] testIds = {0L, 1L, 42L, 9999L, 1234567L, 987654321L, 5000000000L};

        for (long id : testIds) {
            String encoded = Base62Encoder.encode(id);
            assertNotNull(encoded);
            assertFalse(encoded.isEmpty());

            long decoded = Base62Encoder.decode(encoded);
            assertEquals(id, decoded, "Decoded value must match original for id: " + id);
        }
    }

    @Test
    @DisplayName("Encoded codes must produce alphanumeric strings")
    void testEncodedFormat() {
        String code = Base62Encoder.encode(100L);
        assertTrue(code.matches("^[a-zA-Z0-9]+$"), "Encoded string must be alphanumeric: " + code);
        assertTrue(code.length() >= 4, "Encoded string should meet minimum length due to offset: " + code);
    }

    @Test
    @DisplayName("Invalid characters in Base62 decode must throw IllegalArgumentException")
    void testInvalidDecode() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode("invalid!char"));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode(""));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode(null));
    }

    @Test
    @DisplayName("Custom alias validation rules")
    void testCustomAliasValidation() {
        // Valid aliases
        assertTrue(Base62Encoder.isValidCustomAlias("my-link"));
        assertTrue(Base62Encoder.isValidCustomAlias("promo_2026"));
        assertTrue(Base62Encoder.isValidCustomAlias("dev"));
        assertTrue(Base62Encoder.isValidCustomAlias("SpringBootRocks123"));

        // Invalid aliases
        assertFalse(Base62Encoder.isValidCustomAlias("ab")); // too short (< 3)
        assertFalse(Base62Encoder.isValidCustomAlias("a".repeat(31))); // too long (> 30)
        assertFalse(Base62Encoder.isValidCustomAlias("link with spaces"));
        assertFalse(Base62Encoder.isValidCustomAlias("link@special#char"));
        assertFalse(Base62Encoder.isValidCustomAlias(null));
        assertFalse(Base62Encoder.isValidCustomAlias(""));
    }
}
