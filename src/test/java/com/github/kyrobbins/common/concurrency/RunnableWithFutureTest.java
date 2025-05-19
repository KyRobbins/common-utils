package com.github.kyrobbins.common.concurrency;

import com.github.kyrobbins.common.utility.TestingUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RunnableWithFutureTest {

    @Test
    void futuresReturnSuccessfulResults() {
        final Runnable runnable = () -> {
        };

        final Runnable wrappedRunnable = new RunnableWithFuture<>((Supplier<Character>) () -> 'w');
        final Supplier<Integer> supplier = () -> 10;
        final Callable<String> callable = () -> "success!";

        final List<RunnableWithFuture<?>> runners = TestingUtils.listOf(
                RunnableWithFuture.fromRunnable(runnable),
                RunnableWithFuture.fromRunnable(wrappedRunnable),
                new RunnableWithFuture<>(supplier),
                new RunnableWithFuture<>(callable));

        runners.forEach(Runnable::run);

        assertEquals(
                TestingUtils.listOf(Optional.empty(), Optional.of('w'), Optional.of(10), Optional.of("success!")),
                runners.stream()
                        .map(RunnableWithFuture::getResult)
                        .map(CompletableFuture::join)
                        .map(Optional::ofNullable)
                        .collect(Collectors.toList()));
    }

    @Test
    void futureReturnsExceptionResult() {
        Callable<String> callable = () -> {
            throw new IOException("test reason");
        };
        final RunnableWithFuture<String> runner = new RunnableWithFuture<>(callable);
        assertThatThrownBy(() -> {
            runner.run();
            runner.getResult().get();
        })
                .isInstanceOf(ExecutionException.class)
                .rootCause()
                .isInstanceOf(IOException.class)
                .hasMessage("test reason");
    }
}
