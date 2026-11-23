package com.urlshortener.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/v1/urls.
 *
 * OPEN QUESTIONS: exact custom alias length rule (exactly 7 or up to 7) and TTL format
 * (seconds vs ISO-8601 duration). The scaffold uses "up to 7 Base62 chars" and "TTL in seconds".
 */
@Schema(description = "Request to shorten a long URL")
public record ShortenRequest(

        @Schema(description = "The original URL to shorten", example = "https://example.com/some/very/long/path?x=1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 2048)
        String longUrl,

        @Schema(description = "Optional custom short code (Base62, no special characters)", example = "myLink1")
        @Pattern(regexp = "^[A-Za-z0-9]{1,7}$", message = "must be 1-7 letters or digits")
        String customAlias,

        @Schema(description = "Optional lifetime of the short URL in seconds. Omit for no expiry.", example = "604800")
        @Positive
        Long ttlSeconds,

        @Schema(description = "Optional id of the user who owns the short URL", example = "user-42")
        @Size(max = 64)
        String userId
) {
}
