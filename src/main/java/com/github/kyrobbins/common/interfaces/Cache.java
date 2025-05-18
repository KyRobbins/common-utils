package com.github.kyrobbins.common.interfaces;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A simple definition of a caching solution that allows for key value lookups utilizing a cache for faster repeated
 * lookups.
 *
 * @param <K> The type of the Key
 * @param <V> The type of the Value
 */
public interface Cache<K, V> {

    /**
     * Attempt to acquire the value associated with the given key, using the fallback method if none currently exists in
     * the cache.
     *
     * @param key      The key associated with the value to retrieve
     * @param fallback A fallback source to retrieve the value from, potentially caching it depending on the
     *                 implementation
     * @return An {@link Optional} potentially containing the associated value
     */
    Optional<V> get(K key, Supplier<V> fallback);

    /**
     * Attempt to acquire the value associated with the given key, using the fallback method if none currently exists in
     * the cache.
     *
     * @param key      The key associated with the value to retrieve
     * @param fallback A fallback source to retrieve the value from, potentially caching it depending on the
     *                 implementation
     * @return An {@link Optional} potentially containing the associated value
     */
    Optional<V> get(K key, Function<K, V> fallback);
}
