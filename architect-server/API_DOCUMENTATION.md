# Architect Server API Documentation

## Overview

The Architect Server provides a **platform-agnostic REST API** for managing application deployments across multiple platforms (Kubernetes, Docker Compose, etc.). The server acts as a control plane, coordinating with platform-specific agents to deploy applications and their dependencies.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Architect Server                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │  REST API    │  │   Services   │  │   Domain     │ │
│  │ (Controllers)│→ │  (Use Cases) │→ │   Models     │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
│         ↓                  ↓                  ↓         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │  WebSocket   │  │  Templates   │  │ Persistence  │ │
│  │  Broadcast   │  │   (Jinjava)  │  │  (Adapters)  │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────┘
           ↓                            ↓
   ┌───────────────┐          ┌───────────────┐
   │ Kubernetes    │          │ Docker Compose│
   │ Agent         │          │ Agent         │
   └───────────────┘          └───────────────┘
```

## Core Concepts

### 1. Applications

Platform-agnostic application definitions that can be deployed to any supported platform.

**Key Fields:**
- `name`: Application identifier
- `type`: APPLICATION, DATABASE, MESSAGE_BROKER, CACHE, STORAGE, SERVICE
- `image`: Container image to deploy
- `instances`: Number of replicas to run
- `exposedPorts`: Ports to expose (with public/private flag)
- `dependencies`: Other applications this depends on

### 2. Templates

Jinjava templates that agents use to generate platform-specific manifests.

**Variables Available:**
- Application fields (name, version, image, etc.)
- Environment variables
- Resource limits
- Port configurations
- Health check settings

### 3. Agents

Platform-specific clients that execute deployments.

**Agent Types:**
- Kubernetes Agent: Maps to Deployments, Services, StatefulSets
- Docker Compose Agent: Maps to docker-compose.yml services

### 4. Deployments

Commands sent to agents to deploy applications with dependencies.

**Deployment Flow:**
1. Server validates application and dependencies
2. Server orders dependencies (required first)
3. Server creates deployment command
4. Agent polls for commands
5. Agent deploys dependencies in order
6. Agent deploys application
7. Agent reports status back to server

## REST API Endpoints

### Applications

#### Create Application
```http
POST /api/applications
Content-Type: application/json

{
  "name": "web-api",
  "version": "1.0.0",
  "type": "APPLICATION",
  "image": "web-api:1.0.0",
  "instances": 3,
  "environment": {
    "DATABASE_URL": "postgres://db:5432/app",
    "LOG_LEVEL": "info"
  },
  "exposedPorts": [
    {
      "port": 8080,
      "protocol": "TCP",
      "public": true
    }
  ],
  "resources": {
    "cpu": "500m",
    "memory": "512Mi",
    "storage": "10Gi"
  },
  "healthCheck": {
    "type": "HTTP",
    "path": "/health",
    "port": 8080,
    "intervalSeconds": 30,
    "timeoutSeconds": 5
  },
  "metadata": {
    "team": "backend",
    "environment": "production"
  },
  "dependencies": [
    {
      "applicationId": "postgres-db",
      "required": true,
      "connectionInfo": {
        "host": "postgres-db",
        "port": "5432"
      }
    }
  ],
  "templateIds": ["deployment-template"]
}
```

**Response:** `201 Created`
```json
{
  "id": "uuid-here",
  "name": "web-api",
  "version": "1.0.0",
  "type": "APPLICATION",
  "...": "..."
}
```

#### List Applications
```http
GET /api/applications
```

**Response:** `200 OK`
```json
[
  {
    "id": "uuid-1",
    "name": "web-api",
    "type": "APPLICATION",
    "...": "..."
  },
  {
    "id": "uuid-2",
    "name": "postgres-db",
    "type": "DATABASE",
    "...": "..."
  }
]
```

#### Get Application
```http
GET /api/applications/{appId}
```

**Response:** `200 OK` or `404 Not Found`

#### Update Application
```http
PUT /api/applications/{appId}
Content-Type: application/json

