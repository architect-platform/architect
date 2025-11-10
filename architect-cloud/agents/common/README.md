# Agent Common Module

Shared code and utilities for all Architect Cloud agents.

## Overview

This module provides common functionality used by all agent types:

- **Domain Models** - Shared data structures (AgentConfig, DeploymentCommand, etc.)
- **Template Rendering** - Jinja2-based template rendering service
- **Server Communication** - HTTP client for communicating with Architect Cloud backend
- **Validation** - Configuration validation utilities

## Components

### AgentConfig

Common configuration for all agents:

```kotlin
data class AgentConfig(
    val id: String,
    val agentType: AgentType,
    val serverUrl: String,
    val supportedEnvironments: List<String>,
    val cloudProvider: String?,
    val region: String?,
    val heartbeatIntervalSeconds: Int,
    val capabilities: List<String>,
    val metadata: Map<String, String>
)
```

### TemplateRenderingService

Jinja2 template rendering:

```kotlin
val service = TemplateRenderingService()
val rendered = service.renderTemplate(template, variables)
val merged = service.renderAndMerge(templates, variables, "---")
```

### ServerCommunicationService

Backend communication:

```kotlin
val service = ServerCommunicationService(httpClient, config)

// Register agent
service.registerAgent()

// Send heartbeat
service.sendHeartbeat()

// Poll for commands
val commands = service.pollDeploymentCommands()

// Report results
service.reportDeploymentResult(result)
```

## Usage

Add as a dependency in your agent:

```kotlin
dependencies {
    implementation(project(":agents:common"))
}
```

Then use the common services in your agent implementation.

## Building

```bash
./gradlew build
```

## Testing

```bash
./gradlew test
```
