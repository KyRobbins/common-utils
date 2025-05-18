package com.github.kyrobbins.common.utility.cache;

import com.github.kyrobbins.common.interfaces.AgeAwareCache;
import lombok.Data;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An {@link AgeAwareCache} implementation that bases the logic on already cached values.  The lifecycle value given in
 * the method calls is treated as a "No older than" value, meaning if the cached value was cached for longer than the
 * specified lifecycle duration, a new value will be retrieved.
 *
 * @param <K> The type of the key
 * @param <V> The type of the value stored
 */
public class MaxAgeCache<K, V> implements AgeAwareCache<K, V> {

    /** Clock instance used to acquire time data */
    private final Clock clock;
    /** The map used to store cached values */
    private final Map<K, TtlEntry<V>> cache = new HashMap<>();

    public MaxAgeCache() {
        this(Clock.systemDefaultZone());
    }

    public MaxAgeCache(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<V> get(K key, Duration lifeCycle, Supplier<V> fallback) {
        return get(key, lifeCycle.toMillis(), fallback);
    }

    @Override
    public Optional<V> get(K key, Duration lifeCycle, Function<K, V> fallback) {
        return get(key, lifeCycle.toMillis(), () -> fallback.apply(key));
    }

    @Override
    public Optional<V> get(K key, long lifeCycleMs, Function<K, V> fallback) {
        return get(key, lifeCycleMs, () -> fallback.apply(key));
    }

    @Override
    public Optional<V> get(K key, Supplier<V> fallback) {
        return get(key, Duration.ZERO, fallback);
    }

    @Override
    public Optional<V> get(K key, Function<K, V> fallback) {
        return get(key, () -> fallback.apply(key));
    }

    /**
     * Attempts to retrieve a value associated with the given key, using the fallback solution if the value does not
     * exist in the cache, or is older than the specified age value
     *
     * @param key         The key associated with the value to retrieve
     * @param noOlderThan The max age the stored value can be before requiring a new lookup.
     * @param fallback    A fallback source to get the value from if is not cached or is too old
     * @return An {@link Optional} potentially containing the associated value
     */
    @Override
    public Optional<V> get(K key, long noOlderThan, Supplier<V> fallback) {
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
