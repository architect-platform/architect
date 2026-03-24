# Repository Decomposition Map

> Maintained as part of Phase 0 baseline | Last updated: 2026-03-24

This document explains what every top-level directory in the repository is for,
why it exists, and how it relates to the other areas. Use it to navigate the
codebase without chasing dead ends.

---

## Conceptual Taxonomy

The repository is a platform monorepo organised around five concerns:

```
architect/
├── Core Platform        architect-api/, architect-core/, architect-engine/, architect-cli/
├── Secondary Products   architect-cloud/, architect-vscode/, architect-intellij/
├── Official Plugins     plugins/
├── SDKs                 sdk/
└── Delivery / Docs      docs/, homebrew/, .github/
```

This reflects the current repository shape. The desired target taxonomy for the
repository is slightly more explicit and is the model future restructuring work
should converge toward.

## Target Taxonomy

```text
architect/
├── platform/            api, core, engine, cli
├── products/            cloud, vscode, intellij
├── plugins/             first-party plugins
├── sdk/                 language SDKs
├── incubating/          incomplete or experimental modules
└── docs-policy/         documentation, governance, delivery metadata
```

Target category rules:

| Category | What belongs there | Current repository examples |
|---|---|---|
| `platform/` | Runtime stack, shared contracts, execution host/client | `architect-api`, `architect-core`, `architect-engine`, `architect-cli` |
| `products/` | End-user applications and editor/IDE integrations | `architect-cloud`, `architect-vscode`, `architect-intellij` |
| `plugins/` | Official first-party plugins | `plugins/*` |
| `sdk/` | Language SDKs for third-party/plugin authors | `sdk/typescript`, `sdk/python`, `sdk/go` |
| `incubating/` | Exploratory or incomplete modules that should not look production-grade | future home for low-maturity modules that should move out of the main product/platform paths |
| `docs-policy/` | Repository-wide docs, contribution policy, release/delivery metadata | `docs/`, `.github/`, `README.md`, `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`, `LICENSE`, `mkdocs.yml`, `homebrew/` |

The taxonomy decision does **not** require immediate directory moves. It first
establishes a stable classification model so later path changes can be
intentional instead of ad hoc.

## Current-to-Target Placement Decisions

| Current top-level area | Decision | Target grouping |
|---|---|---|
| `architect-api/` | move under grouped parent | `platform/api/` |
| `architect-core/` | move under grouped parent | `platform/core/` |
| `architect-engine/` | move under grouped parent | `platform/engine/` |
| `architect-cli/` | move under grouped parent | `platform/cli/` |
| `architect-cloud/` | move under grouped parent | `products/cloud/` |
| `architect-vscode/` | move under grouped parent | `products/vscode/` |
| `architect-intellij/` | move under grouped parent | `products/intellij/` |
| `plugins/` | stay top-level | category root |
| `sdk/` | stay top-level | category root |
| `docs/` | stay top-level | category root |
| `.github/` | stay top-level | repository governance/delivery root |
| `homebrew/` | stay top-level | repository delivery root |

Root files remain at the repository root unless there is a separate governance
decision to group policy/configuration files more aggressively.

---

## Core Platform

These four modules form the primary execution stack. Every other module either
depends on them, integrates with them, or extends them.

Execution flow for most user-facing operations is:

```text
CLI -> Engine -> Core runtime -> API contracts -> Plugins
```

There is also an embedded path for selected CLI operations, where the CLI uses
core runtime components in-process instead of calling a long-lived engine.
Contributors debugging task execution should therefore expect both remote and
embedded execution paths to exist in the codebase.

### `architect-api/`

**Role**: Contracts library — the only module that all plugins and the engine
must agree on.

| Sub-path | Purpose |
|---|---|
| `api/` | Kotlin library: `Task`, `Phase`, `ArchitectPlugin`, `TaskRegistry`, `Environment`, `ProjectContext`, `ArchitectPluginTestKit`, contract test suite |
| `docs/` | Component-level MkDocs docs |

Published to GitHub Packages. Plugins and core depend on this artifact.
Version: **2.1.0** (active).

---

### `architect-core/`

**Role**: Shared runtime — plugin loading, execution utilities, secret
resolution, file watching, project caching.

Canonical ownership note: when a runtime concern is needed by both embedded and
server execution paths, `architect-core` is the authoritative implementation.
`architect-engine` should depend on these implementations rather than carrying a
parallel copy.

