# Architect Cloud Backend

The centralized backend service for tracking and managing multiple Architect Engine instances.

## Overview

The Architect Cloud Backend provides a RESTful API that allows multiple architect engine instances to:
- Register themselves with unique identifiers
- Report project information including name, path, and metadata
- Stream execution events and task status updates
- Track outputs and logs for all executions

## Architecture

The backend uses:
- **Micronaut Framework** - Lightweight, fast JVM-based framework
- **Kotlin** - Modern, concise programming language
- **Micronaut Data JDBC** - Simplified data access layer
- **H2 Database** - In-memory database for development (configurable for production)

## API Endpoints

### Engine Management

- `POST /api/engines` - Register a new engine instance
- `POST /api/engines/heartbeat` - Send heartbeat to keep engine active
- `GET /api/engines/{engineId}` - Get engine details
- `GET /api/engines` - List all engines
- `GET /api/engines/active` - List active engines

### Project Management

- `POST /api/projects` - Register a new project
- `GET /api/projects/{projectId}` - Get project details
- `GET /api/projects` - List all projects
- `GET /api/projects/engine/{engineId}` - List projects for a specific engine

### Execution Management

- `POST /api/executions` - Report execution status
- `GET /api/executions/{executionId}` - Get execution details
- `GET /api/executions/project/{projectId}` - List executions for a project
- `GET /api/executions/engine/{engineId}` - List executions for an engine
- `POST /api/executions/events` - Report an execution event
- `GET /api/executions/{executionId}/events` - Get events for an execution

## Data Model

### EngineInstance
- `id`: Unique engine identifier
- `hostname`: Engine hostname
- `port`: Engine port
- `version`: Engine version
- `status`: ACTIVE | INACTIVE | OFFLINE
- `createdAt`: Registration timestamp
- `lastHeartbeat`: Last heartbeat timestamp

### Project
- `id`: Unique project identifier
- `name`: Project name
- `path`: Project file system path
- `engineId`: Associated engine ID
- `description`: Optional description
- `createdAt`: Registration timestamp

### Execution
- `id`: Unique execution identifier
- `projectId`: Associated project ID
- `engineId`: Associated engine ID
- `taskId`: Task being executed
- `status`: STARTED | RUNNING | COMPLETED | FAILED | SKIPPED
- `message`: Status message
- `errorDetails`: Error information if failed
- `startedAt`: Execution start time
- `completedAt`: Execution completion time

### ExecutionEvent
- `id`: Unique event identifier
- `executionId`: Associated execution ID
- `eventType`: Event type (e.g., task.started, task.completed)
- `taskId`: Associated task ID
- `message`: Event message
- `output`: Task output/logs
- `success`: Boolean success flag
- `timestamp`: Event timestamp

## Running the Application

### Development

```bash
./gradlew run
```

The application will start on port 8080.

### Testing

```bash
./gradlew test
```

### Building

```bash
./gradlew build
```

## Configuration

Configuration is in `src/main/resources/application.yml`:

```yaml
micronaut:
  server:
    port: 8080

datasources:
  default:
    url: jdbc:h2:mem:architectCloudDb
    driverClassName: org.h2.Driver
```

For production, configure a persistent database like PostgreSQL or MySQL.

## Example Usage

### Register an Engine

```bash
curl -X POST http://localhost:8080/api/engines \
  -H "Content-Type: application/json" \
  -d '{
    "id": "engine-001",
    "hostname": "localhost",
    "port": 9292,
    "version": "1.3.0"
  }'
```

### Register a Project

```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{
    "id": "proj-001",
    "name": "my-project",
    "path": "/path/to/project",
    "engineId": "engine-001",
    "description": "My awesome project"
  }'
```

### Report an Execution

```bash
curl -X POST http://localhost:8080/api/executions \
  -H "Content-Type: application/json" \
  -d '{
    "id": "exec-001",
    "projectId": "proj-001",
    "engineId": "engine-001",
    "taskId": "build-task",
    "status": "STARTED",
    "message": "Build started"
  }'
```

### Report an Event

```bash
curl -X POST http://localhost:8080/api/executions/events \
  -H "Content-Type: application/json" \
  -d '{
    "id": "event-001",
    "executionId": "exec-001",
    "eventType": "task.output",
    "taskId": "build-task",
    "output": "Building project...",
    "success": true
  }'
```

## Integration with Architect Engine

The Architect Engine will be updated to automatically report to this cloud backend:
- Register itself on startup
- Report all project registrations
- Stream execution events in real-time
- Send periodic heartbeats

## Next Steps

1. Integrate with Architect Engine
2. Build UI for visualization
3. Add authentication/authorization
4. Implement WebSocket support for real-time updates
5. Add metrics and analytics