{
  "version": "1.1.0",
  "instances": 5,
  "environment": {
    "LOG_LEVEL": "debug"
  }
}
```

**Response:** `200 OK`

#### Delete Application
```http
DELETE /api/applications/{appId}
```

**Response:** `200 OK`
```json
{
  "deleted": true
}
```

#### Deploy Application
```http
POST /api/applications/{appId}/deploy?agentId=k8s-agent-1&operation=APPLY
```

**Query Parameters:**
- `agentId` (required): ID of agent to deploy to
- `operation` (optional): APPLY (default), DELETE, UPDATE

**Response:** `200 OK`
```json
{
  "id": "deployment-uuid",
  "agentId": "k8s-agent-1",
  "applicationDefinitionId": "app-uuid",
  "applicationName": "web-api",
  "dependencies": ["postgres-db"],
  "operation": "APPLY",
  "status": "PENDING",
  "createdAt": "2025-11-10T20:00:00Z"
}
```

### Templates

#### Create Template
```http
POST /api/templates
Content-Type: application/json

{
  "name": "kubernetes-deployment",
  "type": "KUBERNETES",
  "content": "apiVersion: apps/v1\nkind: Deployment\n...",
  "variables": {
    "name": "string",
    "instances": "integer",
    "image": "string"
  }
}
```

**Response:** `201 Created`

#### List Templates
```http
GET /api/templates
```

**Response:** `200 OK`

#### Get Template
```http
GET /api/templates/{templateId}
```

**Response:** `200 OK`

#### Update Template
```http
PUT /api/templates/{templateId}
```

**Response:** `200 OK`

#### Delete Template
```http
DELETE /api/templates/{templateId}
```

**Response:** `200 OK`

### Agents

#### Register Agent
```http
POST /api/agents/register
Content-Type: application/json

{
  "name": "k8s-agent-1",
  "type": "KUBERNETES",
  "version": "1.0.0",
  "capabilities": ["DEPLOYMENT", "SERVICE", "STATEFULSET"]
}
```

**Response:** `200 OK`
```json
{
  "agentId": "agent-uuid",
  "token": "auth-token-here"
}
```

#### Heartbeat
```http
POST /api/agents/{agentId}/heartbeat
```

**Response:** `200 OK`

#### List Agents
```http
GET /api/agents
```

**Response:** `200 OK`

#### Get Agent
```http
GET /api/agents/{agentId}
```

**Response:** `200 OK`

#### Update Agent
```http
PUT /api/agents/{agentId}
```

**Response:** `200 OK`

#### Delete Agent
```http
DELETE /api/agents/{agentId}
```

**Response:** `200 OK`

### Deployments

#### Get Pending Commands
```http
GET /api/deployments/commands/pending?agentId=k8s-agent-1
```

**Response:** `200 OK`
```json
[
  {
    "id": "cmd-uuid",
    "agentId": "k8s-agent-1",
    "applicationName": "web-api",
    "dependencies": ["postgres-db"],
    "templates": ["deployment-yaml-here"],
    "variables": {...},
    "operation": "APPLY",
    "status": "PENDING"
  }
]
```

#### Report Result
```http
POST /api/deployments/commands/{commandId}/result
Content-Type: application/json

{
  "success": true,
  "message": "Deployment successful",
  "appliedResources": [
    {
      "kind": "Deployment",
      "name": "web-api",
      "namespace": "default"
    }
  ]
}
```

**Response:** `200 OK`

## WebSocket Events

Connect to `/ws/events` for real-time updates.

### Event Types

#### Agent Registered
```json
{
  "type": "AGENT_REGISTERED",
  "entityType": "AGENT",
  "entityId": "agent-uuid",
  "timestamp": "2025-11-10T20:00:00Z",
  "data": {
    "agentId": "agent-uuid",
    "name": "k8s-agent-1",
    "type": "KUBERNETES"
  }
}
```

#### Deployment Created
```json
{
  "type": "DEPLOYMENT_COMMAND_CREATED",
  "entityType": "DEPLOYMENT_COMMAND",
  "entityId": "cmd-uuid",
  "timestamp": "2025-11-10T20:00:00Z",
  "data": {
    "id": "cmd-uuid",
    "applicationName": "web-api",
    "operation": "APPLY",
    "status": "PENDING"
  }
}
```

#### Deployment Completed
```json
{
  "type": "DEPLOYMENT_COMPLETED",
  "entityType": "DEPLOYMENT_COMMAND",
  "entityId": "cmd-uuid",
  "timestamp": "2025-11-10T20:00:00Z",
  "data": {
    "id": "cmd-uuid",
    "status": "SUCCESS",
    "success": true
  }
}
```

## Example Workflows

### Deploy Web Application with Database

#### 1. Create Database
```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{
    "name": "postgres-db",
    "version": "15.0",
    "type": "DATABASE",
    "image": "postgres:15",
    "instances": 1,
    "environment": {
      "POSTGRES_USER": "app",
      "POSTGRES_PASSWORD": "secret",
      "POSTGRES_DB": "appdb"
    },
    "exposedPorts": [{"port": 5432, "public": false}],
    "resources": {"cpu": "1000m", "memory": "2Gi", "storage": "50Gi"},
    "templateIds": ["postgres-statefulset"]
  }'
