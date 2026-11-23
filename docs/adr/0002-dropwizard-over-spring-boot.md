# ADR-0002: Use Dropwizard instead of Spring Boot

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

The service was first planned on Spring Boot 3.3. The owner then asked for the code to be Dropwizard friendly, and decided to switch to Dropwizard completely.

## Decision

Build the service on **Dropwizard 5** (Jetty, Jersey/JAX-RS, Jackson, Dropwizard Metrics, Hibernate Validator). Wiring is explicit in `UrlShortenerApplication.run()`; there is no dependency-injection container. Configuration is a typed `UrlShortenerConfiguration` bound from `config.yml`, with `${ENV:-default}` substitution.

## Consequences

- Small, explicit runtime with health checks, metrics and an admin port built in.
- No Spring ecosystem (Spring Data, Spring Cache); persistence and caching are wired by hand (see ADR-0004, ADR-0005).
- Business code (`service/`, `database/`, `cache/`) stays framework-light and easy to test with plain fakes.
