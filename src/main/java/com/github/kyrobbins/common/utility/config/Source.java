package com.github.kyrobbins.common.utility.config;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public abstract class Source {

    /** Label associated with this source, for debugging / auditing purposes */
    private final String label;

    /**
     * Implementation defined logic to get the value associated with the given key
     *
     * @param key The key associated with he desired value
     * @return The value associated with the given key, or null if not found
     */
    protected abstract String findValueForKey(@Nonnull String key);
}