package com.github.kyrobbins.common.utility.config;

import com.github.kyrobbins.common.exception.ConfigurationException;
import com.github.kyrobbins.common.interfaces.TriFunction;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigLoaderIT {

    @Test
    void getBoolean_returnsMappedValue() {
        Map<String, String> mapSource = new HashMap<>();
        mapSource.put("feature.flag.a", "true");
        mapSource.put("feature.flag.b", "false");

        ConfigLoader configLoader = ConfigLoader.builder()
                .addSource(mapSource, "Test sources")
                .addSource(new ConfigLoader.PropertiesFile("test.properties", true, true))
                .build();

        // Invoke method to be tested
        assertTrue(configLoader.getBoolean("feature.flag.a").orElseThrow(), "Unexpected key resolution");
        assertFalse(configLoader.getBoolean("feature.flag.b").orElseThrow(), "Unexpected key resolution");
        assertFalse(configLoader.getBoolean("missing.key").isPresent(), "Unexpected key resolution");

        assertThatThrownBy(() -> configLoader.getBoolean("test_key_string"))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Could not parse 'test_key_string' value as type 'java.lang.Boolean'");
    }

    @Test
    void getLong_returnsMappedValue() {
        Map<String, String> mapSource = new HashMap<>();
        mapSource.put("key1", "4");

        ConfigLoader configLoader = ConfigLoader.builder()
                .addSource(mapSource, "Test sources")
                .addSource(new ConfigLoader.PropertiesFile("test.properties", true, true))
                .build();

        assertEquals(new ConfigLoader.Value<>("key1", 4L), configLoader.getLong("key1"));
        assertThatThrownBy(() -> configLoader.getLong("test_key_string"))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Could not parse 'test_key_string' value as type 'java.lang.Long'");
    }

    @Test
    void getInteger_returnsMappedValue() {
        Map<String, String> mapSource = new HashMap<>();
        mapSource.put("key1", "4");

        ConfigLoader configLoader = ConfigLoader.builder()
                .addSource(mapSource, "Test sources")
                .addSource(new ConfigLoader.PropertiesFile("test.properties", true, true))
                .build();

        assertEquals(new ConfigLoader.Value<>("key1", 4), configLoader.getInteger("key1"));
        assertThatThrownBy(() -> configLoader.getInteger("test_key_string"))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Could not parse 'test_key_string' value as type 'java.lang.Integer'");
    }

    @Test
    void getDouble_returnsMappedValue() {
        Map<String, String> mapSource = new HashMap<>();
        mapSource.put("key1", "4.1");

        ConfigLoader configLoader = ConfigLoader.builder()
                .addSource(mapSource, "Test sources")
                .addSource(new ConfigLoader.PropertiesFile("test.properties", true, true))
                .build();

        assertEquals(new ConfigLoader.Value<>("key1", 4.1), configLoader.getDouble("key1"));
        assertThatThrownBy(() -> configLoader.getDouble("test_key_string"))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Could not parse 'test_key_string' value as type 'java.lang.Double'");
    }

    @Test
    void getFloat_returnsMappedValue() {
        Map<String, String> mapSource = new HashMap<>();
        mapSource.put("key1", "4.1");

        ConfigLoader configLoader = ConfigLoader.builder()
                .addSource(mapSource, "Test sources")
                .addSource(new ConfigLoader.PropertiesFile("test.properties", true, true))
                .build();

        assertEquals(new ConfigLoader.Value<>("key1", 4.1F), configLoader.getFloat("key1"));
        assertThatThrownBy(() -> configLoader.getFloat("test_key_string"))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Could not parse 'test_key_string' value as type 'java.lang.Float'");
    }

    public static Stream<Arguments> getString() {
        return Stream.of(
                Arguments.of("expanded.last.1", "foo"),
                Arguments.of("expanded.first", "foo.bar.foo"),
                // Should be loaded from the test.properties file
                Arguments.of("test_key_string", "apple"),
                Arguments.of("expanded.never", "${expanded.missing}"),
                Arguments.of("some.flag.for.{kiwi}", "green"),
                Arguments.of("some.flag.for.{banana}", "yellow"),
                Arguments.of("some.flag.for.{sky}", "rainbow"),
                Arguments.of("{roses}.are", "red"),
                Arguments.of("{violets}.are", "blue"),
                Arguments.of("{dark-energies}.are", "I don't know"),
                Arguments.of("all.{cows}.eat", "grass"),
                Arguments.of("all.{cars}.eat", "gas"),
                Arguments.of("all.{other-things}.eat", "stuff"),
                Arguments.of("all.cars.eat", "gas"),
                Arguments.of("some.flag.for.{${found.key}}", "yellow"),
                Arguments.of("some.flag.{${missing.key}}.for", "rainbow"),
                Arguments.of("missing.value", null));
    }

    @ParameterizedTest
    @MethodSource
    void getString(String key, String expectedResult) {
        Map<String, String> mapSource = new HashMap<>();
        mapSource.put("expanded.first", "${expanded.second}");
        mapSource.put("expanded.second", "${expanded.last.1}.${expanded.last.2}.${expanded.last.1}");
        mapSource.put("expanded.never", "${expanded.missing}");
        mapSource.put("expanded.last.1", "foo");
        mapSource.put("expanded.last.2", "bar");
        mapSource.put("found.key", "banana");
        mapSource.put("some.flag.for.kiwi", "green");
        mapSource.put("some.flag.for.banana", "yellow");
        mapSource.put("some.flag.for", "rainbow");
        mapSource.put("roses.are", "red");
        mapSource.put("violets.are", "blue");
        mapSource.put("are", "I don't know");
        mapSource.put("all.cars.eat", "gas");
        mapSource.put("all.cows.eat", "grass");
        mapSource.put("all.eat", "stuff");

        ConfigLoader configLoader = ConfigLoader.builder()
                .addSource(mapSource, "Test sources")
                .addSource(new ConfigLoader.PropertiesFile("test.properties", true, true))
                .build();

        // Invoke method to be tested
        ConfigLoader.Value<String> actualResult = configLoader.getString(key);

        // Verify test results
        assertEquals(new ConfigLoader.Value<>(key, expectedResult), actualResult);
    }

    public static Stream<Arguments> cachedLookup_returnsCachedValue() {
        List<String> stringsToSet = listOf("value1", "value2", "value3", "value4", "value5");
        List<String> expectedStrings = listOf("value1", "value2", "value2", "value2", "value4", "value5");

        List<String> boolsToSet = listOf("true", "false", "true", "true", "false");
        List<Boolean> expectedBools = listOf(true, false, false, false, true, false);

        List<String> intsToSet = listOf("1", "2", "3", "4", "5");
        List<Integer> expectedInts = listOf(1, 2, 2, 2, 4, 5);
        List<Long> expectedLongs = listOf(1L, 2L, 2L, 2L, 4L, 5L);

        List<String> floatsToSet = listOf("1.5", "2.5", "3.5", "4.5", "5.5");
        List<Float> expectedFloats = listOf(1.5F, 2.5F, 2.5F, 2.5F, 4.5F, 5.5F);
        List<Double> expectedDoubles = listOf(1.5, 2.5, 2.5, 2.5, 4.5, 5.5);

        BiFunction<ConfigLoader, String, ConfigLoader.Value<?>> stringBiFunction = ConfigLoader::getString;
        TriFunction<ConfigLoader, String, Duration, ConfigLoader.Value<?>> stringTriFunction = ConfigLoader::getString;

        BiFunction<ConfigLoader, String, ConfigLoader.Value<?>> boolBiFunction = ConfigLoader::getBoolean;
        TriFunction<ConfigLoader, String, Duration, ConfigLoader.Value<?>> boolTriFunction = ConfigLoader::getBoolean;

        BiFunction<ConfigLoader, String, ConfigLoader.Value<?>> intBiFunction = ConfigLoader::getInteger;
        TriFunction<ConfigLoader, String, Duration, ConfigLoader.Value<?>> intTriFunction = ConfigLoader::getInteger;

        BiFunction<ConfigLoader, String, ConfigLoader.Value<?>> longBiFunction = ConfigLoader::getLong;
        TriFunction<ConfigLoader, String, Duration, ConfigLoader.Value<?>> longTriFunction = ConfigLoader::getLong;

        BiFunction<ConfigLoader, String, ConfigLoader.Value<?>> floatBiFunction = ConfigLoader::getFloat;
        TriFunction<ConfigLoader, String, Duration, ConfigLoader.Value<?>> floatTriFunction = ConfigLoader::getFloat;

        BiFunction<ConfigLoader, String, ConfigLoader.Value<?>> doubleBiFunction = ConfigLoader::getDouble;
        TriFunction<ConfigLoader, String, Duration, ConfigLoader.Value<?>> doubleTriFunction = ConfigLoader::getDouble;

        return Stream.of(
                Arguments.of(stringBiFunction, stringTriFunction, stringsToSet, expectedStrings),
                Arguments.of(boolBiFunction, boolTriFunction, boolsToSet, expectedBools),
                Arguments.of(intBiFunction, intTriFunction, intsToSet, expectedInts),
                Arguments.of(longBiFunction, longTriFunction, intsToSet, expectedLongs),
                Arguments.of(floatBiFunction, floatTriFunction, floatsToSet, expectedFloats),
                Arguments.of(doubleBiFunction, doubleTriFunction, floatsToSet, expectedDoubles));
    }

    @ParameterizedTest
    @MethodSource
    void cachedLookup_returnsCachedValue(
            BiFunction<ConfigLoader, String, ConfigLoader.Value<?>> loaderGetter,
            TriFunction<ConfigLoader, String, Duration, ConfigLoader.Value<?>> loaderGetterWithDuration,
            List<String> valuesToSet,
            List<Object> expectedValues) {
        // Using a mock clock for this test because we need to control he time for efficient testing
        Clock mockClock = mock(Clock.class);

        when(mockClock.millis())
                .thenReturn(5_000L)
                .thenReturn(5_000L)
                .thenReturn(5_000L)
                .thenReturn(8_000L)
                .thenReturn(10_000L);

        Map<String, String> testSource = new HashMap<>();

        ConfigLoader loader = ConfigLoader.builder().addSource(testSource, "Test source").enableCache().build(mockClock);

        // No second parameter means fresh lookup (no cache)
        testSource.put("key1", valuesToSet.get(0));
        assertEquals(expectedValues.get(0), loaderGetter.apply(loader, "key1").orElseThrow());
        testSource.put("key1", valuesToSet.get(1));
        assertEquals(expectedValues.get(1), loaderGetter.apply(loader, "key1").orElseThrow());
        // The second parameter means fresh lookup if cached value is equal to or older than given
        testSource.put("key1", valuesToSet.get(2));
        assertEquals(expectedValues.get(2), loaderGetterWithDuration.apply(loader, "key1", Duration.ofSeconds(5)).orElseThrow());
        testSource.put("key1", valuesToSet.get(3));
        assertEquals(expectedValues.get(3), loaderGetterWithDuration.apply(loader, "key1", Duration.ofSeconds(4)).orElseThrow());
        // Only when the second parameter is shorter than the cached age does it look up a new value
        assertEquals(expectedValues.get(4), loaderGetterWithDuration.apply(loader, "key1", Duration.ofSeconds(2)).orElseThrow());
        testSource.put("key1", valuesToSet.get(4));
        assertEquals(expectedValues.get(5), loaderGetter.apply(loader, "key1").orElseThrow());
    }

    @Test
    void getString_expandedValueRecursive_returnsError() {
        Map<String, String> mapSource = new HashMap<>();
        mapSource.put("expanded.first", "${expanded.second}");
        mapSource.put("expanded.second", "${expanded.last}.${expanded.first}");
        mapSource.put("expanded.last", "value");

        ConfigLoader configLoader = ConfigLoader.builder()
                .addSource(mapSource, "Test source")
                .build();

        assertThatThrownBy(() -> configLoader.getString("expanded.first"))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Property Expansion Loop");
    }

    @Test
    void getString_givenDeferredLoaderThatLoads_returnsFunction() {
        // Creates a deferred operation that relies on another source from the configuration loader.
        // The deferred key is needed for the deferred source to be instantiated
        Map<String, String> mapSource = new HashMap<>();
        mapSource.put("some.key", "some value");

        ConfigLoader.DeferredSource deferredSource = ConfigLoader.defer(cl -> cl.getBoolean("deferredKey").orElse(false) ? key -> mapSource.get(key) : ConfigLoader.EMPTY_OPERATOR, "Deferred Source");

        Source staticSource = new Source("Static Source") {
            @Override
            protected String findValueForKey(@Nonnull String key) {
                return key.equals("deferredKey") ? "true" : null;
            }
        };

        ConfigLoader configLoader = ConfigLoader.builder()
                .addSource(deferredSource)
                .addSource(staticSource)
                .build();

        // Invoke method to be tested
        ConfigLoader.Value<String> actual = configLoader.getString("some.key");

        // Verify test results
        assertEquals(new ConfigLoader.Value<>("some.key", "some value"), actual);
    }

    @Test
    void addSource_addPropertiesFileDoesNotExist_returnsError() {
        ConfigLoader.PropertiesFile file = new ConfigLoader.PropertiesFile("non-existing.properties", true);

        assertThatThrownBy(() -> ConfigLoader.builder().addSource(file))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Missing required .properties file for configuration: non-existing.properties");
    }

    @Test
    void buildSourceList() {
        ConfigLoader.builder()
                .addSource(new HashMap<>(), "Map source")
                .addSource(new Properties(), "Properties source")
                .addSource(key -> null, "Null source")
                .addSource(new Source("Custom source") {
                    @Override
                    protected String findValueForKey(@Nonnull String key) {
                        return "";
                    }
                })
                .build();

        // Test the source build string
        String expectedBuildString = "Building ConfigLoader with the following sources (in descending order of priority): [ROOT, Map source, Properties source, Null source, Custom source";
    }

    @Test
    void duplicateSourceLabels() {
        ConfigLoader.Builder builder = ConfigLoader.builder()
                .addSource(new HashMap<>(), "Test source 1")
                .addSource(new HashMap<>(), "Test source 2")
                .addSource(new Properties(), "Test source 1");

        // Invoke method to be tested
        assertThatThrownBy(builder::build)
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Duplicate source label 'Test source 1' found");
    }

    @SafeVarargs
    private static final <R> List<R> listOf(R... items) {
        return Stream.of(items).collect(Collectors.toList());
    }
}
