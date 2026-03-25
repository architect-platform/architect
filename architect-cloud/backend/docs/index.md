# Architect Cloud Backend

## Overview

The Architect Cloud Backend is a Micronaut-based REST API service written in
Kotlin. It follows a **hexagonal architecture** (ports and adapters) with
ArchUnit rules that enforce layer boundaries at compile time.

The backend acts as the central coordination point for the Architect Cloud
platform: engine instances register themselves, report project metadata, and
stream execution events. The UI dashboard and any external tooling consume
the resulting query APIs.

## Key Features

- **Engine management** — registration, heartbeat monitoring, status tracking
- **Project tracking** — multi-engine project visibility and metadata storage
- **Execution monitoring** — real-time event ingestion, task status, output/log collection
- **Hexagonal architecture** — domain logic is isolated from framework and persistence concerns
- **ArchUnit enforcement** — automated architecture tests validate layer dependencies

## Technology Stack

| Layer | Technology |
|---|---|
| Framework | Micronaut 4.x |
| Language | Kotlin (JVM 17+) |
| Persistence | Micronaut Data JDBC, H2 (dev) / PostgreSQL (prod) |
| Build | Gradle (Kotlin DSL) |
| Tests | JUnit 5, Micronaut Test, ArchUnit |

## Build & Test

```bash
# Run in development mode
./gradlew run            # starts on http://localhost:8080

# Run the test suite
./gradlew test           # 57 tests — unit + integration + ArchUnit

# Produce a distributable JAR
./gradlew build
```

## Project Layout

```
backend/
├── src/main/kotlin/     # Application source (domain, ports, adapters, controllers)
├── src/main/resources/  # application.yml, Flyway migrations
├── src/test/kotlin/     # Unit, integration, and architecture tests
├── build.gradle.kts     # Build configuration
└── settings.gradle.kts  # Composite-build wiring
```

## Further Reading

- [Backend README](../README.md) — API endpoints, data model, example usage
- [Cloud README](../../README.md) — umbrella module overview
- [STATUS.md](../STATUS.md) — current support level and graduation criteria
