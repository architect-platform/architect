# security-architected

> **Status**: Incubating — see [STATUS.md](STATUS.md)

## Overview

`security-architected` adds a security verification layer to Architect projects. It can orchestrate file-system scanning, ecosystem-specific dependency audits, and SBOM generation while enforcing a severity threshold directly from `architect.yml`.

Supported integrations include:

- Trivy filesystem scanning and SBOM generation
- Snyk vulnerability testing
- CodeQL SARIF analysis (with explicit database/query configuration)
- `npm audit`
- `pip-audit`
- `cargo audit`

## Tasks

| Task ID | Phase | Description |
|---|---|---|
| `security-scan` | `VERIFY` | Run configured scanners such as Trivy, Snyk, and CodeQL |
| `security-audit` | `VERIFY` | Run dependency audits for npm, Python, and Cargo ecosystems |
| `security-sbom` | `VERIFY` | Generate a CycloneDX or SPDX SBOM |

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: security-architected
    repo: architect-platform/security-architected

security:
  enabled: true
  workingDirectory: .
  scan:
    enabled: true
    tools: [trivy, npm-audit]
    failOn: critical
    codeqlDatabase: .codeql/java-db
    codeqlQuerySuite: codeql/java-queries:codeql-suites/java-security-and-quality.qls
  audit:
    enabled: true
    failOn: high
  sbom:
    enabled: true
    format: cyclonedx
    output: sbom.json
```

| Field | Type | Default | Description |
|---|---|---|---|
| `enabled` | `boolean` | `true` | Enable or disable all security tasks |
| `workingDirectory` | `string` | `.` | Directory to inspect and execute from |
| `scan.enabled` | `boolean` | `true` | Enable or disable `security-scan` |
| `scan.tools` | `string[]` | `[trivy]` | Scanner selection. Audit tools listed here are passed to `security-audit`. |
| `scan.failOn` | `string` | `critical` | Minimum severity that should fail `security-scan` |
| `scan.codeqlDatabase` | `string` | `null` | CodeQL database path used by the built-in CodeQL command |
| `scan.codeqlQuerySuite` | `string` | `null` | Query suite used by the built-in CodeQL command |
| `scan.codeqlOutput` | `string` | `build/reports/security/codeql.sarif` | SARIF output path for CodeQL |
| `audit.enabled` | `boolean` | `true` | Enable or disable `security-audit` |
| `audit.tools` | `string[]` | `[]` | Explicit dependency audit tools; defaults to auto-detection or `scan.tools` fallback |
| `audit.failOn` | `string` | `high` | Minimum severity that should fail `security-audit` |
| `sbom.enabled` | `boolean` | `true` | Enable or disable `security-sbom` |
| `sbom.format` | `string` | `cyclonedx` | Output format: `cyclonedx` or `spdx` |
| `sbom.output` | `string` | `sbom.json` | SBOM output path relative to `workingDirectory` |
| `sbom.tool` | `string` | `trivy` | SBOM generator implementation |

## Auto-detection behavior

`security-audit` auto-detects common ecosystems when `audit.tools` is omitted:

| Tool | Signals |
|---|---|
| `npm-audit` | `package.json` |
| `pip-audit` | `requirements.txt`, `pyproject.toml`, `Pipfile` |
| `cargo-audit` | `Cargo.toml` |

If `scan.tools` includes audit-only tools such as `npm-audit`, `security-scan` skips them and `security-audit` reuses that configuration automatically.

## Threshold enforcement

- `security-scan` aggregates scanner findings and fails when any issue is at or above `scan.failOn`.
- `security-audit` parses supported audit outputs and fails when any issue is at or above `audit.failOn`.
- Unknown severities are surfaced in task data so teams can decide when to tighten command overrides.

## Usage examples

```bash
# Run the configured scanners
architect security-scan

# Audit dependencies in the current project
architect security-audit

# Generate an SPDX SBOM to a custom path
architect security-sbom -- --output reports/security.spdx.json
```

## Local build and test

```bash
cd plugins/security-architected/app
./gradlew test
./gradlew build
```
