# Architect Data

**Execution Tracking and Workflow Monitoring Service**

## Overview

Architect Data is a centralized backend service for tracking and monitoring workflow executions across multiple Architect Engine instances. It provides:

- **Engine Monitoring** - Track engine instances, health, and status
- **Project Tracking** - Monitor projects across all engines
- **Execution History** - Complete execution event tracking
- **Real-time Streaming** - WebSocket-based live updates

## Architecture

Architect Data follows **Clean/Hexagonal Architecture**:

```
┌────────────────────────────────────────────────────┐
│         ADAPTERS (Infrastructure)                   │
│  REST API │ WebSocket │ JDBC Persistence           │
├────────────────────────────────────────────────────┤
│         APPLICATION (Business Logic)                │
│  Services │ Use Cases │ Event Broadcasting         │
├────────────────────────────────────────────────────┤
│         PORTS (Interfaces)                          │
│  Inbound Use Cases │ Outbound Repositories         │
├────────────────────────────────────────────────────┤
│         DOMAIN (Core Business)                      │
│  EngineInstance │ Project │ Execution │ Event     │
└────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Start the Backend

```bash
cd architect-data/backend
./gradlew run
```

The backend starts on **http://localhost:8090**

### 2. Configure Engine to Report

In your engine's `application.yml`:

```yaml
architect:
  data:
    enabled: true
    url: http://localhost:8090
```

### 3. Start Your Engine

```bash
cd architect-engine/engine
./gradlew run
```

The engine will automatically:
- Register itself
- Report project registrations
- Stream execution events
- Send periodic heartbeats

## API Endpoints

### Engine Management

- `POST /api/engines` - Register engine
- `POST /api/engines/heartbeat` - Send heartbeat
- `GET /api/engines` - List all engines
- `GET /api/engines/{id}` - Get engine details
- `GET /api/engines/active` - List active engines

### Project Management

- `POST /api/projects` - Register project
- `GET /api/projects` - List all projects
- `GET /api/projects/{id}` - Get project details
- `GET /api/projects/engine/{engineId}` - Projects by engine

### Execution Tracking

- `POST /api/executions` - Report execution
- `GET /api/executions/{id}` - Get execution
- `GET /api/executions/project/{projectId}` - By project
- `GET /api/executions/engine/{engineId}` - By engine
- `GET /api/executions/recent?limit=50` - Recent executions

### Execution Events

- `POST /api/executions/events` - Report event
- `GET /api/executions/{id}/events` - Get events for execution

### Real-time Streaming

- `ws://localhost:8090/ws/events` - WebSocket for live updates

## Data Model

### EngineInstance
```kotlin
data class EngineInstance(
    val id: String,
    val hostname: String,
    val port: Int,
    val version: String?,
    val status: EngineStatus,  // ACTIVE, INACTIVE, OFFLINE
    val createdAt: Instant,
    val lastHeartbeat: Instant
)
```

### Project
```kotlin
data class Project(
    val id: String,
    val name: String,
    val path: String,
    val engineId: String,
    val description: String?,
    val createdAt: Instant
)
```

### Execution
```kotlin
data class Execution(
    val id: String,
    val projectId: String,
    val engineId: String,
    val taskId: String,
    val status: ExecutionStatus,  // STARTED, RUNNING, COMPLETED, FAILED, SKIPPED
    val message: String?,
    val errorDetails: String?,
    val startedAt: Instant,
    val completedAt: Instant?
)
```

### ExecutionEvent
```kotlin
data class ExecutionEvent(
    val id: String,
    val executionId: String,
    val eventType: String,
    val taskId: String?,
    val message: String?,
    val output: String?,
    val success: Boolean,
    val timestamp: Instant
)
```

## Features

✅ **Engine Monitoring** - Track health and status of engine instances  
✅ **Project Registry** - Central registry of all projects across engines  
✅ **Execution History** - Complete audit trail of workflow executions  
✅ **Real-time Events** - WebSocket streaming for live updates  
✅ **Clean Architecture** - Testable, maintainable, swappable components  
✅ **JDBC Persistence** - H2 (dev), PostgreSQL/MySQL (prod)  

## Configuration

### Database Configuration

Edit `src/main/resources/application.yml`:

```yaml
datasources:
  default:
    url: jdbc:h2:file:./data/architectDataDb  # H2
    # url: jdbc:postgresql://localhost:5432/architect_data  # PostgreSQL
    # url: jdbc:mysql://localhost:3306/architect_data  # MySQL
```

### Server Port

```yaml
micronaut:
  server:
    port: 8090  # Change to your preferred port
```

## Development

### Build

```bash
./gradlew build
```

### Test

```bash
./gradlew test
```

### Run

```bash
./gradlew run
```

## Technology Stack

- **Framework**: Micronaut 4.6
- **Language**: Kotlin 1.9.25
- **Database**: Micronaut Data JDBC
- **Reactive**: Project Reactor
- **WebSocket**: Micronaut WebSocket
- **Architecture**: Clean/Hexagonal

## License

MIT License - see LICENSE for details
