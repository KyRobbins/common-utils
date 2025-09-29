package com.github.kyrobbins.common.utility.config;

import com.github.kyrobbins.common.utility.cache.MaxAgeCache;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;

class EmptyMaxAgeCacheTest {

    @Test
    void get_returnsEmpty() {
        MaxAgeCache<String, String> cache = new EmptyMaxAgeCache<>();

        assertFalse(cache.get("key", Duration.ZERO, supplier(() -> "value")).isPresent());
        assertFalse(cache.get("key", Duration.ZERO, function(k -> "value")).isPresent());
        assertFalse(cache.get("key", 0L, supplier(() -> "value")).isPresent());
        assertFalse(cache.get("key", 0L, function(k -> "value")).isPresent());
        assertFalse(cache.get("key", () -> "value").isPresent());
        assertFalse(cache.get("key", k -> "value").isPresent());
    }

    private static Supplier<String> supplier(Supplier<String> supplier) {
        return supplier;
    }

    private static Function<String, String> function(Function<String, String> function) {
        return function;
    }

}
