package com.github.kyrobbins.common.concurrency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SharedThreadPoolTest {

    private static final int MAX_THREADS = 25;

    private SharedThreadPool threadPool;

    @BeforeEach
    public void setUp() {
        threadPool = new SharedThreadPool(MAX_THREADS, Duration.ofMinutes(1));
    }

    @AfterEach
    public void tearDown() {
        assertTrue(threadPool.shutdown());
        threadPool = null;
    }

    @Test
    void canExecuteJobsAndEnforcesLimit() {
        final ActiveCountCallable callable = new ActiveCountCallable();
        final List<RunnableWithFuture<Integer>> runnables = IntStream.rangeClosed(1, 4 * MAX_THREADS)
                .boxed()
                .map(ignored -> new RunnableWithFuture<>((Callable<Integer>) callable))
                .collect(Collectors.toList());
        runnables.forEach(threadPool::execute);
        final Integer maxActiveCount = runnables.stream()
                .map(RunnableWithFuture::getResult)
                .map(CompletableFuture::join)
                .max(Comparator.naturalOrder())
                .orElse(Integer.MIN_VALUE);

        assertEquals(MAX_THREADS, maxActiveCount);
    }
}
