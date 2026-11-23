# ADR-0014: One JSON error shape for every error status

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

Dropwizard's default validation error body (`{"errors":[...]}`) differs from the app's own errors, which makes clients handle two formats.

## Decision

All errors return `ErrorResponse`: `code`, `message`, plus `suggestions` for 409 and `details` (one entry per invalid field) for 422. Exception mappers in `errors/ExceptionMappers` map domain exceptions to 404, 409, 410, 422 and 503, and replace the default validation mapper.

## Consequences

- Clients parse one shape.
- New error cases add a mapper, not a new format.
