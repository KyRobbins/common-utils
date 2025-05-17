package com.github.kyrobbins.common.exception;

/** Used to denote an issue with a configuration execution */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
