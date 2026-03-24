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

---

## Core Platform

These four modules form the primary execution stack. Every other module either
depends on them, integrates with them, or extends them.

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

| Sub-path | Purpose |
|---|---|
| `core/` | Kotlin library: `ProjectService`, `ProjectPluginLoader`, `TaskDependencyResolver`, `SecretResolver`, `ClassLoaderResourceExtractor`, `LocalPluginSource`, JMH benchmarks |

Does **not** publish to GitHub Packages; consumed internally.
Version: **1.6.1** (active).

---

### `architect-engine/`

**Role**: REST API server — the process that orchestrates task execution.

| Sub-path | Purpose |
|---|---|
| `engine/` | Kotlin Micronaut server: `TaskService`, `ProjectService`, `ExecutionService`, `ExecutionEventService`, SSE streaming, plugin loading host |
| `docs/` | Component-level MkDocs docs |

End-users run `architect engine start` to launch this.
Version: **1.6.1** (incubating — baseline tests currently broken).

---

### `architect-cli/`

**Role**: End-user CLI — the `architect` binary.

| Sub-path | Purpose |
|---|---|
| `cli/` | Kotlin Micronaut + Picocli CLI: command routing, HTTP client to engine, embedded task executor, native-image packaging |
| `docs/` | Component-level MkDocs docs |

Delivered via Homebrew and as a native binary.
Version: **1.1.0** (incubating — integration tests currently failing).

---

## Secondary Products

These are user-facing products built on top of the core platform. They are in
various states of maturity.

### `architect-cloud/`

**Role**: Multi-tenant cloud service for running Architect remotely.

| Sub-path | Purpose |
|---|---|
| `backend/` | Kotlin Micronaut server with hexagonal architecture; handles project registration, remote execution, audit |
| `ui/` | Vite + Vue 3 frontend; currently minimal |
| `agents/` | Docker Compose and Kubernetes agent definitions |
| `api/` | Shared API types between backend and agents |
| `docs/` | Component docs |

Backend status: **beta** (tests passing). UI status: **incubating** (no real tests).

---

### `architect-vscode/`

**Role**: VS Code extension for Architect.

Provides YAML schema validation, task tree view, and task execution from the
editor. Status: **incubating** (no automated tests; brittle YAML implementation).

---

### `architect-intellij/`

**Role**: IntelliJ IDEA plugin for Architect.

Provides schema association for `architect.yml` and a task line marker.
Status: **incubating** (no automated tests; minimal feature depth).

---

## Official Plugins

`plugins/` contains all first-party plugins. Each plugin is an independent
Gradle project that implements `ArchitectPlugin` and is discovered via Java SPI.

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
| `architect.yml` | Root project config; uses docs, git, github plugins |
| `mkdocs.yml` | Root MkDocs config for unified docs site |
| `detekt.yml` | Detekt static analysis config |
| `Dockerfile` | Container image for the engine |
| `SECURITY.md` | Security policy and vulnerability disclosure |
| `CODE_OF_CONDUCT.md` | Community standards |
| `LICENSE` | Apache 2.0 |

---

## Key Contributor Journeys

### "I want to add a new task to an existing plugin"

1. Open the plugin's `app/src/main/kotlin/.../` source
2. Add your `SimpleTask` / `TaskWithArgs` to the plugin's `register()` method
3. Write a test under `app/src/test/kotlin/.../`
4. Run `./gradlew test` from inside `plugins/<name>/app/`

### "I want to create a new plugin"

1. Run `architect plugin create` or copy an active plugin as a template
2. Implement `ArchitectPlugin<YourContext>`
3. Register via `META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin`
4. Depend on `architect-api:2.1.0`

### "I want to run the engine locally"

```bash
cd architect-engine/engine
./gradlew run
```

### "I want to build the CLI"

```bash
cd architect-cli/cli
./gradlew build
```

### "I want to run all tests for a module"

```bash
cd <module-dir>
./gradlew test
```

There is no root Gradle wrapper. Each module is independently built.
See CONTRIBUTING.md for per-module entry points.

---

## What Does Not Exist (No Longer Referenced)

The following were mentioned in early planning docs but are **not present** in
the repository:

- `architect-data/` — removed or never created
- `architect-server/` — removed or never created
- `architect-x/` — removed or never created
