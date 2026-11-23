package com.urlshortener.api;

import com.urlshortener.validation.HttpUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/v1/urls. {@code longUrl} and {@code userId} are required. Custom alias: 3-7 Base62 characters (ADR-0009). TTL: whole seconds,
 * 60 seconds to 10 years, omitted for no expiry (ADR-0010).
 */
@Schema(description = "Request to shorten a long URL")
public record ShortenRequest(

        @Schema(description = "The original URL to shorten (http or https only)", example = "https://example.com/some/very/long/path?x=1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 2048)
        @HttpUrl
        String longUrl,

        @Schema(description = "Optional custom short code: 3-7 letters or digits. "
                + "Reserved words such as 'docs' or 'api' are rejected.", example = "myLink1")
        @Pattern(regexp = ShortCodes.ALIAS_PATTERN,
                message = "must be " + ShortCodes.ALIAS_MIN_LENGTH + "-" + ShortCodes.MAX_LENGTH + " letters or digits")
        String customAlias,

        @Schema(description = "Optional lifetime in seconds (60 to 315360000, i.e. 10 years). Omit for no expiry. "
                + "Ignored when the user's existing short URL is reused.", example = "604800", minimum = "60", maximum = "315360000")
        @Min(MIN_TTL_SECONDS)
        @Max(MAX_TTL_SECONDS)
        Long ttlSeconds,

        @Schema(description = "Id of the user who owns the short URL: 1-64 letters, digits, '-', '_' or '.'. "
                + "Each user gets their own short URLs (ADR-0020).", example = "user-42",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Pattern(regexp = USER_ID_PATTERN, message = "must be 1-64 letters, digits, '-', '_' or '.'")
        String userId
) {
    public static final String USER_ID_PATTERN = "[A-Za-z0-9._-]{1,64}";
    public static final long MIN_TTL_SECONDS = 60;
    public static final long MAX_TTL_SECONDS = 10L * 365 * 24 * 60 * 60;
}
