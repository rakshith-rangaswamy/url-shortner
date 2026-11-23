package com.urlshortener.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Rules for generating and resolving short codes. */
public class ShortenerConfig {

    /** Public base used to build the returned short URL, e.g. https://sho.rt */
    @NotBlank
    @JsonProperty
    private String baseUrl = "http://localhost:8080";

    /** OPEN QUESTION: SHA-256 or MD5. */
    @NotBlank
    @JsonProperty
    private String hashAlgorithm = "SHA-256";

    /** Length of a generated Base62 short code. */
    @Min(1)
    @Max(32)
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

    public String getBaseUrl() { return baseUrl; }
    public String getHashAlgorithm() { return hashAlgorithm; }
    public int getCodeLength() { return codeLength; }
    public int getMaxRehashAttempts() { return maxRehashAttempts; }
    public int getAliasSuggestionCount() { return aliasSuggestionCount; }
}
