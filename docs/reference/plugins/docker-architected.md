# docker-architected

Docker and Docker Compose integration for Architect.

**Source**: `architect-platform/docker-architected`

## Installation

```yaml
plugins:
  - name: docker-architected
    type: github
    repo: architect-platform/docker-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `docker-build` | `BUILD` | Build a Docker image |
| `docker-push` | `PUBLISH` | Push image to a container registry |
| `docker-run` | `RUN` | Run a container from the built image |
| `docker-compose-up` | `RUN` | Start services with Docker Compose |
| `docker-compose-down` | `RUN` | Stop and remove Compose services |
| `docker-compose-logs` | `RUN` | Stream logs from Compose services |

## Configuration

Configuration key: `docker`

```yaml
docker-architected:
  image: my-org/my-app
  tag: latest
  dockerfile: Dockerfile
  context: .
  platforms:
    - linux/amd64
    - linux/arm64
  buildArgs:
    BUILD_DATE: "2024-01-01"
    GIT_COMMIT: ""
  push: false
  composeFile: docker-compose.yml
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `image` | `string` | — | Image name (e.g. `my-org/my-app`) |
| `tag` | `string` | `latest` | Image tag |
| `dockerfile` | `string` | `Dockerfile` | Path to Dockerfile |
| `context` | `string` | `.` | Docker build context directory |
| `platforms` | `string[]` | `[linux/amd64]` | Target platforms for multi-arch builds |
| `buildArgs` | `map<string,string>` | `{}` | `--build-arg` key-value pairs |
| `push` | `boolean` | `false` | Push after build |
| `composeFile` | `string` | `docker-compose.yml` | Path to Docker Compose file |

## Usage examples

```bash
# Build image
architect docker-build

# Build and push multi-arch
architect docker-build
architect docker-push

# Start Compose stack
architect docker-compose-up

# Tail logs
architect docker-compose-logs

# Stop stack
architect docker-compose-down
```
