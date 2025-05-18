package com.github.kyrobbins.common.utility.config;

import com.github.kyrobbins.common.exception.ConfigurationException;
import com.github.kyrobbins.common.exception.ParserException;
import com.github.kyrobbins.common.interfaces.AgeAwareCache;
import com.github.kyrobbins.common.utility.cache.MaxAgeCache;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * API used for handling multiple sources of configuration data, enabling retrieval through a single point of entry
 * API.
 *
 * <p>{@link ConfigLoader} is based off a hierarchical approach, where configuration sources are added by their
 * priority, in reverse, where the last source added has the highest priority (checked first)</p>
 */
@RequiredArgsConstructor
@Slf4j
public class ConfigLoader {

    /** An empty {@link Source} that can be used for optional source logic */
    public static final Source EMPTY_SOURCE = new EmptySource();
    /** An empty {@link UnaryOperator} that can be used for optional source logic */
    public static final UnaryOperator<String> EMPTY_OPERATOR = EMPTY_SOURCE::findValueForKey;

    /** The list of {@link Source}s to check through for key values */
    private final List<Source> sources;
    /** The cache used for faster lookups when desired */
    private final AgeAwareCache<String, String> lookupMaxAgeCache;
    /** Denotes if a Cache is used for the data lookups */
    private final boolean cacheEnabled;

    private final PropertyParser propertyParser = new PropertyParser();

    public ConfigLoader(@Nonnull List<Source> sources) {
        this(sources, false);
    }

    public ConfigLoader(@Nonnull List<Source> sources, boolean enableCache) {
        this(sources, enableCache, Clock.systemDefaultZone());
    }

    public ConfigLoader(@Nonnull List<Source> sources, @Nonnull Clock clock) {
        this(sources, false, clock);
    }

    public ConfigLoader(@Nonnull List<Source> sources, boolean enableCache, @Nonnull Clock clock) {
        this.sources = sources;
        this.cacheEnabled = enableCache;
        this.lookupMaxAgeCache = enableCache ? new MaxAgeCache<>(clock) : new EmptyAgeAwareCache<>();
    }

