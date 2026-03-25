# javascript-architected

> **Status**: Experimental (incubating tier) — see [STATUS.md](STATUS.md)

## Overview

javascript-architected integrates JavaScript/Node.js toolchain into the Architect task lifecycle, providing tasks for building, testing, and linting npm/yarn/pnpm projects. It enables teams to manage JavaScript development through standardized Architect commands.

## Tasks

| Task ID | Description |
|---|---|
| `javascript-install` | Install dependencies with the configured package manager |
| `javascript-build` | Run the project build script |
| `javascript-test` | Run the test script |
| `javascript-lint` | Run the lint script |
| `javascript-dev` | Start the development script |

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: javascript-architected
    repo: architect-platform/javascript-architected

javascript:
  packageManager: npm
  workingDirectory: .
```

## Local Build and Test

```bash
cd plugins/javascript-architected/app
./gradlew build
./gradlew test
```

## Limitations

This plugin is at incubating tier. Tests and documentation are still below the active plugin standard, and behavior needs broader verification across npm, yarn, and pnpm workflows. See [STATUS.md](STATUS.md) for graduation criteria.
