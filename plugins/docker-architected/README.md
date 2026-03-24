# docker-architected

> **Status**: Experimental (incubating tier) — see [STATUS.md](STATUS.md)

Docker build, push, and compose workflow automation for Architect projects.

## Tasks

| Task ID | Description |
|---|---|
| `docker-build` | Build a Docker image using the project Dockerfile |
| `docker-push` | Push the built image to the configured registry |
| `docker-run` | Run the Docker image locally |
| `docker-compose-up` | Start services defined in the compose file |

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
./gradlew build
./gradlew test
```

## Limitations

This plugin is at incubating tier. Tests and documentation are minimal. See [STATUS.md](STATUS.md) for graduation criteria.
