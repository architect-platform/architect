# Platform-Agnostic Application API

## Overview

The Architect Server provides a **clean, simple, platform-agnostic API** for deploying applications and their dependencies. The API is designed to be independent of any specific platform (Kubernetes, Docker, etc.), allowing agents to map these generic concepts to their platform-specific implementations.

## Key Principles

1. **Platform Agnostic**: No Kubernetes, Docker, or platform-specific concepts in the API
2. **Simple & Clean**: Easy to understand and use
3. **Dependency Support**: First-class support for databases, brokers, and service dependencies
4. **Agent Mapping**: Agents translate generic concepts to platform-specific implementations

## Application Types

The API supports multiple application types:

- `APPLICATION`: Regular application/service
- `DATABASE`: Database services (PostgreSQL, MySQL, MongoDB, etc.)
- `MESSAGE_BROKER`: Message brokers (RabbitMQ, Kafka, Redis, etc.)
- `CACHE`: Cache services (Redis, Memcached, etc.)
- `STORAGE`: Storage services (MinIO, S3-compatible, etc.)
- `SERVICE`: Generic service

## Application Definition

### Core Fields

```json
{
  "name": "my-app",
  "version": "1.0.0",
  "type": "APPLICATION",
  "image": "myapp:1.0.0",
  "instances": 3,
  "environment": {
    "ENV": "production",
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
  "templateIds": ["app-deployment-template"]
}
```

## Dependencies

### Required Dependencies

Dependencies marked as `required: true` are deployed **before** the application and must be healthy before the application starts.

### Example: Application with Database

#### 1. Create Database

```json
POST /api/applications
{
  "name": "postgres-db",
  "version": "15.0",
  "type": "DATABASE",
  "image": "postgres:15",
  "instances": 1,
  "environment": {
    "POSTGRES_USER": "app_user",
    "POSTGRES_PASSWORD": "secret",
    "POSTGRES_DB": "app_database"
  },
  "exposedPorts": [
    {
      "port": 5432,
      "protocol": "TCP",
      "public": false
    }
  ],
  "resources": {
    "cpu": "1000m",
    "memory": "2Gi",
    "storage": "50Gi"
  },
  "healthCheck": {
    "type": "TCP",
    "port": 5432,
    "intervalSeconds": 10
  },
  "metadata": {
    "managed": "true"
  },
  "templateIds": ["postgres-template"]
}
```

#### 2. Create Application with Database Dependency

```json
POST /api/applications
{
  "name": "web-api",
  "version": "1.0.0",
  "type": "APPLICATION",
  "image": "web-api:1.0.0",
  "instances": 3,
  "environment": {
    "DATABASE_URL": "postgresql://app_user:secret@postgres-db:5432/app_database"
  },
  "exposedPorts": [
    {
      "port": 8080,
      "protocol": "TCP",
      "public": true
    }
  ],
  "dependencies": [
    {
      "applicationId": "postgres-db",
      "required": true,
      "connectionInfo": {
        "host": "postgres-db",
        "port": "5432",
        "database": "app_database"
      }
    }
  ],
  "templateIds": ["app-deployment-template"]
}
```

### Example: Microservices with Message Broker

#### 1. Create RabbitMQ Broker

```json
POST /api/applications
{
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
    {
      "port": 5672,
      "protocol": "TCP",
      "public": false
    },
    {
      "port": 15672,
      "protocol": "TCP",
      "public": true
    }
  ],
  "resources": {
    "cpu": "500m",
    "memory": "1Gi"
  },
  "templateIds": ["rabbitmq-template"]
}
```

#### 2. Create Producer Service

```json
POST /api/applications
{
  "name": "order-service",
  "version": "1.0.0",
  "type": "APPLICATION",
  "image": "order-service:1.0.0",
  "instances": 2,
  "environment": {
    "RABBITMQ_URL": "amqp://admin:secret@rabbitmq:5672"
  },
  "dependencies": [
    {
      "applicationId": "rabbitmq",
      "required": true,
      "connectionInfo": {
        "host": "rabbitmq",
        "port": "5672"
      }
    }
  ],
  "templateIds": ["app-deployment-template"]
}
```

#### 3. Create Consumer Service

```json
POST /api/applications
{
  "name": "notification-service",
  "version": "1.0.0",
  "type": "APPLICATION",
  "image": "notification-service:1.0.0",
  "instances": 3,
  "environment": {
    "RABBITMQ_URL": "amqp://admin:secret@rabbitmq:5672"
  },
  "dependencies": [
    {
      "applicationId": "rabbitmq",
      "required": true,
      "connectionInfo": {
        "host": "rabbitmq",
        "port": "5672"
      }
    }
  ],
  "templateIds": ["app-deployment-template"]
}
```

