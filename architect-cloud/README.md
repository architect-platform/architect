# Architect Cloud

**Multi-Cloud Infrastructure Deployment and Orchestration Platform**

## Overview

Architect Cloud is a platform-agnostic infrastructure deployment solution that enables you to deploy applications across multiple cloud providers and orchestration platforms from a single API.

### Key Features

✅ **Multi-Cloud Support** - Deploy to AWS, GCP, Azure, Kubernetes, Docker, and more  
✅ **Environment Management** - Separate deployments for dev, staging, and production  
✅ **Deployment History** - Complete audit trail of all deployments and rollbacks  
✅ **Rollback Capabilities** - Quick rollback to any previous deployment  
✅ **Resource Tagging** - Organize and track resources with custom tags  
✅ **Template-Based** - Flexible Jinja2 templates for any platform  
✅ **Agent Architecture** - Deploy anywhere with platform-specific agents  
✅ **Clean Architecture** - Testable, maintainable, domain-driven design

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
```

## Quick Start

### 1. Start the Backend

```bash
cd architect-cloud/backend
./gradlew run
```

Backend starts on **http://localhost:8080**

### 2. Start an Agent

For Kubernetes:
```bash
cd architect-cloud/agents/kubernetes-agent
./gradlew run
```

For Docker Compose:
```bash
cd architect-cloud/agents/docker-compose-agent
./gradlew run
```

### 3. Create an Application Definition

```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{
    "id": "my-app",
    "name": "my-application",
    "version": "1.0.0",
    "type": "APPLICATION",
    "image": "nginx:latest",
    "instances": 2,
    "targetEnvironment": "DEVELOPMENT",
    "cloudProvider": "KUBERNETES",
    "exposedPorts": [
      {"port": 80, "protocol": "TCP", "public": true}
    ],
    "tags": {
      "team": "platform",
      "cost-center": "engineering"
    }
  }'
```

### 4. Deploy the Application

```bash
curl -X POST http://localhost:8080/api/deployments \
  -H "Content-Type: application/json" \
  -d '{
    "applicationDefinitionId": "my-app",
    "agentId": "kubernetes-agent-1",
    "targetEnvironment": "development"
  }'
```

## Core Concepts

### Application Definition

Platform-agnostic description of your application:

```kotlin
data class ApplicationDefinition(
    val id: String,
    val name: String,
    val version: String,
    val type: ApplicationType,
    val image: String,
    val instances: Int,
    val targetEnvironment: DeploymentEnvironment,
    val cloudProvider: CloudProvider?,
    val tags: Map<String, String>
    // ... resources, health checks, dependencies
)
```

### Deployment Command

Instruction sent to agents to perform deployments:

```kotlin
data class DeploymentCommand(
    val id: String,
    val agentId: String,
    val applicationDefinitionId: String,
    val operation: DeploymentOperation,  // APPLY, DELETE, UPDATE, ROLLBACK
    val targetEnvironment: String,
    val deploymentVersion: Int,
    val previousCommandId: String?  // For rollbacks
)
```

### Deployment History

Audit trail of all deployments:

```kotlin
data class DeploymentHistory(
    val id: String,
    val applicationName: String,
    val version: String,
    val operation: DeploymentOperation,
    val environment: String,
    val success: Boolean,
    val startedAt: Instant,
    val completedAt: Instant?,
    val duration: Long?
)
```

### Agents

Platform-specific deployment executors:

```kotlin
data class Agent(
    val id: String,
    val agentType: AgentType,  // KUBERNETES, DOCKER_COMPOSE, AWS_ECS, etc.
    val supportedEnvironments: List<String>,
    val cloudProvider: String?,
    val status: AgentStatus
)
```

## Environment Management

Architect Cloud supports multiple deployment environments:

- **DEVELOPMENT** - Local development and testing
- **STAGING** - Pre-production environment
- **PRODUCTION** - Live production environment
- **TEST** - Automated testing environment

### Environment-Specific Configuration

```kotlin
val devApp = appDefinition.copy(
    targetEnvironment = DeploymentEnvironment.DEVELOPMENT,
    instances = 1,
    resources = ResourceLimits(cpu = "100m", memory = "128Mi")
)

val prodApp = appDefinition.copy(
    targetEnvironment = DeploymentEnvironment.PRODUCTION,
    instances = 5,
    resources = ResourceLimits(cpu = "1000m", memory = "2Gi")
)
```

## Multi-Cloud Support

Deploy to any cloud provider:

```kotlin
// Kubernetes
val k8sApp = appDefinition.copy(
    cloudProvider = CloudProvider.KUBERNETES
)

// AWS ECS
val awsApp = appDefinition.copy(
    cloudProvider = CloudProvider.AWS
)

