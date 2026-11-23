# ADR-0009: Custom aliases: Base62 only, reserved words rejected, 409 with suggestions when taken

- **Status:** Accepted (length rule Proposed)
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

Users may pick their own short code. It must not shadow app routes and must be unique.

## Decision

- Format: letters and digits only (no special characters).
- **Reserved words** (`docs`, `swagger`, `openapi`, `api`, `admin`, `health`, `metrics`, `static`, `assets`, case-insensitive, configurable via `shortener.reservedAliases`) are rejected with **422**, because `/docs` and other paths are served by the app.
- An alias already in use returns **409 Conflict** with up to **3** suggested free aliases (`shortener.aliasSuggestionCount`).

**Open:** length. The owner said length 7; the code currently accepts **1 to 7** characters until it is confirmed whether exactly 7 is required.

## Consequences

- Suggestions require extra existence checks on the conflict path only.
