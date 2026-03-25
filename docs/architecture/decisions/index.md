# Architecture Decision Records

Architecture Decision Records (ADRs) capture the key architectural decisions
made for the Architect Platform, along with their context and consequences.
They serve as a persistent log so that current and future contributors
understand *why* the system is shaped the way it is.

## Format

We use a lightweight ADR template inspired by
[Michael Nygard's original proposal](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions):
**Title → Status → Context → Decision → Consequences**.

## Decision Log

| ID  | Title | Status | Date |
|-----|-------|--------|------|
| [001](001-monorepo-structure.md) | Use a monorepo with independent Gradle builds | Accepted | 2025-01-15 |
| [002](002-api-as-published-contract.md) | Publish only architect-api to GitHub Packages | Accepted | 2025-01-15 |
| [003](003-plugin-loading-via-spi.md) | Use Java SPI for plugin discovery | Accepted | 2025-01-15 |
| [004](004-process-plugin-protocol.md) | Support non-JVM plugins via JSON-RPC subprocess protocol | Accepted | 2025-01-15 |
| [005](005-support-tier-governance.md) | Explicit support tiers for all modules | Accepted | 2025-01-15 |

## Contributing

When proposing a new architectural decision:

1. Copy the template above into a new file named `NNN-short-title.md`.
2. Set the status to **Proposed**.
3. Open a pull request for team review.
4. Once accepted, update the status to **Accepted** and add the entry to the
   table above.

Superseded decisions should have their status changed to **Superseded by
[ADR-NNN](NNN-short-title.md)** rather than being deleted.
