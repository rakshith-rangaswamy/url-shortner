package com.urlshortener.errors;

import com.urlshortener.api.ErrorResponse;
import com.urlshortener.service.AliasTakenException;
import com.urlshortener.service.ReservedAliasException;
import com.urlshortener.service.ShortCodeExhaustedException;
import com.urlshortener.service.ShortUrlExpiredException;
import com.urlshortener.service.ShortUrlNotFoundException;
import io.dropwizard.jersey.validation.ConstraintMessage;
import io.dropwizard.jersey.validation.JerseyViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import java.util.List;

/** Maps service exceptions to the HTTP contract (404, 409, 410, 422, 503). */
public final class ExceptionMappers {

    private ExceptionMappers() {
    }

    public static List<Object> all() {
        return List.of(new Exhausted(), new AliasTaken(), new Expired(), new NotFound(),
                new Reserved(), new Validation());
    }

    private static Response json(int status, ErrorResponse body) {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(body).build();
    }

    public static class Exhausted implements ExceptionMapper<ShortCodeExhaustedException> {
        @Override
        public Response toResponse(ShortCodeExhaustedException e) {
            return json(503, new ErrorResponse(503, e.getMessage()));
        }
    }

    public static class AliasTaken implements ExceptionMapper<AliasTakenException> {
        @Override
        public Response toResponse(AliasTakenException e) {
            return json(409, ErrorResponse.withSuggestions(409, e.getMessage(), e.suggestions()));
        }
    }

    public static class Expired implements ExceptionMapper<ShortUrlExpiredException> {
        @Override
        public Response toResponse(ShortUrlExpiredException e) {
            return json(410, new ErrorResponse(410, e.getMessage()));
        }
    }

    public static class Reserved implements ExceptionMapper<ReservedAliasException> {
        @Override
        public Response toResponse(ReservedAliasException e) {
            return json(422, ErrorResponse.withDetails(422, "Invalid request", List.of("customAlias " + e.getMessage())));
        }
    }

    /** Replaces Dropwizard's default {"errors":[...]} body so 422s use the same ErrorResponse shape. */
    public static class Validation implements ExceptionMapper<JerseyViolationException> {
        @Override
        public Response toResponse(JerseyViolationException e) {
            List<String> details = e.getConstraintViolations().stream()
                    .map(v -> ConstraintMessage.getMessage(v, e.getInvocable()))
                    .sorted()
                    .toList();
            return json(422, ErrorResponse.withDetails(422, "Invalid request", details));
        }
    }

    public static class NotFound implements ExceptionMapper<ShortUrlNotFoundException> {
        @Override
        public Response toResponse(ShortUrlNotFoundException e) {
            return json(404, new ErrorResponse(404, e.getMessage()));
        }
    }
}
