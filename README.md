# Architect

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-purple.svg)](https://kotlinlang.org/)
[![API Build](https://github.com/architect-platform/architect/actions/workflows/architect-api-pipeline.yml/badge.svg)](https://github.com/architect-platform/architect/actions/workflows/architect-api-pipeline.yml)

Architect is a plugin-based task execution framework for automating project
workflows. It provides a unified CLI and an optional REST API for managing
documentation, releases, builds, tests, and deployments across technology stacks
through an extensible plugin architecture. Written in Kotlin, it runs on the JVM
and uses a YAML-driven configuration model (`architect.yml`).

## Repository Topology

> **There is no root Gradle wrapper.** Each module is independently built from
> its own directory. See [STATUS.md](STATUS.md) for the complete support matrix.

```
architect/
├── architect-api/api/            Core contracts & SPI                      [active]
├── architect-core/core/          Shared runtime (plugin loading, config)   [active]
├── architect-engine/engine/      REST API execution host (Micronaut)       [incubating]
├── architect-cli/cli/            Command-line interface (PicoCLI)          [incubating]
├── architect-cloud/              Cloud product surface
│   ├── backend/                    Hexagonal backend service               [beta]
│   ├── ui/                         Web UI                                  [incubating]
│   └── agents/                     AI agent integrations                   [incubating]
├── architect-vscode/             VS Code extension (reference)             [incubating]
├── architect-intellij/           IntelliJ plugin (reference)               [incubating]
├── plugins/                      Official plugins (17)
│   ├── docs-architected/           Docs (MkDocs, Docusaurus, VuePress)    [active]
│   ├── git-architected/            Git integration                        [active]
│   ├── github-architected/         GitHub CI/CD & releases                [active]
│   ├── gradle-architected/         Gradle build integration               [active]
│   ├── scripts-architected/        Custom shell scripts                   [active]
│   ├── pipelines-architected/      Pipeline management                    [active]
│   ├── javascript-architected/     npm / yarn / pnpm                      [incubating]
│   ├── architecture-architected/   Architecture validation                [incubating]
│   ├── docker-architected/         Docker                                 [incubating]
│   ├── go-architected/             Go builds                              [incubating]
│   ├── kubernetes-architected/     Kubernetes                             [incubating]
│   ├── maven-architected/          Maven builds                           [incubating]
│   ├── nx-architected/             Nx monorepo                            [incubating]
│   ├── python-architected/         Python tooling                         [incubating]
│   ├── rust-architected/           Rust tooling                           [incubating]
│   ├── testing-architected/        Cross-language testing                 [incubating]
│   └── terraform-architected/      Terraform                              [incubating]
├── sdk/                          Language SDKs for plugin authoring
│   ├── typescript/                                                        [incubating]
│   ├── python/                                                            [incubating]
│   └── go/                                                                [incubating]
├── docs/                         MkDocs documentation source              [active]
├── homebrew/                     Homebrew formula for CLI                  [active]
├── gradle/                       Shared Gradle conventions & version catalog
├── scripts/                      Repo maintenance scripts
├── architect.yml                 Root project configuration
└── mkdocs.yml                    Documentation site configuration
```

**Note:** Incubating plugins without a version number in STATUS.md are
template-level scaffolds — they define the plugin structure and configuration
schema but have minimal or no runtime implementation.

## Quick Start

### Prerequisites

- Java 17 or higher
- Git

### Install the CLI

**Homebrew (macOS):**

```bash
brew tap architect-platform/architect
brew install architect
```

**From source:**

```bash
cd architect-cli/cli
./gradlew installDist
# Binary: build/install/cli/bin/cli
```

### Create a Project

Create an `architect.yml` in your project root:

```yaml
project:
  name: my-project
  description: "My project"

plugins:
  - name: git-architected
    repo: architect-platform/architect
  - name: docs-architected
    repo: architect-platform/architect

docs:
  build:
    framework: "mkdocs"
    siteName: "My Project Docs"
  publish:
    enabled: true
    githubPages: true
```

Run tasks:

```bash
architect              # List available tasks
architect docs-build   # Build documentation
architect docs-publish # Publish to GitHub Pages
```

### Optional: Run the Engine

The CLI can operate standalone (embedded mode) or connect to the Engine for
persistent project management:

```bash
architect engine install
architect engine start
# architect engine stop
```

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                     Architect CLI                         │
│              (PicoCLI command-line interface)             │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│                   Architect Engine                        │
│            (Micronaut REST API — optional)                │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│                    Architect Core                         │
│      (Plugin loading, execution, config, secrets)        │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│                     Architect API                         │
│         (Contracts, interfaces, SPI for plugins)         │
└──────────────────────┬───────────────────────────────────┘
                       │
           ┌───────────┴───────────┐
           ▼                       ▼
    ┌─────────────┐         ┌─────────────┐
    │  Official   │         │  Language    │
    │  Plugins    │         │  SDKs       │
    │  (17)       │         │  (3)        │
    └─────────────┘         └─────────────┘
```

### Workflow Phases

Tasks are organized into a linear workflow:

```
INIT → LINT → VERIFY → BUILD → TEST → RUN → RELEASE → PUBLISH
```

Plugins register tasks against these phases. A hooks workflow integrates with
Git:

```
PRE_COMMIT → PREPARE_COMMIT_MSG → COMMIT_MSG → POST_COMMIT → PRE_PUSH
```

## Self-Hosting

This repository uses Architect to manage itself. All standard operations are available as Architect commands:

| Operation | Command |
|-----------|---------|
| Build all Kotlin modules | `architect gradle-build` |
| Test all modules | `architect gradle-test` |
| Lint (ktlint) | `architect scripts-ktlint` |
| Static analysis (detekt) | `architect scripts-detekt` |
| Convention checks | `architect scripts-convention-check` |
| Build documentation | `architect docs-build` |
| Publish documentation | `architect docs-publish` |

Configuration is centralized in `architect.yml` — see [Architect Commands Guide](docs/guides/architect-commands.md).

## Support Tiers

See [STATUS.md](STATUS.md) for the complete module-by-module support matrix.

| Tier            | Meaning                                                    |
| --------------- | ---------------------------------------------------------- |
| **active**      | Fully supported, tests passing, versioned, maintained      |
| **beta**        | Feature-complete but not yet stabilised; tests pass        |
| **incubating**  | Work in progress; partial features, possibly failing tests |
| **placeholder** | Scaffolded but not implemented                             |
| **deprecated**  | Scheduled for removal                                      |

**Current state:** The API and Core are active and stable. The CLI and Engine
are incubating with known compilation issues (see STATUS.md). Six plugins are
active; eleven are incubating at varying maturity levels. All three language SDKs are
incubating.

## Building

There is no root Gradle wrapper. Each module is built independently:

```bash
# Core platform (active — tests pass)
cd architect-api/api      && ./gradlew build
cd architect-core/core    && ./gradlew build

# CLI and Engine (incubating — compilation currently broken)
cd architect-cli/cli      && ./gradlew build
cd architect-engine/engine && ./gradlew build

# Cloud backend (beta — tests pass)
cd architect-cloud/backend && ./gradlew build
```

Each `./gradlew build` runs compilation, tests, and static analysis (Detekt).

## Testing

```bash
# Unit tests for stable modules
cd architect-api/api       && ./gradlew test              # passes
cd architect-core/core     && ./gradlew test              # 179 tests

# Test with coverage report
cd architect-api/api       && ./gradlew test jacocoTestReport

# Cloud backend
cd architect-cloud/backend && ./gradlew test              # 57 tests

# IDE extensions
cd architect-vscode        && npm test                    # 3 tests
cd architect-intellij      && ./gradlew test              # 4 tests

# Repo-level checks
./scripts/convention-check.sh
./scripts/release-readiness-check.sh
```

See `docs/guides/testing-standard.md` for the full testing matrix.

## Documentation

The documentation site uses [MkDocs](https://www.mkdocs.org/) with the Material
theme and the monorepo plugin:

```bash
# Install dependencies
pip install mkdocs mkdocs-material mkdocs-monorepo-plugin

# Serve locally (http://127.0.0.1:8000)
mkdocs serve

# Build static site (output: site/)
mkdocs build
```

The navigation structure is defined in `mkdocs.yml`. Component docs are pulled
in from `architect-api/`, `architect-engine/`, and `architect-cli/` via the
monorepo plugin.

## Contributing

We welcome contributions. Please read [CONTRIBUTING.md](CONTRIBUTING.md) for
guidelines on:

- Bug reports and feature requests
- Pull request workflow (fork → branch → test → PR)
- Coding standards (Kotlin, 2-space indent, 120-char lines)
- Conventional Commits format (`feat:`, `fix:`, `docs:`, etc.)

Development prerequisites: Java 17+, Gradle 8.x (via wrapper), Git,
Node.js 18+ (for VS Code extension / Cloud UI), Python 3.x (for docs).

## Repository as Reference

This monorepo demonstrates several patterns for managing complex multi-product repositories:

- **Module Structure Consistency** — Every module has README.md, architect.yml, docs/, STATUS.md
- **Self-Hosting** — The repository dogfoods Architect for all operations
- **Config Generation** — CI workflows generated from templates with drift detection
- **Plugin Architecture** — 16 plugins following identical Context/Plugin/Task patterns
- **Documentation Integration** — Module docs aggregated via MkDocs monorepo plugin
- **Reusable CI Pipelines** — Thin generated wrappers + hand-maintained reusable workflows
- **Release Automation** — All releases via `architect release` and `architect publish`

See [Monorepo Patterns Guide](docs/guides/monorepo-patterns.md) for detailed documentation.

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for
details.

## Links

- [Documentation](docs/)
- [Status](STATUS.md)
- [Architect Platform Docs](https://architect-platform.github.io/architect/)
