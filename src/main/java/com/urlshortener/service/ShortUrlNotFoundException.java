package com.urlshortener.service;

/** No mapping for the short code. Mapped to HTTP 404. */
public class ShortUrlNotFoundException extends RuntimeException {
    public ShortUrlNotFoundException(String shortCode) {
        super("Short URL '" + shortCode + "' not found");
    }
}
