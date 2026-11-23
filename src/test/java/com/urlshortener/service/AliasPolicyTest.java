package com.urlshortener.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AliasPolicyTest {

    private final AliasPolicy policy = new AliasPolicy(Set.of("docs", "API"));

    @Test
    void reservedWordsAreRejectedCaseInsensitively() {
        assertThat(policy.isReserved("docs")).isTrue();
        assertThat(policy.isReserved("Docs")).isTrue();
        assertThat(policy.isReserved("api")).isTrue();
        assertThatThrownBy(() -> policy.requireAllowed("DOCS")).isInstanceOf(ReservedAliasException.class);
    }

    @Test
    void otherAliasesAreAllowed() {
        assertThat(policy.isReserved("myLink1")).isFalse();
        assertThat(policy.isReserved("docs1")).isFalse();
        assertThat(policy.isReserved(null)).isFalse();
        policy.requireAllowed("myLink1");
    }
}
