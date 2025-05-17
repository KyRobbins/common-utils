package com.github.kyrobbins.common.utility.config;

import com.github.kyrobbins.common.exception.ConfigurationException;
import com.github.kyrobbins.common.exception.ParserException;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;

import java.text.ParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RequiredArgsConstructor
class PropertyParser {

    /**
     * Parses the given property key, verifying its syntactical correctness and returning a {@link PropertyParseResult}
     * which can be used to unwrap overrides
     *
     * @param propertyKey The property key to parse
     * @return The parsed {@link PropertyParseResult}
     */
    public PropertyParseResult parse(@Nonnull final String propertyKey) {
        try {
            Context context = new Context(propertyKey);
            context.pushPropertyPart(PropertyPart.Builder.Type.ROOT);
            PropertyPart.Builder rootPart = context.currentPropertyPart;
            context.pushPropertyPart(PropertyPart.Builder.Type.LITERAL);

            // Keep parsing till it's at the end of the key, and all the context stacks are closed out
            Supplier<Boolean> stillParsing = () -> context.cursorIndex < propertyKey.length() || context.currentPropertyPart != null;

            while (stillParsing.get()) {
                Runnable deferredAction = () -> {};
                char currentChar = context.scanChar();

                switch (currentChar) {
                    case '$':
                        if (context.peekAhead() == '{') {
                            context.startPlaceholderPart();
                        } else {
                            throw new ParseException(
                                    "Unexpected '$', placeholders require brackets", context.cursorIndex);
                        }
                        break;
                    case '{':
                        context.startOverridePart();
                        break;
                    case '.':
                        deferredAction = () -> context.pushPropertyPart(PropertyPart.Builder.Type.LITERAL);
                    case '}':
                    case '\0':
                        context.endCurrentPart();
                        break;
                    default:
                        context.consumeCharacter();
                }

                deferredAction.run();
            }

            return new PropertyParseResult(rootPart.build());
        } catch (ParseException e) {
            throw new ParserException("Could not parse property key, error at index " + e.getErrorOffset(), e);
        }
    }

    /** Defines a parsing result with a single method for unwrapping the override values, creating an absolute key */
    @RequiredArgsConstructor
    public static class PropertyParseResult {

        /** The raw property key part data */
        private final PropertyPart propertyPart;

        /**
         * Unwraps the override values in the property key
         *
         * @param keepOverrides Dictates if the override values are simply unwrapped or removed entirely
         * @return The property key with override values unwrapped
         */
        public String get(boolean keepOverrides) {
            return propertyPart.unwrap(keepOverrides);
        }
    }

    /**
     * A {@link PropertyPart} implementation for whole keys
     *
     * <p>A whole key is one that can include multiple dot delimited parts, i.e. {@code my.property.key}</p>
     */
    public static class WholePropertyPart extends PropertyPart {

        public WholePropertyPart(String stringValue, int startIndex) {
            super(stringValue, startIndex);
        }

        @Override
        public String unwrap(boolean keepOverrides) {
            String unwrappedValue = super.unwrap(keepOverrides);
            int lastIndex = unwrappedValue.length() - 1;

            if (unwrappedValue.charAt(lastIndex) == '.') {
                unwrappedValue = unwrappedValue.substring(0, lastIndex);
            }

            return unwrappedValue;
        }
    }

    /**
     * A {@link PropertyPart} implementation for literal key parts
     *
     * <p>A literal key part is a simple string literal element of a key, such as {@code my} in {@code my.property.key}</p>
     */
    public static class LiteralPropertyPart extends PropertyPart {

        public LiteralPropertyPart(String stringValue, int startIndex) {
            super(stringValue, startIndex);
        }

        @Override
        public String unwrap(boolean keepOverrides) {
            String unwrappedValue = super.unwrap(keepOverrides);
            return unwrappedValue.equals(".") ? "" : unwrappedValue;
        }
    }

    /**
     * A {@link PropertyPart implementation for override key parts
     *
     * <p>An override key part is one wrapped in {}, such as {@code my.{override}.key}
     */
    public static class OverridePropertyPart extends PropertyPart {

        public OverridePropertyPart(String stringValue, int startIndex) {
            super(stringValue, startIndex);
        }

