package com.urlshortener.service;

import com.urlshortener.api.ShortenResponse;

/**
 * Outcome of a shorten call.
 *
 * @param response the short URL
 * @param created  true when a new mapping was stored (HTTP 201), false when an existing unexpired one was reused (HTTP 200)
 */
public record ShortenResult(ShortenResponse response, boolean created) {
}
