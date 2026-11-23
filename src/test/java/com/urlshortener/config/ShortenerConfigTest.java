package com.urlshortener.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.api.ShortCodes;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ShortenerConfigTest {

    private final ObjectMapper mapper = Jackson.newObjectMapper();
    private final Validator validator = Validators.newValidator();

    private Set<ConstraintViolation<ShortenerConfig>> validate(String json) throws Exception {
        return validator.validate(mapper.readValue(json, ShortenerConfig.class));
    }

    @Test
    void defaultsAreValid() throws Exception {
        assertThat(validate("{}")).isEmpty();
    }

    @Test
    void onlySha256OrMd5AreAccepted() throws Exception {
        assertThat(validate("{\"hashAlgorithm\":\"SHA-256\"}")).isEmpty();
        assertThat(validate("{\"hashAlgorithm\":\"MD5\"}")).isEmpty();
        assertThat(validate("{\"hashAlgorithm\":\"SHA-1\"}")).hasSize(1);
    }

    @Test
    void codeLengthCannotExceedColumnAndRouteLimit() throws Exception {
        assertThat(validate("{\"codeLength\":" + ShortCodes.MAX_LENGTH + "}")).isEmpty();
        assertThat(validate("{\"codeLength\":" + (ShortCodes.MAX_LENGTH + 1) + "}")).hasSize(1);
    }

    @Test
    void routePatternMatchesMaxLength() {
        assertThat("a".repeat(ShortCodes.MAX_LENGTH)).matches(ShortCodes.PATTERN);
        assertThat("a".repeat(ShortCodes.MAX_LENGTH + 1)).doesNotMatch(ShortCodes.PATTERN);
        assertThat("ab-cd").doesNotMatch(ShortCodes.PATTERN);
    }
}
