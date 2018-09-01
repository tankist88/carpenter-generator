package com.github.tankist88.carpenter.generator.exception;

public class DeserializationException extends Exception {
    public DeserializationException(String message) {
        super(message);
    }

    public DeserializationException(String message, Throwable t) {
        super(message, t);
    }
}