        @Override
        public String unwrap(boolean keepOverrides) {
            String unwrappedValue = super.unwrap(keepOverrides);
            return keepOverrides ? unwrappedValue.substring(1, unwrappedValue.length() - 1) : "";
        }
    }

    /**
     * A {@link PropertyPart} implementation for placeholder key parts
     *
     * <p>A placeholder key part is one wrapped in ${}, such as {@code my.${placeholder}.key}, where the placeholder is
     * an expandable, interpolated key reference</p>
     */
    public static class PlaceholderPropertyPart extends PropertyPart {

        public PlaceholderPropertyPart(String stringValue, int startIndex) {
            super(stringValue, startIndex);
        }
    }

    /** Abstract type representing a basic property part (a small piece of a property key) */
    @RequiredArgsConstructor
    public abstract static class PropertyPart {

        private final String stringValue;
        private final int startIndex;
        private final List<PropertyPart> propertyParts = new ArrayList<>();

        /**
         * Unwraps the {@link PropertyPart}, applying any transformations that are needed
         *
         * @param keepOverrides Denotes if the unwrapped override values should be kept for not
         * @return The unwrapped property key
         */
        public String unwrap(boolean keepOverrides) {
            String unwrappedValue = stringValue;

            // Executing in reverse so indexes don't need to be updated
            for (int i = propertyParts.size() - 1; i >= 0; i--) {
                final PropertyPart propertyPart = propertyParts.get(i);
                String unwrappedPart = propertyPart.unwrap(keepOverrides);

                final int partStartIndex = propertyPart.startIndex;
                final int partEndIndex = partStartIndex + propertyPart.stringValue.length();
                unwrappedValue = unwrappedValue.substring(0, partStartIndex - startIndex)
                        + unwrappedPart
                        + unwrappedValue.substring(partEndIndex - startIndex);
            }

            return unwrappedValue;
        }

        public static Builder builder(String fullPropertyValue) {
            return new Builder(fullPropertyValue);
        }

        @RequiredArgsConstructor
        public static class Builder {
            /** THe original full property key */
            private final String fullPropertyValue;
            /** The type of property part being built */
            private Type type;
            /** The index of where this part begins in the full property key */
            private int startIndex;
            /** The index of where this part ends in the full property key */
            private int endIndex;
            /** The child property parts of this one */
            private final List<PropertyPart.Builder> propertyParts = new ArrayList<>();

            /** The different types of property parts that can be parsed */
            public enum Type {
                /** The root property part that every other property part is a child of, used as a marker */
                ROOT,
                /** A whole property part, such as {@code my.property.key} */
                WHOLE,
                /** A property part literal, such as {@code property} in {@code my.property.part} */
                LITERAL,
                /** A property part override, such as in {@code my.{override}.key} */
                OVERRIDE,
                /** aA property part placeholder, such as in {@code my.${placeholder}.key} */
                PLACEHOLDER
            }

            public Builder type(Type type) {
                this.type = type;
                return this;
            }

            public Builder startIndex(int startIndex) {
                this.startIndex = startIndex;
                return this;
            }

            // UnusedReturnResult - This is a builder pattern
            @SuppressWarnings("UnusedReturnValue")
            public Builder endIndex(int endIndex) {
                this.endIndex = endIndex;
                return this;
            }

            public PropertyPart build() {
                final String partValue = fullPropertyValue.substring(startIndex, endIndex);

                PropertyPart propertyPart;

                switch (type) {
                    case WHOLE:
                    case ROOT:
                        propertyPart = new WholePropertyPart(partValue, startIndex);
                        break;
                    case LITERAL:
                        propertyPart = new LiteralPropertyPart(partValue, startIndex);
                        break;
                    case OVERRIDE:
                        propertyPart = new OverridePropertyPart(partValue, startIndex);
                        break;
                    case PLACEHOLDER:
                        propertyPart = new PlaceholderPropertyPart(partValue, startIndex);
                        break;
                    default:
                        throw new ParserException(String.format("Invalid type: '%s'", type.getClass().getCanonicalName()));
                 }

                 propertyPart.propertyParts.addAll(propertyParts.stream().map(PropertyPart.Builder::build).collect(Collectors.toList()));
                return propertyPart;
            }
        }
    }

