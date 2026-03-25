# ADR-001: Use a monorepo with independent Gradle builds

| Field    | Value      |
|----------|------------|
| **Status** | Accepted |
| **Date**   | 2025-01-15 |
| **Authors** | Architect Platform contributors |

## Context

The Architect Platform comprises multiple components at different layers of the
stack: a public API (`architect-api`), a core library (`architect-core`), an
execution engine (`architect-engine`), a CLI (`architect-cli`), cloud tooling
(`architect-cloud`), IDE extensions (VS Code, IntelliJ), first-party plugins,
and multi-language SDKs (TypeScript, Python, Go).

We evaluated three repository strategies:

1. **Polyrepo** — one repository per component. Maximizes isolation but makes
   cross-cutting changes painful and increases CI/CD surface area.
2. **Monorepo with unified build** — a single Gradle build that composes all
   modules via `include()`. Provides a single `./gradlew build` but forces every
   module onto the same build system and Gradle version.
3. **Monorepo with independent builds** — all source lives in one repository,
   but each module owns its own build toolchain (`gradlew`, `package.json`,
   `go.mod`, etc.).

## Decision

We adopt a **monorepo with independent Gradle builds per module**.

Each Kotlin/JVM module carries its own Gradle wrapper (`gradlew`, `gradle/`)
and `build.gradle.kts`. Non-JVM modules use the idiomatic build tool for their
language (npm for TypeScript, pip/Poetry for Python, Go modules for Go).

There is no root-level Gradle wrapper. Build orchestration across modules is
handled by CI workflows and the `architect.yml` project descriptor.

## Consequences

### Positive

- A single repository for all platform code makes cross-cutting changes (e.g.
  renaming an API concept) a single PR across all affected modules.
- Each module can be built and tested independently without compiling unrelated
  code, keeping local development fast.
- Different modules can use the build system best suited to their language
  without a lowest-common-denominator wrapper.

### Negative

- There is no single `./gradlew build` command from the repository root;
  contributors must navigate to the correct module directory.
- Cross-module dependency management (e.g. aligning `architect-api` versions
  consumed by `architect-core` and plugins) requires manual version alignment
  and is not enforced by the build system.
- CI pipelines must selectively trigger builds based on changed paths to avoid
  rebuilding the entire repository on every push.
