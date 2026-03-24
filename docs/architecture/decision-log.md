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

## ADR-006: Shell argument escaping via central ShellUtils

**Status**: Accepted  
**Date**: 2025-03

### Context

Multiple plugins were constructing shell commands by string interpolation, creating shell injection vulnerabilities (CWE-78).

### Decision

Introduce `io.github.architectplatform.api.core.utils.ShellUtils` in `architect-api` with `escapeShellArg()`, `escapeShellArgs()`, and `requireSafeIdentifier()`. All plugins must use these utilities when constructing commands. `requireSafeIdentifier()` validates identifiers (profiles, targets, etc.) against an allowlist pattern.

### Consequences

- **Pro**: Single, auditable escape implementation.
- **Pro**: `architect-api` is the natural shared location — all JVM plugins already depend on it.
- **Con**: Process plugins (TypeScript, Go, Python) must implement equivalent escaping in their respective SDKs.
- Related: task 24.6; see `ShellUtils.kt` and `ShellUtilsTest.kt`.

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
