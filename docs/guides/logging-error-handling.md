# Logging and Error-Handling Conventions

This document defines the logging and error-handling standards for each module type in the Architect Platform.

## Logging Framework

All Kotlin modules **must** use SLF4J (`org.slf4j.Logger`) for structured logging.

- CLI user-facing output uses `println()` intentionally (console UX), but operational diagnostics must use SLF4J.
- Plugins may omit logging if task execution is fully captured by `TaskResult`.
- Never use `System.out.println()` or `System.err.println()` for diagnostic logging.

### Logger Declaration

```kotlin
import org.slf4j.LoggerFactory

class MyService {
  private val logger = LoggerFactory.getLogger(this::class.java)
}
```

## Log Levels by Purpose

| Level   | Use For                                                        |
|---------|----------------------------------------------------------------|
| `error` | Unrecoverable failures, exceptions that stop execution         |
| `warn`  | Recoverable issues, degraded behavior, fallback paths taken    |
| `info`  | Significant lifecycle events (startup, shutdown, registration) |
| `debug` | Execution flow details useful for troubleshooting              |
| `trace` | Fine-grained data (request/response payloads, cache lookups)   |

## Conventions by Module Type

### Core Libraries (`architect-api`)

- **Logging**: Minimal or none. API contracts should not log.
- **Error handling**: Throw `ArchitectException` subtypes for infrastructure errors. Return `TaskResult` for task execution outcomes.
- **Rationale**: API consumers control their own logging; the library should not impose a logging implementation.

### Shared Runtime (`architect-core`)

- **Logging**: SLF4J at `debug`/`info` for plugin loading, task execution, and cache operations.
- **Error handling**: Use `TaskResult` for task execution outcomes. Use the `Result<T>` sealed class for infrastructure operations that may fail (downloads, I/O). Throw `ArchitectException` subtypes only for unrecoverable errors.
- **Log on**: Plugin load start/complete, task batch execution, cache hits/misses, process lifecycle events.

### Server (`architect-engine`)

- **Logging**: SLF4J at `info` for HTTP endpoints and lifecycle events. `debug` for internal service calls.
- **Error handling**: Throw exceptions for invalid requests (translated to HTTP error responses by Micronaut). Use event-driven error propagation for async task execution via SSE.
- **Log on**: Project registration, task execution start/complete, SSE connection lifecycle, startup/shutdown.

### CLI (`architect-cli`)

- **Logging**: `println()` for user-facing output (task progress, results, errors). SLF4J at `warn`/`error` for operational issues (history read failures, engine connection problems).
- **Error handling**: Use `runCatching` with `getOrElse` for graceful degradation. Log exceptions before swallowing them. Use `exitProcess(1)` only for fatal errors.
- **Never**: Silently swallow exceptions without at minimum a `logger.warn()`.

### Plugins

- **Logging**: Optional. Plugins that shell out to external commands should log the command at `debug` level.
- **Error handling**: Wrap `execute()` in try/catch, return `TaskResult.failure()` with a descriptive message including the task ID and error details.
- **Pattern**:
  ```kotlin
  override fun execute(environment: Environment, projectContext: ProjectContext, args: List<String>): TaskResult {
    return try {
      // ... execution logic
      TaskResult.success("Task $id completed successfully")
    } catch (e: Exception) {
      TaskResult.failure("Task $id failed: ${e.message ?: "Unknown error"}")
    }
  }
  ```

## Error Model

The codebase uses three complementary error mechanisms:

| Mechanism                  | Scope                    | When to Use                                      |
|---------------------------|--------------------------|---------------------------------------------------|
| `TaskResult`              | Task execution outcomes  | Always for task `execute()` return values          |
| `ArchitectException` tree | Infrastructure failures  | Missing projects/tasks, plugin load failures, config errors |
| `Result<T>` sealed class  | Fallible operations      | Downloads, I/O, parsing — where failure is expected |

### Exception Hierarchy

```
ArchitectException (base — RuntimeException)
├── TaskNotFoundException        — task ID not found in registry
├── ProjectNotFoundException     — project name not registered
├── TaskExecutionException       — task failed during execution
├── PluginLoadException          — plugin JAR/process could not load
├── ConfigurationException       — invalid architect.yml or settings
└── TaskConditionException       — precondition check failed
```

**Rules**:
- Never throw raw `Exception` or `RuntimeException` — use the hierarchy.
- Always chain the underlying cause: `ArchitectException("msg", cause = e)`.
- Include context fields (taskId, projectName) for debuggability.

## Anti-Patterns

| ❌ Don't                                         | ✅ Do Instead                                      |
|--------------------------------------------------|-----------------------------------------------------|
| `catch (e: Exception) { /* ignore */ }`          | `catch (e: Exception) { logger.warn("...", e) }`   |
| `println("Error: $msg")` in server/core code     | `logger.error("Error: $msg")`                        |
| Throw `IllegalArgumentException` in services     | Throw `ConfigurationException` or `TaskNotFoundException` |
| Log stack traces at `info` level                 | Log stack traces at `error` or `debug` level         |
| Return `TaskResult.failure()` without task ID    | Include `"Task $id failed: ..."` in message          |
