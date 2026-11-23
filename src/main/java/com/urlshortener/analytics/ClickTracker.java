package com.urlshortener.analytics;

/**
 * Records redirect clicks. 302 redirects are used so every click reaches the service.
 *
 * Click counting is out of scope for now (ADR-0018), so the app wires {@link #NO_OP}. A real tracker can be
 * plugged in without changing the redirect code.
 */
public interface ClickTracker {

    void recordClick(String shortCode);

    ClickTracker NO_OP = shortCode -> { };
}