    /**
     * Creates a {@link ConfigLoader.Builder} for defining sources, prioritized by reverse definition order, for
     * example:
     *
     * <pre>{@code
     * public ConfigLoader configLoader() {
     *     return ConfigLoader.builder()
     *       .addSource(System.getenv(), "Host Environment Variables")
     *       .addSource(System.getProperties(), "System Properties")
     *       // Additional sources
     *       .build();
     * }
     * }</pre>
     *
     * @return The created {@link ConfigLoader.Builder}
     */
    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a {@link DeferredSource}, indicating the source depends on other configuration sources to be loaded
     * first, before this one can.  Note that a {@link DeferredSource} defines a "Source Factory" (Not a
     * {@link Source}), which takes a ConfigLoader instance to build the source.  It's also only currently supporting a
     * single pass for loading {@link DeferredSource}s, so nesting is not supported.  Example:
     *
     * <pre>{@code
     * // ...
     * .addSource(ConfigLoader.defer(configLoader -> FooSource.build(
     *   new FooSource(ConfigLoader.getString("bar.value").orElseThrow())
     * // ...
     * }</pre>
     *
     * @param sourceFactory A factory function for creating the source
     * @param label         A readable name for this source.
     * @return The wrapped {@link DeferredSource}
     */
    @Nonnull
    public static DeferredSource defer(@Nonnull Function<ConfigLoader, UnaryOperator<String>> sourceFactory, @Nonnull String label) {
        return defer(cl -> {
            UnaryOperator<String> source = sourceFactory.apply(cl);

            return source == EMPTY_OPERATOR ? EMPTY_SOURCE : new SimpleSource(sourceFactory.apply(cl), label);
        });
    }

    /**
     * Creates a {@link DeferredSource}, indicating the source depends on other configuration sources to be loaded
     * first, before this one can.
     *
     * @param sourceFactory A factory function for creating the source
     * @return The wrapped {@link DeferredSource}.
     */
    @Nonnull
    public static DeferredSource defer(@Nonnull Function<ConfigLoader, Source> sourceFactory) {
        return new DeferredSource(sourceFactory);
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key.  Supports override values when key is enclosed in {brackets}.
     *
     * <p>Override Example: The following code will attempt to fetch the value for `feature.ops.activated`, and if it
     * is not able to find it, it will then attempt to find a value for `feature.activated`.</p>
     *
     * <pre>{@code
     * configLoader.getString("feature.{ops}.activated");
     * }</pre>
     *
     * <p>This allows the keys to have specific context overrides for the same key, where a default value can be
     * denoted by configuring a property that resembles the override without the override part.</p>
     *
     * @param key The key associated with the desired value to search for
     * @return A {@link Value} potentially containing the value.
     */
    @Nonnull
    public Value<String> getString(@Nonnull String key) {
        return getString(key, 0L);
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key.
     *
     * @param key    The key associated with the desired value to search for
     * @param maxAge The age a cached value can be before it must be looked up fresh
     * @return A {@link Value} potentially containing the value.
     */
    @Nonnull
    public Value<String> getString(@Nonnull String key, @Nonnull Duration maxAge) {
        return getString(key, maxAge.toMillis());
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key.
     *
     * @param key      The key associated with the desired value to search for
     * @param maxAgeMs The age a cached value can be (in milliseconds) before it must be looked up fresh
     * @return A {@link Value} potentially containing the value.
     */
    @Nonnull
    private Value<String> getString(@Nonnull String key, long maxAgeMs) {
        String value;

        if (cacheEnabled) {
            value = lookupMaxAgeCache.get(key, maxAgeMs, this::getString0).orElse(null);
        } else {
            value = getString0(key);
        }

        return Value.ofNullable(key, value);
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key.
     *
     * @param key The key associated with the desired value to search for
     * @return The associated value, or null if no value was found
     */
    @Nullable
    private String getString0(@Nonnull String key) {
        Set<String> expandedKeys = new HashSet<>();
        expandedKeys.add(key);
        return getExpandedNormalizedKey(key, expandedKeys);
    }

    /**
     * Expands the provided key and returns the value associated with it, if any exist.
     *
     * @param expandableKey The key associated with the desired value to search for
     * @param expandedKeys  The {@link Set} of previously expanded keys
     * @return The {@link String} value associated with the given key, or null if it was not found
     */
    @Nullable
    private String getExpandedNormalizedKey(@Nonnull String expandableKey, @Nonnull Set<String> expandedKeys) {
        String expandedKey = getExpandedKey(expandableKey, expandedKeys);
        return getStringForNormalizedKey(expandedKey, expandedKeys);
    }

    /**
     * Expands the given key by iteratively resolving any placeholders that may exist within it
     *
     * @param expandableKey The key that potentially contains expandable placeholders
     * @param expandedKeys  The {@link Set} of previously expanded keys
     * @return The fully expanded key that can now be attempted to be resolved to an associated value
     */
    @Nonnull
    private String getExpandedKey(@Nonnull String expandableKey, @Nonnull Set<String> expandedKeys) {
        List<KeyRegion> leafRegionsToExpand = findExpandableLeafRegions(expandableKey);

        String expandedKey = expandableKey;
        var itr = leafRegionsToExpand.listIterator(leafRegionsToExpand.size());

        // Iterate through the regions backwards to mitigate index shifting after interpolation
        while (itr.hasPrevious()) {
            KeyRegion keyRegion = itr.previous();
            String regionKey = keyRegion.getKey();

            // Each branch of expanding regions should have its own set for tracking infinite loops
            Set<String> regionExpandedKeys = new HashSet<>(expandedKeys);

            if (!regionExpandedKeys.add(regionKey)) {
                throw new ConfigurationException("Property Expansion Loop");
            }

            // Recursively resolve placeholders that are in the key's associated value
            String regionKeyValue = getExpandedNormalizedKey(regionKey, regionExpandedKeys);
            expandedKey = expandedKey.substring(0, keyRegion.getStart())
                    // If no value found for the key, put the placeholder back in for traceability
                    + defaultIfNull(regionKeyValue, keyRegion.getPlaceholder())
                    + expandedKey.substring(keyRegion.getEnd() + 1);
        }

        // By this point, there should be no more expandable regions, so just fetch using the normalized key
        return expandedKey;
    }

    /**
     * Searches for a value associated with the given key, trying both of it's normalized formats (if it can be
     * interpreted in more than one way).
     *
     * @param key          The key associated with the desired value eto search for
     * @param expandedKeys The {@link Set} of previously expanded keys
     * @return The {@link String} value associated with the given key, or null if it was not found
     * @see #normalizeKey(String, boolean)
     */
    @Nullable
    private String getStringForNormalizedKey(@Nonnull String key, @Nonnull Set<String> expandedKeys) {
        // Search for the full key's value first (minus override characters)
        String specificKey = normalizeKey(key, true);

        String value = getStringForAbsoluteKey(specificKey, expandedKeys);

        if (value == null) {
            // If we didn't find it, search for the less specific (default) key's value
            String genericKey = normalizeKey(key, false);

            if (!specificKey.equals(genericKey)) {
                // Only bother to search if the new key is different
                return getStringForAbsoluteKey(genericKey, expandedKeys);
            }
        }

        return value;
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key.
     *
     * @param key          The key associated with the desired value to search for
     * @param expandedKeys The {@link Set} of previously expanded keys
     * @return The {@link String} value associated with the given key, or null if it was not found
     * @throws ConfigurationException Thrown if the key is present in the expanded key list, signaling an expansion
     *                                loop.
     */
    @Nullable
    private String getStringForAbsoluteKey(@Nonnull String key, @Nonnull Set<String> expandedKeys) {
        String value;
        int index = sources.size() - 1;

        // Search through each source in reverse order (the highest priority first)
        do {
            value = sources.get(index).findValueForKey(key);
            --index;
        } while (value == null && index >= 0);

        String returnValue;

        if (value != null) {
            log.info("Key [{}] was found in '{}'", key, sources.get(index + 1).getLabel());
            // Calling getExpandedKey() because this is a key "value", so we don't want to treat it like a pure key
            returnValue = getExpandedKey(value, expandedKeys);
        } else {
            log.info("Key [{}] could not be found", key);
            returnValue = null;
        }

        return returnValue;
    }

    /**
     * If present, removes the override value decorators from a key, then returns the new key.  Key overrides are
     * denoted by a set of curly brackets ("{" and "}").  The second argument will control whether these specific values
     * are included in the new key or removed, to create a generic version of the key.
     *
     * <p>`enable.feature.{username}` would return as `enable.feature.username` (if specific), or `enable.feature`
     * (if generic)</p>
     *
     * <p>Only support for full key-part overrides currently, i.e. {@code my{specific}.key}, not
     * {@code my.{very}specific.key}</p>
     *
     * <p>Note that multiple specific keys (i.e. {@code my.{specific}.{device}.key}) are undefined behavior currently
     * and are unsupported</p>
     *
     * @param key            The property key to transform
     * @param keepProperties Whether to keep the specific key components
     * @return The transformed property key
     */
    @Nonnull
    private String normalizeKey(@Nonnull String key, boolean keepProperties) {
        return propertyParser.parse(key).get(keepProperties);
    }

    /**
     * Finds all the expandable leaf regions within a given value.
     *
     * <p>A "leaf region" is defined as a resolvable placeholder that doesn't have other embedded values that require
     * further resolution</p>
     *
     * <p>i.e., in the case of {@code "my.${resolvable.${special}}.key"}, the leaf region is the {@code special} value.
     * The higher level placeholder can't be resolved without first resolving {@code ${special}}, thus it is not a leaf
     * region.</p>
     *
     * @param value The value to search for expandable regions
     * @return The list of expandable regions that were found
     */
    @Nonnull
    private List<KeyRegion> findExpandableLeafRegions(@Nonnull String value) {
        List<KeyRegion> placeholderRegions = new ArrayList<>();

        IntFunction<Character> lookAhead = cursor -> {
            int nextCursor = cursor + 1;

            return nextCursor < value.length() ? value.charAt(nextCursor) : '\0';
        };

        int bracketDepth = -1;
        int placeholderStartIndex = -1;

        for (int cursor = 0; cursor < value.length(); ++cursor) {
            if (value.charAt(cursor) == '$' && lookAhead.apply(cursor) == '{') {
                placeholderStartIndex = cursor;
                bracketDepth = -1;
            } else if (value.charAt(cursor) == '{') {
                ++bracketDepth;
            } else if (value.charAt(cursor) == '}') {
                if (bracketDepth > 0) {
                    --bracketDepth;
                } else if (placeholderStartIndex > -1) {
                    String placeholder = value.substring(placeholderStartIndex, cursor + 1);
                    String regionKey = value.substring(placeholderStartIndex + 2, cursor);
                    placeholderRegions.add(new KeyRegion(placeholderStartIndex, cursor, regionKey, placeholder));
                    placeholderStartIndex = -1;
                    bracketDepth = -1;
                }
            }
        }

        return placeholderRegions;
    }

    /** Helper class for defining a region within a string that has a placeholder value */
    @Data
    private static class KeyRegion {

        /** The start of the region (including placeholder syntax characters) */
        private final int start;
        /** The end of the region (including placeholder syntax characters) */
        private final int end;
        /** The placeholder value (not including placeholder syntax characters) */
        private final String key;
        /** The entire placeholder (including placeholder syntax characters) */
        private final String placeholder;
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key, parsed as a boolean.  Does NOT use cached values
     *
     * @param key The key associated with the desired value to search for
     * @return A {@link Value} potentially containing the desired value
     */
    @Nonnull
    public Value<Boolean> getBoolean(@Nonnull String key) {
        return getBoolean(key, Duration.ZERO);
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key, parsed as a boolean.
     *
     * @param key    The key associated with the desired value to search for
     * @param maxAge The age a cached value can be before it must be looked up fresh
     * @return A {@link Value} potentially containing the desired value
     */
    @Nonnull
    public Value<Boolean> getBoolean(@Nonnull String key, @Nonnull Duration maxAge) {
        return parsedOption(key, maxAge.toMillis(), Boolean.class, stringValue -> {
            if ("true".equalsIgnoreCase(stringValue)) {
                return Boolean.TRUE;
            } else if ("false".equalsIgnoreCase(stringValue)) {
                return Boolean.FALSE;
            } else {
                throw new ParserException("Cannot convert value to boolean");
            }
        });
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key, parsed as an {@link Integer}
     *
     * @param key The key associated with the desired value to search for
     * @return A {@link Value} potentially containing the desired value
     */
    @Nonnull
    public Value<Integer> getInteger(@Nonnull String key) {
        return getInteger(key, Duration.ZERO);
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key, parsed as an {@link Integer}
     *
     * @param key    The key associated with the desired value to search for
     * @param maxAge The max age a cached value can be to still use it
     * @return A {@link Value} potentially containing the desired value
     */
    @Nonnull
    public Value<Integer> getInteger(@Nonnull String key, @Nonnull Duration maxAge) {
        return parsedOption(key, maxAge.toMillis(), Integer.class, Integer::parseInt);
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key, parsed as a {@link Long}
     *
     * @param key The key associated with the desired value to search for
     * @return A {@link Value} potentially containing the desired value
     */
    @Nonnull
    public Value<Long> getLong(@Nonnull String key) {
        return getLong(key, Duration.ZERO);
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key, parsed as a {@link Long}
     *
     * @param key    The key associated with the desired value to search for
     * @param maxAge The max age a cached value can be to still use it
     * @return A {@link Value} potentially containing the desired value
     */
    @Nonnull
    public Value<Long> getLong(@Nonnull String key, @Nonnull Duration maxAge) {
        return parsedOption(key, maxAge.toMillis(), Long.class, Long::parseLong);
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key, parsed as a {@link Float}
     *
     * @param key The key associated with the desired value to search for
     * @return A {@link Value} potentially containing the desired value
     */
    @Nonnull
    public Value<Float> getFloat(@Nonnull String key) {
        return getFloat(key, Duration.ZERO);
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key, parsed as a {@link Float}
     *
     * @param key    The key associated with the desired value to search for
     * @param maxAge The max age a cached value can be to still use it
     * @return A {@link Value} potentially containing the desired value
     */
    @Nonnull
    public Value<Float> getFloat(@Nonnull String key, @Nonnull Duration maxAge) {
        return parsedOption(key, maxAge.toMillis(), Float.class, Float::parseFloat);
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key, parsed as a {@link Double}
     *
     * @param key The key associated with the desired value to search for
     * @return A {@link Value} potentially containing the desired value
     */
    @Nonnull
    public Value<Double> getDouble(@Nonnull String key) {
        return getDouble(key, Duration.ZERO);
    }

    /**
     * Searches through the defined sources, in reverse order (the highest priority first) for the value associated with
     * a provided key, parsed as a {@link Double}
     *
     * @param key    The key associated with the desired value to search for
     * @param maxAge The max age a cached value can be to still use it
     * @return A {@link Value} potentially containing the desired value
     */
    @Nonnull
    public Value<Double> getDouble(@Nonnull String key, @Nonnull Duration maxAge) {
        return parsedOption(key, maxAge.toMillis(), Double.class, Double::parseDouble);
    }

    /**
     * Returns a {@link Value} containing the parsed value using the given parser
     *
     * @param key      The key associated with the desired value to search for
     * @param maxAgeMs The maximum age a cached value can be to still use it
     * @param clazz    The desired class type of the parsed value
     * @param parser   The parser to use to parse the value to the desired type
     * @param <T>      The desired class type of the parsed value
     * @return A {@link Value} containing the parsed value, or a {@link Value#empty(String)} if the value was not found
     */
    @Nonnull
    private <T> Value<T> parsedOption(@Nonnull String key, long maxAgeMs, @Nonnull Class<T> clazz, @Nonnull Function<String, T> parser) {
        Value<String> stringValue = getString(key, maxAgeMs);

        if (stringValue.isPresent()) {
            return new Value<>(stringValue.propertyName, parseString(key, stringValue.value, clazz, parser));
        }

        return new Value<>(stringValue.propertyName, null);
    }

    /**
     * Parses the given {@link String} value into the required type using the given parser
     *
     * @param key    The key associated with the desired value to search for
     * @param value  The value eto be parsed
     * @param clazz  The desired class type of the parsed value
     * @param parser The parser to use to parse the value to the desired type
     * @param <T>    The desired class type of the parsed value
     * @return The parsed value
     */
    @Nonnull
    private <T> T parseString(@Nonnull String key, @Nonnull String value, @Nonnull Class<T> clazz, @Nonnull Function<String, T> parser) {
        try {
            return parser.apply(value);
        } catch (RuntimeException e) {
            throw new ConfigurationException(parseFailedMessage(key, clazz));
        }
    }

    /**
     * Helper method for creating a parsing error message
     *
     * @param key   The key associated with the value that couldn't be parsed
     * @param clazz The type the value was being parsed to
     * @return An error message denoting the issue that occurred
     */
    @Nonnull
    private String parseFailedMessage(@Nonnull String key, @Nonnull Class<?> clazz) {
        return String.format("Could not parse '%s' value as type '%s'", key, clazz.getCanonicalName());
    }

    /**
     * Helper to return a default value if the provided value is null
     *
     * @param value        The value to check for null
     * @param defaultValue The default value to use
     * @return THe originally given value if it is not null, or the default value if it is
     */
    @Nonnull
    private static String defaultIfNull(@Nullable String value, @Nonnull String defaultValue) {
        return value != null ? value : defaultValue;
    }

    /** A helper class for building instances of the {@link ConfigLoader} class */
    public static class Builder {

        /** The list of sources to use to build the {@link ConfigLoader} class */
        private final List<InitializableSource> sources;
        private boolean useCache;

        private Builder() {
            sources = new ArrayList<>();
            addSource(new InitializableSource(new RootSource()));
        }

        /**
         * Enables the use of a {@link com.github.kyrobbins.common.interfaces.Cache} for {@link ConfigLoader} instance
         * that is built by this builder object.
         *
         * @return This {@link Builder} instance
         */
        @Nonnull
        public Builder enableCache() {
            this.useCache = true;
            return this;
        }

        /**
         * Builds a {@link ConfigLoader} instance with the configured {@link Source}s
         *
         * <p>This method will first build a {@link ConfigLoader} with the sources already initialized, then initialize
         * {@link DeferredSource}s using those configurations before building a new {@link ConfigLoader} with all the
         * now initialized sources</p>
         *
         * @return The constructed {@link ConfigLoader}
         */
        @Nonnull
        public ConfigLoader build() {
            return build(Clock.systemDefaultZone());
        }

        /**
         * Builds a {@link ConfigLoader} instance with the configured {@link Source}s
         *
         * <p>This method will first build a {@link ConfigLoader} with the sources already initialized, then initialize
         * {@link DeferredSource}s using those configurations before building a new {@link ConfigLoader} with all the
         * now initialized sources</p>
         *
         * @return The constructed {@link ConfigLoader}
         */
        @Nonnull
        public ConfigLoader build(@Nonnull Clock clock) {
            // Note: This logic is only set up to expect one level of expansion for deferred sources.  More work is
            // needed if infinite expansion is needed (i.e., if one source depends on another source, which then also
            // depends on another source itself...)
            // Build a ConfigLoader using already initialized sources
            List<Source> newSourceList = sources.stream()
                    .filter(InitializableSource::isInitialized)
                    .map(InitializableSource::getSource)
                    .collect(Collectors.toList());

            ConfigLoader configLoader = new ConfigLoader(newSourceList, useCache, clock);

            Set<String> sourceLabels = new HashSet<>();

            // Initialize the rest of the sources and re-build the ConfigLoader
            List<Source> initializedSources = sources.stream()
                    .map(s -> s.initialize(configLoader))
                    .map(InitializableSource::getSource)
                    .filter(source -> {
                        if (source != EMPTY_SOURCE) {
                            String sourceLabel = source.getLabel();

                            // make sure no duplicate labels used
                            if (!sourceLabels.add(sourceLabel)) {
                                throw new ConfigurationException(String.format("Duplicate source label '%s' found", sourceLabel));
                            }

                            return true;
                        }

                        return false;
                    })
                    .collect(Collectors.toList());

            log.info("Building ConfigLoader with the following sources (in descending order of priority): [{}]", buildSourceList(initializedSources));

            return new ConfigLoader(initializedSources, useCache, clock);
        }

        /**
         * Helper method to create a comma-delimited source list of the given sources
         *
         * @param sources The sources being used to build the list
         * @return The {@link String} representation of the comma-delimited source list
         */
        @Nonnull
        private String buildSourceList(@Nonnull List<Source> sources) {
            return sources.stream()
                    .map(Source::getLabel)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
        }

        /**
         * Adds a code implemented {@link Source} for searching for key values.
         *
         * @param source The source to search for key values
         * @return This {@link Builder} instance
         * @throws ConfigurationException If the sources already contained a source with the same label
         */
        @Nonnull
        public Builder addSource(@Nonnull final Source source) {
            return addSource(new InitializableSource(source));
        }

        /**
         * Adds a generic source for searching for key values
         *
         * @param source      The source to search for key values
         * @param sourceLabel A readable name for this source
         * @return This {@link Builder} instance
         * @throws ConfigurationException If the sources already contained a source with the same label
         */
        @Nonnull
        public Builder addSource(@Nonnull final UnaryOperator<String> source, @Nonnull final String sourceLabel) {
            return addSource(new InitializableSource(new SimpleSource(source, sourceLabel)));
        }

        /**
         * Adds a {@link Properties} source for searching for key values
         *
         * @param properties  The source to search for key values
         * @param sourceLabel A readable name for this source
         * @return This {@link Builder} instance
         * @throws ConfigurationException If the sources already contained a source with the same label
         */
        @Nonnull
        public Builder addSource(@Nonnull Properties properties, @Nonnull String sourceLabel) {
            return addSource(properties::getProperty, sourceLabel);
        }

        /**
         * Adds a {@link Map} source for searching for key values
         *
         * @param map         The source to search for key values
         * @param sourceLabel A readable name for this source
         * @return This {@link Builder} instance
         * @throws ConfigurationException If the sources already contained a source with the same label
         */
        @Nonnull
        public Builder addSource(@Nonnull Map<String, String> map, @Nonnull String sourceLabel) {
            return addSource(map::get, sourceLabel);
        }

        /**
         * Adds a {@link DeferredSource} for searching key values
         *
         * @param source The source to search for key values
         * @return This {@link Builder} instance
         * @throws ConfigurationException If the sources already contained a source with the same label
         * @see DeferredSource
         */
        @Nonnull
        public Builder addSource(@Nonnull DeferredSource source) {
            return addSource(new InitializableSource(source.sourceFactory));
        }

        /**
         * Adds an {@link InitializableSource} for searching for key values
         *
         * @param initializableSource The source to search for key values
         * @return This {@link Builder} instance
         * @throws ConfigurationException If the sources already contained a source with the same label
         */
        @Nonnull
        private Builder addSource(@Nonnull InitializableSource initializableSource) {
            this.sources.add(initializableSource);
            return this;
        }

        /**
         * Adds a {@link PropertiesFile} source for searching for key values
         *
         * @param propertiesFile The source to search for key values
         * @return This {@link Builder} instance
         * @throws ConfigurationException If the sources already contained a source with the same label
         * @see PropertiesFile
         */
        @Nonnull
        public Builder addSource(@Nonnull PropertiesFile propertiesFile) {
            try {
                Properties properties = new Properties();
                InputStream inputStream;

                if (propertiesFile.isResourceFile()) {
                    inputStream = getClass().getClassLoader().getResourceAsStream(propertiesFile.getFilePath());
                } else {
                    File targetFile = new File(System.getProperty("user.dir"), propertiesFile.getFilePath());
                    inputStream = targetFile.exists() ? Files.newInputStream(targetFile.toPath()) : null;
                }

                if (inputStream != null) {
                    properties.load(inputStream);
                    inputStream.close();
                    addSource(properties, propertiesFile.getFilePath());
                } else if (propertiesFile.isRequired()) {
                    throw new ConfigurationException("Missing required .properties file for configuration: " + propertiesFile.getFilePath());
                }

                return this;
            } catch (IOException e) {
                throw new ConfigurationException("Failed to load .properties file for configuration: " + propertiesFile.getFilePath(), e);
            }
        }
    }

    /** Thin wrapper for distinguishing the type of resource file being used to source config data */
    @Data
    public static class PropertiesFile {

        /** Path to the file */
        private final String filePath;
        /** Denotes if it's a resource file (in resources), otherwise it checks the current working directory */
        private final boolean resourceFile;
        /** Denotes if the resource is required to exist */
        private final boolean required;

        public PropertiesFile(@Nonnull String filePath, boolean resourceFile) {
            this(filePath, resourceFile, true);
        }

        public PropertiesFile(@Nonnull String filePath, boolean resourceFile, boolean required) {
            this.filePath = filePath;
            this.resourceFile = resourceFile;
            this.required = required;
        }
    }

    /**
     * A Thin wrapper for denoting that this source should be initialized only after every possible source has been
     * added.  This allows {@link DeferredSource}s to use configurations from the already initialized sources to build
     * themselves.
     */
    @RequiredArgsConstructor
    public static class DeferredSource {

        @Nonnull
        private final Function<ConfigLoader, Source> sourceFactory;
    }

    /** A basic definition of a {@link Source} to search for key values */
    private static class SimpleSource extends Source {

        private final UnaryOperator<String> source;

        private SimpleSource(@Nonnull UnaryOperator<String> source, @Nonnull String label) {
            super(label);
            this.source = source;
        }

        @Override
        public String findValueForKey(@Nonnull String key) {
            return source.apply(key);
        }
    }

    /** Derived {@link Source} type that requires initialization before use */
    @Getter
    static class InitializableSource {

        private final Source source;

        @Getter(AccessLevel.NONE)
        private final Function<ConfigLoader, Source> sourceFactory;

        private final boolean initialized;

        private InitializableSource(@Nonnull Source source) {
            this.source = source;
            this.initialized = true;
            this.sourceFactory = null;
        }

        private InitializableSource(@Nonnull Function<ConfigLoader, Source> sourceFactory) {
            this.source = EMPTY_SOURCE;
            this.initialized = false;
            this.sourceFactory = sourceFactory;
        }

        @Nonnull
        public InitializableSource initialize(@Nonnull ConfigLoader configLoader) {
            if (sourceFactory != null) {
                return new InitializableSource(sourceFactory.apply(configLoader));
            } else {
                return this;
            }
        }
    }

    /**
     * An empty source, which will be removed during the {@link Builder#build()} call
     *
     * <p>This can be used by externally defined source logic to conditionally add a source</p>
     */
    public static class EmptySource extends Source {

        private EmptySource() {
            super(null);
        }

        @Override
        public String findValueForKey(@Nonnull String key) {
            return null;
        }
    }

    static class RootSource extends Source {

        private RootSource() {
            super("ROOT");
        }

        @Override
        public String findValueForKey(@Nonnull String key) {
            return null;
        }
    }

    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Value<T> {

        @Nonnull
        private final String propertyName;

        private final T value;

        @Nonnull
        public static <T> Value<T> ofNullable(@Nonnull String propertyName, T value) {
            return new Value<>(propertyName, value);
        }

        @Nonnull
        public static <T> Value<T> empty(@Nonnull String propertyName) {
            return ofNullable(propertyName, null);
        }

        public boolean isPresent() {
            return value != null;
        }

        @Nonnull
        public T orElseThrow() {
            return orElseThrow(
                    () -> new ConfigurationException(String.format("Key for [%s] not configured", propertyName)));
        }

        @Nonnull
        public <E extends Exception> T orElseThrow(Supplier<E> exceptionSupplier) throws E {
            if (!isPresent()) {
                throw exceptionSupplier.get();
            }

            return value;
        }

        public T orElse(T fallback) {
            return isPresent() ? value : fallback;
        }
    }
}