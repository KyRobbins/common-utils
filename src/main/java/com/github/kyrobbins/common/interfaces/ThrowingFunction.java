package com.github.kyrobbins.common.interfaces;

import java.util.function.Function;

/**
 * A {@link FunctionalInterface} for representing a function that takes in some value and returns a result, possibly
 * throwing some {@link Throwable} during the operation's processing.
 *
 * @param <T> The type of value taken in by the function
 * @param <R> The type of value returned by the function
 * @param <E> The type of {@link Throwable} potentially thrown by the function
 */
@FunctionalInterface
public interface ThrowingFunction <T, R, E extends Throwable> {

    /**
     * Takes in a value, and returns another value, possibly throwing during the operation
     *
     * @param in The value being taken in by the function
     * @return The result of the operation
     * @throws E If there was an exception during the function's execution
     */
    R apply(T in) throws E;
}
