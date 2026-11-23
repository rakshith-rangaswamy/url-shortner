package com.urlshortener.service;

/** Every candidate code collided. Practically unreachable with 62^7 codes; mapped to HTTP 503. */
public class ShortCodeExhaustedException extends RuntimeException {
    public ShortCodeExhaustedException(int attempts) {
        super("Could not find a free short code after " + attempts + " attempts");
    }
}
