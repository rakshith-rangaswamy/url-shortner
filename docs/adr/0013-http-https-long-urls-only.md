# ADR-0013: Accept only absolute http and https long URLs

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

The long URL becomes the redirect `Location`. Accepting `javascript:`, `data:` or relative URLs would turn the service into an attack vector.

## Decision

Validate `longUrl` with `@HttpUrl`: it must parse as an absolute URI with scheme `http` or `https` and a host, and be at most 2048 characters. Anything else is rejected with 422.

## Consequences

- Some unusual but valid URLs (other schemes) cannot be shortened.
