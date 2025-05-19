package com.github.kyrobbins.common.utility;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Utility for helper methods for testing */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestingUtils {

    @SafeVarargs
    public static <R> List<R> listOf(R... items) {
        return Stream.of(items).collect(Collectors.toList());
    }
}
