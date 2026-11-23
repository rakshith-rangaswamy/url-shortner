package com.urlshortener.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class HttpUrlValidatorTest {

    private final HttpUrlValidator validator = new HttpUrlValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com",
            "https://example.com/a/b?c=d#e",
            "HTTPS://Example.com:8443/path",
            "https://sub.domain.co.uk"
    })
    void acceptsHttpAndHttpsUrls(String url) {
        assertThat(validator.isValid(url, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "data:text/html,<script>alert(1)</script>",
            "ftp://example.com/file",
            "file:///etc/passwd",
            "mailto:someone@example.com",
            "example.com",
            "/relative/path",
            "http://",
            "https:///no-host",
            "http://exa mple.com",
            ""
    })
    void rejectsEverythingElse(String url) {
        assertThat(validator.isValid(url, null)).isFalse();
    }

    @org.junit.jupiter.api.Test
    void nullIsLeftToNotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
