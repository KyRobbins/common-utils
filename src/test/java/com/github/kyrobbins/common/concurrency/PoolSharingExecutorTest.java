package com.github.kyrobbins.common.concurrency;

import com.github.kyrobbins.common.utility.ThreadingUtility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PoolSharingExecutorTest {

    @Test
    void enforcesLimitsAcrossAllTypes() {
        // Infinitely expanding thread pool
        ExecutorService sharedService = Executors.newCachedThreadPool();

        PoolSharingExecutor executor = new PoolSharingExecutor(sharedService, 11);

        try {
            final ActiveCountCallable callable = new ActiveCountCallable();
            List<CompletableFuture<Integer>> futures = new ArrayList<>();

            // Submit 25 suppliers
            IntStream.range(0, 25)
                    .boxed()
                    .map(i -> executor.submitSupplier(callable))
                    .forEach(futures::add);

            // Submit 5 lists of 10 suppliers each
            IntStream.range(0, 5)
                    .boxed()
                    .map(i -> IntStream.range(0, 10)
                            .boxed()
                            .map(k -> (Supplier<Integer>) callable)
                            .collect(Collectors.toList()))
                    .map(executor::submitSuppliers)
                    .forEach(futures::addAll);

            // Submit 25 callables
            IntStream.range(0, 25)
                    .boxed()
                    .map(i -> executor.submitCallable(callable))
                    .forEach(futures::add);

            // Submit 25 runnables
            IntStream.range(0, 25)
                    .boxed()
                    .map(i -> new RunnableWithFuture<>((Callable<Integer>) callable))
                    .forEach(runnable -> {
                        futures.add(runnable.getResult());
                        executor.execute(runnable);
                    });

            // Verify that total active tasks never exceeded defined limit
            final int maxActiveCount = futures.stream()
                    .map(CompletableFuture::join)
                    .max(Comparator.naturalOrder())
                    .orElse(Integer.MIN_VALUE);
            assertEquals(11, maxActiveCount);
        } finally {
            new ThreadingUtility().shutdownAndAwaitTermination(sharedService, Duration.ofMinutes(1));
        }
    }
}
