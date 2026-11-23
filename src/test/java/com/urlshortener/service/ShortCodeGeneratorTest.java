package com.urlshortener.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Target behaviour for code generation and the collision policy. Disabled until the logic is implemented;
 * enable each test as its feature lands.
 */
@Disabled("Business logic not implemented yet")
class ShortCodeGeneratorTest {

    @Test
    void base62EncodesUsingOnlyTheAlphabet() {
        // Base62.encode(bytes) contains only Base62.ALPHABET characters
    }

    @Test
    void sameInputGivesSameCode() {
        // candidate(url, user, 0) is deterministic
    }

    @Test
    void codeHasConfiguredLength() {
        // candidate(...).length() == config.getCodeLength()
    }

    @Test
    void eachRehashAttemptGivesADifferentCode() {
        // candidate(url, user, 0..maxRehashAttempts) are all distinct
    }

    @Test
    void attemptsBeyondRehashLimitUseSaltOrCounter() {
        // candidate(url, user, maxRehashAttempts + n) still unique and of the right length
    }
}
