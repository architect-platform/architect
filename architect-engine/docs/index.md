# Architect Engine

RESTful API server for managing task execution and project orchestration.

## Overview

The Architect Engine is the core execution environment for the Architect platform. It provides a REST API for:
- Project registration and management
- Task discovery and execution
- Real-time execution monitoring via Server-Sent Events (SSE)
- Plugin loading and lifecycle management

## Architecture

```
┌─────────────────────────────────────────┐
│         REST API Layer                  │
│  (Controllers + Endpoints)              │
└──────────────┬──────────────────────────┘
               │
┌──────────────┴──────────────────────────┐
│        Application Layer                │
│  (Services + Business Logic)            │
└──────────────┬──────────────────────────┘
               │
┌──────────────┴──────────────────────────┐
│         Domain Layer                    │
│  (Core Models + Interfaces)             │
└──────────────┬──────────────────────────┘
               │
┌──────────────┴──────────────────────────┐
│      Infrastructure Layer               │
│  (Persistence + External Services)      │
└─────────────────────────────────────────┘
```

## Key Features

### 1. Project Management

- Register projects from Git repositories
- Load and parse architect.yml configuration
- Manage project lifecycle and state
- Cache project information for performance

### 2. Task Execution

- Discover tasks from loaded plugins
- Resolve task dependencies
- Execute tasks with proper isolation
- Handle task failures and retries

### 3. Plugin System

- Dynamic plugin loading from JARs
- Service Provider Interface (SPI) based discovery
- Plugin context injection and management
- Lifecycle hooks (init, register, cleanup)

### 4. Event Streaming

- Real-time execution events via SSE
- Task progress updates
- Error and warning notifications
- Completion status

## REST API

### Projects

#### List Projects

```http
GET /api/projects
```

**Response:**
```json
{
  "projects": [
    {
      "name": "my-project",
      "description": "Project description",
      "path": "/path/to/project"
    }
  ]
}
```

#### Register Project

```http
POST /api/projects
Content-Type: application/json

{
  "path": "/path/to/project",
  "branch": "main"
}
```

**Response:**
```json
{
  "name": "my-project",
  "message": "Project registered successfully"
}
```

#### Get Project Details

```http
GET /api/projects/{name}
```

**Response:**
```json
{
  "name": "my-project",
  "description": "Project description",
  "path": "/path/to/project",
  "plugins": ["docs-architected", "git-architected"],
  "tasks": [
    {
      "id": "docs-build",
      "phase": "BUILD",
      "description": "Build documentation"
    }
  ]
}
```

### Tasks

#### List Tasks

```http
GET /api/projects/{projectName}/tasks
```

**Response:**
```json
{
  "tasks": [
    {
      "id": "docs-build",
      "phase": "BUILD",
      "description": "Build documentation",
      "dependencies": []
    }
  ]
}
```

#### Execute Task

```http
POST /api/projects/{projectName}/tasks/{taskName}
Content-Type: application/json

{
  "args": ["--verbose"]
}
```

**Response:**
```json
{
  "executionId": "exec-123",
  "status": "RUNNING",
  "startTime": "2024-01-01T12:00:00Z"
}
```

### Execution Events

#### Stream Events (SSE)

```http
GET /api/executions/{executionId}/events
Accept: text/event-stream
```

**Event Stream:**
```
event: task-started
data: {"taskId":"docs-build","timestamp":"2024-01-01T12:00:00Z"}

event: task-progress
data: {"taskId":"docs-build","progress":50,"message":"Building..."}

event: task-completed
data: {"taskId":"docs-build","status":"SUCCESS","timestamp":"2024-01-01T12:01:00Z"}
```

## Configuration

The engine is configured via `application.yml`:

```yaml
micronaut:
  server:
    port: 9292
    cors:
      enabled: true
  
engine:
  project:
    cache:
      enabled: true
      ttl: 3600
  execution:
    timeout: 600
    parallel: true
  plugins:
    directory: "plugins/"
    autoLoad: true
```

## Building

```bash
cd architect-engine/engine
./gradlew build

# Build native image (GraalVM)
./gradlew nativeCompile
```

## Running

### Development Mode

```bash
./gradlew run
```

### Production

```bash
# Using JAR
java -jar build/libs/engine.jar

# Using native binary
./build/native/nativeCompile/engine
```

### Docker

```bash
# Build image
./gradlew dockerBuild

# Run container
docker run -p 9292:9292 architect-engine:latest
```

## Testing

```bash
# Run all tests
./gradlew test

# Run integration tests
./gradlew integrationTest

# Run with coverage
./gradlew jacocoTestReport
```

## Monitoring

### Health Check

```http
GET /health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "jdbc": {"status": "UP"}
  }
}
```

### Metrics

```http
GET /metrics
```

Returns Prometheus-formatted metrics for:
- Task execution times
- API request rates
- Memory usage
- Thread pool statistics

## Security

### Authentication

The engine supports multiple authentication methods:
- JWT tokens
- API keys
- OAuth2

Configure in `application.yml`:

```yaml
security:
  enabled: true
  token:
    jwt:
      enabled: true
      secret: "${JWT_SECRET}"
```

### Authorization

Role-based access control (RBAC):
- `ADMIN`: Full access
- `USER`: Execute tasks
- `READONLY`: View-only

## Performance Tuning

### Thread Pool

```yaml
engine:
  execution:
    threads:
      core: 10
      max: 50
      queueSize: 100
```

### Caching

```yaml
engine:
  project:
    cache:
      enabled: true
      maxSize: 100
      ttl: 3600
```

### Connection Pool

```yaml
datasources:
  default:
    maximum-pool-size: 20
    minimum-idle: 5
```

## Logging

Configure logging levels:

```yaml
logger:
  levels:
    io.github.architectplatform: DEBUG
    io.micronaut: INFO
```

## Troubleshooting

### High Memory Usage

```bash
# Increase heap size
java -Xmx2g -jar engine.jar
```

### Slow Task Execution

Check thread pool configuration and increase parallelism.

### Plugin Not Loading

Verify plugin JAR is in the plugins directory and implements the ArchitectPlugin interface.

## Contributing

See [CONTRIBUTING.md](../../CONTRIBUTING.md) for contribution guidelines.

## License

MIT License - see [LICENSE](../../LICENSE)
