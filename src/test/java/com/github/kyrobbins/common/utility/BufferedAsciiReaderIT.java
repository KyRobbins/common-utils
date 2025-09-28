package com.github.kyrobbins.common.utility;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BufferedAsciiReaderIT {

    private static final String TEST_INPUT_STRING = "The quick brown fox jumped over the lazy dog";

    @Test
    void bufferedParser_usingString() throws Exception {
        BufferedAsciiReader reader = new BufferedAsciiReader(TEST_INPUT_STRING, 5);
        bufferedParserTests(reader);
    }

    @Test
    void bufferedParser_usingInputStream() throws Exception {
        BufferedAsciiReader reader = new BufferedAsciiReader(new ByteArrayInputStream(TEST_INPUT_STRING.getBytes(StandardCharsets.UTF_8)), 5);
        bufferedParserTests(reader);
    }

    private void bufferedParserTests(BufferedAsciiReader tmpReader) throws Exception {
        char[] readChars = new char[15];

        try (BufferedAsciiReader reader = tmpReader) {
            assertEquals(4, reader.read(readChars, 0, 4));
            assertEquals("The ", new String(readChars, 0, 4));
            assertEquals(5, reader.read(readChars, 0, 5));
            assertEquals("quick", new String(readChars, 0, 5));

            assertEquals(' ', reader.peekChar());
            assertEquals(' ', reader.peekChar());

            assertEquals(' ', reader.nextChar());
            assertEquals(15, reader.read(readChars, 0, 15));
            assertEquals("brown fox jumpe", new String(readChars));
            assertEquals(15, reader.read(readChars, 0, 15));
            assertEquals("d over the lazy", new String(readChars));

            assertFalse(reader.isEndOfStream());
            assertEquals(4, reader.read(readChars, 0, 15));
            assertEquals(" dog", new String(readChars, 0, 4));
            assertTrue(reader.isEndOfStream());
            assertFalse(tmpReader.isClosed());
        } finally {
            assertTrue(tmpReader.isClosed());
        }
    }
}
