# Architect Cloud Agents

## Overview

The agents module defines container-based execution environments for running
Architect tasks remotely. It provides Docker Compose definitions for local
development and Kubernetes manifests for production deployment.

Agents allow the Architect Cloud platform to offload task execution to
dedicated containers, enabling horizontal scaling and isolation of build
workloads from the coordination backend.

## Components

- **Docker Compose** — Local multi-container setup for development and testing
- **Kubernetes** — Production manifests for scalable remote execution

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Kubernetes cluster (for production deployment)
- A running Architect Cloud Backend instance

### Local Development

```bash
cd architect-cloud/agents
docker compose up       # start local agent containers
docker compose down     # tear down the environment
```

### Kubernetes Deployment

```bash
kubectl apply -f k8s/   # apply production manifests
```

## Configuration

Agents are configured as part of the Architect Cloud module in `architect.yml`:

```yaml
# Example architect.yml snippet — cloud agents are managed
# through the architect-cloud umbrella module configuration.
# See architect-cloud/README.md for full configuration details.
```

## Status

This module is **incubating**. Agent definitions are scaffolded but not yet
production-validated.

## Links

- [Cloud README](../../README.md) — umbrella module overview
- [STATUS.md](../STATUS.md) — current support level and graduation criteria
- [Architect Platform Docs](https://architect-platform.github.io/architect/)
