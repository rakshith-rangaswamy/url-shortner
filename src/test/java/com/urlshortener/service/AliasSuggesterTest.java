package com.urlshortener.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AliasSuggesterTest {

    private final Set<String> taken = new HashSet<>();
    private final AliasPolicy policy = new AliasPolicy(Set.of("docs1", "docs2"));

    private AliasSuggester suggester(int max) {
        return new AliasSuggester(max, policy, codes -> {
            Set<String> hit = new HashSet<>(codes);
            hit.retainAll(taken);
            return hit;
        }, new Random(42));
    }

    @Test
    void appendsDigitsFirstWhenThereIsRoom() {
        assertThat(suggester(3).suggest("myLink")).containsExactly("myLink1", "myLink2", "myLink3");
    }

    @Test
    void skipsTakenCandidates() {
        taken.add("myLink1");
        taken.add("myLink3");
        assertThat(suggester(3).suggest("myLink")).containsExactly("myLink2", "myLink4", "myLink5");
    }

    @Test
    void replacesLastCharacterWhenAliasIsAtMaxLength() {
        List<String> suggestions = suggester(3).suggest("myLink1");
        assertThat(suggestions).hasSize(3).allMatch(s -> s.length() == 7).doesNotContain("myLink1");
        assertThat(suggestions).containsExactly("myLink0", "myLink2", "myLink3");
    }

    @Test
    void neverSuggestsReservedWords() {
        assertThat(suggester(3).suggest("docs")).doesNotContain("docs1", "docs2").hasSize(3);
    }

    @Test
    void fallsBackToRandomSuffixesWhenDigitVariantsAreTaken() {
        for (char d = '0'; d <= '9'; d++) {
            taken.add("myLink" + d);
        }
        List<String> suggestions = suggester(3).suggest("myLink1");
        assertThat(suggestions).hasSize(3).allMatch(s -> s.startsWith("myLin") && s.matches("[A-Za-z0-9]{7}"));
    }

    @Test
    void returnsAtMostTheConfiguredNumber() {
        assertThat(suggester(1).suggest("abc")).hasSize(1);
        assertThat(suggester(0).suggest("abc")).isEmpty();
    }
}
