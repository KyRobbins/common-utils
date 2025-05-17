package com.github.kyrobbins.common.utility.config;

import com.github.kyrobbins.common.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// NewClasNamingConvention - Underscore used for better clarity of test focus
@SuppressWarnings("NewClassNamingConvention")
class ConfigLoader_ValueTest {

    @Test
    void require_withOutException_givenNoResult_throwsException() {
        // Initialize class to be tested
        ConfigLoader.Value<String> value = new ConfigLoader.Value<>("some.key", null);

        // Invoke method to be tested
        assertThatThrownBy(value::orElseThrow)
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Key for [some.key] not configured");
    }

    @Test
    void require_withException_givenNoResult_throwsException() {
        // Initialize class to be tested
        ConfigLoader.Value<String> value = new ConfigLoader.Value<>("some.key", null);

        // Invoke method to be tested
        assertThatThrownBy(() -> value.orElseThrow(() -> new ConfigurationException("test reason")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("test reason");
    }
}
