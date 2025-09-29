package com.github.kyrobbins.common.utility.config;

import com.github.kyrobbins.common.interfaces.MaxAgeCache;
import com.github.kyrobbins.common.interfaces.ThrowingFunction;
import com.github.kyrobbins.common.interfaces.ThrowingSupplier;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/** Simple {@link MaxAgeCache} implementation that always returns an empty result */
public class EmptyMaxAgeCache<K, V> implements MaxAgeCache<K, V> {

    @Override
    public Optional<V> get(K key, Duration lifeCycle, Supplier<V> fallback) {
        return Optional.empty();
    }

    @Override
    public <E extends Exception> Optional<V> get(K key, Duration lifeCycle, ThrowingSupplier<V, E> fallback) {
        return Optional.empty();
    }

    @Override
    public Optional<V> get(K key, Duration lifeCycle, Function<K, V> fallback) {
        return Optional.empty();
    }

    @Override
    public <E extends Exception> Optional<V> get(K key, Duration lifeCycle, ThrowingFunction<K, V, E> fallback) {
        return Optional.empty();
    }

    @Override
    public Optional<V> get(K key, long lifeCycleMs, Supplier<V> fallback) {
        return Optional.empty();
    }

    @Override
    public <E extends Exception> Optional<V> get(K key, long lifeCycleMs, ThrowingSupplier<V, E> fallback) {
        return Optional.empty();
    }

    @Override
    public Optional<V> get(K key, long lifeCycleMs, Function<K, V> fallback) {
        return Optional.empty();
    }

    @Override
    public <E extends Exception> Optional<V> get(K key, long lifeCycleMs, ThrowingFunction<K, V, E> fallback) {
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
