# Architect Distributed System

## Overview

Architect is a distributed Kubernetes orchestration platform that separates control (Server) from execution (Agents), enabling secure multi-cluster deployments without exposing cluster credentials.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Architect Server                       │
│                   (Control Plane)                        │
│                                                          │
│  - Template Management (Jinjava templates)              │
│  - Resource Definitions (Applications)                   │
│  - Agent Registration & Heartbeat                        │
│  - Deployment Command Dispatch                           │
│  - Audit Logs & State Tracking                          │
│                                                          │
└──────────────────┬──────────────────────────────────────┘
                   │
                   │ HTTPS / gRPC
                   │ (REST API defined in architect-server-api)
                   │
    ┌──────────────┼──────────────┬──────────────┐
    │              │              │              │
    ▼              ▼              ▼              ▼
┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐
│ Agent 1 │  │ Agent 2 │  │ Agent 3 │  │ Agent N │
│  (K8s)  │  │ (K8s)   │  │ (Docker)│  │(Future) │
└────┬────┘  └────┬────┘  └────┬────┘  └─────────┘
     │            │            │
     ▼            ▼            ▼
┌─────────┐  ┌─────────┐  ┌─────────┐
│Cluster 1│  │Cluster 2│  │ Docker  │
└─────────┘  └─────────┘  └─────────┘
```

## Components

### 1. Architect Server (`architect-server/`)

**Control Plane** - Spring Boot + Kotlin backend with REST API.

**Responsibilities:**
- Accept application definitions (name, version, templates, variables)
- Store and manage reusable Kubernetes YAML templates
- Send deployment commands to connected Agents
- Track deployment history and resource state
- Manage agent registration and health monitoring
- Provide authentication and authorization

**Key Features:**
- Clean/Hexagonal Architecture (enforced with ArchUnit)
- Real-time WebSocket events for UI updates
- H2 (dev) / PostgreSQL (prod) database
- Multi-tenant and multi-cluster management

### 2. Architect Server API (`architect-server-api/`)

**Shared API Contract** - Pure Kotlin data classes and interfaces.

**Purpose:**
- Define DTOs for agent-server communication
- Provide type safety across server and agents
- Version API contracts
- Enable multiple agent implementations

**Contains:**
- `AgentRegistration` - Registration and heartbeat DTOs
- `DeploymentCommand` - Deployment command and result DTOs
- `ResourceDefinition` - Application resource definitions
- `Template` - Kubernetes YAML template definitions
- `ApiEndpoints` - Centralized endpoint constants

### 3. Architect Kubernetes Agent (`architect-agents/architect-kubernetes-agent/`)

**Cluster Executor** - Lightweight Micronaut service running inside a Kubernetes cluster.

**Responsibilities:**
- Register with Architect Server
- Poll for deployment commands
- Render templates with cluster-specific configuration
- Apply manifests to local Kubernetes API
- Report deployment status back to Server
- Send periodic heartbeats

**Key Features:**
- Template rendering with Jinjava
- Kubernetes deployment via Fabric8 client
- Namespace isolation per application
- Secure communication with Server (token-based)
- No cluster credentials exposed to Server

### 4. Future Agents

The architecture supports additional agent types:
- **Docker Agent** - Deploy to Docker hosts
- **Cloud Run Agent** - Deploy to Google Cloud Run
- **ECS Agent** - Deploy to AWS ECS
- **Generic Agent** - Custom deployment targets

## Core Concepts

### Template-Driven Deployment

1. **Define Templates**: Create reusable Kubernetes YAML templates with Jinjava variables
   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: {{ name }}
     namespace: {{ namespace }}
   spec:
     replicas: {{ replicas }}
     template:
       spec:
         containers:
         - name: {{ name }}
           image: {{ image }}
           env:
           {% for key, value in env.items() %}
           - name: {{ key }}
             value: "{{ value }}"
           {% endfor %}
   ```

2. **Create Resource Definitions**: Define applications with specific values
   ```json
   {
     "name": "my-app",
     "version": "1.0.0",
     "namespace": "production",
     "image": "myapp:1.0.0",
     "replicas": 3,
     "env": {
       "ENV": "production",
       "DB_HOST": "postgres.prod.svc"
     }
   }
   ```

3. **Deploy**: Server renders templates and sends commands to appropriate Agent

