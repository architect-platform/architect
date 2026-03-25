# Module Support Matrix

> Source of truth: [STATUS.md](../../STATUS.md) · Last updated: 2026-03-24

This document provides a comprehensive matrix of every module in the Architect
Platform repository, grouped by bounded context.  Use it to understand what is
production-ready, what is under active development, and what is still
scaffolding.

---

## Matrix

### Core Platform

| Module | Context | Build System | Support Tier | Version | Tests | CI | Notes |
|---|---|---|---|---|---|---|---|
| architect-api | Core Platform | Gradle (Kotlin DSL) | **active** | 2.2.0 | ✅ pass | ✅ `architect-api-pipeline.yml` | Published to GitHub Packages; authoritative contracts |
| architect-core | Core Platform | Gradle (Kotlin DSL) | **active** | 1.6.1 | ✅ pass (179) | ✅ via composite build | Shared runtime; JMH benchmarks present |
| architect-engine | Core Platform | Gradle (Kotlin DSL) | **incubating** | 1.6.1 | ❌ broken | ✅ `architect-engine-pipeline.yml` | Compilation failure in composite-built CLI (`ArchitectLauncher.kt`) |
| architect-cli | Core Platform | Gradle (Kotlin DSL) | **incubating** | 1.1.0 | ❌ broken | ✅ `architect-cli-pipeline.yml` | Compilation failure at `ArchitectLauncher.kt` (`taskService`, `TaskConditionChecker`) |

### Product Surfaces

| Module | Context | Build System | Support Tier | Version | Tests | CI | Notes |
|---|---|---|---|---|---|---|---|
| architect-cloud/backend | Product Surfaces | Gradle (Kotlin DSL) | **beta** | 1.0.0 | ✅ pass (57) | ✅ `architect-cloud-backend.yml` | Hexagonal architecture with ArchUnit enforcement |
| architect-cloud/ui | Product Surfaces | npm | **incubating** | 1.0.0 | ❌ none | ✅ `architect-cloud-ui.yml` | Empty `lint` and `test` scripts in `package.json` |
| architect-vscode | Product Surfaces | npm | **incubating** | 0.1.0 | ✅ pass (3) | ❌ none | Thin reference integration; no marketplace/release pipeline |
| architect-intellij | Product Surfaces | Gradle (Kotlin DSL) | **incubating** | 0.1.0 | ✅ pass (4) | ❌ none | Thin reference integration; minimal and unpublished |

### SDKs

| Module | Context | Build System | Support Tier | Version | Tests | CI | Notes |
|---|---|---|---|---|---|---|---|
| TypeScript Plugin SDK | SDKs | npm | **incubating** | 0.1.0 | ✅ present | ❌ none | Protocol coverage documented; requires Node ≥ 18 |
| Python Plugin SDK | SDKs | setuptools (pyproject.toml) | **incubating** | 0.1.0 | ✅ present | ❌ none | Protocol coverage documented; requires Python ≥ 3.11 |
| Go Plugin SDK | SDKs | Go modules | **incubating** | — | ✅ present | ❌ none | Protocol coverage documented |

### Delivery & Infrastructure

| Module | Context | Build System | Support Tier | Version | Tests | CI | Notes |
|---|---|---|---|---|---|---|---|
| docs | Delivery | MkDocs (Material) | **active** | — | — | ❌ none | MkDocs monorepo with `!include` component docs |
| homebrew | Delivery | Homebrew formula | **active** | — | — | ✅ `update-homebrew.yml` | CLI delivery; multi-platform (macOS + Linux, arm64 + x86_64) |
| scripts | Delivery | Bash | **active** | — | — | ❌ none | `convention-check.sh`, `release-readiness-check.sh` |

---

## Tier Definitions

Full definitions live in [STATUS.md](../../STATUS.md).  Summary:

| Tier | Meaning |
|---|---|
| **Active** | Production-ready, tests passing, versioned, full CI, actively maintained |
| **Beta** | Feature-complete but not yet stabilised; tests pass |
| **Incubating** | Work in progress; partial features, possibly failing tests, API unstable |
| **Placeholder** | Scaffolded but not implemented |
| **Deprecated** | Scheduled for removal |

---

## Promotion Criteria

A module moves **up** one tier when it meets the following gates:

### Placeholder → Incubating

- Source code compiles without errors.
- At least one meaningful unit test exists.
- A `README.md` describes the module's purpose and basic usage.

### Incubating → Beta

- All existing tests pass on CI.
- Test coverage reaches the repository baseline (see `guides/testing-standard.md`).
- Public API surface is documented.
- No known critical or high-severity bugs.
- A dedicated CI workflow exists and runs on every PR.

### Beta → Active

- CI pipeline includes lint, test, and publish stages.
- Integration or contract tests cover cross-module boundaries.
- At least one full release has been published (versioned artifact).
- A runbook or operational guide is available for production use.
- Module appears in the documentation site navigation.
- `release-readiness-check.sh` passes for the module.

### Active → Deprecated

- A deprecation notice is added to the module's README and STATUS.md.
- A migration guide is published.
- A removal date is announced (minimum 2 release cycles).

---

*See also: [Repository Map](repository-map.md) · [Ownership Map](ownership-map.md) · [Baseline Failures](baseline-failures.md)*
