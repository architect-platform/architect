# Architect VS Code Extension

VS Code extension for the [Architect Platform](https://github.com/architect-platform/architect).

## Features

- **YAML Auto-complete & Validation** — `architect.yml` files get schema-driven completion and inline error highlighting via the JSON Schema published by Architect.
- **Task Panel** — sidebar panel listing all inline tasks defined in `architect.yml`, with run and plan buttons.
- **Output Panel** — dedicated output channel showing live task execution output.
- **Commands** — `Architect: Run Task`, `Architect: Plan Task`, `Architect: Validate Config`.

## Prerequisites

- [Red Hat YAML extension](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-yaml) (installed automatically as a dependency)
- `architect` CLI on your PATH (or configure `architect.executablePath`)

## Configuration

| Setting | Default | Description |
|---------|---------|-------------|
| `architect.executablePath` | `architect` | Path to the architect CLI |
| `architect.embedded` | `true` | Use embedded mode (no daemon) |

## Development

```bash
cd architect-vscode
npm install
npm run compile
# Press F5 in VS Code to launch Extension Development Host
```
