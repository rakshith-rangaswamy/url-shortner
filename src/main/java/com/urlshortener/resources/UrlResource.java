package com.urlshortener.resources;

import com.codahale.metrics.annotation.Timed;
import com.urlshortener.api.ErrorResponse;
import com.urlshortener.api.ShortenRequest;
import com.urlshortener.api.ShortenResponse;
import com.urlshortener.service.ShortenResult;
import com.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

/** POST /api/v1/urls: create a short URL. */
@Path("/api/v1/urls")
@Tag(name = "Short URLs")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UrlResource {

    private final UrlShortenerService service;

    public UrlResource(UrlShortenerService service) {
        this.service = service;
    }

    @POST
    @Timed(name = "shorten")
    @Operation(summary = "Shorten a URL",
            description = "Creates a short URL (201), or returns the existing one (200) if this URL was already "
                    + "shortened and has not expired. Optional custom alias and TTL.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Existing, unexpired short URL reused",
                            content = @Content(schema = @Schema(implementation = ShortenResponse.class))),
                    @ApiResponse(responseCode = "201", description = "New short URL created; Location header points at it",
                            content = @Content(schema = @Schema(implementation = ShortenResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Custom alias already taken; suggestions included",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "422", description = "Invalid request body or reserved alias",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "501", description = "Not implemented yet (scaffold)",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response shorten(@NotNull @Valid ShortenRequest request) {
        ShortenResult result = service.shorten(request);
        ShortenResponse body = result.response();
        return result.created()
                ? Response.created(URI.create(body.shortUrl())).entity(body).build()
                : Response.ok(body).build();
    }
}
