package com.urlshortener.service;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Words that cannot be used as custom aliases because the app already serves those paths (or may later).
 * Matching is case-insensitive so "Docs" is rejected as well as "docs".
 */
public class AliasPolicy {

    private final Set<String> reserved;

    public AliasPolicy(Set<String> reservedAliases) {
        this.reserved = reservedAliases.stream()
                .map(a -> a.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isReserved(String alias) {
        return alias != null && reserved.contains(alias.toLowerCase(Locale.ROOT));
    }

    /** @throws ReservedAliasException if the alias is reserved */
    public void requireAllowed(String alias) {
        if (isReserved(alias)) {
            throw new ReservedAliasException(alias);
        }
    }
}
