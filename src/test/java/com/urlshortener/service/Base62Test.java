package com.urlshortener.service;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62Test {

    @Test
    void encodesKnownValues() {
        assertThat(Base62.encode(BigInteger.ZERO)).isEqualTo("0");
        assertThat(Base62.encode(BigInteger.valueOf(61))).isEqualTo("Z");
        assertThat(Base62.encode(BigInteger.valueOf(62))).isEqualTo("10");
        assertThat(Base62.encode(BigInteger.valueOf(3843))).isEqualTo("ZZ");
        assertThat(Base62.encode(new byte[]{0x01, 0x00})).isEqualTo("48"); // 256 = 4*62 + 8
    }

    @Test
    void bytesAreUnsigned() {
        assertThat(Base62.encode(new byte[]{(byte) 0xFF})).isEqualTo("47"); // 255 = 4*62 + 7
    }

    @Test
    void fixedWidthPadsAndTruncatesToLowOrderDigits() {
        assertThat(Base62.encodeFixed(new byte[]{0x01}, 7)).isEqualTo("0000001");
        byte[] big = new byte[32];
        java.util.Arrays.fill(big, (byte) 0xAB);
        String code = Base62.encodeFixed(big, 7);
        assertThat(code).hasSize(7).matches("[0-9a-zA-Z]{7}");
        assertThat(Base62.encode(big)).endsWith(code);
    }

    @Test
    void rejectsNegative() {
        assertThatThrownBy(() -> Base62.encode(BigInteger.valueOf(-1))).isInstanceOf(IllegalArgumentException.class);
    }
}
