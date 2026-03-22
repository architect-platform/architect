# Architect IntelliJ Plugin

IntelliJ IDEA plugin for the [Architect Platform](https://github.com/architect-platform/architect).

## Features

- **JSON Schema Association** — `architect.yml` and `architect.yaml` files get auto-complete and inline validation using the Architect JSON Schema.
- **Run Configurations** — create "Architect Task" run configurations to execute tasks from the IDE.
- **Gutter Icons** — inline tasks defined under `tasks:` in `architect.yml` show a run icon in the gutter for one-click execution.

## Prerequisites

- IntelliJ IDEA 2024.1+
- YAML plugin (bundled)
- `architect` CLI on your PATH

## Development

```bash
cd architect-intellij
./gradlew buildPlugin    # Build the plugin
./gradlew runIde          # Launch a sandbox IDE with the plugin installed
```
