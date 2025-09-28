package com.github.kyrobbins.common.utility;

import com.github.kyrobbins.common.exception.ParserException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.ParseException;
import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeUtilsIT {

    public static Stream<Arguments> parseFromTimeString() {
        return Stream.of(
                Arguments.of("3m5s", Duration.ofMinutes(3).plusSeconds(5)),
                Arguments.of("-3m-5s", Duration.ZERO.minusMinutes(3).minusSeconds(5)),
                Arguments.of("3m-5s", Duration.ofMinutes(3).minusSeconds(5)),
                Arguments.of("+3m+5s", Duration.ofMinutes(3).plusSeconds(5)),
                Arguments.of("+3m-5s", Duration.ofMinutes(3).minusSeconds(5)),
                Arguments.of(" 3m 5s", Duration.ofMinutes(3).plusSeconds(5)),
                Arguments.of(" -3m -5s", Duration.ZERO.minusMinutes(3).minusSeconds(5)),
                Arguments.of("  3m   -5s", Duration.ofMinutes(3).minusSeconds(5)),
                Arguments.of("  +3m   +5s", Duration.ofMinutes(3).plusSeconds(5)),
                Arguments.of("  +3m   -5s", Duration.ofMinutes(3).minusSeconds(5)),
                Arguments.of("9w8d7h6m5s4ms3us2ns", Duration.ofDays(63).plusDays(8).plusHours(7).plusMinutes(6).plusSeconds(5).plusMillis(4).plusNanos(3002)));
    }

    @ParameterizedTest
    @MethodSource
    void parseFromTimeString(String inputString, Duration expected) {
        // Invoke method to be tested
        Duration actual = assertDoesNotThrow(() -> TimeUtils.parseFromTimeString(inputString));

        // Verify test results
        assertEquals(expected, actual);
    }

    public static Stream<Arguments> parseFromTimeString_throwsException() {
        return Stream.of(
                Arguments.of("3mm5s", "Failed to parse time string, error at index 2", "Invalid time unit 'mm'"),
                Arguments.of("0s", "Failed to parse time string, error at index 1", "Cannot set time value to 0"),
                Arguments.of("3m 4 2s", "Failed to parse time string, error at index 4", "Unexpected whitespace"),
                Arguments.of("3m -4 2s", "Failed to parse time string, error at index 5", "Unexpected whitespace"),
                Arguments.of("3m +4 2s", "Failed to parse time string, error at index 5", "Unexpected whitespace"),
                Arguments.of("3m +4u", "Failed to parse time string, error at index 5", "Invalid time unit 'u'"),
                Arguments.of("-3-1s", "Failed to parse time string, error at index 2", "Unexpected character '-'"),
                Arguments.of("-3+1s", "Failed to parse time string, error at index 2", "Unexpected character '+'"),
                Arguments.of("-s", "Failed to parse time string, error at index 1", "Unexpected character 's'"),
                Arguments.of("+s", "Failed to parse time string, error at index 1", "Unexpected character 's'"),
                Arguments.of("-3s s", "Failed to parse time string, error at index 4", "Illegal character 's'"),
                Arguments.of("-", "Failed to parse time string, error at index 1", "Unexpected end of expression"),
                Arguments.of("+", "Failed to parse time string, error at index 1", "Unexpected end of expression"),
                Arguments.of("-3", "Failed to parse time string, error at index 2", "Unexpected end of expression"),
                Arguments.of("+3", "Failed to parse time string, error at index 2", "Unexpected end of expression"));
    }

    @ParameterizedTest
    @MethodSource
    void parseFromTimeString_throwsException(String inputString, String expectedError, String expectedRootError) {
        assertThatThrownBy(() -> TimeUtils.parseFromTimeString(inputString))
                .isInstanceOf(ParserException.class)
                .hasMessage(expectedError)
                .rootCause()
                .isInstanceOf(ParseException.class)
                .hasMessage(expectedRootError);
    }
}
