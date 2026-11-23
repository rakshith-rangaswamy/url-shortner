# ADR-0020: Short URLs belong to one user; reuse is per user

- **Status:** Accepted
- **Date:** 2026-11-23
- **Deciders:** Rakshith
- **Supersedes:** ADR-0008

## Context

ADR-0008 reused one short URL for the same long URL across all users, so the second user to shorten a URL received a link owned by someone else. The owner asked to tie each short URL to its specific userId and to make every request per user.

## Decision

1. **Every short URL belongs to exactly one `userId`.** `userId` is required on `POST /api/v1/urls` (1 to 64 letters, digits, `-`, `_` or `.`, in the JSON body); missing or invalid gets 422. `url_mapping.user_id` is NOT NULL.
2. **Reuse is per user.** The same user shortening the same URL gets their existing unexpired link (200, new TTL ignored, ADR-0019). A different user gets a new short URL of their own (201).
3. **Per-user codes.** Generated codes hash `userId + "|" + longUrl`, so different users start from different candidates. Re-hash, salt and unique-index rules are unchanged (ADR-0006, ADR-0007). A concurrent-insert row is reused only if it has the same user.
4. **Custom aliases stay globally unique.** The alias is the public path, so `/promo` can point to one link only; a taken alias still gives 409 with suggestions, whoever owns it.
5. **Redirects stay public.** `GET /{shortCode}` needs no userId.
6. **Migration.** `V2__short_urls_belong_to_a_user.sql` assigns existing rows without a user to `anonymous`, makes `user_id` NOT NULL, and replaces the single-column indexes with `(user_id, long_url_hash)`.

## Consequences

- More rows when many users shorten the same URL (one per user), in exchange for clear ownership.
- Without authentication (ADR-0012) a caller can still send someone else's userId; adding auth later should derive userId from the credentials instead of the body.
- Old links created before this change keep working, owned by `anonymous`.
