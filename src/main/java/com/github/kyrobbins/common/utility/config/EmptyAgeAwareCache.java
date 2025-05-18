package com.github.kyrobbins.common.utility.config;

import com.github.kyrobbins.common.interfaces.AgeAwareCache;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/** Simple {@link AgeAwareCache} implementation that always returns an empty result */
public class EmptyAgeAwareCache<K, V> implements AgeAwareCache<K, V> {
    @Override
    public Optional<V> get(K key, Duration lifeCycle, Supplier<V> fallback) {
        return Optional.empty();
    }

    @Override
    public Optional<V> get(K key, Duration lifeCycle, Function<K, V> fallback) {
        return Optional.empty();
    }

    @Override
    public Optional<V> get(K key, long lifeCycleMs, Supplier<V> fallback) {
        return Optional.empty();
    }

    @Override
    public Optional<V> get(K key, long lifeCycleMs, Function<K, V> fallback) {
        return Optional.empty();
    }

    @Override
    public Optional<V> get(K key, Supplier<V> fallback) {
        return Optional.empty();
    }

    @Override
    public Optional<V> get(K key, Function<K, V> fallback) {
        return Optional.empty();
    }
}
