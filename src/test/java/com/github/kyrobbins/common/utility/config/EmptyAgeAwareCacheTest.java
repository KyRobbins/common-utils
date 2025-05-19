package com.github.kyrobbins.common.utility.config;

import com.github.kyrobbins.common.interfaces.AgeAwareCache;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;

class EmptyAgeAwareCacheTest {

    @Test
    void get_returnsEmpty() {
        AgeAwareCache<String, String> cache = new EmptyAgeAwareCache<>();

        assertFalse(cache.get("key", Duration.ZERO, () -> "value").isPresent());
        assertFalse(cache.get("key", Duration.ZERO, k -> "value").isPresent());
        assertFalse(cache.get("key", 0L, () -> "value").isPresent());
        assertFalse(cache.get("key", 0L, k -> "value").isPresent());
        assertFalse(cache.get("key", () -> "value").isPresent());
        assertFalse(cache.get("key", k -> "value").isPresent());
    }

}
