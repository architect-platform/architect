# Repository Status Matrix

> Generated: 2026-03-24 | Phase 0 baseline

This document records the support status and baseline health of every subproject in the repository.
It is the authoritative reference for Phase 0 of the refactor plan (PLAN.md).

## Status Definitions

| Status | Meaning |
|---|---|
| **active** | Fully supported, tests passing, versioned, maintained |
| **beta** | Feature-complete but not yet stabilised; tests pass |
| **incubating** | Work in progress; partial features, possibly failing tests |
| **placeholder** | Scaffolded but not implemented |
| **deprecated** | Scheduled for removal |

---

## Core Platform

| Module | Path | Status | Version | Tests | Notes |
|---|---|---|---|---|---|
| architect-api | `architect-api/api` | **active** | 2.1.0 | ✅ pass | Published to GitHub Packages; authoritative contracts |
| architect-core | `architect-core/core` | **active** | 1.6.1 | ✅ pass (179) | Shared runtime; JMH benchmarks present |
| architect-engine | `architect-engine/engine` | **incubating** | 1.6.1 | ❌ broken | Compilation blocker around `PluginConfig`; see baseline failures |
| architect-cli | `architect-cli/cli` | **incubating** | 1.1.0 | ❌ broken | 8 `CliEngineIntegrationTest` failures; see baseline failures |

---

## Secondary Products

| Module | Path | Status | Version | Tests | Notes |
|---|---|---|---|---|---|
| architect-cloud/backend | `architect-cloud/backend` | **beta** | 1.0.0 | ✅ pass (57) | Hexagonal architecture with ArchUnit enforcement |
| architect-cloud/ui | `architect-cloud/ui` | **incubating** | — | ❌ none | Empty `lint` and `test` scripts in `package.json` |
| architect-vscode | `architect-vscode` | **incubating** | — | ❌ none | No automated tests; brittle YAML parsing |
| architect-intellij | `architect-intellij` | **incubating** | — | ❌ none | Schema association only; minimal feature depth |

---

## Official Plugins

### Active / Mature

| Plugin | Path | Status | Version | Notes |
|---|---|---|---|---|
| docs-architected | `plugins/docs-architected` | **active** | 2.1.0 | Multi-framework docs (MkDocs, Docusaurus, VuePress); rich README |
| git-architected | `plugins/git-architected` | **active** | 1.0.0 | Git integration; contract tests present |
| gradle-architected | `plugins/gradle-architected` | **active** | 1.1.0 | Gradle build integration |
| scripts-architected | `plugins/scripts-architected` | **active** | 1.0.0 | Custom shell script execution |
| github-architected | `plugins/github-architected` | **active** | 1.0.3 | GitHub CI/CD and release automation |
| pipelines-architected | `plugins/pipelines-architected` | **active** | 1.0.0 | Pipeline management; rich README |

### Incubating / Thin

| Plugin | Path | Status | Version | Notes |
|---|---|---|---|---|
| javascript-architected | `plugins/javascript-architected` | **incubating** | 1.0.0 | npm/yarn/pnpm; minimal tests |
| architecture-architected | `plugins/architecture-architected` | **incubating** | — | Architecture validation; template-level |
| docker-architected | `plugins/docker-architected` | **incubating** | — | Docker integration; minimal tests/docs |
| go-architected | `plugins/go-architected` | **incubating** | — | Go build integration; template-level |
| kubernetes-architected | `plugins/kubernetes-architected` | **incubating** | — | Kubernetes; template-level |
| maven-architected | `plugins/maven-architected` | **incubating** | — | Maven build; template-level |
| nx-architected | `plugins/nx-architected` | **incubating** | — | Nx monorepo; template-level |
| python-architected | `plugins/python-architected` | **incubating** | — | Python tooling; template-level |
| rust-architected | `plugins/rust-architected` | **incubating** | — | Rust tooling; template-level |
| terraform-architected | `plugins/terraform-architected` | **incubating** | — | Terraform; template-level |

---

## SDKs

| SDK | Path | Status | Notes |
|---|---|---|---|
| TypeScript Plugin SDK | `sdk/typescript` | **incubating** | Protocol coverage documented; tests/examples present |
| Python Plugin SDK | `sdk/python` | **incubating** | Protocol coverage documented; tests/examples present |
| Go Plugin SDK | `sdk/go` | **incubating** | Protocol coverage documented; tests/examples present |

---

## Delivery / Infrastructure

| Area | Path | Status | Notes |
|---|---|---|---|
| Documentation | `docs/` | **active** | MkDocs monorepo; served from component docs |
| Homebrew Formula | `homebrew/` | **active** | Homebrew delivery for CLI |
| Root CI/CD | `.github/workflows/` | **incubating** | Highly repetitive generated workflows; reuse needed |

---

## Not Present (Previously Referenced)

The following directories were referenced in historical docs but **do not exist** in the current repository and can be removed from planning scope:
- `architect-data` — not present
- `architect-server` — not present
- `architect-x` — not present
