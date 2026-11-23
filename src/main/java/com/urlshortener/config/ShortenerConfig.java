package com.urlshortener.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import com.urlshortener.api.ShortCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

/** Rules for generating and resolving short codes. */
public class ShortenerConfig {

    /** Public base used to build the returned short URL, e.g. https://sho.rt */
    @NotBlank
    @JsonProperty
    private String baseUrl = "http://localhost:8080";

    /** SHA-256 (default, ADR-0006) or MD5. */
    @NotBlank
    @Pattern(regexp = "SHA-256|MD5", message = "must be SHA-256 or MD5")
    @JsonProperty
    private String hashAlgorithm = "SHA-256";

    /** Length of a generated Base62 short code. Capped by the DB column and route, see {@link ShortCodes}. */
    @Min(1)
    @Max(ShortCodes.MAX_LENGTH)
    @JsonProperty
    private int codeLength = 7;

    /** Re-hash attempts on collision before falling back to a salt/counter. */
    @Min(0)
    @JsonProperty
    private int maxRehashAttempts = 5;

    /** How many alternatives to suggest when a custom alias is taken. */
    @Min(0)
    @JsonProperty
    private int aliasSuggestionCount = 3;

    /** Custom aliases that would shadow app paths (case-insensitive). */
    @NotNull
    @JsonProperty
    private Set<String> reservedAliases = Set.of(
            "docs", "swagger", "openapi", "api", "admin", "health", "metrics", "static", "assets");

    public String getBaseUrl() { return baseUrl; }
    public String getHashAlgorithm() { return hashAlgorithm; }
    public int getCodeLength() { return codeLength; }
    public int getMaxRehashAttempts() { return maxRehashAttempts; }
    public int getAliasSuggestionCount() { return aliasSuggestionCount; }
    public Set<String> getReservedAliases() { return reservedAliases; }
}
