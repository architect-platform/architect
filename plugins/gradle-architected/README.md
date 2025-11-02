# Gradle Architected Plugin

An Architect plugin that integrates Gradle build tasks into the Architect workflow system, enabling seamless Gradle project management.

## Overview

The Gradle Architected plugin provides integration between Gradle build tools and the Architect platform. It allows you to:
- Execute Gradle tasks within the Architect workflow
- Manage multi-module Gradle projects
- Configure project-specific Gradle settings
- Publish packages to GitHub Packages

## Features

### 1. Build Lifecycle Integration

The plugin maps Gradle tasks to Architect workflow phases:

| Architect Phase | Gradle Task | Purpose |
|----------------|-------------|---------|
| `INIT` | (none) | Initialize Gradle context |
| `BUILD` | `build` | Compile and assemble project |
| `TEST` | `test` | Run unit tests |
| `RUN` | `run` | Execute the application |
| `PUBLISH` | `publishGprPublicationToGitHubPackagesRepository` | Publish to GitHub Packages |

### 2. Multi-Project Support

Configure multiple Gradle projects within a single Architect project:

```yaml
gradle:
  projects:
    - name: "api"
      path: "api"
      gradlePath: "./gradlew"
    - name: "web"
      path: "web"
      gradlePath: "../gradlew"
      githubPackageRelease: true
```

### 3. Conditional Execution

Tasks can be conditionally enabled based on project configuration:
- `githubPackageRelease`: Controls whether the project should be published to GitHub Packages

## Architecture

### Core Components

- **GradlePlugin**: Main plugin class implementing `ArchitectPlugin`
- **GradleTask**: Task executor for Gradle commands
- **GradleContext**: Container for all Gradle project configurations
- **GradleProjectContext**: Configuration for individual Gradle projects

### Configuration Model

```kotlin
GradleContext(
  projects: List<GradleProjectContext>
)

GradleProjectContext(
  name: String,              // Project identifier
  path: String,              // Relative path from root
  githubPackageRelease: Boolean,  // Enable publishing
  gradlePath: String         // Path to gradlew script
)
```

## Tasks

### `gradle-` (Init)
- **Phase**: `INIT`
- **Purpose**: Initialize Gradle context
- **Command**: (none)

### `gradle-build`
- **Phase**: `BUILD`
- **Purpose**: Build all configured projects
- **Command**: `./gradlew build`

### `gradle-test`
- **Phase**: `TEST`
- **Purpose**: Run tests for all projects
- **Command**: `./gradlew test`

### `gradle-run`
- **Phase**: `RUN`
- **Purpose**: Run application
- **Command**: `./gradlew run`

### `gradle-publishGprPublicationToGitHubPackagesRepository`
- **Phase**: `PUBLISH`
- **Purpose**: Publish to GitHub Packages
- **Command**: `./gradlew publishGprPublicationToGitHubPackagesRepository`
- **Condition**: Only runs if `githubPackageRelease` is enabled

## Building

```bash
cd app
./gradlew build
```

**Note**: Building requires access to the Architect API package. Set the following environment variables:
- `GITHUB_USER` or `githubUser` property
- `REGISTRY_TOKEN` or `GITHUB_TOKEN` environment variable

## Usage

### Basic Configuration

Add the plugin configuration to your `architect.yml`:

```yaml
gradle:
  projects:
    - name: "main"
      path: "."
```

### Multi-Module Configuration

```yaml
gradle:
  projects:
    - name: "core"
      path: "core"
      gradlePath: "../gradlew"
    - name: "api"
      path: "api"
      gradlePath: "../gradlew"
    - name: "web"
      path: "web"
      gradlePath: "../gradlew"
      githubPackageRelease: true
```

### Custom Gradle Wrapper Path

```yaml
gradle:
  projects:
    - name: "legacy"
      path: "legacy-module"
      gradlePath: "./legacy-module/gradlew"
```

### Publishing Configuration

To enable GitHub Package publishing for a project:

```yaml
gradle:
  projects:
    - name: "library"
      path: "lib"
      githubPackageRelease: true
```

## Execution Flow

1. **Task Invocation**: Architect invokes a Gradle task (e.g., `build`)
2. **Project Iteration**: The plugin iterates over all configured projects
3. **Condition Check**: Validates if task should run for each project
4. **Command Execution**: Runs the Gradle command in the project directory
5. **Result Aggregation**: Collects results from all projects
6. **Success Check**: Fails if any project task fails

## Technical Stack

- **Language**: Kotlin 1.9.25
- **JVM**: Java 17
- **Dependencies**: Architect API 1.1.2
- **Build Tool**: Gradle 8.14.3

## Development

### Adding New Gradle Tasks

To add a new Gradle task to the plugin:

```kotlin
registry.add(
    GradleTask(
        "taskName",           // Gradle command
        WorkflowPhase.PHASE,  // Architect phase
        context               // Gradle context
    )
)
```

### Adding Conditional Tasks

For tasks that should only run under certain conditions:

```kotlin
registry.add(
    GradleTask(
        "taskName",
        WorkflowPhase.PHASE,
        context
    ) { projectContext ->
        projectContext.someCondition  // Return Boolean
    }
)
```

### Customizing Command Execution

The `CommandExecutor` service is used to run Gradle commands:

```kotlin
commandExecutor.execute(
    "${gradleProjectContext.gradlePath} taskName ${args.joinToString(" ")}",
    workingDir = gradleProjectDir.toString()
)
```

## Error Handling

The plugin provides robust error handling:
- **Project-level Failures**: Each project failure is captured separately
- **Detailed Messages**: Exceptions include project context
- **Aggregated Results**: Overall task result shows all project statuses
- **Early Exit**: Task fails if any project fails

## Task Results

Task results are hierarchical:

```
✅ Gradle task: gradle-build executed successfully for all projects
  ├── ✅ Gradle task: gradle-build over gradle project: core completed successfully
  ├── ✅ Gradle task: gradle-build over gradle project: api completed successfully
  └── ✅ Gradle task: gradle-build over gradle project: web completed successfully
```

## Best Practices

1. **Use Relative Paths**: Configure paths relative to the project root
2. **Shared Gradle Wrapper**: Use a single `gradlew` for all modules when possible
3. **Conditional Publishing**: Only enable `githubPackageRelease` for library projects
4. **Error Handling**: Ensure Gradle tasks return proper exit codes
5. **Working Directory**: Let the plugin manage working directories for each project

## Limitations

- Gradle must be installed or a Gradle wrapper must be available
- Project paths are relative to the Architect project root
- Gradle tasks must support command-line execution
- Publishing requires proper GitHub credentials configuration in Gradle
