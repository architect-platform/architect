# python-architected

> **Status**: Experimental (incubating tier) — see [STATUS.md](STATUS.md)

Python workflow automation for Architect projects (supports uv, pip, pytest, ruff).

## Tasks

| Task ID | Description |
|---|---|
| `py-install` | Install dependencies using the configured tool |
| `py-lint` | Run linter (default: ruff) |
| `py-test` | Run test suite (default: pytest) |
| `py-build` | Build distribution artifacts |
| `py-publish` | Publish the package to PyPI |

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: python-architected
    repo: architect-platform/python-architected

python:
  tool: uv
  pythonVersion: "3.12"
  testRunner: pytest
  linter: ruff
```

## Local Build and Test

```bash
cd plugins/python-architected/app
gradle build
gradle test
```

## Limitations

This plugin is at incubating tier. Tests and documentation are minimal. See [STATUS.md](STATUS.md) for graduation criteria.