## Agent Mapping

Agents receive the platform-agnostic definition and map it to their specific implementation:

### Kubernetes Agent

Maps generic concepts to Kubernetes resources:
- `instances` → `replicas` in Deployment
- `exposedPorts` with `public: true` → Service with type LoadBalancer
- `exposedPorts` with `public: false` → Service with type ClusterIP
- `resources.cpu` → `resources.requests.cpu` and `resources.limits.cpu`
- `resources.memory` → `resources.requests.memory` and `resources.limits.memory`
- `resources.storage` → PersistentVolumeClaim
- `healthCheck.type: HTTP` → livenessProbe and readinessProbe with httpGet
- `healthCheck.type: TCP` → livenessProbe and readinessProbe with tcpSocket
- `dependencies` → initContainers to wait for dependencies
- `type: DATABASE` → StatefulSet instead of Deployment

### Docker Compose Agent

Maps generic concepts to Docker Compose:
- `instances` → `deploy.replicas`
- `exposedPorts` with `public: true` → ports published to host
- `exposedPorts` with `public: false` → ports exposed only internally
- `resources.cpu` → `deploy.resources.limits.cpus`
- `resources.memory` → `deploy.resources.limits.memory`
- `healthCheck` → `healthcheck` configuration
- `dependencies` → `depends_on` with health checks
- `type: DATABASE` → volumes for data persistence

## API Endpoints

### Application Management

```
POST   /api/applications              - Create application
GET    /api/applications              - List all applications
GET    /api/applications/{appId}      - Get application
PUT    /api/applications/{appId}      - Update application
DELETE /api/applications/{appId}      - Delete application
POST   /api/applications/{appId}/deploy - Deploy application
```

### Deployment with Dependencies

When deploying an application with dependencies:

1. Server checks all dependencies exist
2. Server orders dependencies (required first)
3. Agent deploys dependencies in order
4. Agent waits for each dependency to be healthy
5. Agent deploys the application
6. Agent reports status back to server

### Example Deployment Request

```bash
POST /api/applications/web-api/deploy?agentId=k8s-agent-1&operation=APPLY
```

The agent will:
1. Deploy `postgres-db` first (required dependency)
2. Wait for postgres to be healthy
3. Deploy `web-api`
4. Report success/failure

## Benefits

### For API Users
- **Simple**: No need to know Kubernetes, Docker, or platform specifics
- **Portable**: Same API works across all platforms
- **Clear**: Self-documenting with explicit types and dependencies

### For Platform Teams
- **Flexible**: Easy to add new platforms (Cloud Run, ECS, etc.)
- **Maintainable**: Changes to platforms don't affect API
- **Extensible**: Easy to add new application types

### For Developers
- **Focus on Application**: Not on infrastructure details
- **Dependency Management**: Built-in support for databases and brokers
- **Consistent**: Same API regardless of where it deploys

## Migration Example

### Before (Kubernetes-specific)

```json
{
  "name": "my-app",
  "namespace": "production",
  "replicas": 3,
  "ports": [
    {
      "name": "http",
      "containerPort": 8080,
      "servicePort": 80
    }
  ],
  "labels": {
    "app": "my-app"
  },
  "annotations": {
    "prometheus.io/scrape": "true"
  }
}
```

### After (Platform-agnostic)

```json
{
  "name": "my-app",
  "type": "APPLICATION",
  "version": "1.0.0",
  "image": "my-app:1.0.0",
  "instances": 3,
  "exposedPorts": [
    {
      "port": 8080,
      "public": true
    }
  ],
  "metadata": {
    "environment": "production",
    "metrics": "prometheus"
  }
}
```

The agent maps `metadata` and `instances` to appropriate platform-specific constructs.

## Template Variables

Templates receive these variables from the application definition:

```json
{
  "name": "my-app",
  "version": "1.0.0",
  "type": "APPLICATION",
  "image": "my-app:1.0.0",
  "instances": 3,
  "environment": { "KEY": "value" },
  "metadata": { "team": "backend" },
  "ports": [
    {
      "port": 8080,
      "protocol": "TCP",
      "public": true
    }
  ],
  "resources": {
    "cpu": "500m",
    "memory": "512Mi"
  },
  "healthCheck": {
    "type": "HTTP",
    "path": "/health",
    "port": 8080
  }
}
```

Agents can then use these variables in templates with proper mapping to platform concepts.

## License

MIT License - See main project LICENSE file.
