# scripts-architected

Custom shell script integration for Architect.

**Source**: `architect-platform/scripts-architected`

## Installation

```yaml
plugins:
  - name: scripts-architected
    type: github
    repo: architect-platform/scripts-architected
```

## Tasks

Tasks are generated from the `scripts` configuration. Each script entry becomes an Architect task.

## Configuration

Configuration key: `scripts`

```yaml
scripts-architected:
  scripts:
    - id: generate-swagger
      description: "Generate OpenAPI spec"
      command: ./scripts/generate-swagger.sh
      phase: BUILD
      workingDirectory: .
      env:
        OUTPUT_DIR: build/swagger

    - id: db-migrate
      description: "Run database migrations"
      command: ./scripts/migrate.sh
      phase: INIT

    - id: smoke-test
      description: "Post-deploy smoke test"
      command: ./scripts/smoke-test.sh
      phase: VERIFY
      dependsOn:
        - k8s-apply
```

### `scripts.scripts[]`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `string` | ✅ | Task ID (used as `architect <id>`) |
| `description` | `string` | | Human-readable description |
| `command` | `string` | ✅ | Shell command to execute |
| `phase` | `string` | | Lifecycle phase |
| `workingDirectory` | `string` | `.` | Working directory for the command |
| `env` | `map<string,string>` | `{}` | Environment variables for this script |
| `dependsOn` | `string[]` | `[]` | Task IDs that must complete first |

## Usage examples

```bash
# Run a specific script task
architect generate-swagger

# Run as part of BUILD phase
architect --phase BUILD

# Run with custom args
architect smoke-test -- --env staging
```
