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

    /**
     * Takes in three values, and returns another value
     *
     * @param in1 The first value being taken in by the function
     * @param in2 The second value being taken in by the function
     * @param in3 The third value being taken in by the function
     * @return The result of the operation
     */
    R apply(T in1, U in2, V in3);
}
