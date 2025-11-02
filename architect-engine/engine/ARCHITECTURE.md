# Architect Engine - Architecture & Design

## Overview

The Architect Engine is a task execution and plugin management system built with clean architecture principles and modern design patterns. It provides a flexible, extensible platform for running tasks across projects with support for custom plugins.

## Architecture

### Core Components

The engine follows a layered architecture with clear separation of concerns:

```
├── core/
│   ├── common/           # Shared types and utilities
│   ├── config/           # Configuration constants
│   ├── execution/        # Command execution
│   ├── plugin/           # Plugin system
│   │   ├── app/         # Application services
│   │   ├── domain/      # Domain models
│   │   └── infra/       # Infrastructure implementations
│   ├── project/         # Project management
│   │   ├── app/         # Application services
│   │   ├── domain/      # Domain models
│   │   ├── infra/       # Infrastructure implementations
│   │   └── interfaces/  # API controllers & DTOs
│   └── tasks/           # Task execution
│       ├── application/ # Application services
│       ├── domain/      # Domain models & events
│       ├── infrastructure/ # Infrastructure implementations
│       └── interfaces/  # API controllers & DTOs
```

## Design Patterns

### 1. Strategy Pattern
**Location**: `core/plugin/`

The plugin loading system uses the Strategy pattern to support multiple plugin sources:

```kotlin
interface PluginSource {
    fun canHandle(type: String): Boolean
    fun resolve(config: PluginSourceConfig): Result<File>
}
```

**Implementations**:
- `LocalPluginSource` - Loads plugins from local filesystem
- `GitHubPluginSource` - Downloads plugins from GitHub releases
- `PluginSourceRegistry` - Selects and delegates to appropriate source

**Benefits**:
- Easy to add new plugin sources without modifying existing code (Open/Closed Principle)
- Each source has single responsibility
- Runtime selection of appropriate strategy

### 2. Repository Pattern
**Location**: `core/utils/`, `core/project/app/repositories/`

Generic repository interface for data access:

```kotlin
interface Repository<T> {
    fun save(key: String, obj: T)
    fun get(key: String): T?
    fun getAll(): List<T>
}
```

**Implementations**:
- `InMemoryRepository<T>` - Base implementation with in-memory storage
- `ProjectRepository` - Specialized repository for projects

**Benefits**:
- Abstraction over data storage
- Easy to swap implementations (in-memory, database, etc.)
- Testable with mock repositories

### 3. Single Responsibility Principle
**Location**: Throughout codebase

Each class has one reason to change:

- `TaskDependencyResolver` - Only resolves task dependencies
- `TaskExecutor` - Only executes tasks
- `TaskCache` - Only manages task result caching
- `BashCommandExecutor` - Only executes bash commands

### 4. Dependency Injection
**Location**: All services

Using Micronaut's DI container:

```kotlin
@Singleton
class TaskService(
    private val projectService: ProjectService,
    private val executor: TaskExecutor,
    private val eventCollector: ExecutionEventCollector
)
```

**Benefits**:
- Loose coupling
- Easy testing with mocks
- Centralized configuration

### 5. Event-Driven Architecture
**Location**: `core/tasks/domain/events/`, `core/plugin/domain/events/`

Events for task execution lifecycle:

- `ExecutionStarted`
- `TaskStarted`
- `TaskCompleted`
- `TaskFailed`
- `ExecutionCompleted`

**Benefits**:
- Decoupled components
- Real-time execution monitoring
- Easy to add new event listeners

### 6. Result Type (Monad Pattern)
**Location**: `core/common/Result.kt`

Functional error handling without exceptions:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure(val message: String, val cause: Throwable?) : Result<Nothing>()
}
```

**Operations**:
- `map`, `flatMap` - Transform success values
- `fold` - Handle both success and failure
- `onSuccess`, `onFailure` - Side effects
- `getOrNull`, `getOrThrow`, `getOrElse` - Value extraction

**Benefits**:
- Type-safe error handling
- Explicit error handling in signatures
- Composable operations

## Configuration

### Centralized Configuration
**Location**: `core/config/EngineConfiguration.kt`

All configuration constants in one place:

```kotlin
object EngineConfiguration {
    object Cache {
        const val ENABLED = "architect.cache.enabled"
        const val DEFAULT_ENABLED = false
    }
    
    object CommandExecutor {
        const val TIMEOUT_SECONDS = "architect.engine.executor.timeout-seconds"
        const val DEFAULT_TIMEOUT_SECONDS = 300L
    }
}
```

**Benefits**:
- Easy to discover all configuration options
- Type-safe defaults
- Consistent naming

### Application Properties
Create `application.yml` to configure:

```yaml
architect:
  cache:
    enabled: true
  engine:
    core:
      project:
        cache:
          enabled: true
    executor:
      timeout-seconds: 600
      redirect-error-stream: true
    events:
      replay-size: 128
      buffer-capacity: 128
    plugins:
      download-timeout-seconds: 300
      download-retry-attempts: 3
      user-agent: "ArchitectPlatform/1.0"
