# ADR-002: Publish only architect-api to GitHub Packages

| Field    | Value      |
|----------|------------|
| **Status** | Accepted |
| **Date**   | 2025-01-15 |
| **Authors** | Architect Platform contributors |

## Context

Plugin authors and third-party consumers need a stable, versioned artifact to
compile against. The platform ships several JVM modules (`architect-api`,
`architect-core`, `architect-engine`), but not all of them present a surface
that external code should depend on.

We considered three publication strategies:

1. **Publish all modules** — gives maximum flexibility to consumers but exposes
   internal implementation details and creates a large compatibility surface.
2. **Publish api + core** — lets plugin authors reuse utility code from core,
   but couples them to internal evolution.
3. **Publish api only** — provides a single, narrow contract surface and keeps
   everything else internal.

## Decision

Only `architect-api` is published to GitHub Packages as the Maven artifact
`io.github.architectplatform:api`. All other modules (`architect-core`,
`architect-engine`, etc.) are consumed only within the monorepo or distributed
as self-contained binaries (e.g. the CLI fat-JAR).

Plugin authors depend exclusively on `architect-api` for compilation. The
engine provides the runtime implementation of the API interfaces.

## Consequences

### Positive

- A single published artifact means a single contract surface to version and
  maintain. Breaking-change analysis is scoped to one module.
- `architect-core` and `architect-engine` can undergo aggressive refactoring
  without publishing new major versions or breaking downstream consumers.
- Plugin authors get a clear, minimal dependency: one artifact, one set of
  interfaces.

### Negative

- Plugin authors cannot directly reuse utility classes from `architect-core`
  (e.g. file helpers, logging wrappers). Useful utilities may need to be
  promoted to the API module over time.
- Version management requires discipline: every published API release must be
  backward-compatible within a major version, even if the underlying engine
  changes significantly.
- Consumers wanting deeper integration (e.g. embedding the engine) must build
  from source or request specific artifacts, as no published engine artifact
  exists.
