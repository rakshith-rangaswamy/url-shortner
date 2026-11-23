package com.urlshortener.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.config.ShortenerConfig;
import io.dropwizard.jackson.Jackson;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ShortCodeGeneratorTest {

    private static final String URL = "https://example.com/some/long/path?x=1";

    private static ShortCodeGenerator generator(String json) throws Exception {
        ObjectMapper mapper = Jackson.newObjectMapper();
        return new ShortCodeGenerator(mapper.readValue(json, ShortenerConfig.class));
    }

    private final ShortCodeGenerator sha256 = generator("{}");

    ShortCodeGeneratorTest() throws Exception {
    }

    @Test
    void digestIsHexSha256OfTheUrl() {
        assertThat(sha256.digest("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void sameInputGivesSameCode() {
        assertThat(sha256.candidate(URL, 0)).isEqualTo(sha256.candidate(URL, 0));
    }

    @Test
    void codesAreSevenBase62Characters() {
        for (int attempt = 0; attempt < 20; attempt++) {
            assertThat(sha256.candidate(URL, attempt)).matches("[0-9a-zA-Z]{7}");
        }
    }

    @Test
    void differentUrlsGiveDifferentCodes() {
        assertThat(sha256.candidate("https://example.com/a", 0)).isNotEqualTo(sha256.candidate("https://example.com/b", 0));
    }

    @Test
    void everyAttemptGivesADifferentCode() {
        Set<String> codes = new HashSet<>();
        for (int attempt = 0; attempt < 20; attempt++) {
            codes.add(sha256.candidate(URL, attempt));
        }
        assertThat(codes).hasSize(20);
    }

    @Test
    void attemptsUpToTheLimitAreRehashesThenSaltedWithACounter() throws Exception {
        ShortCodeGenerator gen = generator("{\"maxRehashAttempts\": 5}");
        byte[] h = java.security.MessageDigest.getInstance("SHA-256").digest(URL.getBytes());
        byte[] rehash1 = java.security.MessageDigest.getInstance("SHA-256").digest(h);
        assertThat(gen.candidate(URL, 1)).isEqualTo(Base62.encodeFixed(rehash1, 7));
        byte[] salted = java.security.MessageDigest.getInstance("SHA-256").digest((URL + "#1").getBytes());
        assertThat(gen.candidate(URL, 6)).isEqualTo(Base62.encodeFixed(salted, 7));
    }

    @Test
    void md5IsSupportedWhenConfigured() throws Exception {
        ShortCodeGenerator md5 = generator("{\"hashAlgorithm\": \"MD5\"}");
        assertThat(md5.digest("abc")).isEqualTo("900150983cd24fb0d6963f7d28e17f72");
        assertThat(md5.candidate(URL, 0)).matches("[0-9a-zA-Z]{7}");
    }
}
