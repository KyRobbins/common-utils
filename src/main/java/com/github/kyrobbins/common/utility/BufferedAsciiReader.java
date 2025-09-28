package com.github.kyrobbins.common.utility;

import jakarta.annotation.Nonnull;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

/** Utility class for reading ascii data with useful methods for parsing */
public class BufferedAsciiReader extends Reader implements Closeable {

    /** The default size of the buffer to use if none specified */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /** The input source for the data being read */
    private final Reader input;
    /** The buffer to be used for filling with data between read operations */
    private final char[] buffer;
    /** The current character to be read */
    private int cursor;
    /** One beyond the last character to be read, or -1 if there are no characters left to read */
    private int limit = -1;
    /** Indicates if the input source is now empty */
    private boolean inputEmpty;
    /** Denotes if the reader is currently closed */
    @Getter
    private boolean closed;

    /** Indicates the number of characters that have been read since the reader was created */
    @Getter
    private int charsRead;
    /** Indicates the number of new lines that have been read since the reader was created */
    @Getter
    private int linesRead;

    public BufferedAsciiReader(String string) {
        this(new StringReader(string));
    }

    public BufferedAsciiReader(String string, int bufferSize) {
        this(new StringReader(string), bufferSize);
    }

    public BufferedAsciiReader(InputStream inputStream) {
        this(new InputStreamReader(inputStream));
    }

    public BufferedAsciiReader(InputStream inputStream, int bufferSize) {
        this(new InputStreamReader(inputStream), bufferSize);
    }

    public BufferedAsciiReader(Reader reader) {
        this(reader, DEFAULT_BUFFER_SIZE);
    }

    public BufferedAsciiReader(Reader reader, int bufferSize) {
        this.input = reader;
        this.buffer = new char[bufferSize];
    }

    /**
     * Returns the current character of the stream without advancing the cursor
     *
     * @return The current character of the stream
     * @throws IOException If there was an issue reading from the stream
     */
    public char peekChar() throws IOException {
        if (!closed) {
            if (!isEndOfStream()) {
                return buffer[cursor];
            } else {
                throw new IOException("No data left to read");
            }
        } else {
            throw new IOException("Reader is closed");
        }
    }

    public char nextChar() throws IOException {
        if (!closed) {
            if (!isEndOfStream()) {
                char[] cbuf = new char[1];
                if (read(cbuf, 0, 1) < 1) {
                    throw new IOException("Failed to read character");
                }
                return cbuf[0];
            } else {
                throw new IOException("No data left to read");
            }
        } else {
            throw new IOException("Reader is closed");
        }
    }

    @Override
    public int read(@Nonnull char[] cbuf, int offset, int length) throws IOException {
        if (!closed) {
            int destIndex = offset;
            int amountToCopy = length;
            int amountCopied = 0;

            if (needsRefill()) {
                fill();
            }

            while (!isEndOfStream() && amountToCopy > 0) {
                boolean cursorStartsBeforeLimit = cursor <= limit;
                int amountAvailable = getAmountAvailable();

                int amountToCopyThisIteration;

                if (cursorStartsBeforeLimit) {
                    amountToCopyThisIteration = (limit - cursor) + 1;
                } else {
                    amountToCopyThisIteration = (buffer.length - cursor) + 1;
                }

                if (amountToCopyThisIteration > amountToCopy) {
                    amountToCopyThisIteration = amountToCopy;
                }

                destIndex = offset + amountCopied;

                if (amountAvailable >= amountToCopyThisIteration) {
                    System.arraycopy(buffer, cursor, cbuf, destIndex, amountToCopyThisIteration);
                    amountCopied += amountToCopyThisIteration;
                    cursor = (cursor + amountToCopyThisIteration) % buffer.length;
                } else {
                    System.arraycopy(buffer, cursor, cbuf, destIndex, amountAvailable);
                    cursor = (cursor + amountAvailable) % buffer.length;
                    amountCopied += amountAvailable;
                    limit = -1;
                    fill();
                }
                if (cursorStartsBeforeLimit & (cursor > limit || cursor == 0)) {
                    limit = -1;
                }

                amountToCopy = length - amountCopied;
            }

            charsRead += amountCopied;

            for (int i = offset; i < offset + length; ++i) {
                if (cbuf[i] == '\n') {
                    ++linesRead;
                }
            }

            return amountCopied;
        } else {
            throw new IOException("Reader is closed");
        }
    }

    /**
     * Returns the amount of data currently available within the buffer
     *
     * @return The amount of data currently available within the buffer
     */
    private int getAmountAvailable() {
        if (limit < 0) {
            return 0;
        } else if (cursor <= limit) {
            return (limit - cursor) + 1;
        } else {
            return (buffer.length - cursor) + limit + 1;
        }
    }

    /**
     * Indicates if the buffer is currently empty but the end of the input stream hasn't been reached yet.
     *
     * @return True if the buffer should be refilled, False otherwise
     */
    private boolean needsRefill() {
        return bufferIsEmpty() && !inputEmpty;
    }

    /**
     * Indicates if the buffer currently has no data to be read
     *
     * @return True if the buffer currently has no data to be read, False otherwise
     */
    private boolean bufferIsEmpty() {
        return limit < 0;
    }

    /**
     * Fills the remainder of the buffer with data
     *
     * @throws IOException If there was an issue reading from the input
     */
    private void fill() throws IOException {
        while (!inputEmpty && (limit < 0 || ((limit + 1) % buffer.length) != cursor)) {
            int amountToPull;

            if (limit < 0) {
                amountToPull = buffer.length;
            } else if (limit < cursor) {
                amountToPull = cursor - limit;
            } else {
                amountToPull = buffer.length - limit;
            }

            int startIndex;

            if (limit < 0) {
                startIndex = 0;
            } else if (limit == buffer.length - 1) {
                startIndex = 0;
            } else {
                startIndex = limit;
            }

            final int amountRead = input.read(buffer, startIndex, amountToPull);

            if (amountRead > 0) {
                limit = (limit + amountRead) % buffer.length;
            } else {
                inputEmpty = true;
            }
        }
    }

    /**
     * Indicates if the end of the stream has been reached, meaning there is no data available in the buffer or in the
     * input source
     *
     * @return True if there is no data in the buffer or input source, False otherwise
     * @throws IOException If there was an issue reading from the input
     */
    public boolean isEndOfStream() throws IOException {
        if (needsRefill()) {
            fill();
        }

        return inputEmpty && limit < 0;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        inputEmpty = true;
        limit = -1;
        input.close();
    }
}
