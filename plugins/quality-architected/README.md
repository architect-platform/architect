# quality-architected

> **Status**: Incubating — see [STATUS.md](STATUS.md)

## Overview

`quality-architected` integrates code quality analysis, linting, and quality gate enforcement into the Architect task lifecycle. It auto-detects linting tools based on project files, supports configurable static analysis integrations, generates quality reports, and enforces quality gates to maintain code standards.

## Tasks

| Task ID            | Phase    | Description                                      |
|--------------------|----------|--------------------------------------------------|
| `quality-lint`     | LINT     | Run detected linters for the project              |
| `quality-analyze`  | VERIFY   | Run static analysis tools (SonarQube, CodeClimate)|
| `quality-report`   | TEST     | Generate an aggregated quality summary report     |
| `quality-gate`     | VERIFY   | Enforce quality gates, fail if any gate violated  |

## Auto-Detection

The plugin automatically detects which linting tool to use based on project files:

| Project Files                              | Tool Detected    | Command                |
|--------------------------------------------|------------------|------------------------|
| `build.gradle.kts` + `detekt.yml`         | Detekt           | `./gradlew detekt`    |
| `package.json` + `.eslintrc*` or `eslint.config.*` | ESLint   | `npx eslint .`        |
| `pyproject.toml` with `[tool.ruff]`       | Ruff             | `ruff check .`        |
| `Cargo.toml`                               | Clippy           | `cargo clippy`        |

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: quality-architected
    repo: architect-platform/quality-architected

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

### Configuration Options

| Field                    | Type      | Default | Description                                    |
|--------------------------|-----------|---------|------------------------------------------------|
| `enabled`                | boolean   | `true`  | Enable or disable all quality checks           |
| `tools`                  | array     | `[]`    | List of quality tools to integrate             |
| `tools[].name`           | string    | —       | Tool name (`sonarqube`, `codeclimate`, etc.)   |
| `tools[].url`            | string    | `null`  | Tool server URL (e.g., SonarQube host)         |
| `tools[].projectKey`     | string    | `null`  | Project key for the tool                       |
| `tools[].configFile`     | string    | `null`  | Path to tool-specific config file              |
| `gates.coverage`         | integer   | `80`    | Minimum code coverage percentage               |
| `gates.duplications`     | integer   | `3`     | Maximum allowed code duplication percentage     |
| `gates.bugs`             | integer   | `0`     | Maximum allowed bug count                       |
| `gates.vulnerabilities`  | integer   | `0`     | Maximum allowed vulnerability count             |

### Disabling Quality Checks

```yaml
quality:
  enabled: false
```

When disabled, all quality tasks return a skipped result without executing any commands.

### Quality Gates

Quality gates define thresholds that your project must meet. If any gate is violated, the `quality-gate` task will fail, blocking the pipeline.

```yaml
quality:
  gates:
    coverage: 90        # Require at least 90% code coverage
    duplications: 2     # Allow at most 2% code duplication
    bugs: 0             # Zero bugs allowed
    vulnerabilities: 0  # Zero vulnerabilities allowed
```

## Local Build and Test

```bash
cd plugins/quality-architected/app
./gradlew build
./gradlew test
```

## Supported Tools

### Linters (Auto-Detected)
- **Detekt** — Kotlin static analysis
- **ESLint** — JavaScript/TypeScript linting
- **Ruff** — Python linting
- **Clippy** — Rust linting

### Static Analysis (Configured)
- **SonarQube** — Continuous code quality inspection
- **CodeClimate** — Automated code review

## Architecture

The plugin follows the standard Architect plugin pattern:

- `QualityPlugin` — Main plugin class implementing `ArchitectPlugin<QualityContext>`
- `QualityContext` — Configuration data class with tools, gates, and enabled flag
- `QualityTask` — Reusable task class with auto-detection and command building logic

## Limitations

- Static analysis tools (SonarQube, CodeClimate) must be pre-installed and accessible
- Auto-detection covers the most common setups; custom configurations may need explicit tool setup
- Quality gate enforcement currently validates configured thresholds and tool execution flow; remote quality platforms still own their authoritative server-side gate evaluation