```

#### 2. Create Application with Database Dependency
```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{
    "name": "web-api",
    "version": "1.0.0",
    "type": "APPLICATION",
    "image": "web-api:1.0.0",
    "instances": 3,
    "environment": {
      "DATABASE_URL": "postgres://postgres-db:5432/appdb"
    },
    "exposedPorts": [{"port": 8080, "public": true}],
    "dependencies": [
      {
        "applicationId": "postgres-db-id",
        "required": true
      }
    ],
    "templateIds": ["app-deployment"]
  }'
```

#### 3. Deploy to Kubernetes
```bash
curl -X POST "http://localhost:8080/api/applications/web-api-id/deploy?agentId=k8s-agent-1"
```

The agent will:
1. Deploy postgres-db first (required dependency)
2. Wait for postgres to be healthy
3. Deploy web-api
4. Report success/failure

### Deploy Microservices with Message Broker

#### 1. Create RabbitMQ Broker
```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{
    "name": "rabbitmq",
    "version": "3.12",
    "type": "MESSAGE_BROKER",
    "image": "rabbitmq:3.12-management",
    "instances": 1,
    "environment": {
      "RABBITMQ_DEFAULT_USER": "admin",
      "RABBITMQ_DEFAULT_PASS": "secret"
    },
    "exposedPorts": [
      {"port": 5672, "public": false},
      {"port": 15672, "public": true}
    ],
    "templateIds": ["rabbitmq-statefulset"]
  }'
```

#### 2. Create Producer Service
```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{
    "name": "order-service",
    "version": "1.0.0",
    "type": "APPLICATION",
    "image": "order-service:1.0.0",
    "instances": 2,
    "dependencies": [
      {"applicationId": "rabbitmq-id", "required": true}
    ],
    "templateIds": ["app-deployment"]
  }'
```

#### 3. Create Consumer Service
```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{
    "name": "notification-service",
    "version": "1.0.0",
    "type": "APPLICATION",
    "image": "notification-service:1.0.0",
    "instances": 3,
    "dependencies": [
      {"applicationId": "rabbitmq-id", "required": true}
    ],
    "templateIds": ["app-deployment"]
  }'
```

## Configuration

### Environment Variables

**Server Configuration:**
- `MICRONAUT_SERVER_PORT`: Server port (default: 8080)
- `DATABASE_URL`: Database connection URL (in-memory by default)

**Agent Configuration:**
- `SERVER_URL`: URL of Architect Server
- `AGENT_NAME`: Agent identifier
- `AGENT_TYPE`: KUBERNETES or DOCKER_COMPOSE
- `POLL_INTERVAL`: Seconds between command polls (default: 10)

## Error Handling

All endpoints return standard HTTP status codes:

- `200 OK`: Request successful
- `201 Created`: Resource created
- `400 Bad Request`: Invalid request data
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

Error response format:
```json
{
  "error": "Error description",
  "message": "Detailed error message",
  "timestamp": "2025-11-10T20:00:00Z"
}
```

## Security

- **Agent Authentication**: Token-based authentication for agents
- **API Access**: Can be secured with API keys or OAuth
- **No Cluster Credentials**: Server never accesses clusters directly
- **Agent Isolation**: Each agent can only access assigned resources

## Best Practices

1. **Use Dependencies**: Define database/broker dependencies explicitly
2. **Set Resource Limits**: Always specify CPU and memory limits
3. **Health Checks**: Configure health checks for all applications
4. **Application Types**: Use correct type (DATABASE, MESSAGE_BROKER, etc.)
5. **Template Variables**: Keep templates generic, use variables for customization
6. **Monitor Events**: Subscribe to WebSocket for real-time deployment status

## Troubleshooting

### Application Won't Deploy
- Check agent is registered and sending heartbeats
- Verify dependencies exist and are healthy
- Check template syntax and variables

### Dependencies Not Working
- Ensure dependency IDs are correct
- Set `required: true` for critical dependencies
- Check agent supports dependency ordering

### Agent Not Receiving Commands
- Verify agent is polling `/api/deployments/commands/pending`
- Check agent authentication token
- Confirm agent is registered with correct type

## License

MIT License - See main project LICENSE file.
