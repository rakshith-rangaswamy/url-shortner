# ADR-0003: Java 21 and Maven

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

A language level and build tool had to be fixed before scaffolding.

## Decision

Use **Java 21** (records, text blocks) and **Maven**. A fat jar is built with the shade plugin (`java -jar ... server config.yml`). Unit tests (`*Test`) run with Surefire in `mvn test`; end-to-end tests (`*IT`) run with Failsafe in `mvn verify`. Dependency versions are declared once in `pom.xml`; the Swagger UI version reaches the code through a filtered `build-info.properties`.

## Consequences

- Java 21 is required to build and run.
- `mvn verify` needs Docker (see ADR-0015).
