# Monorepo Patterns

This guide documents the patterns used in the Architect Platform repository for managing a complex multi-product monorepo. These patterns can be adopted by any project using Architect.

## Module Structure

Every module follows an identical structure:

- `README.md` — Module overview, quick start, links
- `architect.yml` — Plugin configuration and build settings
- `docs/` — Module documentation (`index.md` + `mkdocs.yml` for monorepo integration)
- `STATUS.md` — Maturity tier and version tracking

This consistency makes it easy to onboard new contributors and to write tooling
that operates across all modules uniformly. The
[convention-check script](../../scripts/convention-check.sh) validates that every
module conforms to this structure.

## Self-Hosting Pattern

The repository uses Architect to manage itself ("dogfooding"):

- Root `architect.yml` declares all plugins and their configurations
- Every build, test, lint, and release operation is an Architect command
- No raw tool invocations (gradle, npm, etc.) required for standard workflows

This ensures that Architect's own development experience reflects what users
encounter and that breaking changes are caught immediately.

### Key commands for this repository

| Operation                 | Command                             |
|---------------------------|-------------------------------------|
| Build all Kotlin modules  | `architect gradle-build`            |
| Test all modules          | `architect gradle-test`             |
| Lint (ktlint)             | `architect scripts-ktlint`          |
| Static analysis (detekt)  | `architect scripts-detekt`          |
| Convention checks         | `architect scripts-convention-check`|
| Build documentation       | `architect docs-build`              |
| Publish documentation     | `architect docs-publish`            |

## Config Generation Pattern

Generated configuration files keep CI in sync with the source of truth:

- **Generated configs**: CI workflow wrappers (13 files) from `.github/scripts/gen_workflows.py`
- **Hand-maintained configs**: `mkdocs.yml`, `detekt.yml`, reusable CI workflows
- **Drift detection**: `workflow-drift-check.yml` validates generated files match templates
- **Generated file markers**: All generated files have header comments identifying them

### Why generate?

Maintaining 13+ nearly-identical CI workflow files by hand is error-prone.
Generation ensures every module pipeline follows the same structure, uses the
same caching strategy, and invokes the same Architect commands.

## CI/CD Architecture

Three-tier pipeline strategy:

1. **Generated wrappers** (~23 lines each) — trigger on module path changes, delegate to reusable workflows
2. **Reusable workflows** — define actual CI policy (JDK version, Gradle caching, Architect commands)
3. **Specialized workflows** — release artifacts, security scanning, monorepo validation

This separation means most CI changes only require editing one reusable workflow,
which automatically applies to all modules.

See [CI/CD Integration Guide](ci-cd-integration.md) for implementation details.

## Plugin Ecosystem Pattern

All 16 plugins follow the canonical structure:

- `Context.kt` — Configuration data class holding plugin settings
- `Plugin.kt` — Plugin registration implementing `ArchitectPlugin<Context>`
- `*Task.kt` — Task implementations for each operation the plugin provides
- Contract tests validating plugin registration and task discovery

This uniform structure means contributors can navigate any plugin without
learning a new layout. The [Plugin Standard](plugin-standard.md) documents
the full checklist.

## Documentation Integration

Module docs are aggregated into the root site via MkDocs monorepo plugin:

- Each module has `docs/mkdocs.yml` with its local navigation
- Root `mkdocs.yml` uses `!include` directives to pull in component docs
- Standard doc template: Overview, Getting Started, Configuration, Links

This pattern produces a single documentation site while letting each module
own its own docs. Changes to a module's documentation are reviewed alongside
code changes in the same PR.

## Convention Enforcement

Automated checks ensure consistency across the entire repository:

- `scripts/convention-check.sh` — 8+ automated checks for module structure
- `monorepo-validation.yml` — CI pipeline for structure audit
- Module structure audit verifies README, `architect.yml`, `docs/`, `STATUS.md`

### Checks performed

| Check                        | What it validates                                   |
|------------------------------|-----------------------------------------------------|
| Module README presence       | Every module has a `README.md`                      |
| architect.yml presence       | Every module has an `architect.yml`                 |
| docs/ directory              | Every module has a `docs/` folder                   |
| STATUS.md presence           | Every module declares its maturity tier              |
| Conventional commit format   | Recent commits follow the convention                |
| Generated file drift         | Generated CI files match their templates            |
| Release readiness            | Active-tier modules meet all release criteria       |
| Forbidden patterns           | No `FIXME:` or `STOPSHIP:` in active modules       |

Run all checks locally before submitting a PR:

```bash
architect scripts-convention-check
```

See [Release Readiness](release-readiness.md) for the per-module readiness
checklist.
