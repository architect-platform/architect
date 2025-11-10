# Architect Cloud

**Multi-Cloud Infrastructure Deployment and Orchestration Platform**

## Overview

Architect Cloud provides a platform-agnostic infrastructure deployment solution that works across multiple cloud providers and orchestration platforms. It consists of:

1. **Backend** - RESTful API service for managing application definitions and deployment commands
2. **Agents** - Platform-specific agents (Kubernetes, Docker Compose, etc.) that execute deployments
3. **UI** - Web-based dashboard for managing and monitoring deployments

## Key Features

✅ **Multi-Cloud Support** - Deploy to AWS, GCP, Azure, Kubernetes, Docker, and more  
✅ **Environment Management** - Separate dev, staging, and production deployments  
✅ **Deployment History** - Track all deployments and rollbacks  
✅ **Rollback Capabilities** - Quick rollback to previous versions  
✅ **Resource Tagging** - Organize and track resources with tags  
✅ **Template-Based** - Flexible Jinja2 templates for any platform  
✅ **Agent Architecture** - Deploy anywhere with platform-specific agents

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│               Architect Cloud Platform                     │
│                                                            │
│   Backend API (Port 8080)                                 │
│   - Application Definitions                               │
│   - Deployment Commands                                   │
│   - Template Management                                   │
│   - Deployment History                                    │
│   - Multi-Environment Support                             │
└──────────┬──────────────────────────────────────────────┘
           │
           │ Deployment Commands
           │
     ┌─────┴─────┬──────────────┬──────────────┐
     │           │              │              │
┌────▼────┐ ┌───▼────┐  ┌─────▼─────┐ ┌──────▼──────┐
│Kubernetes│ │ Docker  │  │AWS ECS/    │ │Google Cloud │
│ Agent   │ │Compose  │  │Fargate     │ │Run Agent    │
│         │ │ Agent   │  │Agent       │ │             │
└─────────┘ └─────────┘  └───────────┘ └─────────────┘
     │           │              │              │
     │    ┌──────┴──────┬───────┴──────┐       │
     │    │             │               │       │
 ┌───▼────▼──┐    ┌────▼────┐    ┌────▼───────▼──┐
 │Kubernetes │    │ Docker   │    │   Cloud        │
 │ Cluster   │    │ Host     │    │   Provider     │
 └───────────┘    └──────────┘    └────────────────┘
```

## Quick Start

### 1. Start the Backend

```bash
cd backend
./gradlew run
```

The backend will start on http://localhost:8080

### 2. Start the Dashboard

```bash
cd ui
npm run serve
```

The dashboard will be available at http://localhost:3000

### 3. Configure Engine to Report to Cloud

Enable cloud reporting in your engine's `application.yml`:

```yaml
architect:
  cloud:
    enabled: true
    url: http://localhost:8080
    engine-id: my-engine-instance  # Optional: auto-generated if not provided
```

Or set environment variables:

```bash
export ARCHITECT_CLOUD_ENABLED=true
export ARCHITECT_CLOUD_URL=http://localhost:8080
export ARCHITECT_CLOUD_ENGINE_ID=my-engine-001
```

### 4. Start Your Engine

```bash
cd architect-engine/engine
./gradlew run
```

The engine will automatically:
- Register itself with the cloud
- Report all project registrations
- Stream execution events in real-time
- Send periodic heartbeats

## Components

### Backend Service

A Micronaut-based REST API service that:
- Receives and stores engine registrations
- Tracks projects across all engines
- Collects execution events and logs
- Provides query APIs for the dashboard
- Uses H2 database (configurable for production)

**[Backend Documentation →](backend/README.md)**

### Dashboard UI

A web-based dashboard that displays:
- Active engine instances
- All registered projects
- Execution history and status
- Real-time updates every 5 seconds

**[UI Documentation →](ui/README.md)**

## Features

### Engine Management
- ✅ Unique engine identification
- ✅ Automatic registration on startup
- ✅ Heartbeat monitoring
- ✅ Status tracking (Active/Inactive/Offline)

### Project Tracking
- ✅ Multi-engine project visibility
- ✅ Project metadata storage
- ✅ Path and description tracking

### Execution Monitoring
- ✅ Real-time execution events
- ✅ Task status tracking
- ✅ Output and log collection
- ✅ Error details capture
- ✅ Execution history

### Dashboard Features
- ✅ Overview statistics
- ✅ Engine instance table
- ✅ Recent executions view
- ✅ Auto-refreshing data
- ✅ Status indicators

## Data Model

### Engine Instance
```yaml
id: string           # Unique engine identifier
hostname: string     # Engine hostname
port: int           # Engine port
version: string     # Engine version
status: enum        # ACTIVE | INACTIVE | OFFLINE
createdAt: timestamp
lastHeartbeat: timestamp
```

### Project
```yaml
id: string          # Unique project identifier
name: string        # Project name
path: string        # Filesystem path
engineId: string    # Associated engine
description: string # Optional description
createdAt: timestamp
```

### Execution
```yaml
id: string              # Execution ID
projectId: string       # Associated project
engineId: string        # Engine that ran it
taskId: string          # Task executed
status: enum            # STARTED | RUNNING | COMPLETED | FAILED | SKIPPED
message: string         # Status message
errorDetails: string    # Error info if failed
startedAt: timestamp
completedAt: timestamp
```

### Execution Event
```yaml
id: string          # Event ID
executionId: string # Associated execution
eventType: string   # Event type (task.started, etc.)
taskId: string      # Task ID
message: string     # Event message
output: string      # Task output/logs
success: boolean    # Success flag
timestamp: timestamp
```

## Configuration

### Backend Configuration

Edit `backend/src/main/resources/application.yml`:

```yaml
micronaut:
  server:
    port: 8080

