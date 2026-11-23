package com.urlshortener.service;

import java.util.List;

/** Proposes free alternatives when a custom alias is already taken (returned with HTTP 409). */
public class AliasSuggester {

    private final int maxSuggestions;

    public AliasSuggester(int maxSuggestions) {
        this.maxSuggestions = maxSuggestions;
    }

    /** Up to {@code maxSuggestions} unused aliases close to {@code takenAlias}. */
    public List<String> suggest(String takenAlias) {
        throw new NotImplementedYetException("AliasSuggester.suggest");
    }

    public int maxSuggestions() {
        return maxSuggestions;
    }
}
