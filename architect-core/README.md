# Architect Core

The Architect Core is the shared runtime library for the Architect platform. It provides plugin loading, task execution, dependency resolution, secret management, project configuration parsing, and classloader infrastructure used by both the engine (HTTP server) and the CLI (embedded mode).

## Overview

Architect Core sits between the API layer (architect-api) and the consumer modules (architect-engine, architect-cli). It implements the runtime behaviour that both consumers share: loading plugins from JARs, resolving task dependency graphs, executing tasks in the correct order, parsing `architect.yml` project files, and managing secrets from multiple sources.

## Key Responsibilities

- **Plugin Loading & Signature Verification** — Discovers, validates, and loads plugin JARs at runtime using a custom classloader infrastructure.
- **Task Execution & Dependency Resolution** — Builds a directed acyclic graph of tasks and executes them respecting declared dependencies and phase ordering.
- **Project Loading & Configuration Parsing** — Reads `architect.yml` files and produces a typed project model that plugins and services consume.
- **Secret Resolution** — Resolves secrets from `.env` files, HashiCorp Vault, cloud provider adapters, and environment variables with a pluggable provider chain.
- **Built-in Workflows & Inline Plugins** — Ships the default workflow definitions (core, code, hooks) and a small set of inline plugins that require no external JARs.
- **Classloader Infrastructure** — Provides isolated classloaders so that plugins with conflicting dependencies can coexist in the same JVM.

## Architecture

```
┌─────────────────────────────────────────┐
│        architect-engine / cli           │
│        (consumers)                      │
└──────────────┬──────────────────────────┘
               │
┌──────────────┴──────────────────────────┐
│          architect-core                 │
│  Plugin loading, task execution,        │
│  secrets, config parsing, classloaders  │
└──────────────┬──────────────────────────┘
               │
┌──────────────┴──────────────────────────┐
│          architect-api                  │
│  Interfaces & contracts                 │
└─────────────────────────────────────────┘
```

Architect Core depends on **architect-api** for all public interfaces and is consumed by **architect-engine** (the long-lived Micronaut server) and **architect-cli** (the user-facing command-line tool).

## Prerequisites

- Java 17 or higher
- Gradle 8.x (wrapper included)

## Building

```bash
cd core
./gradlew build
```

To build without tests:

```bash
./gradlew build -x test
```

## Testing

Run the test suite:

```bash
cd core
./gradlew test
```

## Project Structure

```
core/
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── io/github/architectplatform/core/
│       │       ├── classloader/        # Classloader infrastructure
│       │       ├── config/             # Project configuration parsing
│       │       ├── execution/          # Task execution & dependency resolution
│       │       ├── plugin/             # Plugin loading & verification
│       │       ├── secrets/            # Secret resolution providers
│       │       └── workflow/           # Built-in workflow definitions
│       └── resources/
├── build.gradle.kts
└── settings.gradle.kts
```

## Further Reading

- [Architecture Documentation](../docs/)
- [Project Status](../STATUS.md)
- [Root README](../README.md)

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](../LICENSE) file for details.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

## Links

- [Documentation](docs/)
- [Status](../STATUS.md)
- [Architect Platform Docs](https://architect-platform.github.io/architect/)
