# ADR-005: Explicit support tiers for all modules

| Field    | Value      |
|----------|------------|
| **Status** | Accepted |
| **Date**   | 2025-01-15 |
| **Authors** | Architect Platform contributors |

## Context

The Architect repository contains modules at vastly different maturity levels.
Some (like `architect-api`) are stable and depended upon by external consumers,
while others (like IDE extensions) are early experiments with no tests or docs.

Without explicit maturity labeling, users and contributors cannot easily
distinguish production-ready modules from experimental ones. This leads to:

- Users relying on unstable modules without understanding the risk.
- Contributors unsure where to focus effort or what quality bar to meet.
- Maintainers unable to communicate expectations around stability and support.

We evaluated two approaches:

1. **Implicit maturity** — rely on README badges, commit frequency, and test
   coverage to signal maturity. Low overhead but inconsistent and easily missed.
2. **Explicit support tiers** — define a fixed set of maturity tiers with
   documented criteria and require every module to declare its tier.

## Decision

We define **five support tiers**, documented in `STATUS.md` at the repository
root:

| Tier          | Meaning                                         |
|---------------|-------------------------------------------------|
| **Active**    | Production-ready, tested, documented, supported |
| **Beta**      | Feature-complete, under stabilization           |
| **Incubating**| Under active development, APIs may change       |
| **Placeholder**| Scaffolded but not yet functional              |
| **Deprecated**| Scheduled for removal, do not use               |

Every module and plugin must declare its tier in `STATUS.md`. Promotion from
one tier to the next requires meeting quality gates defined in the module
matrix (`docs/architecture/module-matrix.md`) and plugin matrix
(`docs/architecture/plugin-matrix.md`).

## Consequences

### Positive

- Users get clear, upfront expectations about what they can rely on. A module
  marked "Incubating" sets the right expectation for breaking changes.
- Contributors know the quality bar for each module and can prioritize work
  toward promotion criteria.
- Governance is transparent: promotion criteria are public and reviewable,
  reducing subjective decisions about module readiness.

### Negative

- Maintaining accurate tiers requires ongoing governance. Tiers that drift
  from reality (e.g. a broken module still marked "Active") erode trust.
- The tier system adds process overhead: every new module must be classified,
  and promotions require explicit review against the quality gates.
- Edge cases will arise where a module partially meets a tier's criteria,
  requiring judgment calls and potentially contentious discussions.
