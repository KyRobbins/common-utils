package com.github.kyrobbins.common.interfaces;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link Cache} extension type that allows for caching based on some age-awareness, where the specific details of the
 * lifecycle handling depends on the implementation.
 *
 * @param <K> The type of the Key
 * @param <V> The type of the Value
 */
public interface AgeAwareCache<K, V> extends Cache<K, V> {

    /**
     * Attempt to acquire the value associated with the given key, using the fallback method if none currently exists in
     * the cache.
     *
     * @param key       The key associated with the value to retrieve
     * @param lifeCycle The lifecycle of the cache entry, exact behavior is implementation-dependent
     * @param fallback  A fallback source to retrieve the value from, potentially caching it depending on the
     *                  implementation
     * @return An {@link Optional} potentially containing the associated value
     */
    Optional<V> get(K key, Duration lifeCycle, Supplier<V> fallback);

    /**
     * Attempt to acquire the value associated with the given key, using the fallback method if none currently exists in
     * the cache.
     *
     * @param key       The key associated with the value to retrieve
     * @param lifeCycle The lifecycle of the cache entry, exact behavior is implementation-dependent
     * @param fallback  A fallback source to retrieve the value from, potentially caching it depending on the
     *                  implementation
     * @return An {@link Optional} potentially containing the associated value
     */
    Optional<V> get(K key, Duration lifeCycle, Function<K, V> fallback);

    /**
     * Attempt to acquire the value associated with the given key, using the fallback method if none currently exists in
     * the cache.
     *
     * @param key         The key associated with the value to retrieve
     * @param lifeCycleMs The lifecycle of the cache entry, exact behavior is implementation-dependent
     * @param fallback    A fallback source to retrieve the value from, potentially caching it depending on the
     *                    implementation
     * @return An {@link Optional} potentially containing the associated value
     */
    Optional<V> get(K key, long lifeCycleMs, Supplier<V> fallback);

    /**
     * Attempt to acquire the value associated with the given key, using the fallback method if none currently exists in
     * the cache.
     *
     * @param key         The key associated with the value to retrieve
     * @param lifeCycleMs The lifecycle of the cache entry, exact behavior is implementation-dependent
     * @param fallback    A fallback source to retrieve the value from, potentially caching it depending on the
     *                    implementation
     * @return An {@link Optional} potentially containing the associated value
     */
    Optional<V> get(K key, long lifeCycleMs, Function<K, V> fallback);
}
