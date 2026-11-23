package com.urlshortener.service;

/** Short URL exists but its TTL has passed. Mapped to HTTP 410 Gone. */
public class ShortUrlExpiredException extends RuntimeException {
    public ShortUrlExpiredException(String shortCode) {
        super("Short URL '" + shortCode + "' has expired");
    }
}
