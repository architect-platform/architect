# Architecture Decision Log

This document records significant architectural decisions made in the Architect Platform, following the [Architectural Decision Record (ADR)](https://adr.github.io/) format.

---

## ADR-001: Plugin system via Java SPI

**Status**: Accepted  
**Date**: 2024-01  

### Context

Architect needs a plugin model that allows third parties to add task suites without modifying the core engine. The system must support zero-config discovery and work with JVM-based plugins.

### Decision

Use Java's built-in [ServiceLoader](https://docs.oracle.com/en/java/se/17/docs/api/java.base/java/util/ServiceLoader.html) (SPI) mechanism. Plugins declare themselves in `META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin` and are discovered at classloading time.

### Consequences

- **Pro**: No additional dependency for plugin discovery; works out of the box with any JVM language.
- **Pro**: Familiar pattern for Kotlin/Java developers.
- **Con**: Each plugin must package a correctly-formed SPI file; `architect plugin validate` was added to catch missing SPI files early.

---

## ADR-002: Engine as a separate REST server

**Status**: Accepted  
**Date**: 2024-02

### Context

Task execution state, SSE streaming, and project registration benefit from a long-lived process. Embedding execution logic in the CLI would slow startup and complicate multi-project scenarios.

### Decision

Architect Engine is an independent Micronaut HTTP server. The CLI communicates with it over HTTP. An `--embedded` flag runs tasks in-process for CI/CD contexts where no daemon is wanted.

### Consequences

- **Pro**: Hot-reload (`architect engine reload-plugins`) without CLI restart.
- **Pro**: Multiple CLI invocations share one engine instance (efficient caching).
- **Con**: Adds latency for first-run engine startup; mitigated by health-check auto-start in the CLI.

---

## ADR-003: Architect API published independently to GitHub Packages

**Status**: Accepted  
**Date**: 2024-02

### Context

Plugins need to depend on the `architect-api` types (`ArchitectPlugin`, `Task`, `TaskRegistry`, etc.) without depending on the entire engine or CLI.

### Decision

`architect-api` is published to GitHub Packages as a versioned Maven artefact. Plugins depend on this published artefact. Engine and CLI also consume it from the same registry.

### Consequences

- **Pro**: Clean contract boundary — only `architect-api` is part of the public plugin API surface.
- **Pro**: Plugins and the engine can evolve independently as long as the API contract is maintained.
- **Con**: Requires GitHub Packages authentication (`GITHUB_TOKEN`) in all build environments that consume plugins.

---

## ADR-004: Architect Plugin Protocol (APP v1) for non-JVM plugins

**Status**: Accepted  
**Date**: 2024-06

### Context

Not all plugin authors want to write Kotlin/Java. TypeScript, Go, and Python are common in the ecosystem. Forcing a JVM runtime on those teams is a hard requirement.

### Decision

Define a language-agnostic **Architect Plugin Protocol** (APP v1) based on JSON over stdin/stdout. Non-JVM plugins run as child processes. The engine communicates via structured messages (list tasks, execute task, emit events). SDKs are provided for TypeScript, Go, and Python.

### Consequences

- **Pro**: Any language with JSON support can author a plugin.
- **Pro**: Process isolation — a crashing plugin cannot take down the engine.
- **Con**: Higher communication overhead compared to in-process JVM plugins.
- See `docs/plugin-protocol.md` for the full specification.

---

## ADR-005: Convention-over-configuration phase ordering

**Status**: Accepted  
**Date**: 2024-03

### Context

Task ordering is critical for correctness (e.g., `test` after `build`). Requiring explicit `dependsOn` declarations for every task is verbose and error-prone.

### Decision

Adopt a fixed lifecycle of phases (`INIT → LINT → VERIFY → BUILD → RUN/TEST → RELEASE → PUBLISH`). Tasks in an earlier phase implicitly run before tasks in a later phase. Custom `dependsOn` declarations are supported for fine-grained ordering within or across phases.

### Consequences

- **Pro**: Zero configuration needed for the happy path — plugins register to a phase, ordering is automatic.
- **Pro**: Consistent mental model across all projects: the same phase names mean the same thing everywhere.
- **Con**: Unusual workflows (e.g., test before build) require explicit `dependsOn` to override the phase order.

---

## ADR-006: Shell argument escaping via central ShellArgumentSanitizer

**Status**: Accepted  
**Date**: 2025-03

### Context

Multiple plugins were constructing shell commands by string interpolation, creating shell injection vulnerabilities (CWE-78).

### Decision

Introduce `io.github.architectplatform.api.core.utils.ShellArgumentSanitizer` in `architect-api` with `escapeShellArg()`, `escapeShellArgs()`, and `requireSafeIdentifier()`. All plugins must use these utilities when constructing commands. `requireSafeIdentifier()` validates identifiers (profiles, targets, etc.) against an allowlist pattern.

### Consequences

- **Pro**: Single, auditable escape implementation.
- **Pro**: `architect-api` is the natural shared location — all JVM plugins already depend on it.
- **Con**: Process plugins (TypeScript, Go, Python) must implement equivalent escaping in their respective SDKs.
- Related: task 24.6; see `ShellArgumentSanitizer.kt` and `ShellArgumentSanitizerTest.kt`.

---

## ADR-007: Composite GitHub Action replaces `curl | bash` installer

**Status**: Accepted  
**Date**: 2025-03

### Context

CI workflows were installing the CLI via `curl https://... | bash`, which is vulnerable to MITM attacks and downloads unverified code.

### Decision

Replace all `curl | bash` patterns with `.github/actions/setup-architect`, a composite GitHub Action that: downloads the CLI/Engine from pinned GitHub Releases, verifies SHA256 checksums, and fails hard on mismatch.

### Consequences

- **Pro**: Eliminates remote code execution risk in CI.
- **Pro**: Checksum verification is auditable in the action source.
- **Con**: Requires a `.sha256` file to be published alongside each release asset.
- Related: task 24.8; 13 workflow files updated.

---

## ADR-008: MkDocs with monorepo plugin for multi-component docs

**Status**: Accepted  
**Date**: 2024-04

### Context

Architect has multiple components (API, Engine, CLI, Plugins, SDKs) each with their own `docs/` directory. A unified documentation site needs to pull from all of them.

### Decision

Use MkDocs with `mkdocs-monorepo-plugin`. Each component has an `mkdocs.yml` with a `docs_dir`. The root `mkdocs.yml` uses `!include` to aggregate them. The `docs-architected` plugin auto-discovers component `docs/` directories.

### Consequences

- **Pro**: Each component team owns its documentation independently.
- **Pro**: Single consistent rendered site with shared themes/search.
- **Con**: MkDocs monorepo plugin must be pinned to a compatible version; breaking updates must be coordinated.

---

## ADR-009: Root execution stays per-module until a real orchestrator exists

**Status**: Accepted  
**Date**: 2026-03

### Context

The repository presents itself as a platform monorepo, but the root does not
ship a Gradle wrapper, a multi-project settings file, or a root Architect task
definition that can reliably build and test all modules. The root
`architect.yml` currently configures documentation, git, and GitHub automation,
not repository-wide build/test orchestration. Leaving this ambiguous causes
contributors to infer a top-level build entry point that does not exist.

### Decision

For the current repository state, the supported execution model is explicit
per-module commands only. Contributors must build and test from each module's
own directory. The root `architect.yml` is not a general-purpose monorepo build
runner.

If the repository later gains a real root orchestrator, that change must be an
intentional Phase 2+ investment with dedicated implementation and documentation,
not an implied convention.

### Consequences

- **Pro**: Matches the repository's actual capabilities today.
- **Pro**: Removes ambiguity between root metadata/configuration and real build
	entry points.
- **Con**: Cross-repository validation remains fragmented until a future
	orchestrator or monorepo task runner is deliberately introduced.

---

## ADR-010: Repository taxonomy centers on platform, products, plugins, SDKs, incubating work, and docs/policy

**Status**: Accepted  
**Date**: 2026-03

### Context

The repository currently mixes mature runtime modules, user-facing products,
official plugins, SDKs, and lower-maturity surfaces at the same visual level.
That makes the top-level layout harder to scan and leaves contributors without
a stable rule for where new code should live.

### Decision

The target repository taxonomy is:

- **Platform/runtime**: core platform modules and execution stack
- **Products**: end-user applications and IDE/editor integrations
- **Official plugins**: first-party extensibility surfaces
- **SDKs**: language SDKs for third-party plugin development
- **Incubating/experimental**: incomplete or exploratory modules that should not
	appear production-adjacent
- **Docs and policy**: repository-wide documentation, governance, and delivery
	metadata

This taxonomy is authoritative even before all physical directory moves happen.
When future restructuring occurs, directories should be grouped to make this
taxonomy visible in the tree instead of leaving it implicit.

### Consequences

- **Pro**: Gives contributors a predictable placement rule for new modules.
- **Pro**: Separates maturity/status concerns from functional ownership.
- **Con**: Later physical path moves will need careful coordination across CI,
	docs, and build tooling.

---

## ADR-011: Existing top-level modules will converge under grouped parents rather than remain flat

**Status**: Accepted  
**Date**: 2026-03

### Context

Now that the target taxonomy is defined, the repository still needs an explicit
placement decision for today's top-level directories. Without that, future path
moves remain subjective and contributors cannot tell whether the current flat
layout is intentional or transitional.

### Decision

The following top-level areas should eventually move under grouped parents:

- `architect-api`, `architect-core`, `architect-engine`, `architect-cli` ->
	`platform/`
- `architect-cloud`, `architect-vscode`, `architect-intellij` -> `products/`

The following top-level areas should remain top-level category roots:

- `plugins/`
- `sdk/`
- `docs/`
- `.github/`
- `homebrew/`

Root governance files such as `README.md`, `CONTRIBUTING.md`, `SECURITY.md`,
`CODE_OF_CONDUCT.md`, `LICENSE`, `PLAN.md`, `STATUS.md`, `mkdocs.yml`, and
`architect.yml` remain at the repository root.

No physical path moves are implied by this decision alone; it defines the
intended destination layout for later phases.

### Consequences

- **Pro**: Makes future reorganization work incremental instead of ad hoc.
- **Pro**: Preserves existing category roots that are already clear (`plugins`,
	`sdk`, `docs`).
- **Con**: Documentation and tooling will temporarily describe both current and
	target layouts until migration actually happens.

---

## ADR-012: Ownership metadata is defined by bounded-area stewardship groups

**Status**: Accepted  
**Date**: 2026-03

### Context

The repository has no `CODEOWNERS` file and no explicit ownership registry for
major bounded areas. That leaves architecture, review, and maintenance
responsibility implicit even after the target taxonomy and placement rules are
defined.

### Decision

Define ownership metadata in `docs/architecture/ownership-map.md` using a small
set of stewardship groups:

- Platform Runtime
- Product Surfaces
- Plugin Ecosystem
- SDK Ecosystem
- Docs and Governance

Each bounded area maps to exactly one primary stewardship group, even if work
often requires cross-group review.

### Consequences

- **Pro**: Gives the repository an explicit ownership model before any GitHub
	team automation exists.
- **Pro**: Aligns ownership with architectural boundaries instead of ad hoc file
	paths.
- **Con**: Still requires a later decision if the project wants enforceable
	CODEOWNERS rules or named team assignments.

---

## ADR-013: Architect-core is the canonical home for shared runtime behavior

**Status**: Accepted  
**Date**: 2026-03

### Context

`architect-core` and `architect-engine` currently contain a large same-path
duplication surface across runtime packages such as plugin loading, project
loading, task execution, secret resolution, workflow plugins, and event DTOs.
An inventory run in Phase 3 found 78 duplicated Kotlin source files under
`src/main/kotlin`, with 25 of them already drifted instead of remaining exact
copies.

This duplication is especially risky because the same responsibilities are split
between the embedded runtime path and the Micronaut engine host. Bug fixes can
land in one module without landing in the other, and contributors cannot tell
which implementation is authoritative.

### Decision

`architect-core` is the canonical home for shared runtime behavior.

That includes:

- plugin loading and plugin source resolution
- secret resolution and environment/config expansion helpers
- project loading, validation, and repository abstractions
- task execution internals, dependency resolution, caches, and domain events
- built-in workflow/installers/inline plugin implementations used by both
	engine-hosted and embedded execution paths
- shared runtime configuration constants and execution utilities

`architect-engine` remains the canonical home for engine-host concerns only:

- Micronaut application bootstrapping and bean wiring
- HTTP controllers, transport DTOs, SSE/event streaming adapters, and request
	validation at the transport boundary
- orchestration services that are meaningful only in the long-lived server host
- startup profiling, cloud reporting, and other engine-only operational code

When a runtime concern needs Micronaut integration, the underlying logic should
live in `architect-core` and the engine should provide only the thinnest host
adapter required for dependency injection, configuration binding, or HTTP
exposure.

### Consequences

- **Pro**: Gives every duplicated runtime concern a single target owner.
- **Pro**: Keeps embedded CLI execution and server execution on the same core
	implementation path.
- **Pro**: Makes future architecture rules straightforward: engine depends on
	core, but core must not depend on engine-host transport code.
- **Con**: Some engine classes will need to be split into core logic plus
	Micronaut adapters before duplicate files can be removed safely.
