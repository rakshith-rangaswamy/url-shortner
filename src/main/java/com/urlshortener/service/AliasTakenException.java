package com.urlshortener.service;

import java.util.List;

/** Custom alias already in use. Mapped to HTTP 409 with suggestions. */
public class AliasTakenException extends RuntimeException {

    private final List<String> suggestions;

    public AliasTakenException(String alias, List<String> suggestions) {
        super("Custom alias '" + alias + "' is already taken");
        this.suggestions = List.copyOf(suggestions);
    }

    public List<String> suggestions() {
        return suggestions;
    }
}
