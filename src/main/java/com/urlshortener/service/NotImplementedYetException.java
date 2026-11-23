package com.urlshortener.service;

/** Thrown by scaffold methods whose business logic has not been written. Mapped to HTTP 501. */
public class NotImplementedYetException extends RuntimeException {
    public NotImplementedYetException(String what) {
        super(what + " is not implemented yet");
    }
}
