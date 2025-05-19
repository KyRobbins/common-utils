package com.github.kyrobbins.common.concurrency;

import com.github.kyrobbins.common.utility.ThreadingUtility;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of {@link Executor} that uses a private thread pool with a fixed number of threads.  Includes a
 * {@link PreDestroy} annotation method so that the pool can automatically terminate at application shutdown.
 */
@Slf4j
public class SharedThreadPool implements Executor {

    /** The thread pool being managed */
    private final ExecutorService threadPool;
    /** Maximum time to wait for the pool to shut down */
    private final Duration shutdownTimeout;
    /** Utility for managing threading actions */
    private final ThreadingUtility threadingUtility;

    public SharedThreadPool(int maxThreads, Duration shutdownTimeout) {
        this(maxThreads, shutdownTimeout, new ThreadingUtility());
    }

    public SharedThreadPool(int maxThreads, Duration shutdownTimeout, ThreadingUtility threadingUtility) {
        this.threadPool = Executors.newFixedThreadPool(maxThreads);
        this.shutdownTimeout = shutdownTimeout;
        this.threadingUtility = threadingUtility;
    }

    @Override
    public void execute(@Nonnull Runnable runnable) {
        threadPool.execute(runnable);
    }

    /**
     * SHuts down the thread pool and waits for all jobs to terminate
     *
     * @return True if shutdown was successful and all jobs were terminated
     */
    @PreDestroy
    public boolean shutdown() {
        threadPool.shutdown();
        final boolean terminated = threadingUtility.shutdownAndAwaitTermination(threadPool, shutdownTimeout);

        if (!terminated) {
            log.error("Thread pool failed to terminate within timeout period");
        }

        return terminated;
    }
}