    /** Utility class used for tracking the current state of the parsing process and tracking parsing data */
    @RequiredArgsConstructor
    private static class Context {
        /** The full, original property key */
        private final String propertyKey;
        /** The property part stack used for tracking property parts still needing to be parsed */
        private final Deque<PropertyPart.Builder> propertyPartBuilders = new ArrayDeque<>();
        /** The property part currently being parsed */
        private PropertyPart.Builder currentPropertyPart;
        /** The last index value of the original property key scanned, used for infinite loop detection */
        private int lastScanIndex = -1;
        /** The last count of property parts waiting to be parsed, used for infinite loop detection */
        private int lastPropertyPartCount = 0;
        /** the current cursor index, dictates the value to be returned by {@link #scanChar()} */
        private int cursorIndex = 0;

        /**
         * Pulls the character at the current cursor index
         *
         * @return The character at the current cursor index
         */
        private char scanChar() {
            return scanChar(true);
        }

        /**
         * Pulls the character at the current cursor index
         *
         * @param doStateCheck Denotes if a state check should be done to check for infinite loop errors
         * @return The character at the current cursor index
         */
        private char scanChar(boolean doStateCheck) {
            char c;

            if (doStateCheck && stateHasNotChanged()) {
                // This should never happen, unless there is a bug in this class logic
                throw new ParserException("Parser logic error, infinite loop detected");
            }

            if (propertyKey.length() > cursorIndex) {
                c = propertyKey.charAt(cursorIndex);
            } else {
                c = '\0';
            }

            return c;
        }

        /**
         * Denotes if the state has not changed since the last time it was checked, which could indicate an infinite
         * loop error
         *
         * @return True if the state has not changed, False otherwise
         */
        private boolean stateHasNotChanged() {
            boolean stateHasNotChanged = lastScanIndex >= cursorIndex && lastPropertyPartCount == propertyPartBuilders.size();

            lastScanIndex = cursorIndex;
            lastPropertyPartCount = propertyPartBuilders.size();

            return stateHasNotChanged;
        }

        /** Advances the cursor to the next character of the property key */
        private void advanceCursor() {
            ++cursorIndex;
        }

        /**
         * Peeks at and returns the next character in the property key without advancing the cursor index
         *
         * @return The next character beyond the cursor index in the property key
         */
        private char peekAhead() {
            final int nextIndex = cursorIndex + 1;
            return nextIndex < propertyKey.length() ? propertyKey.charAt(nextIndex) : '\0';
        }

        /**
         * Peeks at and returns the previous character in the property key without moving the cursor index
         *
         * @return The previous character before the cursor index in the property key
         */
        private char peekBehind() {
            final int prevIndex = cursorIndex - 1;
            return prevIndex >= 0 ? propertyKey.charAt(prevIndex) : '\0';
        }

        /**
         * Attempts to start tracking a new literal property part, advancing the cursor if needed
         *
         * @see LiteralPropertyPart
         * @see PropertyPart.Builder.Type#LITERAL
         */
        private void attemptStartLiteralPart() {
            // To keep logic simple, this method can be used for every alphanumeric character, and this logic just
            // skips making a literal part if it's already in one.
            if (currentPropertyPart.type != PropertyPart.Builder.Type.LITERAL) {
                pushPropertyPart(PropertyPart.Builder.Type.LITERAL);
                advanceCursor();
            }
        }

        /**
         * Starts tracking a new placeholder property part, advancing the cursor
         *
         * @see PlaceholderPropertyPart
         * @see PropertyPart.Builder.Type#PLACEHOLDER
         */
        private void startPlaceholderPart() {
            pushPropertyPart(PropertyPart.Builder.Type.PLACEHOLDER);
            advanceCursor();
            advanceCursor();
            pushPropertyPart(PropertyPart.Builder.Type.WHOLE);
        }

        /**
         * Starts tracking a new override property part, advancing the cursor
         *
         * @see OverridePropertyPart
         * @see PropertyPart.Builder.Type#OVERRIDE
         */
        private void startOverridePart() {
            pushPropertyPart(PropertyPart.Builder.Type.OVERRIDE);
            advanceCursor();
            pushPropertyPart(PropertyPart.Builder.Type.WHOLE);
        }

