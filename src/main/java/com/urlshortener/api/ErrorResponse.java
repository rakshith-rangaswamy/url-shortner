package com.urlshortener.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Error body. For a taken custom alias (409) it carries up to N suggested alternatives. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "Error details")
public record ErrorResponse(
        @Schema(example = "409") int code,
        @Schema(example = "Custom alias 'myLink1' is already taken") String message,
        @Schema(description = "Suggested free aliases (only for 409)", example = "[\"myLink2\", \"myLnk1a\", \"myLink9\"]")
        List<String> suggestions
) {
    public ErrorResponse(int code, String message) {
        this(code, message, List.of());
    }
}
