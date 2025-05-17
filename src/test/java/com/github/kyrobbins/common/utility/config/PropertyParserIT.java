package com.github.kyrobbins.common.utility.config;

import com.github.kyrobbins.common.exception.ConfigurationException;
import com.github.kyrobbins.common.exception.ParserException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.ParseException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertyParserIT {

    public static Stream<Arguments> propertyParser_givenInvalidProperties_throwsException() {
        return Stream.of(
                Arguments.of("my.@property.key", 3, "Unsupported character '@' in property key"),
                Arguments.of("my..property.key", 3, "Unexpected end of property part"),
                Arguments.of(".my.property.key", 0, "Unexpected end of property part"),
                Arguments.of("my.property.key.", 15, "Property part cannot be blank"),
                Arguments.of("my.property.key}", 15, "Unexpected '}'"),
                Arguments.of("my.property.key{", 15, "Property part cannot be blank"),
                Arguments.of("{my.property.key", 15, "Unexpected end of property part, expected '}'"),
                Arguments.of("my.property.key{}", 16, "Property part cannot be blank"),
                Arguments.of("my.property.{}.key", 13, "Property part cannot be blank"),
                Arguments.of("my.{property.key", 15, "Unexpected end of property part, expected '}'"),
                Arguments.of("my.property}.key", 11, "Unexpected '}'"),
                Arguments.of("{my}.property.key}", 17, "Unexpected '}'"),
                Arguments.of("{my.{property}}.key}", 19, "Unexpected '}'"),
                Arguments.of("my.{{property.key}", 17, "Unexpected end of property part, expected '}'"),
                Arguments.of("my.prop-erty.key}", 16, "Unexpected '}'"),
                Arguments.of("{my.$property}.key", 4, "Unexpected '$', placeholders require brackets"),
                Arguments.of("{my.property$}.key", 12, "Unexpected '$', placeholders require brackets"),
                Arguments.of("{my.prop$erty}.key", 8, "Unexpected '$', placeholders require brackets"),
                Arguments.of("my.${property}.key}", 18, "Unexpected '}'"),
                Arguments.of("my.${property}}.key", 14, "Unexpected '}'"),
                Arguments.of("$my.property.key}", 0, "Unexpected '$', placeholders require brackets"),
                Arguments.of("my.property.key${}", 17, "Property part cannot be blank"),
                Arguments.of("my.property.{key${}", 18, "Property part cannot be blank"),
                Arguments.of("my.property.${}.key", 14, "Property part cannot be blank"),
                Arguments.of("my.property.${{}.key", 15, "Property part cannot be blank"),
                Arguments.of("my.-property.key", 3, "Unexpected '-', illegal use of hyphen"),
                Arguments.of("my.property-.key", 11, "Unexpected '-', illegal use of hyphen"),
                Arguments.of("my.pro--perty.key", 6, "Unexpected '-', illegal use of hyphen"),
                Arguments.of("my._property.key", 3, "Unexpected '_', illegal use of underscore"),
                Arguments.of("my.property_.key", 11, "Unexpected '_', illegal use of underscore"),
                Arguments.of("my.pro__perty.key", 6, "Unexpected '_', illegal use of underscore"),
                Arguments.of("my.p{{rop}erty.key", 17, "Unexpected end of property part, expected '}'"),
                Arguments.of("my.pr${{ope}rty.key", 18, "Unexpected end of property part, expected '}'"));
    }

    @ParameterizedTest
    @MethodSource
    void propertyParser_givenInvalidProperties_throwsException(String propertyKey, int expectedErrorIndex, String expectedRootCause) {
        // Initialize class to be tested
        PropertyParser propertyParser = new PropertyParser();

        // Invoke method to be tested
        assertThatThrownBy(() -> propertyParser.parse(propertyKey))
                .isInstanceOf(ParserException.class)
                .hasMessage("Could not parse property key, error at index " + expectedErrorIndex)
                .rootCause()
                .isInstanceOf(ParseException.class)
                .hasMessage(expectedRootCause);
    }

    public static Stream<Arguments> propertyParser_givenValidProperty_parsesProperty() {
        return Stream.of(
                Arguments.of("persistence.db.{${application.name}}.username", "persistence.db.username", "persistence.db.${application.name}.username"),
                Arguments.of("persistence.db.${application.name}.username", "persistence.db.${application.name}.username", "persistence.db.${application.name}.username"),
                Arguments.of("persistence.db{${application.name}}.username", "persistence.db.username", "persistence.db${application.name}.username"),
                Arguments.of("persistence.db.{user}name", "persistence.db.name", "persistence.db.username"),
                Arguments.of("{persistence}.db.username", "db.username", "persistence.db.username"),
                Arguments.of("persistence.{db}.username", "persistence.username", "persistence.db.username"),
                Arguments.of("persistence.db.{username}", "persistence.db", "persistence.db.username"));
    }

    @ParameterizedTest
    @MethodSource
    void propertyParser_givenValidProperty_parsesProperty(String propertyKey, String expectedKeyShort, String expectedKeyLong) {
        // Initialize class to be tested
        PropertyParser propertyParser = new PropertyParser();

        // Invoke method to be tested
        PropertyParser.PropertyParseResult results = propertyParser.parse(propertyKey);

        // Verify test results
        assertEquals(expectedKeyShort, results.get(false));
        assertEquals(expectedKeyLong, results.get(true));
    }
}
