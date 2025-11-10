# Architect Docker Compose Agent

**Lightweight agent for deploying applications using Docker Compose.**

## Overview

The Docker Compose Agent is part of the Architect distributed system. It runs on a Docker host and:

- Receives deployment commands from Architect Server
- Renders Docker Compose templates using Jinjava
- Deploys applications using `docker-compose`
- Reports deployment status back to the server
- Sends periodic heartbeats

## Features

- ✅ Template rendering with Jinjava
- ✅ Docker Compose deployment (up/down)
- ✅ Server communication via REST API
- ✅ Health monitoring and heartbeat
- ✅ Configuration via environment variables
- ✅ Clean architecture with SOLID principles
- ✅ Design patterns: Command, Template Method, Proxy

## Architecture

```
┌─────────────────────────────────────┐
│    Architect Docker Compose Agent    │
│                                     │
│  ┌────────────────────────────────┐ │
│  │  ServerCommunicationService    │ │
│  │  (Polls for commands)          │ │
│  └────────────┬───────────────────┘ │
│               │                     │
│               ▼                     │
│  ┌────────────────────────────────┐ │
│  │  TemplateRenderingService      │ │
│  │  (Renders compose templates)   │ │
│  └────────────┬───────────────────┘ │
│               │                     │
│               ▼                     │
│  ┌────────────────────────────────┐ │
│  │ DockerComposeDeploymentService │ │
│  │  (Executes docker-compose)     │ │
│  └────────────┬───────────────────┘ │
│               │                     │
└───────────────┼─────────────────────┘
                │
                ▼
        ┌─────────────┐
        │docker-compose│
        └──────┬───────┘
               │
               ▼
        ┌────────────┐
        │   Docker   │
        └────────────┘
```

## Configuration

Configure via environment variables or `application.yml`:

```bash
# Agent identification
AGENT_ID=docker-agent-001

# Server connection
ARCHITECT_SERVER_URL=http://localhost:8080
ARCHITECT_SERVER_TOKEN=your-token-here

# Docker Compose settings
DOCKER_COMPOSE_WORKING_DIR=/tmp/docker-compose
DOCKER_COMPOSE_COMMAND=docker-compose

# Heartbeat interval
HEARTBEAT_INTERVAL=30
```

## Running the Agent

### Prerequisites

- Java 17 or higher
- Docker and Docker Compose installed
- Access to Architect Server

### Start the Agent

```bash
./gradlew run
```

Or with custom configuration:

```bash
export ARCHITECT_SERVER_URL=http://localhost:8080
export AGENT_ID=my-docker-agent
./gradlew run
```

### Build Distribution

```bash
./gradlew shadowJar
java -jar build/libs/architect-docker-compose-agent-1.0.0-all.jar
```

## How It Works

### 1. Agent Registration

On startup, the agent registers with the server:

```
POST /api/agents/register
{
  "agentId": "docker-agent-001",
  "agentType": "DOCKER",
  "capabilities": ["docker-compose", "template-rendering"]
}
```

### 2. Polling for Commands

The agent periodically polls for deployment commands:

```
GET /api/deployments/{agentId}/commands
```

### 3. Template Rendering

When a command is received, templates are rendered with variables:

```yaml
# Template
version: '3.8'
services:
  {{ name }}:
    image: {{ image }}
    ports:
      - "{{ port }}:{{ port }}"
    environment:
      {% for key, value in env.items() %}
      {{ key }}: {{ value }}
      {% endfor %}
```

### 4. Docker Compose Deployment

The rendered compose file is saved and deployed:

```bash
docker-compose -f docker-compose.yml -p my-project up -d
```

### 5. Status Reporting

Deployment results are reported back to the server:

```
POST /api/deployments/result
{
  "commandId": "cmd-123",
  "result": {
    "success": true,
    "message": "Deployed 3 services",
    "appliedResources": [...]
  }
}
```

## Example Deployment

### 1. Create Template on Server

```yaml
version: '3.8'
services:
  web:
    image: {{ image }}
    ports:
      - "{{ port }}:80"
    replicas: {{ replicas }}
```

### 2. Create Resource Definition

```json
{
  "name": "my-web-app",
  "image": "nginx:latest",
  "port": 8080,
  "replicas": 2,
  "templateIds": ["template-id-123"]
}
```

### 3. Deploy

```bash
POST /api/resources/{resourceId}/deploy?agentId=docker-agent-001
```

### 4. Agent Deploys

The agent:
1. Receives the command
2. Renders the template
3. Writes `docker-compose.yml`
4. Runs `docker-compose up -d`
5. Reports success/failure

## Design Patterns

### Command Pattern
- `DeploymentCommand` encapsulates deployment requests
- Commands are queued and processed

### Template Method Pattern
- `DockerComposeDeploymentService.deploy()` defines deployment algorithm
- Steps: render → prepare → write → execute → report

### Proxy Pattern
- `ServerCommunicationService` acts as proxy for remote server
- Handles HTTP communication details

### Single Responsibility Principle
- Each service has one reason to change
- `TemplateRenderingService`: only rendering
- `DockerComposeDeploymentService`: only deployment
- `ServerCommunicationService`: only communication

### Dependency Inversion
- Services depend on `AgentConfig` abstraction
- Easy to swap implementations

## Troubleshooting

### Agent Won't Start

- Check Docker Compose is installed: `docker-compose version`
- Verify server URL is accessible
- Check logs for connection errors

### Deployments Fail

- Verify docker-compose command works: `docker-compose version`
- Check working directory permissions
- Review rendered compose file in logs
- Ensure Docker daemon is running

### Connection Issues

- Verify server URL and port
- Check authentication token
- Review firewall rules

## Development

### Building

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Code Structure

```
src/main/kotlin/
├── domain/              # Pure business logic
│   ├── DeploymentCommand.kt
│   └── AgentConfig.kt
├── application/         # Use cases and services
│   ├── TemplateRenderingService.kt
│   ├── DockerComposeDeploymentService.kt
│   └── ServerCommunicationService.kt
├── config/             # Configuration
│   └── AgentConfiguration.kt
└── Application.kt      # Main entry point
```

## License

MIT License - See main project LICENSE file.
