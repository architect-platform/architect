# quality-architected

Unified linting, static analysis, and quality gate checks for Architect projects.

**Source**: `architect-platform/quality-architected`

## Installation

```yaml
plugins:
  - name: quality-architected
    type: github
    repo: architect-platform/quality-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `quality-lint` | `LINT` | Run the auto-detected linter for the current project |
| `quality-analyze` | `VERIFY` | Run configured analyzers such as SonarQube or CodeClimate |
| `quality-report` | `TEST` | Print an aggregated summary of configured gates and tool selection |
| `quality-gate` | `VERIFY` | Enforce configured quality thresholds and fail fast when invalid |

## Configuration

Configuration key: `quality`

```yaml
quality:
  enabled: true
  tools:
    - name: sonarqube
      url: https://sonar.example.com
      projectKey: my-project
    - name: codeclimate
  gates:
    coverage: 80
    duplications: 3
    bugs: 0
    vulnerabilities: 0
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | `boolean` | `true` | Enable or disable all quality tasks |
| `tools` | `object[]` | `[]` | Configured analysis tools |
| `tools[].name` | `string` | — | `sonarqube` or `codeclimate` |
| `tools[].url` | `string` | `null` | SonarQube server URL |
| `tools[].projectKey` | `string` | `null` | SonarQube project key |
| `tools[].configFile` | `string` | `null` | Optional tool-specific config file |
| `gates.coverage` | `integer` | `80` | Minimum required coverage percentage |
| `gates.duplications` | `integer` | `3` | Maximum allowed duplication percentage |
| `gates.bugs` | `integer` | `0` | Maximum allowed bug count |
| `gates.vulnerabilities` | `integer` | `0` | Maximum allowed vulnerability count |

## Auto-detection

| Tool | Signals | Command |
|------|---------|---------|
| Detekt | `build.gradle.kts` + `detekt.yml` | `./gradlew detekt` |
| ESLint | `package.json` + `.eslintrc*` or `eslint.config.*` | `npx eslint .` |
| Ruff | `pyproject.toml` with `[tool.ruff]` | `ruff check .` |
| Clippy | `Cargo.toml` | `cargo clippy` |

## Notes

- `quality-analyze` is configuration-driven; it does not auto-detect remote analyzers.
- `quality-gate` exists alongside the three plan-listed tasks so teams can fail pipelines explicitly on quality-gate checks.
- Remote platforms such as SonarQube remain the authoritative source for advanced quality metrics and gate status.
