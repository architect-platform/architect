# Architect Cloud API

**Shared API types and contracts between the Cloud Backend and Agents.**

## Overview

The `architect-cloud/api` module defines the data-transfer objects, request and
response contracts, and shared constants used by both the Cloud Backend and the
agent modules. Keeping these definitions in a single place ensures that backend
and agents always agree on the wire format.

## Contents

| Area | Description |
|---|---|
| DTOs | Data-transfer objects for engines, projects, executions, and events |
| Enums | Status enumerations (`EngineStatus`, `ExecutionStatus`, …) |
| Constants | Shared API paths, header names, and version identifiers |

## Usage

The API module is consumed as a Gradle project dependency by `architect-cloud/backend`
and any JVM-based agent implementation.

```kotlin
// settings.gradle.kts of a consumer
includeBuild("../api")
```

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":architect-cloud-api"))
}
```

## Building

```bash
cd architect-cloud/api
../../gradlew build
```

> **Note:** The module currently contains only generated Gradle caches and has
> no source code yet. It is scaffolded to become the canonical home for shared
> API types as the cloud platform matures.

## Design Principles

1. **Single source of truth** — every type that crosses a network boundary
   lives here, not in backend or agent code.
2. **No runtime dependencies** — the module should depend only on the Kotlin
   standard library and serialisation annotations.
3. **Backward compatibility** — fields may be added but never removed or
   renamed without a version bump.

## Related Modules

- [Architect Cloud (parent)](../README.md) — umbrella cloud module
- [Cloud Backend](../backend/README.md) — primary consumer of these types
- [Cloud Agents](../agents/README.md) — agent-side consumer