| Sub-path | Purpose |
|---|---|
| `core/` | Kotlin library: `ProjectService`, `ProjectPluginLoader`, `TaskDependencyResolver`, `SecretResolver`, `ClassLoaderResourceExtractor`, `LocalPluginSource`, JMH benchmarks |

Does **not** publish to GitHub Packages; consumed internally.
Version: **1.6.1** (active).

Primary downstream consumers: `architect-engine/engine` and
`architect-cli/cli`.

---

### `architect-engine/`

**Role**: REST API server — the process that orchestrates task execution.

Canonical ownership note: `architect-engine` owns host concerns only
(Micronaut bootstrapping, HTTP/SSE transport, long-lived orchestration, cloud
reporting). Shared runtime behavior belongs in `architect-core`.

| Sub-path | Purpose |
|---|---|
| `engine/` | Kotlin Micronaut server: `TaskService`, `ProjectService`, `ExecutionService`, `ExecutionEventService`, SSE streaming, plugin loading host |
| `docs/` | Component-level MkDocs docs |

End-users run `architect engine start` to launch this.
Version: **1.6.1** (incubating — baseline tests currently broken).

Depends on `architect-core` for shared runtime behavior and on `architect-api`
for task/plugin contracts.

---

### `architect-cli/`

**Role**: End-user CLI — the `architect` binary.

| Sub-path | Purpose |
|---|---|
| `cli/` | Kotlin Micronaut + Picocli CLI: command routing, HTTP client to engine, embedded task executor, native-image packaging |
| `docs/` | Component-level MkDocs docs |

Delivered via Homebrew and as a native binary.
Version: **1.1.0** (incubating — integration tests currently failing).

Depends on `architect-core` and `architect-api`, and usually talks to the
engine over HTTP even though some commands can execute in embedded mode.

---

## Secondary Products

These are user-facing products built on top of the core platform. They are in
various states of maturity.

Areas that should **not** be treated as production-adjacent today:

- `architect-cloud/ui` — minimal frontend surface with no meaningful lint/test
	entry points
- `architect-vscode` — useful starter extension, but still incubating and only
	supported as a thin reference integration rather than a product-grade editor offering
- `architect-intellij` — schema/helper plugin with minimal feature depth, also
	classified as a thin reference integration rather than a supported IDE product

### `architect-cloud/`

**Role**: Multi-tenant cloud service for running Architect remotely.

| Sub-path | Purpose |
|---|---|
| `backend/` | Kotlin Micronaut server with hexagonal architecture; handles project registration, remote execution, audit |
| `ui/` | Vite + React frontend; currently minimal |
| `agents/` | Docker Compose and Kubernetes agent definitions |
| `api/` | Shared API types between backend and agents |
| `docs/` | Component docs |

Backend status: **beta** (tests passing). UI status: **incubating** (no real tests).

---

### `architect-vscode/`

**Role**: VS Code extension for Architect.

Provides YAML schema validation, task tree view, and task execution from the
editor. Status: **incubating** thin reference integration (basic parser-model tests
exist, but the extension is still CLI-dependent and has no marketplace/release pipeline).

---

### `architect-intellij/`

**Role**: IntelliJ IDEA plugin for Architect.

Provides schema association for `architect.yml` and a task line marker.
Status: **incubating** thin reference integration (basic plugin tests exist, but the
plugin remains minimal, CLI-dependent, and unpublished).

---

## Official Plugins

`plugins/` contains all first-party plugins. Each plugin is an independent
Gradle project that implements `ArchitectPlugin` and is discovered via Java SPI.

Local plugin development path:

1. Create or modify a plugin under `plugins/<plugin-name>/app/`
2. Implement `ArchitectPlugin<Context>` and register tasks in `register()`
3. Add the SPI entry under
	`app/src/main/resources/META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin`
4. Run `./gradlew test` from the plugin's `app/` directory
5. Treat the plugin as a standalone Gradle module during development; the root
	repository does not provide a global plugin build runner

### Active (mature, tested, documented)

| Directory | What it provides |
|---|---|
| `docs-architected/` | MkDocs, Docusaurus, VuePress documentation tasks |
| `git-architected/` | Git status, commit, push, branch tasks |
| `gradle-architected/` | Gradle build, test, publish tasks |
| `github-architected/` | GitHub release, PR, CI/CD tasks |
| `scripts-architected/` | Custom shell script execution tasks |
| `pipelines-architected/` | Pipeline orchestration tasks |

