package com.urlshortener.resources;

import com.codahale.metrics.annotation.Timed;
import com.urlshortener.api.ErrorResponse;
import com.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** GET /{shortCode}: redirect to the original URL. Hot path with a 200ms budget. */
@Path("/")
@Tag(name = "Redirect")
@Produces(MediaType.APPLICATION_JSON)
public class RedirectResource {

    private final UrlShortenerService service;

    public RedirectResource(UrlShortenerService service) {
        this.service = service;
    }

    @GET
    @Path("{shortCode: [A-Za-z0-9]{1,7}}")
    @Timed(name = "redirect")
    @Operation(summary = "Redirect to the original URL",
            description = "Responds with 302 so every click reaches the service (for click counting and cache warmth).",
            responses = {
                    @ApiResponse(responseCode = "302", description = "Redirect to the original URL",
                            headers = @Header(name = "Location", description = "The original URL")),
                    @ApiResponse(responseCode = "404", description = "Unknown short code",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "410", description = "Short URL has expired",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "501", description = "Not implemented yet (scaffold)",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response redirect(@Parameter(description = "Base62 short code", example = "aB3dE9x")
                             @PathParam("shortCode") String shortCode) {
        return Response.status(Response.Status.FOUND).location(service.resolve(shortCode)).build();
    }
}
