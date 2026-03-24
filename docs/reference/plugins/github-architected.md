# github-architected

GitHub release automation and CI/CD integration for Architect.

**Source**: `architect-platform/github-architected`

## Installation

```yaml
plugins:
  - name: github-architected
    type: github
    repo: architect-platform/github-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `github-init-dependencies` | `INIT` | Validate required GitHub environment variables |
| `github-init-pipelines` | `INIT` | Sync pipeline definitions to `.github/workflows/` |
| `github-release-task` | `RELEASE` | Create a GitHub Release with changelog |

## Configuration

Configuration key: `github`

```yaml
github-architected:
  owner: my-org
  repo: my-app
  token: ${GITHUB_TOKEN}
  releasePrefix: "v"
  changelog:
    enabled: true
    sections:
      - feat
      - fix
      - perf
      - docs
  pipelines:
    sync: true
    directory: .github/workflows
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `owner` | `string` | — | GitHub organisation or user |
| `repo` | `string` | — | Repository name |
| `token` | `string` | `$GITHUB_TOKEN` | GitHub API token (use env var reference) |
| `releasePrefix` | `string` | `v` | Tag prefix (e.g. `v` gives `v1.2.3`) |
| `changelog.enabled` | `boolean` | `true` | Generate changelog from conventional commits |
| `changelog.sections` | `string[]` | `[feat, fix, perf]` | Conventional commit types to include |
| `pipelines.sync` | `boolean` | `false` | Sync pipeline templates on INIT |
| `pipelines.directory` | `string` | `.github/workflows` | Output directory for synced pipelines |

## Usage examples

```bash
# Create a GitHub release for the current tag
architect github-release-task

# Sync pipeline templates
architect github-init-pipelines
```

## Required environment variables

| Variable | Description |
|----------|-------------|
| `GITHUB_TOKEN` | Personal access token or `GITHUB_TOKEN` in Actions |
