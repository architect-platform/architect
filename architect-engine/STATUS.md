# architect-engine status

- Status: incubating
- Last reviewed: 2026-03-24
- Owner group: Platform Runtime

Current scope:
- Hosts the long-lived Micronaut execution server and transport adapters.
- Depends on architect-core for shared runtime behavior.

Current limitations:
- Baseline reliability is still being hardened during the active refactor plan.

Graduation criteria:
- Stable module test baseline and architecture-boundary checks remain green.
- Runtime ownership boundaries stay aligned with architect-core.