4. **Agent Applies**: Agent applies manifests to its local Kubernetes cluster

5. **Status Reported**: Agent reports success/failure back to Server

### Security Model

- **Agent Authentication**: Token-based authentication for agent-server communication
- **No Credential Exposure**: Server never has direct access to cluster credentials
- **Namespace Isolation**: Each application deployed to its own namespace
- **Audit Trail**: All deployments tracked and logged

## API Endpoints

### Agent Management

```
POST   /api/agents/register       - Register new agent
POST   /api/agents/heartbeat      - Send heartbeat
GET    /api/agents                - List all agents
GET    /api/agents/{id}           - Get agent details
GET    /api/agents/{id}/commands  - Poll for commands
```

### Template Management

```
POST   /api/templates             - Create template
GET    /api/templates             - List templates
GET    /api/templates/{id}        - Get template
PUT    /api/templates/{id}        - Update template
DELETE /api/templates/{id}        - Delete template
GET    /api/templates/type/{type} - Get by type
POST   /api/templates/validate    - Validate template
```

### Resource Definitions

```
POST   /api/resources                  - Create resource
GET    /api/resources                  - List resources
GET    /api/resources/{id}             - Get resource
PUT    /api/resources/{id}             - Update resource
DELETE /api/resources/{id}             - Delete resource
GET    /api/resources/namespace/{ns}   - Get by namespace
POST   /api/resources/{id}/deploy      - Deploy resource
```

### Deployments

```
POST   /api/deployments/result         - Report result (from agent)
GET    /api/deployments/{id}/status    - Get deployment status
GET    /api/deployments/history        - Get deployment history
```

## Getting Started

### Running the Server

```bash
cd architect-server/backend
./gradlew run
```

Server starts on http://localhost:8080

### Running the Kubernetes Agent

```bash
cd architect-agents/architect-kubernetes-agent

# Configure agent
export ARCHITECT_SERVER_URL=http://localhost:8080
export ARCHITECT_SERVER_TOKEN=your-token
export KUBERNETES_NAMESPACE=default

# Run agent
./gradlew run
```

Agent connects to server and starts polling for commands.

### Example Deployment Flow

1. **Create a template**:
   ```bash
   curl -X POST http://localhost:8080/api/templates \
     -H "Content-Type: application/json" \
     -d '{
       "name": "basic-deployment",
       "templateType": "DEPLOYMENT",
       "content": "apiVersion: apps/v1..."
     }'
   ```

2. **Create a resource definition**:
   ```bash
   curl -X POST http://localhost:8080/api/resources \
     -H "Content-Type: application/json" \
     -d '{
       "name": "my-app",
       "version": "1.0.0",
       "image": "nginx:latest",
       "replicas": 2,
       "templateIds": ["template-id"]
     }'
   ```

3. **Deploy**:
   ```bash
   curl -X POST http://localhost:8080/api/resources/{id}/deploy
   ```

4. Agent polls, receives command, applies to Kubernetes, reports status

## Development Status

### ✅ Completed

- [x] Repository structure (architect-server, architect-server-api, architect-agents)
- [x] Renamed cloud to server
- [x] Created shared API contract module
- [x] Kubernetes Agent implementation with template rendering and deployment
- [x] Agent domain models and services
- [x] Server domain models (Template, ResourceDefinition, Agent, DeploymentCommand)
- [x] Server ports (use cases and repository interfaces)

### 🚧 In Progress

- [ ] Server service implementations
- [ ] Server REST controllers
- [ ] Persistence adapters (JPA/JDBC)
- [ ] Agent registration flow
- [ ] Deployment command dispatch
- [ ] End-to-end testing

### 📋 Planned

- [ ] Authentication & authorization
- [ ] WebSocket updates for deployment status
- [ ] UI dashboard updates
- [ ] Docker Agent implementation
- [ ] Multi-cluster management
- [ ] GitOps integration

## Benefits

### Multi-Cluster Orchestration
Deploy to multiple Kubernetes clusters without exposing credentials

### Template Reusability
Define templates once, use for multiple applications

### Separation of Concerns
Control plane (Server) separate from execution (Agents)

### Security
No cluster credentials in control plane, token-based agent auth

### Extensibility
Easy to add new agent types for different platforms

### Audit & Compliance
Complete deployment history and state tracking

## License

MIT License - See main project LICENSE file.
