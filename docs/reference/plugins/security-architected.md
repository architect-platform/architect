# security-architected

Security scanning, dependency auditing, and SBOM generation for Architect projects.

**Source**: `architect-platform/security-architected`

## Installation

```yaml
plugins:
  - name: security-architected
    type: github
    repo: architect-platform/security-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `security-scan` | `VERIFY` | Run configured scanners such as Trivy, Snyk, and CodeQL |
| `security-audit` | `VERIFY` | Audit npm, Python, and Cargo dependencies |
| `security-sbom` | `VERIFY` | Generate a CycloneDX or SPDX SBOM |

## Configuration

Configuration key: `security`

```yaml
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
|-------|------|---------|-------------|
| `enabled` | `boolean` | `true` | Enable or disable the plugin |
| `workingDirectory` | `string` | `.` | Directory used for detection and command execution |
| `scan.enabled` | `boolean` | `true` | Enable `security-scan` |
| `scan.tools` | `string[]` | `[trivy]` | Scanner selection. Audit-only tools are delegated to `security-audit`. |
| `scan.failOn` | `string` | `critical` | Fail threshold for scanner findings |
| `scan.codeqlDatabase` | `string` | `null` | CodeQL database path |
| `scan.codeqlQuerySuite` | `string` | `null` | CodeQL query suite |
| `scan.codeqlOutput` | `string` | `build/reports/security/codeql.sarif` | SARIF output path |
| `audit.enabled` | `boolean` | `true` | Enable `security-audit` |
| `audit.tools` | `string[]` | `[]` | Explicit audit tool selection |
| `audit.failOn` | `string` | `high` | Fail threshold for dependency advisories |
| `sbom.enabled` | `boolean` | `true` | Enable `security-sbom` |
| `sbom.format` | `string` | `cyclonedx` | SBOM format |
| `sbom.output` | `string` | `sbom.json` | Output path |
| `sbom.tool` | `string` | `trivy` | SBOM generator |

## Detection and integrations

| Integration | Behavior |
|-------------|----------|
| Trivy | Filesystem scanning and SBOM generation |
| Snyk | `snyk test --json` with severity threshold tracking |
| CodeQL | SARIF analysis when a database + query suite are configured |
| npm audit | Auto-detected from `package.json` |
| pip-audit | Auto-detected from `requirements.txt`, `pyproject.toml`, or `Pipfile` |
| cargo audit | Auto-detected from `Cargo.toml` |

## Behavior notes

- `security-audit` auto-detects supported ecosystems when `audit.tools` is empty.
- `security-scan` skips audit-only tools listed in `scan.tools` and reports that delegation in task data.
- The plugin returns structured severity counts in `TaskResult.data` to support downstream automation.

## Usage examples

```bash
# Scan the project filesystem
architect security-scan

# Audit dependencies with the configured threshold
architect security-audit

# Generate an SPDX SBOM
architect security-sbom
```
