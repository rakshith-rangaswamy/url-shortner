package com.urlshortener.api;

/**
 * Single source of truth for the short-code format. Used by the request DTO, the redirect route, the config
 * limit, and checked against the {@code url_mapping.short_code} column by a test.
 */
public final class ShortCodes {

    /** Must match VARCHAR(n) of url_mapping.short_code. */
    public static final int MAX_LENGTH = 7;

    /** Base62 characters only, 1..MAX_LENGTH long. Compile-time constant so it can be used in annotations. */
    public static final String PATTERN = "[A-Za-z0-9]{1," + MAX_LENGTH + "}";

    private ShortCodes() {
    }
}
