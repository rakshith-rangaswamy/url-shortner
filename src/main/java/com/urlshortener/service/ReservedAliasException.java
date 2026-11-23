package com.urlshortener.service;

/** Custom alias collides with a reserved path such as "docs". Mapped to HTTP 422. */
public class ReservedAliasException extends RuntimeException {
    public ReservedAliasException(String alias) {
        super("Custom alias '" + alias + "' is reserved");
    }
}
