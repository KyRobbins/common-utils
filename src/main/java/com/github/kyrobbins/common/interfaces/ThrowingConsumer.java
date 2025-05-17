package com.github.kyrobbins.common.interfaces;

/**
 * A {@link FunctionalInterface} for representing an operation that takes a single value, with a potential for
 * throwing some {@link Throwable}.
 *
 * @param <T> The type of value taken in by the operation
 * @param <E> The type of throwable the operation might throw
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {

    /**
     * Takes in a value to perform some operation
     *
     * @param in The value being taken in by the operation
     * @throws E If there was an exception during the operation's execution
     */
    void accept(T in) throws E;
}
