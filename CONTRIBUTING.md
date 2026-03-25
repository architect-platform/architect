# Contributing to Architect Platform

Welcome — and thank you for contributing!
Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before participating.

## Repository Layout

This is a multi-module repository with **no root Gradle wrapper**.
Each module builds independently from its own directory.
See the **Repository Topology** section in [README.md](README.md) for the full map.

## Support Tiers

Every module declares a support tier in [STATUS.md](STATUS.md).
The tier determines what quality gates apply (see [Quality Gates](#quality-gates) below).

| Tier | Meaning |
|---|---|
| **active** | Fully supported, tests green, versioned, maintained |
| **beta** | Feature-complete, tests passing, not yet stabilised |
| **incubating** | Work in progress; partial features, may have failing tests |

New modules must declare their tier in both their `README.md` and `STATUS.md`
before they can be merged.

## Development Setup

### Prerequisites

| Tool | Version | Used by |
|---|---|---|
| JDK | 17+ | All Kotlin/JVM modules |
| Gradle | 8.x (via wrapper) | All Kotlin/JVM modules |
| Kotlin | 1.9.25 | All Kotlin/JVM modules |
| Node.js | 18+ | `architect-cloud/ui`, `architect-vscode` |
| Python | 3.x | MkDocs documentation |
| Git | any recent | Everything |

### Clone

```bash
git clone https://github.com/architect-platform/architect.git
cd architect
```

## Building

Build each module from its own directory.
`./gradlew build` compiles, runs tests, and executes detekt analysis.

```bash
# Core platform (Kotlin/JVM)
cd architect-api/api     && ./gradlew build
cd architect-core/core   && ./gradlew build
cd architect-engine/engine && ./gradlew build
cd architect-cli/cli     && ./gradlew build

# Cloud backend (Kotlin/JVM)
cd architect-cloud/backend && ./gradlew build

# Cloud UI (Node.js)
cd architect-cloud/ui && npm ci && npm run build

# Plugins (Kotlin/JVM)
cd plugins/<plugin>/app && ./gradlew build
```

## Testing

Run tests per module — there is no root-level test task.
See [`docs/guides/testing-standard.md`](docs/guides/testing-standard.md) for the
full minimum-test matrix by module type.

```bash
# Kotlin/JVM module tests
cd architect-core/core && ./gradlew test

# Run a single test class
cd architect-api/api && ./gradlew test --tests TaskServiceTest

# Coverage report (modules that support it)
cd architect-api/api && ./gradlew test jacocoTestReport

# Plugin tests
cd plugins/docs-architected/app && ./gradlew test

# Frontend / IDE extension tests
cd architect-cloud/ui && npm test
cd architect-vscode   && npm test
cd architect-intellij && gradle test
```

### What the testing standard requires

- **Libraries / Core**: unit tests for public API + at least one negative-path test
- **Services**: unit + HTTP boundary tests + config smoke test
- **Plugins**: positive-path task execution + error/validation-path + `ArchitectPluginContractTestSuite`
- **Frontend**: lint + typecheck + component tests + production build smoke test
- **IDE extensions**: config parsing tests + behaviour tests

## Code Style

Kotlin modules enforce style automatically via **ktlint** and **detekt** (run
as part of `./gradlew build`). Key settings from [`detekt.yml`](detekt.yml):

- 2-space indentation, no tabs
- 120-character max line length
- Max 80 lines per method, max 8 parameters, max 20 functions per file
- No wildcard imports (except `java.util.*`, `kotlinx.coroutines.*`)
- `FIXME:` and `STOPSHIP:` comments are forbidden (`maxIssues: 0`)
- Test functions may use backtick names (`@Test fun \`should do X\`()`)

Naming conventions: `<Name>Plugin.kt`, `<Name>Context.kt`, `<Name>Task.kt`,
`<Name>Utils.kt` — see [`docs/guides/plugin-standard.md`](docs/guides/plugin-standard.md).

## Commit Convention

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>
```

| Type | Purpose |
|---|---|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `test` | Adding or updating tests |
| `chore` | Build, CI, or tooling changes |
| `perf` | Performance improvement |

Examples:

```
feat(core): add secret-rotation support for Vault provider
fix(engine): prevent NPE when task condition evaluator receives null context
docs(plugins): update gradle-architected README with test commands
test(api): add contract regression tests for TaskResult serialisation
```

## Quality Gates

What must pass before a PR can merge, **by tier**:

| Gate | active | beta | incubating |
|---|---|---|---|
| Module builds | ✅ | ✅ | ✅ |
| All module tests pass | ✅ | ✅ | basic / best-effort |
| ktlint | ✅ | ✅ | — |
| detekt (`maxIssues: 0`) | ✅ | — | — |
| Release-readiness check | ✅ | — | — |

The release-readiness script validates README, STATUS.md, version declaration,
test presence, SPI registration (plugins), and absence of blocking TODOs:

```bash
./scripts/release-readiness-check.sh <module-path> --tier active
```

See [`docs/guides/release-readiness.md`](docs/guides/release-readiness.md) for
the full checklist.

## Plugin Development

Use the scaffolding command to create a new plugin:

```bash
architect plugin create my-plugin                      # Kotlin JVM
architect plugin create my-process-plugin typescript   # TypeScript process plugin
```

Key steps:

1. Implement `ArchitectPlugin<C>` and register via Java SPI
2. Write tests using `ArchitectPluginTestKit` (JVM) or protocol-level tests (process)
3. Run `architect plugin validate` before submitting
4. Add a docs page under `docs/reference/plugins/`

Full guide: [`docs/guides/authoring-plugins.md`](docs/guides/authoring-plugins.md)
Standard checklist: [`docs/guides/plugin-standard.md`](docs/guides/plugin-standard.md)

## Documentation

Docs are built with [MkDocs](https://www.mkdocs.org/) + Material theme:

```bash
pip install mkdocs mkdocs-material mkdocs-monorepo-plugin
mkdocs serve       # local preview at http://127.0.0.1:8000
mkdocs build       # production build
```

When changing code, update relevant docs and component READMEs.

## Pull Request Process

1. Branch from `main`; use a descriptive branch name (`feat/secret-rotation`)
2. Make focused commits following the [commit convention](#commit-convention)
3. Ensure the [quality gates](#quality-gates) for your module's tier pass locally
4. Open a PR with a clear title in Conventional Commits format and a description
   of **what** changed and **why**
5. At least one maintainer review is required
6. Security-sensitive changes require an additional security review

## Standards References

All project standards live in `docs/guides/`:

| Guide | Covers |
|---|---|
| [testing-standard.md](docs/guides/testing-standard.md) | Minimum test matrix per module type |
| [plugin-standard.md](docs/guides/plugin-standard.md) | Plugin layout, naming, checklist |
| [authoring-plugins.md](docs/guides/authoring-plugins.md) | End-to-end plugin authoring guide |
| [release-readiness.md](docs/guides/release-readiness.md) | Release-readiness criteria and script |
| [ci-cd-integration.md](docs/guides/ci-cd-integration.md) | CI/CD pipeline integration |
| [security-requirements.md](docs/guides/security-requirements.md) | Security requirements |
| [logging-error-handling.md](docs/guides/logging-error-handling.md) | Logging and error handling |
| [observability.md](docs/guides/observability.md) | Observability and metrics |
| [performance-testing.md](docs/guides/performance-testing.md) | Performance testing |

## License

By contributing you agree that your contributions will be licensed under the
project's [MIT License](LICENSE).
