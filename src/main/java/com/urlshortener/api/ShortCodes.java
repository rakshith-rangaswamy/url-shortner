package com.urlshortener.api;

/**
 * Single source of truth for the short-code format. Used by the request DTO, the redirect route, the config
 * limit, and checked against the {@code url_mapping.short_code} column by a test.
 */
public final class ShortCodes {

    /** Must match VARCHAR(n) of url_mapping.short_code. Generated codes are always this long. */
    public static final int MAX_LENGTH = 7;

    /** Shortest custom alias accepted (ADR-0009). */
    public static final int ALIAS_MIN_LENGTH = 3;

    /** Any stored code: Base62, 1..MAX_LENGTH. Compile-time constant so it can be used in annotations. */
    public static final String PATTERN = "[A-Za-z0-9]{1," + MAX_LENGTH + "}";

    /** A custom alias: Base62, ALIAS_MIN_LENGTH..MAX_LENGTH. */
    public static final String ALIAS_PATTERN = "[A-Za-z0-9]{" + ALIAS_MIN_LENGTH + "," + MAX_LENGTH + "}";

    private ShortCodes() {
    }
}