### Incubating (template-level; limited tests/docs)

| Directory | Intended domain |
|---|---|
| `javascript-architected/` | npm / yarn / pnpm workflows |
| `architecture-architected/` | Architecture validation |
| `docker-architected/` | Docker build and push |
| `go-architected/` | Go build and test |
| `kubernetes-architected/` | Kubernetes deploy |
| `maven-architected/` | Maven build |
| `nx-architected/` | Nx monorepo tasks |
| `python-architected/` | Python build and test |
| `rust-architected/` | Rust build and test |
| `terraform-architected/` | Terraform apply/plan |

These plugins are intentionally listed as incubating so contributors do not
assume they meet the same support and testing bar as the mature plugin set.

The `plugins/architect.yml` at the root of the plugins directory configures
the plugins area for local development.

---

## SDKs

`sdk/` contains language SDKs that allow third-party plugins to be written in
languages other than Kotlin/JVM. All are **incubating**.

| Directory | Language | Notes |
|---|---|---|
| `sdk/typescript/` | TypeScript | Protocol coverage documented; tests and examples present |
| `sdk/python/` | Python | Protocol coverage documented; tests and examples present |
| `sdk/go/` | Go | Protocol coverage documented; tests and examples present |

---

## Delivery and Infrastructure

| Directory | Purpose |
|---|---|
| `docs/` | MkDocs monorepo root; aggregates component docs via `mkdocs-monorepo-plugin` |
| `homebrew/` | Homebrew formula for CLI distribution |
| `.github/workflows/` | CI/CD pipelines (currently repetitive; reuse planned in Phase 6) |
| `.github/copilot-instructions.md` | AI assistant guidance for this codebase |

---

## Root-level Files

| File | Purpose |
|---|---|
| `README.md` | Repository front door; project overview and quick start |
| `CONTRIBUTING.md` | Contributor guide; build/test entry points per module |
| `PLAN.md` | Active refactor plan tracking all outstanding work |
| `STATUS.md` | Repository status matrix (module health at a glance) |
| `architect.yml` | Root project config for documentation publishing plus git/GitHub automation metadata; not a repository-wide build/test orchestrator |
| `mkdocs.yml` | Root MkDocs config for unified docs site |
| `detekt.yml` | Detekt static analysis config |
| `Dockerfile` | Container image for the engine |
| `SECURITY.md` | Security policy and vulnerability disclosure |
| `CODE_OF_CONDUCT.md` | Community standards |
| `LICENSE` | Apache 2.0 |

---

## Key Contributor Journeys

These journeys use the **current repository paths**. The target taxonomy defined
earlier in this document is the future destination, not the current on-disk
layout.

### "I want to add a new task to an existing plugin"

1. Open the plugin's `app/src/main/kotlin/.../` source
2. Add your `SimpleTask` / `TaskWithArgs` to the plugin's `register()` method
3. Write a test under `app/src/test/kotlin/.../`
4. Run `./gradlew test` from inside `plugins/<name>/app/`

### "I want to create a new plugin"

1. Run `architect plugin create <name> [template]` from the CLI, or copy an
	active plugin as a fallback/manual template
2. Implement `ArchitectPlugin<YourContext>`
3. Register via `META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin`
4. Depend on `architect-api:2.1.0`
5. Treat the generated/copied plugin as a standalone module under
	`plugins/<name>/app/`

### "I want to run the engine locally"

Current status warning: `architect-engine/engine` is still **incubating** and
its baseline test path is currently blocked by CLI compilation issues. Use this
path for exploration/development, not as proof of a stable green baseline.

```bash
cd architect-engine/engine
./gradlew run
```

### "I want to build the CLI"

Current status warning: `architect-cli/cli` is still **incubating** and its
baseline test/build health is not yet fully restored.

```bash
cd architect-cli/cli
./gradlew build
```

### "I want to run all tests for a module"

```bash
cd <module-dir>
./gradlew test
```

There is no root Gradle wrapper or supported root build/test orchestrator. Each
module is independently built.
See CONTRIBUTING.md for per-module entry points.

---

## What Does Not Exist (No Longer Referenced)

The following were mentioned in early planning docs but are **not present** in
the repository:

- `architect-data/` — removed or never created
- `architect-server/` — removed or never created
- `architect-x/` — removed or never created
