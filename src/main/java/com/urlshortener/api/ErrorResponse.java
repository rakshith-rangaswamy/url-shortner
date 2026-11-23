package com.urlshortener.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Error body used for every error status. A taken custom alias (409) carries suggestions; a validation
 * failure (422) carries one entry per invalid field in details.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "Error details")
public record ErrorResponse(
        @Schema(example = "409") int code,
        @Schema(example = "Custom alias 'myLink1' is already taken") String message,
        @Schema(description = "Suggested free aliases (only for 409)", example = "[\"myLink2\", \"myLnk1a\", \"myLink9\"]")
        List<String> suggestions,
        @Schema(description = "Per-field validation errors (only for 422)", example = "[\"longUrl must be an absolute http or https URL\"]")
        List<String> details
) {
    public ErrorResponse(int code, String message) {
        this(code, message, List.of(), List.of());
    }

    public static ErrorResponse withSuggestions(int code, String message, List<String> suggestions) {
        return new ErrorResponse(code, message, suggestions, List.of());
    }

    public static ErrorResponse withDetails(int code, String message, List<String> details) {
        return new ErrorResponse(code, message, List.of(), details);
    }
}
