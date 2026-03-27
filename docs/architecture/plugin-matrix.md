# Plugin Maturity Matrix

> Source of truth: [STATUS.md](../../STATUS.md) · Last updated: 2026-03-24

This document provides a comprehensive maturity matrix for every official
Architect plugin.  All plugins live under `plugins/` and are built with
Gradle (Kotlin DSL).

---

## Matrix

### Active Plugins (6)

| Plugin | Version | Support Tier | Tests | Contract Tests | README | Config Schema | Notes |
|---|---|---|---|---|---|---|---|
| docs-architected | 2.1.0 | **active** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Multi-framework: MkDocs, Docusaurus, VuePress |
| git-architected | 1.0.0 | **active** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Git operations and integration |
| gradle-architected | 1.1.0 | **active** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Gradle build integration |
| scripts-architected | 1.0.0 | **active** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Custom shell script execution |
| github-architected | 1.0.3 | **active** | ✅ | ✅ | ✅ | ✅ `architect.yml` | GitHub CI/CD and release automation |
| pipelines-architected | 1.0.0 | **active** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Pipeline orchestration and management |

### Incubating Plugins (11)

| Plugin | Version | Support Tier | Tests | Contract Tests | README | Config Schema | Notes |
|---|---|---|---|---|---|---|---|
| javascript-architected | 1.0.0 | **incubating** | ✅ | ✅ | ✅ | ✅ `architect.yml` | npm / yarn / pnpm support |
| architecture-architected | 1.0.0 | **incubating** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Architecture validation rules |
| docker-architected | 1.0.0 | **incubating** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Docker build and push |
| go-architected | 1.0.0 | **incubating** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Go build and test |
| kubernetes-architected | 1.0.0 | **incubating** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Kubernetes deployment |
| maven-architected | 1.0.0 | **incubating** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Maven build integration |
| nx-architected | 1.0.0 | **incubating** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Nx monorepo support |
| python-architected | 1.0.0 | **incubating** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Python tooling (pip, venv, pytest) |
| rust-architected | 1.0.0 | **incubating** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Rust build and test (cargo) |
| security-architected | 1.0.0 | **incubating** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Security scanning, dependency audits, and SBOM generation with severity thresholds |
| quality-architected | 1.0.0 | **incubating** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Unified linting, static analysis, reporting, and quality-gate checks |
| testing-architected | 1.0.0 | **incubating** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Unified testing interface with coverage aggregation and threshold enforcement |
| terraform-architected | 1.0.0 | **incubating** | ✅ | ✅ | ✅ | ✅ `architect.yml` | Terraform plan / apply / destroy |

---

## CI Coverage

The following plugins have dedicated CI workflows in `.github/workflows/`:

| Plugin | Workflow |
|---|---|
| docs-architected | `docs-architected-pipeline.yml` |
| git-architected | `git-architected-pipeline.yml` |
| github-architected | `github-architected-pipeline.yml` |
| gradle-architected | `gradle-architected-pipeline.yml` |
| javascript-architected | `javascript-architected.yml` |
| security-architected | `security-architected.yml` |
| quality-architected | `quality-architected.yml` |
| testing-architected | `testing-architected.yml` |
| pipelines-architected | `pipelines-architected.yml` |
| scripts-architected | `scripts-architected.yml` |
| architecture-architected | `architecture-architected.yml` |

Plugins without a dedicated workflow rely on the reusable pipeline
(`reusable-kotlin-pipeline.yml` / `reusable-kotlin-no-release-pipeline.yml`).

---

## Plugin Quality Gates

Each support tier requires a minimum set of artifacts and practices:

### Incubating

- [ ] Plugin compiles and passes `./gradlew build`.
- [ ] At least one unit test exists under `app/src/test/`.
- [ ] A `README.md` describes the plugin's purpose and configuration.
- [ ] An `architect.yml` config schema is present.

### Active

All incubating requirements, **plus**:

- [ ] Contract test class exists and passes (e.g. `*ContractTest.kt`).
- [ ] Test coverage meets the repository baseline.
- [ ] A dedicated or reusable CI workflow runs on every PR.
- [ ] Plugin is listed in the documentation site (`mkdocs.yml` nav).
- [ ] At least one versioned release has been published.
- [ ] `release-readiness-check.sh` passes for the plugin.

---

## Promotion Path

Steps to promote a plugin from **incubating** to **active**:

1. **Harden tests** — Expand unit tests to cover happy-path, error, and
   edge-case scenarios.  Ensure the contract test validates all declared
   tasks against the plugin protocol.

2. **Document thoroughly** — Update `README.md` with configuration
   reference, usage examples, and any prerequisites.  Add the plugin to
   the documentation site navigation in `mkdocs.yml`.

3. **Set up CI** — Create a dedicated workflow or confirm the plugin is
   covered by a reusable pipeline.  The workflow must run lint, test,
   and (for active plugins) publish stages.

4. **Pass release readiness** — Run `scripts/release-readiness-check.sh`
   against the plugin and resolve any failures.

5. **Cut a versioned release** — Publish the plugin artifact with a
   semantic version.  Update the version in `gradle/libs.versions.toml`.

6. **Update STATUS.md** — Change the plugin's tier from *incubating* to
   *active* and record the promotion date.

---

*See also: [Plugin Authoring Guide](../guides/authoring-plugins.md) · [Plugin Standard](../guides/plugin-standard.md) · [Module Matrix](module-matrix.md)*
