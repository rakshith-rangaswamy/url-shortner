# ADR-0001: Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

Design choices for the URL shortener are being made in conversation and spread across the README, code comments and PR descriptions. Future contributors need one place to see what was decided, why, and what is still open.

## Decision

Keep Architecture Decision Records in `docs/adr/`, one Markdown file per decision, numbered in order, using [`0000-template.md`](0000-template.md). A decision that changes an earlier one gets a new ADR that marks the old one **Superseded by ADR-NNNN**; old ADRs are not rewritten. Decisions not yet confirmed are recorded with status **Proposed** and the default currently in the code.

## Consequences

- Every architecture-relevant PR adds or updates an ADR and the index in [`README.md`](README.md).
- Open questions stay visible until they are decided.
