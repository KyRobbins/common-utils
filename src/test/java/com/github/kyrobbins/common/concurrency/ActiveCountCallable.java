package com.github.kyrobbins.common.concurrency;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ActiveCountCallable implements Callable<Integer>, Supplier<Integer> {

    private final AtomicInteger activeCount = new AtomicInteger(0);

    @Override
    public Integer call() throws Exception {
        final int beforeCount = activeCount.incrementAndGet();
        Thread.sleep(100);
        final int afterCount = activeCount.decrementAndGet();
        return Math.max(beforeCount, afterCount);
    }

    @Override
    public Integer get() {
        try {
            return call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
