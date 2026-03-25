# Architect VS Code Extension

VS Code extension providing first-class support for the Architect platform.

## Overview

The Architect VS Code extension brings the Architect workflow directly into your
editor. It activates automatically when an `architect.yml` or `architect.yaml`
file is detected in the workspace.

### Key Features

- **YAML Schema Validation** — auto-complete and inline diagnostics for
  `architect.yml` powered by the published JSON Schema.
- **Task Tree View** — an activity-bar panel listing every task defined in the
  project, with one-click run and plan actions.
- **Task Execution** — run or plan any task from the command palette or the tree
  view; output streams into the integrated terminal.
- **Graph Visualisation** — display the task dependency graph for the current
  project.

## Installation

The extension is not yet published to the VS Code Marketplace. To run it
locally:

```bash
cd architect-vscode
npm install
npm run compile
# Press F5 in VS Code to launch the Extension Development Host
```

## Configuration

| Setting | Default | Description |
|---|---|---|
| `architect.executablePath` | `architect` | Path to the Architect CLI |
| `architect.embedded` | `true` | Use embedded mode (no daemon) |

## Development

```bash
npm run watch   # incremental compilation
npm run test    # run test suite
```

## Status

**Incubating** — thin reference integration demonstrating IDE capabilities.
Not a supported product; see `STATUS.md` at the repository root.

## Links

- [VS Code Extension README](../README.md)
- [STATUS.md](../../STATUS.md)
- [Architect Platform Docs](https://architect-platform.github.io/architect/)
