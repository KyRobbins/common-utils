package com.github.kyrobbins.common.utility.cache;

import com.github.kyrobbins.common.interfaces.AgeAwareCache;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class MaxAgeCacheTest {

    private final Clock mockClock = mock(Clock.class);

    private final AgeAwareCache<String, String> cacheSpy = spy(new MaxAgeCache<>(mockClock));

    @Test
    void get_givenFunction_callsGet() {
        ArgumentCaptor<Supplier<String>> lookupCaptor = ArgumentCaptor.forClass(Supplier.class);

        doReturn(null).when(cacheSpy).get(anyString(), any(Supplier.class));
        doReturn(Optional.of("test result")).when(cacheSpy).get(eq("key"), any(Supplier.class));

        // Invoke method to be tested
        Function<String, String> lookup = k -> "expected lookup";
        Optional<String> actual = cacheSpy.get("key", lookup);

        // Verify test results
        verify(cacheSpy).get("key", lookup);
        assertEquals(Optional.of("test result"), actual);
        verify(cacheSpy).get(eq("key"), lookupCaptor.capture());
        assertEquals("expected lookup", lookupCaptor.getValue().get());
        verifyNoMoreInteractions(mockClock, cacheSpy);
    }

    @Test
    void get_givenDurationAndFunction_callsGet() {
        ArgumentCaptor<Supplier<String>> lookupCaptor = ArgumentCaptor.forClass(Supplier.class);

        doReturn(null).when(cacheSpy).get(anyString(), anyLong(), any(Supplier.class));
        doReturn(Optional.of("test result")).when(cacheSpy).get(eq("key"), eq(1_000L), any(Supplier.class));

        // Invoke method to be tested
        Function<String, String> lookup = k -> "expected lookup";
        Optional<String> actual = cacheSpy.get("key", Duration.ofSeconds(1), lookup);

        // Verify test results
        verify(cacheSpy).get("key", Duration.ofSeconds(1), lookup);
        assertEquals(Optional.of("test result"), actual);
        verify(cacheSpy).get(eq("key"), eq(1_000L), lookupCaptor.capture());
        assertEquals("expected lookup", lookupCaptor.getValue().get());
        verifyNoMoreInteractions(mockClock, cacheSpy);
    }

    @Test
    void get_givenDurationMsAndFunction_callsGet() {
        ArgumentCaptor<Supplier<String>> lookupCaptor = ArgumentCaptor.forClass(Supplier.class);

        doReturn(null).when(cacheSpy).get(anyString(), anyLong(), any(Supplier.class));
        doReturn(Optional.of("test result")).when(cacheSpy).get(eq("key"), eq(1_000L), any(Supplier.class));

        // Invoke method to be tested
        Function<String, String> lookup = k -> "expected lookup";
        Optional<String> actual = cacheSpy.get("key", 1_000L, lookup);

        // Verify test results
        verify(cacheSpy).get("key", 1_000L, lookup);
        assertEquals(Optional.of("test result"), actual);
        verify(cacheSpy).get(eq("key"), eq(1_000L), lookupCaptor.capture());
        assertEquals("expected lookup", lookupCaptor.getValue().get());
        verifyNoMoreInteractions(mockClock, cacheSpy);
    }

    @Test
    void get_givenSupplier_callsGet() {
        Supplier<String> lookup = () -> "expected lookup";

        doReturn(null).when(cacheSpy).get(anyString(), any(Duration.class), any(Supplier.class));
        doReturn(Optional.of("test result")).when(cacheSpy).get("key", Duration.ZERO, lookup);

        // Invoke method to be tested
        Optional<String> actual = cacheSpy.get("key", lookup);

        // Verify test results
        verify(cacheSpy).get("key", lookup);
        assertEquals(Optional.of("test result"), actual);
        verify(cacheSpy).get("key", Duration.ZERO, lookup);
        verifyNoMoreInteractions(mockClock, cacheSpy);
    }

    @Test
    void get_givenDurationAndSupplier_callsGet() {
        Supplier<String> lookup = () -> "expected lookup";

        doReturn(null).when(cacheSpy).get(anyString(), anyLong(), any(Supplier.class));
        doReturn(Optional.of("test result")).when(cacheSpy).get("key", 1_000L, lookup);

        // Invoke method to be tested
        Optional<String> actual = cacheSpy.get("key", Duration.ofSeconds(1), lookup);

        // Verify test results
        verify(cacheSpy).get("key", Duration.ofSeconds(1), lookup);
        assertEquals(Optional.of("test result"), actual);
        verify(cacheSpy).get("key", 1_000L, lookup);
        verifyNoMoreInteractions(mockClock, cacheSpy);
    }

    @Test
    void get_givenDurationMsAndSupplier_returnsValue() {
        when(mockClock.millis())
                .thenReturn(5_000L)
                .thenReturn(5_000L)
                .thenReturn(5_000L)
                .thenReturn(8_000L)
                .thenReturn(10_000L)
                .thenReturn(10_000L);

        assertEquals(Optional.of("value1"), cacheSpy.get("key", 0, () -> "value1"));
        assertEquals(Optional.of("value2"), cacheSpy.get("key", 0, () -> "value2"));
        assertEquals(Optional.of("value2"), cacheSpy.get("key", 5_000, () -> "value3"));
        assertEquals(Optional.of("value2"), cacheSpy.get("key", 4_000, () -> "value4"));
        assertEquals(Optional.of("value5"), cacheSpy.get("key", 2_000, () -> "value5"));
        assertEquals(Optional.of("value6"), cacheSpy.get("key", 0, () -> "value6"));
    }
}
