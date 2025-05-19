package com.github.kyrobbins.common.concurrency;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Implementation of {@link Executor} that maintains its own queue of unsubmitted jobs and submits them to a shared
 * {@link Executor} while limiting the number of active jobs at any given time.  This prevents jobs from any single
 * instance completely saturating the shared pool.
 *
 * <p>Instances can be safely garbage collected without any special cleanup.  All jobs submitted to this pool will run to
 * completion before this class is eligible for collection.</p>
 */
@Slf4j
public class PoolSharingExecutor implements Executor {

    /** The real {@link Executor} used to run jobs in parallel. */
    private final Executor sharedPool;
    /** The maximum number of jobs that will be allowed to be active at any given moment */
    @Getter
    private final int maxActiveJobs;
    /** Queue of jobs waiting to be submitted to the shared pool */
    private final List<ReleaseAndSubmitRunner> unsubmittedJobs;
    /** Number of jobs currently active in the shared pool */
    private int activeJobs;

    /**
     * Initializes an instance to use the specified shared pool and active job limit.
     *
     * @param sharedPool The shared pool
     * @param maxActiveJobs The maximum number of jobs allowed to be active at one time.
     */
    public PoolSharingExecutor(Executor sharedPool, int maxActiveJobs) {
        this.sharedPool = sharedPool;
        this.maxActiveJobs = maxActiveJobs;
        unsubmittedJobs = new LinkedList<>();
        activeJobs = 0;
    }

    @Nonnull
    public <T> CompletableFuture<T> submitSupplier(@Nonnull Supplier<T> supplier) {
        final CompletableFuture<T> result = addJobToQueue(new RunnableWithFuture<>(supplier));
        executeQueuedJobs();
        return result;
    }

    @Nonnull
    public <T> List<CompletableFuture<T>> submitSuppliers(@Nonnull List<Supplier<T>> suppliers) {
        final List<CompletableFuture<T>> results = suppliers.stream()
                .map(RunnableWithFuture::new)
                .map(this::addJobToQueue)
                .collect(Collectors.toList());
        executeQueuedJobs();
        return results;
    }

    @Nonnull
    public <T> CompletableFuture<T> submitCallable(@Nonnull Callable<T> callable) {
        final CompletableFuture<T> result = addJobToQueue(new RunnableWithFuture<>(callable));
        executeQueuedJobs();
        return result;
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        addJobToQueue(RunnableWithFuture.fromRunnable(command));
        executeQueuedJobs();
    }

    private synchronized <T> CompletableFuture<T> addJobToQueue(RunnableWithFuture<T> runner) {
        unsubmittedJobs.add(new ReleaseAndSubmitRunner(runner));
        return runner.getResult();
    }

    private synchronized void jobComplete() {
        --activeJobs;
    }

    private synchronized void executeQueuedJobs() {
        while (activeJobs < maxActiveJobs && !unsubmittedJobs.isEmpty()) {
            sharedPool.execute(unsubmittedJobs.remove(0));
            ++activeJobs;
        }
    }

    /** Runnable that executes another runnable and then attempts to run others waiting in the queue */
    @AllArgsConstructor
    private class ReleaseAndSubmitRunner implements Runnable {

        private final RunnableWithFuture<?> realRunnable;

        @Override
        public void run() {
            try {
                realRunnable.run();
            } catch (RuntimeException e) {
                // Should never happen, if it does happen, there is no way to handle it than to log it
                log.error("Runnable threw unchecked exception!", e);
            } finally {
                jobComplete();
            }

            executeQueuedJobs();
        }
    }
}