// Docker Compose
val dockerApp = appDefinition.copy(
    cloudProvider = CloudProvider.DOCKER
)
```

## Rollback

Quick rollback to previous deployments:

```bash
# Get deployment history
curl http://localhost:8080/api/deployments/history/my-app

# Rollback to previous version
curl -X POST http://localhost:8080/api/deployments/rollback \
  -H "Content-Type: application/json" \
  -d '{
    "deploymentCommandId": "cmd-123"
  }'
```

## Resource Tagging

Organize and track resources:

```kotlin
val app = appDefinition.withTags(mapOf(
    "team" to "platform",
    "cost-center" to "engineering",
    "environment" to "production",
    "project" to "architect-cloud"
))
```

## API Endpoints

### Application Management

- `POST /api/applications` - Create application definition
- `GET /api/applications` - List all applications
- `GET /api/applications/{id}` - Get application details
- `PUT /api/applications/{id}` - Update application
- `DELETE /api/applications/{id}` - Delete application

### Deployment Management

- `POST /api/deployments` - Create deployment
- `GET /api/deployments` - List deployments
- `GET /api/deployments/{id}` - Get deployment status
- `POST /api/deployments/rollback` - Rollback deployment
- `GET /api/deployments/history/{appId}` - Get deployment history

### Agent Management

- `POST /api/agents` - Register agent
- `GET /api/agents` - List agents
- `GET /api/agents/{id}` - Get agent details
- `POST /api/agents/heartbeat` - Send heartbeat

### Template Management

- `POST /api/templates` - Create template
- `GET /api/templates` - List templates
- `GET /api/templates/{id}` - Get template
- `PUT /api/templates/{id}` - Update template

## Agents

### Supported Platforms

| Agent Type | Platform | Status |
|------------|----------|--------|
| Kubernetes | Kubernetes clusters | ✅ Available |
| Docker Compose | Docker + Docker Compose | ✅ Available |
| AWS ECS | Amazon ECS/Fargate | 🚧 Coming Soon |
| Google Cloud Run | Google Cloud Run | 🚧 Coming Soon |
| Azure Container Instances | Azure ACI | 🚧 Coming Soon |

### Agent Configuration

Agents are configured via `application.yml`:

```yaml
agent:
  id: kubernetes-agent-1
  type: KUBERNETES
  namespace: default
  supportedEnvironments:
    - development
    - staging
    - production
  cloudProvider: KUBERNETES
  server:
    url: http://localhost:8080
  heartbeat:
    intervalSeconds: 30
```

## Templates

Architect Cloud uses Jinja2 templates for flexible deployment definitions:

```yaml
# kubernetes-deployment.yaml.j2
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ name }}
  namespace: {{ targetEnvironment }}
  labels:
    app: {{ name }}
    version: {{ version }}
    {% for key, value in tags.items() %}
    {{ key }}: {{ value }}
    {% endfor %}
spec:
  replicas: {{ instances }}
  selector:
    matchLabels:
      app: {{ name }}
  template:
    metadata:
      labels:
        app: {{ name }}
        version: {{ version }}
    spec:
      containers:
      - name: {{ name }}
        image: {{ image }}
        ports:
        {% for port in ports %}
        - containerPort: {{ port.port }}
          protocol: {{ port.protocol }}
        {% endfor %}
        {% if resources %}
        resources:
          limits:
            cpu: {{ resources.cpu }}
            memory: {{ resources.memory }}
        {% endif %}
        env:
        {% for key, value in environment.items() %}
        - name: {{ key }}
          value: "{{ value }}"
        {% endfor %}
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
    url: jdbc:h2:file:./data/architectCloudDb
    # url: jdbc:postgresql://localhost:5432/architect_cloud
    # url: jdbc:mysql://localhost:3306/architect_cloud
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
- **Database**: Micronaut Data JDBC (H2, PostgreSQL, MySQL)
- **Templates**: Jinjava (Jinja2 for JVM)
- **Architecture**: Clean/Hexagonal

## Roadmap

- [x] Multi-cloud provider support
- [x] Environment management
- [x] Deployment history tracking
- [x] Rollback capabilities
- [x] Resource tagging
- [ ] AWS ECS agent
- [ ] Google Cloud Run agent
- [ ] Azure Container Instances agent
- [ ] WebSocket real-time updates
- [ ] UI Dashboard
- [ ] Authentication/Authorization
- [ ] Multi-tenancy
- [ ] Deployment approval workflows
- [ ] Cost estimation
- [ ] Resource optimization suggestions

## License

MIT License - see LICENSE for details

## Related Projects

- **[Architect Data](../architect-data/README.md)** - Execution tracking and workflow monitoring
- **[Architect Engine](../architect-engine/README.md)** - Task execution engine
- **[Architect CLI](../architect-cli/README.md)** - Command-line interface