datasources:
  default:
    url: jdbc:h2:file:./data/architectCloudDb  # Persistent H2
    # url: jdbc:postgresql://localhost:5432/architect  # PostgreSQL
    # url: jdbc:mysql://localhost:3306/architect  # MySQL
```

### Engine Configuration

Add to engine's `application.yml`:

```yaml
architect:
  cloud:
    enabled: true                    # Enable cloud reporting
    url: http://localhost:8080       # Cloud backend URL
    engine-id: ${ARCHITECT_ENGINE_ID:}  # Optional engine ID
```

### UI Configuration

Edit `ui/public/app.js`:

```javascript
const API_BASE_URL = 'http://localhost:8080/api';
const REFRESH_INTERVAL = 5000;  // milliseconds
```

## Deployment

### Docker Deployment

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  cloud-backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - DATASOURCE_URL=jdbc:postgresql://db:5432/architect
    depends_on:
      - db

  cloud-ui:
    build: ./ui
    ports:
      - "3000:80"
    environment:
      - API_BASE_URL=http://cloud-backend:8080/api

  db:
    image: postgres:15
    environment:
      - POSTGRES_DB=architect
      - POSTGRES_PASSWORD=password
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

### Kubernetes Deployment

See deployment examples in the `deployment/` directory.

## Testing

### Backend Tests

```bash
cd backend
./gradlew test
```

### Integration Testing

1. Start the cloud backend
2. Start an engine instance with cloud reporting enabled
3. Register a project
4. Execute a task
5. View the dashboard to see real-time updates

## Development

### Building from Source

```bash
# Build backend
cd backend
./gradlew build

# Package UI (already static files)
cd ui
# No build needed
```

### Running Tests

```bash
cd backend
./gradlew test
```

## API Documentation

### Engine Registration
```http
POST /api/engines
Content-Type: application/json

{
  "id": "engine-001",
  "hostname": "localhost",
  "port": 9292,
  "version": "1.3.0"
}
```

### Project Registration
```http
POST /api/projects
Content-Type: application/json

{
  "id": "proj-001",
  "name": "my-project",
  "path": "/path/to/project",
  "engineId": "engine-001",
  "description": "My project"
}
```

### Report Execution
```http
POST /api/executions
Content-Type: application/json

{
  "id": "exec-001",
  "projectId": "proj-001",
  "engineId": "engine-001",
  "taskId": "build",
  "status": "STARTED"
}
```

See [Backend API Documentation](backend/README.md) for complete API reference.

## Roadmap

- [ ] WebSocket support for real-time updates
- [ ] Advanced filtering and search
- [ ] Execution logs viewer
- [ ] Metrics and analytics
- [ ] Alert system
- [ ] User authentication
- [ ] Multi-tenancy support
- [ ] Export functionality

## Contributing

Contributions are welcome! Please see the main repository [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

## License

MIT License - see [LICENSE](../LICENSE) for details.

## Support

- **Documentation**: Check component READMEs
- **Issues**: GitHub Issues
- **Discussions**: GitHub Discussions
