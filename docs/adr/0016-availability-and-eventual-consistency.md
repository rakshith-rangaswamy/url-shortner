# ADR-0016: Stateless instances, health checks and eventual consistency for availability

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

The service should be highly available; eventual consistency is acceptable.

## Decision

Keep app instances stateless so they can be scaled horizontally behind a load balancer. Expose health checks for MySQL (built in) and Redis on the admin port (`/healthcheck`) and timers via Dropwizard Metrics. Accept eventual consistency between cache and database (cache entries expire on their own; concurrent duplicate inserts are tolerated, ADR-0008).

## Consequences

- MySQL is a single point of failure until replication or a managed HA setup is added (future ADR).
- A Redis outage degrades latency, not availability (ADR-0005).
