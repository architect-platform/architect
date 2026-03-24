# pipelines-architected

Composite pipeline definitions and orchestration for Architect.

**Source**: `architect-platform/pipelines-architected`

## Installation

```yaml
plugins:
  - name: pipelines-architected
    type: github
    repo: architect-platform/pipelines-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `pipelines-init` | `INIT` | Load and validate pipeline definitions |
| `pipelines-list` | `BUILD` | List all registered pipelines |
| `pipelines-execute` | `BUILD` | Execute a named pipeline |
| `pipelines-<name>` | Configured | One task per declared pipeline |

## Configuration

Configuration key: `pipelines`

```yaml
pipelines-architected:
  pipelines:
    - name: full-ci
      description: "Complete CI pipeline"
      steps:
        - task: gradle-build
        - task: gradle-test
          dependsOn: [gradle-build]
        - task: docker-build
          dependsOn: [gradle-test]
        - task: docker-push
          dependsOn: [docker-build]

    - name: deploy-prod
      description: "Deploy to production"
      steps:
        - task: k8s-apply
        - task: k8s-rollout
          dependsOn: [k8s-apply]
```

### `pipelines.pipelines[]`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | `string` | ✅ | Pipeline identifier (becomes `pipelines-<name>` task) |
| `description` | `string` | | Human-readable description |
| `steps` | `object[]` | ✅ | Ordered list of pipeline steps |

### `pipelines.pipelines[].steps[]`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `task` | `string` | ✅ | Task ID to execute |
| `dependsOn` | `string[]` | | Step-level dependencies (within this pipeline) |
| `condition` | `string` | | Condition expression for conditional step |

## Usage examples

```bash
# Execute a named pipeline
architect pipelines-full-ci

# List all pipelines
architect pipelines-list

# Execute interactively
architect pipelines-execute -- --pipeline deploy-prod
```
