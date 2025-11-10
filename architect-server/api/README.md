# Architect Cloud API

## Overview

This module contains the **shared API contract** between the Architect Server and platform-specific agents (Kubernetes, Docker Compose, etc.). It provides type-safe DTOs for communication and ensures consistency across the distributed system.

## Purpose

The API module serves as a:
- **Contract**: Defines the interface between server and agents
- **Type Safety**: Ensures compile-time validation of data structures
- **Platform Agnostic**: No platform-specific concepts (Kubernetes, Docker, etc.)
- **Shared Library**: Used by both server and all agent implementations

## Key Components

### 1. Application Definition DTO

Platform-agnostic representation of a deployable application with dependency support.

**Key Features:**
- Simple, clean field names
- Support for APPLICATION, DATABASE, MESSAGE_BROKER, CACHE, STORAGE, SERVICE types
- First-class dependency management
- Platform-agnostic port and resource definitions

### 2. Dependency Management

Applications can declare dependencies on databases, brokers, and other services:
- **Required dependencies**: Must be deployed and healthy before the application
- **Connection info**: Pass connection details between services
- **Automatic ordering**: Server orders dependencies for deployment

### 3. Agent Communication

- Agent registration and heartbeat
- Deployment command polling
- Result reporting
- Real-time status updates

## Design Principles

### Platform Agnostic

**Removed Kubernetes-specific concepts:**
- ❌ `namespace`
- ❌ `containerPort` vs `servicePort`
- ❌ `labels` and `annotations`
- ❌ `replicas`

**Use generic concepts instead:**
- ✅ `instances` (number of replicas)
- ✅ `port` (single port number)
- ✅ `metadata` (generic key-value)
- ✅ `public` flag (expose externally vs internally)

### Simple and Clean

- Minimal required fields
- Sensible defaults
- Clear, intuitive naming
- Self-documenting structure

## Building

```bash
./gradlew build
```

## Usage

### In Server

```kotlin
dependencies {
    implementation(project(":architect-cloud-api"))
}
```

### In Agents

```kotlin
dependencies {
    implementation("io.github.architectplatform:architect-cloud-api:1.0.0")
}
```

## Examples

See `PLATFORM_AGNOSTIC_API.md` in the project root for comprehensive examples.

## License

MIT License - See main project LICENSE file.
