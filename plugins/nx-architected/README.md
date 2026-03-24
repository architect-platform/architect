# nx-architected

> **Status**: Experimental (incubating tier) — see [STATUS.md](STATUS.md)

Nx monorepo workflow integration for Architect projects.

## Tasks

Tasks are registered dynamically from the `targets` list in configuration. For each declared target, two tasks are created:

| Task ID pattern | Description |
|---|---|
| `nx-<target>` | Run the Nx target across all projects |
| `nx-<target>` (affected) | Run the Nx target only for affected projects (when `affected: true`) |

Example tasks with `targets: [build, test, lint]`:
- `nx-build`, `nx-test`, `nx-lint`

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: nx-architected
    repo: architect-platform/nx-architected

nx:
  targets:
    - build
    - test
    - lint
  parallel: 3
  affected: false
  nxCloud: false
```

## Local Build and Test

```bash
cd plugins/nx-architected/app
gradle build
gradle test
```

## Limitations

This plugin is at incubating tier. Tests and documentation are minimal. See [STATUS.md](STATUS.md) for graduation criteria.
