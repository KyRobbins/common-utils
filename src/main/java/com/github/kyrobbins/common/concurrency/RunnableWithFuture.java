package com.github.kyrobbins.common.concurrency;

import jakarta.annotation.Nonnull;
import lombok.Getter;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Runnable that performs some work using a {@link Callable} and passes its result to a {@link CompletableFuture}
 *
 * @param <T> The type of result
 */
@Getter
public class RunnableWithFuture<T> implements Runnable {

    /** Performs the work to compute a result */
    private final Callable<T> work;
    /** Allows clients to wait for and retrieve the computed result */
    private final CompletableFuture<T> result;

    /**
     * Initialize an instance using an arbitrary {@link Runnable}
     *
     * @param work Called to produce a result
     */
    public RunnableWithFuture(@Nonnull Runnable work) {
        this.work = () -> {
            work.run();
            return null;
        };
        result = new CompletableFuture<>();
    }

    /**
     * Initialize an instance using a {@link java.util.function.Supplier}
     *
     * @param work Called to produce a result
     */
    public RunnableWithFuture(@Nonnull Supplier<T> work) {
        this.work = work::get;
        result = new CompletableFuture<>();
    }

    /**
     * Initialize an instance using a {@link Callable}
     *
     * @param work Called to produce the result
     */
    public RunnableWithFuture(@Nonnull Callable<T> work) {
        this.work = work;
        result = new CompletableFuture<>();
    }

    /**
     * Converts an arbitrary {@link Runnable} into an instance.  If the provided object is already an instance, just
     * cast and return it, otherwise create a new instance that just returns null when executed.
     *
     * @param runnable Arbitrary {@link Runnable} to convert
     * @return Valid instance that executes the provided {@link Runnable}
     */
    public static RunnableWithFuture<?> fromRunnable(Runnable runnable) {
        if (runnable instanceof RunnableWithFuture<?>) {
            return (RunnableWithFuture<?>) runnable;
        } else {
            return new RunnableWithFuture<Void>(runnable);
        }
    }

    /** Performs the work and sends the result or exception to the {@link CompletableFuture} */
    @Override
    public void run() {
        try {
            final T value = work.call();
            result.complete(value);
        } catch (Exception e) {
            result.completeExceptionally(e);
        }
    }
}
