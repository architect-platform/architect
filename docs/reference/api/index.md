# API Reference (KDoc)

The Architect API KDoc reference documents all public types, interfaces, and utilities that plugin authors depend on.

## Generating the KDoc reference

```bash
cd architect-api/api
./gradlew dokkaHtml
# Output: architect-api/api/build/dokka/html/
```

Open `build/dokka/html/index.html` in your browser to browse the full API.

## Key packages

| Package | Description |
|---------|-------------|
| `io.github.architectplatform.api.core.plugins` | `ArchitectPlugin` interface — the contract every JVM plugin must implement |
| `io.github.architectplatform.api.core.tasks` | `Task`, `TaskRegistry`, `TaskResult`, `Environment` |
| `io.github.architectplatform.api.core.tasks.phase` | `Phase` interface and built-in phase enums |
| `io.github.architectplatform.api.components.workflows.core` | `CoreWorkflow` phases (INIT → PUBLISH) |
| `io.github.architectplatform.api.components.workflows.code` | `CodeWorkflow` phases (CODE-init → CODE-publish) |
| `io.github.architectplatform.api.components.workflows.hooks` | `HooksWorkflow` phases (pre-commit, pre-push, commit-msg) |
| `io.github.architectplatform.api.components.execution` | `CommandExecutor`, `ResourceExtractor` |
| `io.github.architectplatform.api.core.project` | `ProjectContext`, path utilities |
| `io.github.architectplatform.api.core.utils` | `ShellUtils` — shell escaping utilities |
| `io.github.architectplatform.api.testing` | `ArchitectPluginTestKit` — in-memory test harness |

## Core interfaces

### `ArchitectPlugin<C>`

```kotlin
interface ArchitectPlugin<C> {
    val id: String
    val contextKey: String
    val ctxClass: Class<C>
    var context: C
    fun register(registry: TaskRegistry)
}
```

### `Task`

```kotlin
interface Task {
    val id: String
    fun phase(): Phase
    fun execute(
        environment: Environment,
        projectContext: ProjectContext,
        args: List<String>
    ): TaskResult
}
```

### `TaskResult`

```kotlin
data class TaskResult(
    val success: Boolean,
    val message: String,
    val results: List<TaskResult> = emptyList()
) {
    companion object {
        fun success(message: String, results: List<TaskResult> = emptyList()): TaskResult
        fun failure(message: String, results: List<TaskResult> = emptyList()): TaskResult
    }
}
```

### `TaskRegistry`

```kotlin
interface TaskRegistry {
    fun add(task: Task)
    fun getAll(): List<Task>
    fun findById(id: String): Task?
}
```

## Testing utilities

Use `ArchitectPluginTestKit` for fast, in-memory plugin tests:

```kotlin
val kit = ArchitectPluginTestKit(MyPlugin())
kit.configure(mapOf("setting" to "value"))
val result = kit.executeTask("my-task")
assertTrue(result.success)
```

## Shell escaping utilities

All JVM plugins should use `ShellUtils` when building shell commands:

```kotlin
import io.github.architectplatform.api.core.utils.ShellUtils

// Escape a single argument
val safe = ShellUtils.escapeShellArg(userInput)

// Escape a list of arguments and join with spaces
val safeArgs = ShellUtils.escapeShellArgs(argList)

// Validate an identifier (e.g. profile name, target triple)
val identifier = ShellUtils.requireSafeIdentifier(ctx.profile, "profile")
```

`requireSafeIdentifier` throws `IllegalArgumentException` for values containing characters outside `[a-zA-Z0-9._-]`.
