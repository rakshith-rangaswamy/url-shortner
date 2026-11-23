package com.urlshortener.service;

/** Base62 alphabet (0-9, a-z, A-Z) used for short codes. Encoding is not implemented yet. */
public final class Base62 {

    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private Base62() {
    }

    public static String encode(byte[] bytes) {
        throw new NotImplementedYetException("Base62.encode");
    }
}