```

## Key Features

### Task Execution
- **Dependency Resolution**: Automatic topological sorting of task dependencies
- **Caching**: Optional task result caching for improved performance
- **Parallel Execution**: Concurrent execution of independent subprojects
- **Event Streaming**: Real-time execution events via Kotlin Flow

### Plugin System
- **Extensible**: Easy to add new plugin sources
- **Typed Configuration**: Type-safe plugin configuration
- **SPI Support**: Service Provider Interface for plugin discovery
- **Caching**: Downloaded plugins cached locally

### Command Execution
- **Configurable Timeout**: Prevent hung commands
- **Error Stream Handling**: Optional stderr redirection
- **Working Directory**: Execute commands in specific directories
- **Logging**: Detailed execution logging

## Testing

### Test Coverage
- **58 tests** covering core functionality
- Unit tests for all major components
- Test utilities for creating test fixtures

### Test Structure
```
test/
├── core/
│   ├── common/           # Result type tests (27 tests)
│   ├── execution/        # BashCommandExecutor tests (15 tests)
│   ├── tasks/
│   │   ├── application/  # TaskCache tests (6 tests)
│   │   └── domain/       # TaskDependencyResolver tests (10 tests)
│   ├── utils/            # Repository tests
│   └── project/          # ProjectService tests
```

## Best Practices

### 1. Error Handling
Use `Result<T>` for operations that can fail:

```kotlin
fun loadPlugin(config: PluginSourceConfig): Result<File> {
    return Result.catching {
        // operation that might throw
    }
}
```

### 2. Logging
Use SLF4J with appropriate log levels:

```kotlin
private val logger = LoggerFactory.getLogger(this::class.java)

logger.debug("Detailed information for debugging")
logger.info("Important state changes")
logger.error("Error conditions", exception)
```

### 3. Configuration
Use configuration objects for configurable values:

```kotlin
@Property(name = EngineConfiguration.Cache.ENABLED, 
         defaultValue = "${EngineConfiguration.Cache.DEFAULT_ENABLED}")
private val cacheEnabled: Boolean = false
```

### 4. Dependency Injection
Inject dependencies through constructor:

```kotlin
@Singleton
class MyService(
    private val dependency1: Dependency1,
    private val dependency2: Dependency2
)
```

### 5. Testing
Write focused unit tests:

```kotlin
@Test
fun `should handle specific scenario`() {
    // Given
    val input = prepareInput()
    
    // When
    val result = serviceUnderTest.method(input)
    
    // Then
    assertEquals(expected, result)
}
```

## Extending the Engine

### Adding a New Plugin Source

1. Create implementation of `PluginSource`:

```kotlin
@Singleton
class MavenPluginSource : PluginSource {
    override fun canHandle(type: String) = type == "maven"
    
    override fun resolve(config: PluginSourceConfig): Result<File> {
        // Implementation
    }
}
```

2. Register in Micronaut context (automatic with `@Singleton`)

3. Use in plugin configuration:

```yaml
plugins:
  - type: maven
    name: my-plugin
    version: 1.0.0
    groupId: com.example
    artifactId: my-plugin
```

### Adding Configuration Options

1. Add to `EngineConfiguration`:

```kotlin
object MyFeature {
    const val OPTION_NAME = "architect.my-feature.option-name"
    const val DEFAULT_VALUE = "default"
}
```

2. Use in component:

```kotlin
@Property(name = EngineConfiguration.MyFeature.OPTION_NAME,
         defaultValue = EngineConfiguration.MyFeature.DEFAULT_VALUE)
private val optionName: String = "default"
```

## Performance Considerations

1. **Caching**: Enable task and project caching for repeated executions
2. **Parallel Execution**: Subprojects execute concurrently
3. **Event Buffering**: Configurable buffer sizes for event streams
4. **Resource Cleanup**: Proper cleanup of resources and threads

## Security Considerations

1. **Plugin Verification**: Validate plugin sources before loading
2. **Command Execution**: Sanitize commands to prevent injection
3. **Network Requests**: Use timeouts and respect rate limits
4. **Error Messages**: Don't expose sensitive information in errors

## Future Enhancements

- [ ] Plugin verification with checksums
- [ ] Retry policies for network operations
- [ ] Metrics collection
- [ ] Distributed task execution
- [ ] Plugin sandboxing
- [ ] Configuration validation
