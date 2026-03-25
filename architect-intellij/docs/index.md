# Architect IntelliJ Plugin

IntelliJ IDEA plugin providing Architect platform integration.

## Overview

The Architect IntelliJ plugin adds Architect awareness to IntelliJ-based IDEs
(IntelliJ IDEA, WebStorm, etc.). It targets IntelliJ 2024.1+ and requires the
bundled YAML plugin.

### Key Features

- **Schema Association** — automatic JSON Schema association for
  `architect.yml` / `architect.yaml` files, enabling auto-complete and
  validation inside the IDE.
- **Task Line Markers** — gutter icons on task definitions that let you run or
  plan a task directly from the editor.

## Installation

The plugin is not yet published to the JetBrains Marketplace. To build and
install locally:

```bash
cd architect-intellij
./gradlew buildPlugin
# Install the ZIP from build/distributions/ via Settings → Plugins → ⚙ → Install from Disk
```

## Development

```bash
./gradlew runIde          # launch a sandboxed IDE with the plugin loaded
./gradlew test            # run plugin tests
./gradlew buildPlugin     # produce distributable ZIP
```

The plugin targets JDK 17 and Kotlin 1.9.

## Configuration

The plugin associates `architect.yml` / `architect.yaml` files with the
Architect JSON Schema automatically. No additional IDE configuration is
required beyond having the `architect` CLI on your PATH.

## Status

**Incubating** — thin reference integration demonstrating IDE capabilities.
Not a supported product; see `STATUS.md` at the repository root.

## Links

- [IntelliJ Plugin README](../README.md)
- [STATUS.md](../../STATUS.md)
- [Architect Platform Docs](https://architect-platform.github.io/architect/)
