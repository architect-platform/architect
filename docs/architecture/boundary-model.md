# Boundary Model

## 1. Overview

Architect is a monorepo housing the entire platform: runtime, plugins, SDKs,
product surfaces, and governance tooling. The repository is organized into
**five bounded contexts** with explicit dependency rules that prevent
architectural erosion over time.

Every module falls into exactly one bounded context. Dependencies flow
**downward** — from product surfaces and plugins toward the core platform —
and lateral or upward dependencies are forbidden. The published contract
surface (`architect-api`) sits at the root of the dependency graph and is the
only artifact pushed to GitHub Packages.

This document is the authoritative reference for where each module lives, what
it may depend on, and what patterns are explicitly prohibited.

---

## 2. Bounded Contexts

### 2.1 Core Platform

The core platform provides the task-execution runtime that everything else
builds on.

| Module | Role |
|---------------------|--------------------------------------------------------------|
| `architect-api` | Published contracts: interfaces, annotations, data models |
| `architect-core` | Shared runtime logic: task graph, resolution, lifecycle |
| `architect-engine` | HTTP adaptation layer (Micronaut-based server) |
| `architect-cli` | User-facing binary; entrypoint for local developer usage |

**Dependency chain:**

```
architect-api → architect-core → architect-engine
                               → architect-cli
```

**Rules:**

- `architect-api` is the **only** published artifact. All other modules
  consume it but are never published to GitHub Packages.
- `architect-core` owns shared runtime logic (task graph construction,
  plugin resolution, lifecycle management). It depends solely on
  `architect-api`.
- `architect-engine` adapts core for HTTP / Micronaut hosting. It adds
  controllers, serialization, and server lifecycle on top of core.
- `architect-cli` provides the local user interface. It can invoke
  `architect-engine` over HTTP **or** embed `architect-core` directly for
  offline / single-process mode.

---

### 2.2 Plugin Ecosystem

Sixteen first-party plugins live under `plugins/`. Each plugin provides
task implementations for a specific technology domain (Git, Docker, Gradle,
Terraform, etc.).

**Contract:**

Every plugin implements `ArchitectPlugin<Context>` from `architect-api`.
Plugins declare their capabilities via the standard SPI mechanism
(`META-INF/services/`).

**Loading:**

At runtime the engine discovers plugin JARs — either bundled locally or
fetched from GitHub Releases — and loads them through a shared
`URLClassLoader`.

**Rules:**

- Plugins depend **only** on `architect-api`. They must never import
  classes from `architect-core`, `architect-engine`, or `architect-cli`.
- Plugin resolution and class-loading are handled entirely by `architect-core`;
  the plugin itself is unaware of the hosting environment.

**Known constraint:**

All first-party plugins share a single `URLClassLoader`. There is no
per-plugin isolation today, which means classpath conflicts between plugins
are possible and must be managed through dependency alignment in each plugin's
build file.

---

### 2.3 SDK Ecosystem

Language-specific SDKs allow plugin authors to write plugins outside the JVM.

| Module | Language |
|---------------------|----------|
| `sdk/typescript` | TypeScript / Node.js |
| `sdk/python` | Python |
| `sdk/go` | Go |

**Protocol:**

All SDKs implement **Architect Plugin Protocol v1**, a JSON-RPC 2.0 protocol
transported over `stdin` / `stdout`. The engine launches SDK-based plugins as
subprocesses and communicates through this channel.

**Rules:**

- SDKs have **no JVM dependency**. They are standalone packages published
  to their respective ecosystem registries (npm, PyPI, Go modules).
- Process plugins are first-class citizens; the engine treats them
  identically to JVM plugins once the protocol handshake completes.
- SDK packages depend on the protocol specification only — never on
  `architect-api` or any other JVM module.

---

### 2.4 Product Surfaces

Product surfaces are the applications that end users interact with directly.

| Module | Description |
|--------------------------|-----------------------------------------------|
| `architect-cloud` | Backend, UI, and AI agents (SaaS product) |
| `architect-vscode` | VS Code extension |
| `architect-intellij` | IntelliJ IDEA plugin |

**Rules:**

- Each product surface is a **separate deployment unit** with its own build,
  release, and versioning lifecycle.
- `architect-cloud` communicates with the engine exclusively via HTTP. It
  must not import `architect-core` or any other JVM runtime module directly.
- IDE extensions (`architect-vscode`, `architect-intellij`) wrap the
  `architect-cli` binary. They depend on the CLI at runtime — not at
  compile time — and invoke it as a subprocess.

---

### 2.5 Delivery & Governance

Supporting modules that handle documentation, distribution, and convention
enforcement.

| Module | Purpose |
|-----------------|-------------------------------------------------------|
| `docs/` | MkDocs-based documentation site |
| `homebrew/` | Homebrew formula for CLI distribution |
| `scripts/` | Convention checks and repository maintenance scripts |
| `.github/` | GitHub Actions CI/CD workflows |

