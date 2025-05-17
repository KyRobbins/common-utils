package com.github.kyrobbins.common.exception;

/** Used to indicate that an error occurred while parsing ascii data */
public class ParserException extends RuntimeException {
    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
