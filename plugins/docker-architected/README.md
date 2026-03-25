# docker-architected

> **Status**: Experimental (incubating tier) — see [STATUS.md](STATUS.md)

## Overview

docker-architected integrates Docker into the Architect task lifecycle, providing tasks for building images, running containers, and managing Docker Compose services. It enables teams to manage containerization through standardized Architect commands.

## Tasks

| Task ID | Description |
|---|---|
| `docker-build` | Build a Docker image using the project Dockerfile |
| `docker-push` | Push the built image to the configured registry |
| `docker-run` | Run the Docker image locally |
| `docker-compose-up` | Start services defined in the compose file |
| `docker-compose-down` | Stop and remove compose services |
| `docker-compose-logs` | Stream logs from compose services |

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: docker-architected
    repo: architect-platform/docker-architected

docker:
  image: myapp
  registry: ghcr.io/my-org
  dockerfile: Dockerfile
  platforms: [linux/amd64, linux/arm64]
  composeFile: docker-compose.yml
```

## Local Build and Test

```bash
cd plugins/docker-architected/app
gradle build
gradle test
```

## Limitations

This plugin is at incubating tier. Tests and documentation are minimal. See [STATUS.md](STATUS.md) for graduation criteria.
