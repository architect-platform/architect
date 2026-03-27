# testing-architected

Unified test execution and coverage enforcement for Architect projects.

**Source**: `architect-platform/testing-architected`

## Installation

```yaml
plugins:
  - name: testing-architected
    type: github
    repo: architect-platform/testing-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `test-unit` | `TEST` | Run unit tests for the configured or auto-detected framework |
| `test-integration` | `TEST` | Run integration tests when a sensible command can be inferred |
| `test-e2e` | `TEST` | Run end-to-end tests when a sensible command can be inferred |
| `test-coverage` | `TEST` | Run coverage, aggregate reports, and enforce a threshold |

## Configuration

Configuration key: `testing`

```yaml
testing:
  enabled: true
  framework: auto
  workingDirectory: .
  parallel: true
  retryFlaky: 2
  commands:
    unit:
    integration:
    e2e:
    coverage:
  coverage:
    enabled: true
    threshold: 80
    reporter: [html, lcov, cobertura]
    reportPaths: []
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | `boolean` | `true` | Enable or disable all testing tasks |
| `framework` | `string` | `auto` | Explicit runtime: `auto`, `junit`, `pytest`, `jest`, `vitest`, `go`, `cargo` |
| `workingDirectory` | `string` | `.` | Directory to inspect and execute from |
| `parallel` | `boolean` | `true` | Hint used by supported auto-commands such as Vitest |
| `retryFlaky` | `integer` | `2` | Retry budget for teams that wire retry-capable custom commands |
| `commands.unit` | `string` | `null` | Override the inferred unit test command |
| `commands.integration` | `string` | `null` | Override the inferred integration test command |
| `commands.e2e` | `string` | `null` | Override the inferred end-to-end test command |
| `commands.coverage` | `string` | `null` | Override the inferred coverage command |
| `coverage.enabled` | `boolean` | `true` | Enable or disable the `test-coverage` task |
| `coverage.threshold` | `integer` | `80` | Minimum required coverage percentage |
| `coverage.reporter` | `string[]` | `[html, lcov, cobertura]` | Requested coverage output formats |
| `coverage.reportPaths` | `string[]` | `[]` | Explicit report paths to aggregate |

## Auto-detection

| Framework | Signals |
|-----------|---------|
| JUnit / Gradle | `build.gradle`, `build.gradle.kts` |
| pytest | `pytest.ini`, `conftest.py`, `tox.ini`, pytest markers in Python config |
| Jest | `jest.config.*` or `jest` in `package.json` |
| Vitest | `vitest.config.*` or `vitest` in `package.json` |
| Go | `go.mod` |
| Cargo | `Cargo.toml` |

## Coverage behavior

- JaCoCo XML, Cobertura / `coverage.py` XML, and LCOV are parsed directly.
- Explicit `coverage.reportPaths` is recommended for multi-module aggregation.
- When no report is found, `test-coverage` falls back to parsing percentages from command output.
- The task fails when coverage is below threshold or cannot be determined.

## Usage examples

```bash
# Run the default unit suite
architect test-unit

# Run coverage with the configured threshold
architect test-coverage

# Use explicit args
architect test-unit -- --tests '*ServiceTest'
```
