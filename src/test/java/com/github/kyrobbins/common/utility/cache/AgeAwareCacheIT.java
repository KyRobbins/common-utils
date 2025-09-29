package com.github.kyrobbins.common.utility.cache;

import com.github.kyrobbins.common.interfaces.ThrowingFunction;
import com.github.kyrobbins.common.interfaces.ThrowingSupplier;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgeAwareCacheIT {

    private final TestClock mockClock = new TestClock(ZoneId.systemDefault());

    private final AgeAwareCache<String, String> cache = new AgeAwareCache<>(mockClock);

    @Test
    void get() throws Exception {
        final String key = "key";

        int valueCounter = 0;
        AtomicInteger expectedValue = new AtomicInteger(0);

        final Supplier<String> nextSupplierValue = () -> Integer.toString(expectedValue.incrementAndGet());
        final ThrowingSupplier<String, Exception> nextTSupplierValue = nextSupplierValue::get;
        final Function<String, String> nextFunctionValue = k -> nextSupplierValue.get();
        final ThrowingFunction<String, String, Exception> nextTFunctionValue = nextFunctionValue::apply;

        final Function<Integer, Optional<String>> optString = i -> Optional.of(Integer.toString(i));

        final List<Callable<Optional<String>>> cacheCalls = Arrays.asList(
                () -> cache.get(key, Duration.ofMillis(5), nextFunctionValue),
                () -> cache.get(key, Duration.ofMillis(5), nextTFunctionValue),
                () -> cache.get(key, Duration.ofMillis(5), nextSupplierValue),
                () -> cache.get(key, Duration.ofMillis(5), nextTSupplierValue));

        // Gets without lifetime should always do new lookup
        mockClock.setTime(0);
        assertEquals(optString.apply(++valueCounter), cache.get(key, nextFunctionValue));
        assertEquals(optString.apply(++valueCounter), cache.get(key, nextTFunctionValue));
        assertEquals(optString.apply(++valueCounter), cache.get(key, nextSupplierValue));
        assertEquals(optString.apply(++valueCounter), cache.get(key, nextTSupplierValue));

        // Gets with lifetime should do lookup when time has expired
        for (int i = 0; i < cacheCalls.size(); ++i) {
            // Test that each method will do lookup when time has expired
            mockClock.setTime((i + 1) * 6L);
            assertEquals(optString.apply(++valueCounter), cacheCalls.get(i).call());

            // Check that every method returns same value when time hasn't expired
            for (final Callable<Optional<String>> callable : cacheCalls) {
                assertEquals(optString.apply(valueCounter), callable.call());
            }
        }
    }

    @RequiredArgsConstructor
    public static class TestClock extends Clock {

        private final ZoneId zoneId;
        @Setter
        private long time;

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new TestClock(zoneId);
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(time);
        }
    }
}
