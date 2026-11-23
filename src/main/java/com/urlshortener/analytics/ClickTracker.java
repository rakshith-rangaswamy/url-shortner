package com.urlshortener.analytics;

/**
 * Records redirect clicks. 302 redirects are used so every click reaches the service.
 *
 * OPEN QUESTION: whether click counting belongs in this phase. The scaffold wires a no-op implementation.
 */
public interface ClickTracker {

    void recordClick(String shortCode);

    ClickTracker NO_OP = shortCode -> { };
}
