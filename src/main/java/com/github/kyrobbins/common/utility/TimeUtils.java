package com.github.kyrobbins.common.utility;

import com.github.kyrobbins.common.exception.ParserException;

import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Utility for parsing time strings into a {@link Duration} object */
public class TimeUtils {

    private static final int MATH_BASE = 10;

    private static final Set<Character> VALID_TIME_UNIT_CHARS = Stream.of('d', 'h', 'm', 'n', 's', 'u', 'w')
            .collect(Collectors.toSet());

    private static final Map<String, ChronoUnit> TIME_UNITS_MAP;

    static {
        TIME_UNITS_MAP = new HashMap<>();
        TIME_UNITS_MAP.put("w", ChronoUnit.WEEKS);
        TIME_UNITS_MAP.put("d", ChronoUnit.DAYS);
        TIME_UNITS_MAP.put("h", ChronoUnit.HOURS);
        TIME_UNITS_MAP.put("m", ChronoUnit.MINUTES);
        TIME_UNITS_MAP.put("s", ChronoUnit.SECONDS);
        TIME_UNITS_MAP.put("ms", ChronoUnit.MILLIS);
        TIME_UNITS_MAP.put("us", ChronoUnit.MICROS);
        TIME_UNITS_MAP.put("ns", ChronoUnit.NANOS);
    }

    private enum ParserPhase {
        SIGNED_VALUE,
        VALUE,
        UNIT
    }

    public static Duration parseFromTimeString(String timeString) throws ParserException {
        boolean isNegative = false;
        int vIndex = 0;
        int timeValue = 0;
        String timeUnitString = "";
        ParserPhase parserPhase = ParserPhase.SIGNED_VALUE;

        Duration duration = Duration.ZERO;

        try (BufferedAsciiReader parser = new BufferedAsciiReader(timeString)) {
            while (!parser.isEndOfStream()) {
                char c = parser.nextChar();
                boolean processingChar;

                do {
                    processingChar = false;

                    if (Character.isWhitespace(c) && parserPhase == ParserPhase.VALUE) {
                        throw new ParseException("Unexpected whitespace", parser.getCharsRead() - 1);
                    }

                    if (!Character.isWhitespace(c) || (parserPhase != ParserPhase.VALUE && parserPhase != ParserPhase.SIGNED_VALUE)) {
                        if (Character.isDigit(c) && (parserPhase == ParserPhase.VALUE || parserPhase == ParserPhase.SIGNED_VALUE)) {
                            timeValue += (int) ((c - '0') * Math.pow(MATH_BASE, vIndex++));
                            parserPhase = ParserPhase.VALUE;
                        } else if (VALID_TIME_UNIT_CHARS.contains(c) && parserPhase == ParserPhase.UNIT) {
                            timeUnitString += c;
                        } else {
                            if (parserPhase == ParserPhase.UNIT && !VALID_TIME_UNIT_CHARS.contains(c)) {
                                try {
                                    duration = calculateNewDuration(duration, timeValue * (isNegative ? -1 : 1), timeUnitString);

                                    timeValue = 0;
                                    vIndex = 0;
                                    timeUnitString = "";
                                    isNegative = false;
                                    parserPhase = ParserPhase.SIGNED_VALUE;
                                    processingChar = true;
                                } catch (ParserException e) {
                                    throw new ParseException(e.getMessage(), parser.getCharsRead() - 2);
                                }
                            } else if (parserPhase == ParserPhase.VALUE && VALID_TIME_UNIT_CHARS.contains(c)) {
                                if (timeValue == 0) {
                                    throw new ParseException("Cannot set time value to 0", parser.getCharsRead() - 1);
                                }

                                parserPhase = ParserPhase.UNIT;
                                processingChar = true;
                            } else if (c == '-') {
                                if (parserPhase == ParserPhase.SIGNED_VALUE) {
                                    if (!parser.isEndOfStream()) {
                                        char peekNext = parser.peekChar();

                                        if (Character.isDigit(peekNext)) {
                                            isNegative = true;
                                        } else {
                                            throw new ParseException(String.format("Unexpected character '%s'", peekNext), parser.getCharsRead());
                                        }

                                        parserPhase = ParserPhase.VALUE;
                                    } else {
                                        throw new ParseException("Unexpected end of expression", parser.getCharsRead());
                                    }
                                } else {
                                    throw new ParseException("Unexpected character '-'", parser.getCharsRead() - 1);
                                }
                            } else if (c == '+') {
                                if (parserPhase == ParserPhase.SIGNED_VALUE) {
                                    if (!parser.isEndOfStream()) {
                                        char peekNext = parser.peekChar();

                                        if (!Character.isDigit(peekNext)) {
                                            throw new ParseException(String.format("Unexpected character '%s'", peekNext), parser.getCharsRead());
                                        }

                                        parserPhase = ParserPhase.VALUE;
                                    } else {
                                        throw new ParseException("Unexpected end of expression", parser.getCharsRead());
                                    }
                                } else {
                                    throw new ParseException("Unexpected character '+'", parser.getCharsRead() - 1);
                                }
                            } else {
                                throw new ParseException(String.format("Illegal character '%s'", c), parser.getCharsRead() - 1);
                            }
                        }
                    }
                } while (processingChar);
            }

            if (parserPhase == ParserPhase.UNIT) {
                try {
                    duration = calculateNewDuration(duration, timeValue * (isNegative ? -1 : 1), timeUnitString);
                } catch (ParserException e) {
                    throw new ParseException(e.getMessage(), parser.getCharsRead() - 1);
                }
            } else if (parserPhase == ParserPhase.VALUE) {
                throw new ParseException("Unexpected end of expression", parser.getCharsRead());
            }
        } catch (ParseException e) {
            throw new ParserException("Failed to parse time string, error at index " + e.getErrorOffset(), e);
        } catch (IOException e) {
            throw new ParserException("Failed to parse time string", e);
        }

        return duration;
    }

    /**
     * Calculate a new {@link Duration} object given a starting {@link Duration} and a time amount to add to it
     *
     * @param oldDuration The starting {@link Duration}
     * @param tValue      The amount of time to add to the duration (can be negative)
     * @param tUnit       The time unit for the duration being added
     * @return A new {@link Duration} representing the calculated total
     */
    private static Duration calculateNewDuration(Duration oldDuration, int tValue, String tUnit) {
        TemporalUnit timeUnit = TIME_UNITS_MAP.get(tUnit);

        if (timeUnit == null) {
            throw new ParserException(String.format("Invalid time unit '%s'", tUnit));
        }

        // Duration API doesn't allow adding weeks because they are "estimated", but weeks are as
        // definite as a day
        return timeUnit == ChronoUnit.WEEKS ? oldDuration.plusDays(tValue * 7L) : oldDuration.plus(tValue, timeUnit);
    }

}
