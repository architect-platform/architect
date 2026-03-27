# testing-architected

> **Status**: Incubating — see [STATUS.md](STATUS.md)

## Overview

`testing-architected` provides a unified testing layer for Architect projects. It auto-detects common test frameworks, exposes dedicated tasks for unit, integration, end-to-end, and coverage workflows, and can enforce a minimum coverage threshold from aggregated reports.

Supported auto-detection includes:

- Gradle / JUnit
- pytest
- Jest
- Vitest
- Go test
- Cargo test

## Tasks

| Task ID | Phase | Description |
|---|---|---|
| `test-unit` | `TEST` | Run the default unit test suite for the detected framework |
| `test-integration` | `TEST` | Run the integration suite when the framework exposes a sensible integration command |
| `test-e2e` | `TEST` | Run the end-to-end suite when the framework exposes a sensible e2e command |
| `test-coverage` | `TEST` | Run coverage, aggregate reports, and fail when coverage is below threshold |

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: testing-architected
    repo: architect-platform/testing-architected

testing:
  enabled: true
  framework: auto         # auto | junit | pytest | jest | vitest | go | cargo
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

### Configuration notes

- `framework: auto` inspects project files and configuration to choose a runtime.
- `commands.*` overrides auto-detected commands when your project uses custom task names.
- `coverage.reportPaths` is the safest way to aggregate multiple module reports without relying on auto-discovery.
- `retryFlaky` is exposed in config so teams can pair the plugin with explicit framework-specific retry commands.

## Coverage aggregation

`test-coverage` understands common coverage formats:

- JaCoCo XML
- Cobertura / `coverage.py` XML
- LCOV

If no report is found, the task falls back to parsing stdout/stderr for a percentage. When coverage cannot be determined, the task fails explicitly instead of returning a success-shaped result.

## Auto-detection heuristics

| Framework | Signals |
|---|---|
| Gradle / JUnit | `build.gradle`, `build.gradle.kts` |
| pytest | `pytest.ini`, `conftest.py`, `tox.ini`, pytest markers in `pyproject.toml` or requirements files |
| Jest | `jest.config.*` or `jest` in `package.json` |
| Vitest | `vitest.config.*` or `vitest` in `package.json` |
| Go test | `go.mod` |
| Cargo test | `Cargo.toml` |

## Local build and test

```bash
cd plugins/testing-architected/app
./gradlew test
./gradlew build
```

## Notes

- Integration and e2e tasks return `skipped` when the plugin cannot infer a sensible command.
- Gradle coverage defaults to `./gradlew test jacocoTestReport` when JaCoCo is available.
- Cargo coverage defaults to `cargo llvm-cov --lcov --output-path lcov.info`; override it when your project uses a different coverage tool.
