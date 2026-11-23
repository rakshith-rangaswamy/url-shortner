package com.urlshortener.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Response of POST /api/v1/urls. */
@Schema(description = "A created (or existing, still valid) short URL")
public record ShortenResponse(
        @Schema(example = "aB3dE9x") String shortCode,
        @Schema(example = "http://localhost:8080/aB3dE9x") String shortUrl,
        @Schema(example = "https://example.com/some/very/long/path?x=1") String longUrl,
        @Schema(description = "When the short URL stops redirecting; null means never") Instant expiresAt,
        @Schema(example = "user-42") String userId
) {
}
