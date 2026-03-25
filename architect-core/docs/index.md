# Architect Core

Shared runtime library for the Architect platform.

## Overview

Architect Core is the central runtime library that powers both the Architect Engine (HTTP server) and the Architect CLI (embedded mode). It implements the shared behaviour that sits between the public API contracts defined in architect-api and the consumer-facing modules.

Every plugin loaded by the platform, every task executed, and every secret resolved passes through architect-core.

## Key Packages

### Plugin Loading

Discovers plugin JARs at runtime, verifies their signatures, and loads them through isolated classloaders. Plugins are resolved from local directories, remote repositories, or inline definitions in `architect.yml`.

### Task Execution & Dependency Resolution

Builds a directed acyclic graph (DAG) from declared task dependencies and phase ordering. Tasks are executed in topological order with support for parallel execution where dependencies allow.

### Secret Resolution

Provides a pluggable provider chain for resolving secrets at task execution time. Built-in providers include:

- `.env` file reader
- Environment variable lookup
- HashiCorp Vault adapter
- Cloud provider adapters (AWS Secrets Manager, GCP Secret Manager)

### Configuration Parsing

Reads and validates `architect.yml` project files into a typed model. Supports nested subproject configurations and plugin-specific context blocks.

### Classloader Infrastructure

Maintains isolated classloaders per plugin so that conflicting transitive dependencies do not interfere with each other or with the host runtime.

### Built-in Workflows

Ships the default workflow definitions consumed by the engine and CLI:

- **CoreWorkflow** — init → lint → verify → build → run/test → release → publish
- **CodeWorkflow** — code-specific sub-phases mapped to core phases
- **HooksWorkflow** — Git hook phases (pre-commit, pre-push, commit-msg)

## Getting Started

### Prerequisites

- JDK 17+
- Gradle 8.x (via wrapper)

### Building

```bash
cd architect-core
architect gradle-build
```

### Testing

```bash
cd architect-core
architect gradle-test
```

## Configuration

Architect Core reads project configuration from `architect.yml`. The core module is registered as a Gradle project in the root configuration:

```yaml
gradle:
  projects:
    - name: architect-core
      path: architect-core/core
```

## Architecture

Architect Core depends on **architect-api** and is consumed by **architect-engine** and **architect-cli**. See the [architecture documentation](../../docs/) for the full platform overview.

## Links

- [Architecture Documentation](../../docs/)
- [STATUS.md](../../STATUS.md)
- [Architect Platform Docs](https://architect-platform.github.io/architect/)
