# gradle-architected

Gradle build tool integration for Architect.

**Source**: `architect-platform/gradle-architected`

## Installation

```yaml
plugins:
  - name: gradle-architected
    type: github
    repo: architect-platform/gradle-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `gradle-` | `INIT` | Initialize Gradle project detection |
| `gradle-build` | `BUILD` | Build all configured Gradle projects |
| `gradle-test` | `TEST` | Run tests for all configured projects |
| `gradle-run` | `RUN` | Run the application |
| `gradle-publishGprPublicationToGitHubPackagesRepository` | `PUBLISH` | Publish to GitHub Packages (conditional) |

## Configuration

Configuration key: `gradle`

```yaml
gradle-architected:
  projects:
    - name: api
      path: architect-api/api
      gradlePath: ./gradlew
      githubPackageRelease: true

    - name: engine
      path: architect-engine/engine
      gradlePath: ./gradlew
      githubPackageRelease: false
```

### `gradle.projects[]`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `name` | `string` | — | **Required.** Identifier for this Gradle sub-project. |
| `path` | `string` | `.` | Relative path from the Architect project root to the Gradle project. |
| `gradlePath` | `string` | `./gradlew` | Path to the Gradle wrapper script. |
| `githubPackageRelease` | `boolean` | `false` | If `true`, the `gradle-publishGpr...` task will run for this project. |

## Usage examples

```bash
# Build all projects
architect gradle-build

# Run tests with extra args
architect gradle-test -- --tests "*IntegrationTest*"

# Publish to GitHub Packages (only projects with githubPackageRelease: true)
architect gradle-publishGprPublicationToGitHubPackagesRepository
```
