package com.github.kyrobbins.common.interfaces;

/**
 * A {@link FunctionalInterface} for representing a function that takes in two values and returns a result, possibly
 * throwing some {@link Throwable} during the operation's processing.
 *
 * @param <T> The type of value 1 taken in by the function
 * @param <U> The type of value 2 taken in by the function
 * @param <R> The type of value returned by the function
 * @param <E> The type of {@link Throwable} potentially thrown by the function
 */
@FunctionalInterface
public interface ThrowingBiFunction<T, U, R, E extends Throwable> {

    /**
     * Takes in two values, and returns another value, possibly throwing during the operation
     *
     * @param in1 The first value being taken in by the function
     * @param in2 The second value being taken in by the function
     * @return The result of the operation
     * @throws E If there was an exception during the function's execution
     */
    R apply(T in1, U in2) throws E;
}
