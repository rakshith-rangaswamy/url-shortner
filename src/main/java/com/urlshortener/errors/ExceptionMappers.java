package com.urlshortener.errors;

import com.urlshortener.api.ErrorResponse;
import com.urlshortener.service.AliasTakenException;
import com.urlshortener.service.NotImplementedYetException;
import com.urlshortener.service.ShortUrlExpiredException;
import com.urlshortener.service.ShortUrlNotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import java.util.List;

/** Maps service exceptions to the HTTP contract (404, 409, 410, 501). */
public final class ExceptionMappers {

    private ExceptionMappers() {
    }

    public static List<Object> all() {
        return List.of(new NotImplemented(), new AliasTaken(), new Expired(), new NotFound());
    }

    private static Response json(int status, ErrorResponse body) {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(body).build();
    }

    public static class NotImplemented implements ExceptionMapper<NotImplementedYetException> {
        @Override
        public Response toResponse(NotImplementedYetException e) {
            return json(501, new ErrorResponse(501, e.getMessage()));
        }
    }

    public static class AliasTaken implements ExceptionMapper<AliasTakenException> {
        @Override
        public Response toResponse(AliasTakenException e) {
            return json(409, new ErrorResponse(409, e.getMessage(), e.suggestions()));
        }
    }

    public static class Expired implements ExceptionMapper<ShortUrlExpiredException> {
        @Override
        public Response toResponse(ShortUrlExpiredException e) {
            return json(410, new ErrorResponse(410, e.getMessage()));
        }
    }

    public static class NotFound implements ExceptionMapper<ShortUrlNotFoundException> {
        @Override
        public Response toResponse(ShortUrlNotFoundException e) {
            return json(404, new ErrorResponse(404, e.getMessage()));
        }
    }
}
