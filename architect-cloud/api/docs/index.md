# Architect Cloud API

## Overview

The cloud API module defines shared types and contracts used between the
cloud backend and remote agents. It provides DTOs, request/response models,
and protocol definitions.

This module serves as the single source of truth for all data structures
exchanged over the wire, ensuring type-safe communication between cloud
components.

## Getting Started

### Prerequisites

- JDK 17+
- Gradle 8.x (via wrapper)

### Building

```bash
cd architect-cloud/api
./gradlew build
```

### Testing

```bash
cd architect-cloud/api
./gradlew test
```

## Configuration

The cloud API module is a library dependency — it does not have its own
runtime configuration. It is consumed by the cloud backend and agents
modules. See the umbrella `architect.yml` for overall cloud configuration:

```yaml
# The cloud API module is included as a dependency of architect-cloud-backend
# in the root architect.yml gradle configuration.
gradle:
  projects:
    - name: architect-cloud-backend
      path: architect-cloud/backend
```

## Status

This module is **incubating**. Type definitions are scaffolded but not yet
fully specified.

## Links

- [Cloud README](../../README.md) — umbrella module overview
- [STATUS.md](../STATUS.md) — current support level and graduation criteria
- [Architect Platform Docs](https://architect-platform.github.io/architect/)
