# python-architected

Python project lifecycle integration for Architect.

**Source**: `architect-platform/python-architected`

## Installation

```yaml
plugins:
  - name: python-architected
    type: github
    repo: architect-platform/python-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `py-install` | `INIT` | Install dependencies (`pip install -r requirements.txt`) |
| `py-lint` | `LINT` | Run linter (configurable: flake8, pylint, ruff) |
| `py-test` | `TEST` | Run test suite (configurable: pytest, unittest) |
| `py-build` | `BUILD` | Build distribution packages |
| `py-publish` | `PUBLISH` | Upload to PyPI or a private registry |

## Configuration

Configuration key: `python`

```yaml
python-architected:
  pythonPath: python3
  pipPath: pip3
  requirementsFile: requirements.txt
  linter: ruff
  testRunner: pytest
  testArgs:
    - --cov=src
    - --cov-report=xml
  buildBackend: hatch    # hatch | flit | setuptools | poetry
  publishRepository: pypi
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `pythonPath` | `string` | `python3` | Python executable |
| `pipPath` | `string` | `pip3` | pip executable |
| `requirementsFile` | `string` | `requirements.txt` | Requirements file for `py-install` |
| `linter` | `string` | `flake8` | Linter: `flake8`, `pylint`, `ruff`, `mypy` |
| `testRunner` | `string` | `pytest` | Test runner: `pytest` or `unittest` |
| `testArgs` | `string[]` | `[]` | Extra arguments forwarded to the test runner |
| `buildBackend` | `string` | `setuptools` | Build backend: `setuptools`, `hatch`, `flit`, `poetry` |
| `publishRepository` | `string` | `pypi` | Upload target (PyPI index name or URL) |

## Usage examples

```bash
# Install dependencies
architect py-install

# Lint
architect py-lint

# Test with coverage
architect py-test

# Build wheel and sdist
architect py-build

# Publish to PyPI
architect py-publish
```
