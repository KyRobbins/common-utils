package com.github.kyrobbins.common.interfaces;

/**
 * A {@link FunctionalInterface} for representing a function that takes in three values and returns a result.
 *
 * @param <T> The type of value1 taken in by the function
 * @param <U> The type of value2 taken in by the function
 * @param <V> The type of value3 taken in by the function
 * @param <R> The type of value returned by the function
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R> {

    R apply(T t, U u, V v);
}