These modules have no runtime dependencies on any other bounded context.
They consume build outputs (binaries, JARs, container images) but never
import source code from other modules.

---

## 3. Dependency Graph

```
architect-api (published contract)
    │
    ▼
architect-core (shared runtime)
    ├──► architect-engine (HTTP adaptation)
    ├──► architect-cli (user interface)
    └──► [plugins/*] (runtime loading, api-only dependency)

architect-cloud/backend (isolated; HTTP to engine)
architect-vscode / architect-intellij (CLI wrappers)
sdk/* (process protocol; no JVM dependency)
```

Key observations:

1. `architect-api` is the single root of the JVM dependency tree.
2. Plugins attach at runtime, not at compile time — their only static
   dependency is `architect-api`.
3. Product surfaces (cloud, IDE extensions) are fully decoupled from the
   core platform and communicate through stable interfaces (HTTP, CLI).
4. SDKs are entirely outside the JVM dependency graph.

---

## 4. Cross-Boundary Rules

These rules are **invariants** — they must hold for every commit on `main`.

| # | Rule |
|---|----------------------------------------------------------------------|
| 1 | `architect-api` is the **only** module published to GitHub Packages. |
| 2 | All JVM modules MAY depend on `architect-api`. |
| 3 | Only `core`, `engine`, and `cli` may depend on `architect-core`. |
| 4 | Plugins **MUST NOT** depend on `core`, `engine`, or `cli`. |
| 5 | `architect-cloud` is independent; it communicates via HTTP only. |
| 6 | IDE extensions depend on the CLI binary at runtime, not at compile time. |
| 7 | SDK packages depend on the protocol specification only. |

Enforcement is automated in CI through dependency checks in `scripts/` and
Gradle build scans.

---

## 5. Forbidden Patterns

The following patterns are explicitly prohibited. Any PR introducing one of
these must be rejected.

| Pattern | Why it is forbidden |
|------------------------------------------------------|----------------------------------------------------------|
| Plugin importing `architect-core`, `engine`, or `cli` | Breaks plugin portability; couples plugins to internals |
| Circular dependency between any two modules | Makes independent builds and releases impossible |
| `architect-cloud` importing `architect-core` directly | Violates service boundary; cloud must use HTTP |
| `architect-engine` importing `architect-cli` | Inverts the dependency direction (except test scope) |
| Any module other than `api` publishing to GitHub Packages | Prevents uncontrolled public API surface growth |
| SDK packages importing JVM artifacts | Breaks language independence of process plugins |
| Plugin depending on another plugin at compile time | Plugins must be independently deployable |

---

## 6. Stewardship Groups

Every module is assigned to exactly one stewardship group. The group is
responsible for architectural coherence, code review, and release readiness
of its modules.

| Group | Modules | Responsibility |
|----------------------|-----------------------------------|-----------------------------------------------|
| **Platform Runtime** | `api`, `core`, `engine`, `cli` | Task execution lifecycle and runtime APIs |
| **Plugin Ecosystem** | `plugins/*` | Task implementations per technology domain |
| **SDK Ecosystem** | `sdk/*` | Process plugin protocol and language bindings |
| **Product Surfaces** | `cloud`, `vscode`, `intellij` | End-user applications and integrations |
| **Docs & Governance**| `docs`, `homebrew`, `scripts`, `.github` | Documentation, distribution, CI/CD |

When a change spans multiple stewardship groups, reviewers from **all**
affected groups must approve the PR before merge.

---

## 7. Adding New Modules

Any new top-level module introduced into the repository must satisfy the
following checklist before it can be merged to `main`.

### Required declarations

1. **Stewardship group ownership** — Identify which group (from §6) will
   own the module. If none fits, propose a new group in the PR description.

2. **Support tier** — Declare one of the following tiers:

   | Tier | Meaning |
   |--------------|------------------------------------------------------|
   | **Active** | Production-ready, fully supported, SLA-backed |
   | **Beta** | Feature-complete but not yet production-hardened |
   | **Incubating** | Experimental; API may change without notice |
   | **Deprecated** | Scheduled for removal; no new features accepted |

3. **Explicit dependency list** — Enumerate every module the new module
   depends on. The list must conform to the cross-boundary rules in §4.

4. **Import deny-list** — State which modules the new module **cannot**
   import. At minimum this must include every module outside its bounded
   context (§2) that is not `architect-api`.

### Process

- Open a PR with the new module and the completed checklist above.
- Add the module to the [Repository Status Matrix](status-matrix.md) and
  the [Ownership Map](ownership-map.md).
- Update this document's §2 and §6 tables to reflect the new module.
- Obtain approval from the owning stewardship group **and** from the
  Platform Runtime group (to validate dependency rules).