        /**
         * Ends processing for the current property part, doing final validation checks to make sure that the property
         * part contained valid syntax, potentially also advancing the cursor if needed
         *
         * @throws ParseException If the property part contained a syntax error
         */
        private void endCurrentPart() throws ParseException {
            final PropertyPart.Builder.Type partType = currentPropertyPart.type;
            final int partEndIndex = cursorIndex;
            final int partStartIndex = currentPropertyPart.startIndex;
            final char c = scanChar(false);

            switch (c) {
                case '.':
                    if (partType == PropertyPart.Builder.Type.LITERAL) {
                        if (partEndIndex - partStartIndex < 1) {
                            throw new ParseException("Unexpected end of property part", partEndIndex);
                        }
                    } else {
                        // This should not be possible, unless a bug exists in this class logic
                        throw new ParseException("Unexpected start of property part", partEndIndex);
                    }

                    advanceCursor();
                    popPropertyPart();
                    break;
                case '}':
                    int emptyValueSize = 0;

                    switch (partType) {
                        case PLACEHOLDER:
                            ++emptyValueSize;
                        case OVERRIDE:
                            ++emptyValueSize;
                            advanceCursor();
                        case LITERAL:
                        case WHOLE:
                            if (partEndIndex - partStartIndex <= emptyValueSize) {
                                throw new ParseException("Property part cannot be blank", partEndIndex);
                            }
                            break;
                        case ROOT:
                            throw new ParseException("Unexpected '}'", partEndIndex);
                    }

                    popPropertyPart();
                    break;
                case '\0':
                    switch (partType) {
                        case LITERAL:
                        case WHOLE:
                        case ROOT:
                            if (partEndIndex - partStartIndex <= 0) {
                                throw new ParseException("Property part cannot be blank", partEndIndex - 1);
                            }
                            break;
                        case OVERRIDE:
                        case PLACEHOLDER:
                            throw new ParseException("Unexpected end of property part, expected '}'", partEndIndex - 1);
                    }

                    popPropertyPart();
            }
        }

        /**
         * Beings tracking a new property part, moving any existing property part being parsed onto the
         * {@link #propertyPartBuilders} stack, to finish processing later
         *
         * @param type THe type of property part to begin parsing for
         */
        private void pushPropertyPart(PropertyPart.Builder.Type type) {
            if (currentPropertyPart != null) {
                propertyPartBuilders.push(currentPropertyPart);
            }

            currentPropertyPart = PropertyPart.builder(propertyKey).type(type).startIndex(cursorIndex);
        }

        /**
         * Pops the previous property part that was being processed back off the {@link #propertyPartBuilders} stack,
         * adding the previously tracked property part as a child to this one.
         */
        private void popPropertyPart() {
            PropertyPart.Builder childPart = currentPropertyPart;
            childPart.endIndex(cursorIndex);

            if (!propertyPartBuilders.isEmpty()) {
                currentPropertyPart = propertyPartBuilders.pop();
                currentPropertyPart.propertyParts.add(childPart);
            } else {
                currentPropertyPart = null;
            }
        }

        /**
         * Consumes the character at the cursor index, advancing the cursor after
         *
         * @throws ParseException If the character was invalid or otherwise unexpected
         */
        private void consumeCharacter() throws ParseException {
            final char currentChar = scanChar(false);

            if (currentChar == '-' || currentChar == '_') {
                String charName = currentChar == '-' ? "hyphen" : "underscore";

                if (!Character.isLetterOrDigit(peekAhead()) || !Character.isLetterOrDigit(peekBehind())) {
                    throw new ParseException(
                            String.format("Unexpected '%s', illegal use of %s", currentChar, charName), cursorIndex);
                }

                advanceCursor();
            } else {
                if (Character.isLetterOrDigit(currentChar)) {
                    // Only start a new part if we're not already doing a literal
                    if (currentPropertyPart.type != PropertyPart.Builder.Type.LITERAL) {
                        attemptStartLiteralPart();
                    } else {
                        advanceCursor();
                    }
                } else {
                    throw new ParseException(String.format("Unsupported character '%s' in property key", currentChar), cursorIndex);
                }
            }
        }

    }
}
