package com.github.kyrobbins.common.utility;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadingUtility {

    /**
     * Attempts to gracefully shut down the given service, disabling any new tasks and allowing time for the currently
     * running tasks to complete before attempting a forceful shutdown.
     *
     * @param service The service to shut down / terminate
     * @param timeout The amount of time to allow the shutdown to happen
     * @return True if the service gracefully shut down, False otherwise
     */
    public boolean shutdownAndAwaitTermination(ExecutorService service, Duration timeout) {
        long halfTimeoutNanos = timeout.toNanos() / 2;
        service.shutdown();

        try {
            if (!service.awaitTermination(halfTimeoutNanos, TimeUnit.NANOSECONDS)) {
                service.shutdownNow();
                service.awaitTermination(halfTimeoutNanos, TimeUnit.NANOSECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            service.shutdownNow();
        }

        return service.isTerminated();
    }
}
