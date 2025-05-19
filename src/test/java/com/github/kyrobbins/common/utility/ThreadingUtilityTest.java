package com.github.kyrobbins.common.utility;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadingUtilityTest {

    private final ExecutorService service = Executors.newSingleThreadExecutor();

    @Test
    void shutdownAndAwaitTermination_shutsDownWithinTime() {
        ThreadingUtility utility = new ThreadingUtility();

        service.execute(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(utility.shutdownAndAwaitTermination(service, Duration.ofMillis(100)));
    }

    @Test
    void shutdownAndAwaitTermination_shutsDownAfterForced() {
        ThreadingUtility utility = new ThreadingUtility();

        service.execute(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(utility.shutdownAndAwaitTermination(service, Duration.ofMillis(50)));
    }
}
