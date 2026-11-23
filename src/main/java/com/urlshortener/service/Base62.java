package com.urlshortener.service;

import java.math.BigInteger;

/** Base62 encoding (0-9, a-z, A-Z) used for short codes. */
public final class Base62 {

    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final BigInteger BASE = BigInteger.valueOf(ALPHABET.length());

    private Base62() {
    }

    /** Encodes {@code bytes} as one unsigned big-endian number. */
    public static String encode(byte[] bytes) {
        return encode(new BigInteger(1, bytes));
    }

    /** Encodes a non-negative number with no padding ("0" for zero). */
    public static String encode(BigInteger value) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException("value must be non-negative");
        }
        if (value.signum() == 0) {
            return "0";
        }
        StringBuilder out = new StringBuilder();
        BigInteger n = value;
        while (n.signum() > 0) {
            BigInteger[] qr = n.divideAndRemainder(BASE);
            out.append(ALPHABET.charAt(qr[1].intValue()));
            n = qr[0];
        }
        return out.reverse().toString();
    }

    /**
     * Exactly {@code width} characters: the value modulo 62^width, left-padded with '0'. Taking the low-order
     * digits keeps every character uniformly distributed for hash input.
     */
    public static String encodeFixed(byte[] bytes, int width) {
        BigInteger modulus = BASE.pow(width);
        String digits = encode(new BigInteger(1, bytes).mod(modulus));
        return "0".repeat(width - digits.length()) + digits;
    }
}
