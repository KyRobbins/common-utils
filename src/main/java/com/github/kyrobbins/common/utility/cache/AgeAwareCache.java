package com.github.kyrobbins.common.utility.cache;

import com.github.kyrobbins.common.interfaces.MaxAgeCache;
import com.github.kyrobbins.common.interfaces.ThrowingFunction;
import com.github.kyrobbins.common.interfaces.ThrowingSupplier;
import lombok.Data;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link MaxAgeCache} implementation that bases the logic on already cached values.  The lifecycle value given in the
 * method calls is treated as a "No older than" value, meaning if the cached value was cached for longer than the
 * specified lifecycle duration, a new value will be retrieved.
 *
 * @param <K> The type of the key
 * @param <V> The type of the value stored
 */
public class AgeAwareCache<K, V> implements MaxAgeCache<K, V> {

    /** Clock instance used to acquire time data */
    private final Clock clock;
    /** The map used to store cached values */
    private final Map<K, TtlEntry<V>> cache = new HashMap<>();

    public AgeAwareCache() {
        this(Clock.systemDefaultZone());
    }

    public AgeAwareCache(Clock clock) {
        this.clock = clock;
    }


    @Override
    public Optional<V> get(K key, Supplier<V> fallback) {
        return get(key, Duration.ZERO, fallback);
    }

    public <E extends Exception> Optional<V> get(K key, ThrowingSupplier<V, E> fallback) throws E {
        return get(key, Duration.ZERO, fallback);
    }

    @Override
    public Optional<V> get(K key, Function<K, V> fallback) {
        return get(key, Duration.ZERO, fallback);
    }

    public <E extends Exception> Optional<V> get(K key, ThrowingFunction<K, V, E> fallback) throws E {
        return get(key, Duration.ZERO, fallback);
    }

    @Override
    public Optional<V> get(K key, Duration noOlderThan, Supplier<V> fallback) {
        return get(key, noOlderThan.toMillis(), fallback);
    }

    @Override
    public <E extends Exception> Optional<V> get(K key, Duration noOlderThan, ThrowingSupplier<V, E> fallback) throws E {
        return get(key, noOlderThan.toMillis(), fallback);
    }

    @Override
    public Optional<V> get(K key, Duration noOlderThan, Function<K, V> fallback) {
        return get(key, noOlderThan.toMillis(), fallback);
    }

    @Override
    public <E extends Exception> Optional<V> get(K key, Duration lifeCycle, ThrowingFunction<K, V, E> fallback) throws E {
        return get(key, lifeCycle.toMillis(), fallback);
    }

    @Override
    public Optional<V> get(K key, long noOlderThan, Function<K, V> fallback) {
        return get(key, noOlderThan, (Supplier<V>) () -> fallback.apply(key));
    }

    @Override
    public <E extends Exception> Optional<V> get(K key, long noOlderThan, ThrowingFunction<K, V, E> fallback) throws E {
        return get(key, noOlderThan, (ThrowingSupplier<V, E>) () -> fallback.apply(key));
    }

    @Override
    public Optional<V> get(K key, long noOlderThan, Supplier<V> fallback) {
        return get(key, noOlderThan, (ThrowingSupplier<V, ? extends RuntimeException>) fallback::get);
    }

    @Override
    public <E extends Exception> Optional<V> get(K key, long noOlderThan, ThrowingSupplier<V, E> fallback) throws E {
        final long milliSecNow = clock.millis();
        final TtlEntry<V> valueFromCache = cache.get(key);
        V valueToReturn;

        boolean requiresLookup = valueFromCache == null || valueFromCache.getCreatedOn() + noOlderThan <= milliSecNow;

        if (requiresLookup) {
            final V valueFromLookup = fallback.get();

            if (valueFromLookup != null) {
                cache.put(key, new TtlEntry<>(valueFromLookup, milliSecNow));
            }

            valueToReturn = valueFromLookup;
        } else {
            valueToReturn = valueFromCache.getValue();
        }

        return Optional.ofNullable(valueToReturn);
    }

    @Data
    private static class TtlEntry<V> {

        private final V value;
        private final long createdOn;
    }
}
