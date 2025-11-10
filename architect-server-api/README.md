# Architect Server API

**Shared API definitions for Architect Server and Agent communication.**

## Overview

This module provides a common API contract that is shared between:
- **Architect Server** (Control Plane)
- **Architect Agents** (Kubernetes, Docker, Cloud Run, etc.)

By defining the API in a separate module, we ensure:
- **Type Safety**: Both server and agents use the same data structures
- **Consistency**: API changes are automatically reflected in all consumers
- **Versioning**: Clear API versioning strategy
- **Reusability**: Multiple agent types can implement the same contract

## Contents

### Agent Communication

- **`AgentRegistration.kt`**: Agent registration and heartbeat DTOs
- **`RegisterAgentRequest`**: Request to register a new agent
- **`AgentHeartbeatRequest`**: Periodic heartbeat from agent
- **`AgentType`**: Supported agent types (Kubernetes, Docker, etc.)
- **`AgentStatus`**: Agent health status

### Deployment

- **`DeploymentCommand.kt`**: Deployment command and result DTOs
- **`DeploymentCommandDTO`**: Command sent from server to agent
- **`DeploymentResultDTO`**: Result reported back from agent
- **`DeploymentOperation`**: Operation types (APPLY, DELETE, UPDATE, ROLLBACK)
- **`DeploymentStatusDTO`**: Deployment status

### Resources

- **`ResourceDefinition.kt`**: Application resource definitions
- **`ResourceDefinitionDTO`**: Defines a Kubernetes application
- **`PortDTO`**: Port configuration
- **`ResourceRequirementsDTO`**: CPU/Memory requirements

### Templates

- **`Template.kt`**: Kubernetes YAML template definitions
- **`TemplateDTO`**: Template with variables
- **`TemplateVariableDTO`**: Template variable definition
- **`TemplateType`**: Template types (Deployment, Service, etc.)

### API Contract

- **`ApiEndpoints.kt`**: Centralized endpoint definitions
- **`ApiHeaders.kt`**: HTTP header constants
- **`ApiVersions.kt`**: API version constants

## Usage

### In Architect Server

Add dependency to `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":architect-server-api"))
}
```

Use in controllers:

```kotlin
@Controller(ApiEndpoints.Agents.BASE)
class AgentController {
    @Post(ApiEndpoints.Agents.REGISTER)
    fun register(@Body request: RegisterAgentRequest): RegisterAgentResponse {
        // Implementation
    }
}
```

### In Architect Agents

Add dependency to `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.architectplatform:architect-server-api:1.0.0")
}
```

Use in HTTP clients:

```kotlin
val request = RegisterAgentRequest(
    agentId = "agent-001",
    agentType = AgentType.KUBERNETES,
    capabilities = listOf("template-rendering", "namespace-isolation")
)

httpClient.post(ApiEndpoints.Agents.REGISTER, request)
```

## Versioning

The API follows semantic versioning:
- **Major version**: Breaking changes
- **Minor version**: Backward-compatible additions
- **Patch version**: Bug fixes

Current version: **1.0.0**

## Building

```bash
./gradlew build
```

## Publishing

```bash
./gradlew publishToMavenLocal
```

## License

MIT License - See main project LICENSE file.
