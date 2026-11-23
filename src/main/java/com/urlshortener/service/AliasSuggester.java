package com.urlshortener.service;

import com.urlshortener.api.ShortCodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

/**
 * Proposes free alternatives when a custom alias is already taken (returned with HTTP 409). Candidates, in order:
 * <ol>
 *   <li>the alias plus a digit ({@code myLink} to {@code myLink1}), when it is shorter than the maximum</li>
 *   <li>the last character replaced by a digit ({@code myLink1} to {@code myLink2})</li>
 *   <li>random Base62 suffixes</li>
 * </ol>
 * Reserved words are skipped, and all candidates are checked against the database in one query.
 */
public class AliasSuggester {

    private static final int RANDOM_CANDIDATES = 20;
    private static final int RANDOM_SUFFIX_LENGTH = 2;

    private final int maxSuggestions;
    private final AliasPolicy policy;
    private final Function<Collection<String>, Set<String>> takenCodes;
    private final Random random;

    /**
     * @param takenCodes returns the subset of the given codes that already exist
     */
    public AliasSuggester(int maxSuggestions, AliasPolicy policy,
                          Function<Collection<String>, Set<String>> takenCodes, Random random) {
        this.maxSuggestions = maxSuggestions;
        this.policy = policy;
        this.takenCodes = takenCodes;
        this.random = random;
    }

    /** Up to {@code maxSuggestions} unused aliases close to {@code takenAlias}. */
    public List<String> suggest(String takenAlias) {
        if (maxSuggestions <= 0) {
            return List.of();
        }
        List<String> candidates = candidates(takenAlias);
        if (candidates.isEmpty()) {
            return List.of();
        }
        Set<String> taken = takenCodes.apply(candidates);
        return candidates.stream()
                .filter(c -> !taken.contains(c))
                .limit(maxSuggestions)
                .toList();
    }

    List<String> candidates(String alias) {
        Set<String> out = new LinkedHashSet<>();
        if (alias.length() < ShortCodes.MAX_LENGTH) {
            for (char d = '1'; d <= '9'; d++) {
                out.add(alias + d);
            }
        }
        String stem = alias.substring(0, alias.length() - 1);
        for (char d = '0'; d <= '9'; d++) {
            out.add(stem + d);
        }
        String base = alias.substring(0, Math.min(alias.length(), ShortCodes.MAX_LENGTH - RANDOM_SUFFIX_LENGTH));
        for (int i = 0; i < RANDOM_CANDIDATES; i++) {
            StringBuilder sb = new StringBuilder(base);
            for (int j = 0; j < RANDOM_SUFFIX_LENGTH; j++) {
                sb.append(Base62.ALPHABET.charAt(random.nextInt(Base62.ALPHABET.length())));
            }
            out.add(sb.toString());
        }
        List<String> valid = new ArrayList<>();
        for (String c : out) {
            if (!c.equals(alias) && c.matches(ShortCodes.ALIAS_PATTERN) && !policy.isReserved(c)) {
                valid.add(c);
            }
        }
        return valid;
    }
}
