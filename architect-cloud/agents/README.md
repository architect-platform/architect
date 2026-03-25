# Architect Cloud Agents

**Docker Compose and Kubernetes agent definitions for remote Architect execution.**

## Overview

The `architect-cloud/agents` module contains the infrastructure definitions that
allow Architect Engine instances to run as managed agents in containerised
environments. Each agent type is a self-contained sub-directory:

| Directory | Purpose |
|---|---|
| `common/` | Shared configuration, health-check scripts, and base images used by all agent types |
| `docker-compose-agent/` | Docker Compose service definition for running an Architect agent on a single host |
| `kubernetes-agent/` | Kubernetes manifests (Deployment, Service, ConfigMap) for running agents in a cluster |

## How It Works

An *agent* is a lightweight Architect Engine instance that registers itself with
the Cloud Backend and accepts task execution requests. The definitions in this
module package the engine together with the correct environment variables,
networking, and health probes so it can be deployed with a single command.

## Quick Start

### Docker Compose

```bash
cd docker-compose-agent
docker compose up -d
```

The agent will start, register with the Cloud Backend at the URL specified in
the environment configuration, and begin accepting work.

### Kubernetes

```bash
cd kubernetes-agent
kubectl apply -k .
```

Adjust the ConfigMap values for your cluster before applying.

## Configuration

Agent behaviour is driven by environment variables passed through the
respective orchestrator. Key variables include:

| Variable | Description | Default |
|---|---|---|
| `ARCHITECT_CLOUD_URL` | Cloud Backend base URL | `http://localhost:8080` |
| `ARCHITECT_ENGINE_ID` | Unique agent identifier | auto-generated |
| `ARCHITECT_AGENT_TAGS` | Comma-separated capability tags | — |

## Related Modules

- [Architect Cloud (parent)](../README.md) — umbrella cloud module
- [Cloud Backend](../backend/README.md) — API service that agents register with
- [Architect Engine](../../architect-engine/) — the engine runtime packaged by agents
