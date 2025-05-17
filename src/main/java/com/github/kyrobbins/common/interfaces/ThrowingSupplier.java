package com.github.kyrobbins.common.interfaces;

/**
 * A {@link FunctionalInterface} for representing an operation that returns a value, with a potential for
 * throwing some {@link Throwable}.
 *
 * @param <R> The type of value returned by the operation
 * @param <E> The type of throwable the operation might throw
 */
@FunctionalInterface
public interface ThrowingSupplier<R, E extends Throwable> {

    /**
     * Returns a value
     *
     * @return The result of the operation
     * @throws E If there was an exception during the operation's execution
     */
    R get() throws E;
}
