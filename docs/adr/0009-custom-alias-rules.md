# ADR-0009: Custom aliases: Base62 only, reserved words rejected, 409 with suggestions when taken

- **Status:** Accepted
- **Date:** 2026-11-22 (length decided 2026-11-23)
- **Deciders:** Rakshith

## Context

Users may pick their own short code. It must not shadow app routes and must be unique.

## Decision

- Format: letters and digits only (no special characters), 3 to 7 long.
- **Reserved words** (`docs`, `swagger`, `openapi`, `api`, `admin`, `health`, `metrics`, `static`, `assets`, case-insensitive, configurable via `shortener.reservedAliases`) are rejected with **422**, because `/docs` and other paths are served by the app.
- An alias already in use returns **409 Conflict** with up to **3** suggested free aliases (`shortener.aliasSuggestionCount`).

**Length: 3 to 7 characters** (decided 2026-11-23). Exactly 7 would reject natural aliases like `sale`; a minimum of 3 keeps one or two character codes from being taken. Generated codes are always exactly 7. Defined in `ShortCodes.ALIAS_PATTERN`.

Suggestions are, in order: the alias plus a digit, the last character replaced by a digit, then random Base62 suffixes, skipping reserved and taken ones (checked in one `IN (...)` query).

An alias whose link has expired **stays taken** (ADR-0019).

## Consequences

- Suggestions require extra existence checks on the conflict path only.
