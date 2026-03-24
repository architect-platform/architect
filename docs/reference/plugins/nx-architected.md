# nx-architected

Nx monorepo tooling integration for Architect.

**Source**: `architect-platform/nx-architected`

## Installation

```yaml
plugins:
  - name: nx-architected
    type: github
    repo: architect-platform/nx-architected
```

## Tasks

Tasks are dynamically generated from the `targets` configuration. Each target produces a task with the ID `nx-<target>`.

| Task ID (example) | Phase | Description |
|------|-------|-------------|
| `nx-build` | `BUILD` | Run `nx run-many --target=build` |
| `nx-test` | `TEST` | Run `nx run-many --target=test` |
| `nx-lint` | `LINT` | Run `nx run-many --target=lint` |
| `nx-affected-build` | Configured | Run `nx affected --target=build` |

## Configuration

Configuration key: `nx`

```yaml
nx-architected:
  targets:
    - name: build
      phase: BUILD
      affected: false
      projects: []          # empty = all projects
    - name: test
      phase: TEST
      affected: true        # run nx affected for this target
      projects: []
    - name: lint
      phase: LINT
      affected: true
      projects: []
  nxArgs:
    - --parallel=4
    - --skip-nx-cache=false
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `targets[].name` | `string` | — | Nx target name (becomes `nx-<name>` task ID) |
| `targets[].phase` | `string` | — | Architect lifecycle phase |
| `targets[].affected` | `boolean` | `false` | Use `nx affected` instead of `nx run-many` |
| `targets[].projects` | `string[]` | `[]` | Scope to specific projects (empty = all) |
| `nxArgs` | `string[]` | `[]` | Global extra arguments appended to all Nx commands |

## Usage examples

```bash
# Run all projects' build target
architect nx-build

# Run affected tests only
architect nx-test
```
