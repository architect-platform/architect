# Architect API

The Architect API is a Kotlin library that provides core abstractions and interfaces for building plugins in the Architect platform. It defines the fundamental building blocks for creating tasks, managing project lifecycles, and implementing custom workflows.

## Overview

The Architect API provides a plugin-based architecture where plugins can register tasks that execute during various phases of a project's lifecycle. This API is designed to be extended by plugins that implement specific functionality for different types of projects and technologies.

## Core Concepts

### Tasks

Tasks are units of work that can be executed as part of a project lifecycle. Each task has:
- A unique `id`
- An optional `phase` it belongs to
- Dependencies on other tasks
- An `execute` method that performs the actual work

```kotlin
interface Task {
  val id: String
  fun description(): String
  fun phase(): Phase?
  fun depends(): List<String>
  fun execute(environment: Environment, projectContext: ProjectContext, args: List<String>): TaskResult
}
```

### Phases

Phases represent stages in a project lifecycle (e.g., init, build, test, release). Tasks are associated with phases to organize the execution flow.

The API provides several built-in workflows:
- **CoreWorkflow**: Main project lifecycle phases (init → lint → verify → build → run/test → release → publish)
- **CodeWorkflow**: Code-specific sub-phases that map to core phases
- **HooksWorkflow**: Git hook phases (pre-commit, pre-push, commit-msg)

### Plugins

Plugins extend the Architect platform by registering custom tasks. Each plugin:
- Has a unique `id`
- Can access a typed context
- Registers tasks during initialization

```kotlin
interface ArchitectPlugin<C> {
  val id: String
  val contextKey: String
  val ctxClass: Class<C>
  var context: C
  
  fun init(context: Any)
  fun register(registry: TaskRegistry)
}
```

### Project Context

The `ProjectContext` provides access to project-specific information:
- Project directory path
- Configuration data (as a key-value map)

### Environment

The `Environment` interface allows tasks to:
- Access services via dependency injection
- Publish events for inter-task communication

## Task Implementations

The API includes two convenience implementations:

### SimpleTask

A simple task implementation that doesn't require command-line arguments:

```kotlin
class SimpleTask(
  override val id: String,
  private val description: String,
  private val phase: Phase,
  private val task: (Environment, ProjectContext) -> TaskResult
)
```

### TaskWithArgs

A task implementation that accepts command-line arguments:

```kotlin
class TaskWithArgs(
  override val id: String,
  val description: String,
  private val phase: Phase,
  private val task: (Environment, ProjectContext, List<String>) -> TaskResult
)
```

## Component Interfaces

### CommandExecutor

Interface for executing shell commands:

```kotlin
interface CommandExecutor {
  fun execute(command: String, workingDir: String? = null)
}
```

### ResourceExtractor

Interface for working with classpath resources:

```kotlin
interface ResourceExtractor {
  fun copyFileFromResources(classLoader: ClassLoader, resourcePath: String, targetDir: Path, targetFileName: String? = null)
  fun copyDirectoryFromResources(classLoader: ClassLoader, resourceRoot: String, targetDirectory: Path)
  fun getResourceFileContent(classLoader: ClassLoader, resourcePath: String): String
  fun listResourceFiles(classLoader: ClassLoader, resourceRoot: String): List<String>
}
```

## Building

The project uses Gradle with Kotlin:

```bash
cd api
./gradlew build
```

## Testing

Run the test suite:

```bash
./gradlew test
```

## Publishing

The library is published to GitHub Packages. To publish:

```bash
./gradlew publish
```

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
