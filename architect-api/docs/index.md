# Architect API

Core abstractions and interfaces for building Architect plugins and extensions.

## Overview

The Architect API is a Kotlin library that provides the foundational building blocks for creating plugins in the Architect platform. It defines core interfaces, data structures, and contracts that plugins must implement to integrate seamlessly with the Architect ecosystem.

## Key Concepts

### 1. Plugins

Plugins are the primary extension mechanism in Architect. They implement the `ArchitectPlugin` interface and can register tasks, hooks, and workflows.

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

### 2. Tasks

Tasks are units of work that can be executed by the Architect Engine. Each task has:
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
  fun execute(
    environment: Environment, 
    projectContext: ProjectContext, 
    args: List<String>
  ): TaskResult
}
```

### 3. Workflows

Workflows organize tasks into logical phases and manage their execution order.

**Built-in Workflows:**
- **CoreWorkflow**: Main project lifecycle phases (init → lint → verify → build → run/test → release → publish)
- **CodeWorkflow**: Code-specific sub-phases that map to core phases
- **HooksWorkflow**: Git hook phases (pre-commit, pre-push, commit-msg)

### 4. Context

Context objects hold configuration and state for plugins, projects, and execution environments.

## Creating a Custom Plugin

### Step 1: Define Your Plugin Class

```kotlin
package com.example.myplugin

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.SimpleTask
import io.github.architectplatform.api.components.workflows.core.CoreWorkflow

class MyPlugin : ArchitectPlugin<MyContext> {
    override val id = "my-plugin"
    override val contextKey = "myplugin"
    override val ctxClass = MyContext::class.java
    override var context: MyContext = MyContext()
    
    override fun register(registry: TaskRegistry) {
        registry.add(SimpleTask(
            id = "my-task",
            description = "My custom task",
            phase = CoreWorkflow.BUILD,
            task = ::executeMyTask
        ))
    }
    
    private fun executeMyTask(
        environment: Environment,
        projectContext: ProjectContext
    ): TaskResult {
        // Implement your task logic
        return TaskResult.success("Task completed!")
    }
}
```

### Step 2: Define Configuration Context

```kotlin
data class MyContext(
    val enabled: Boolean = true,
    val customSetting: String = "default-value"
)
```

### Step 3: Register the Plugin

Create `META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin`:

```
com.example.myplugin.MyPlugin
```

## Workflow Phases

| Phase | Purpose | Example Tasks |
|-------|---------|---------------|
| `INIT` | Initialize project structure | `docs-init`, `git-init` |
| `LINT` | Code quality checks | `ktlint`, `eslint` |
| `VERIFY` | Security and validation | `audit`, `scan` |
| `BUILD` | Compile and build | `gradle-build`, `npm-build` |
| `TEST` | Run tests | `gradle-test`, `jest` |
| `RUN` | Execute application | `gradle-run`, `npm-start` |
| `RELEASE` | Version and tag | `github-release` |
| `PUBLISH` | Deploy artifacts | `docs-publish`, `npm-publish` |

## Component Interfaces

### CommandExecutor

Execute shell commands safely:

```kotlin
val executor = environment.service(CommandExecutor::class.java)
executor.execute("npm install", workingDir)
```

### ResourceExtractor

Extract embedded resources from plugin JARs:

```kotlin
val extractor = environment.service(ResourceExtractor::class.java)
val content = extractor.getResourceFileContent(
    classLoader,
    "templates/config.yml"
)
```

## Best Practices

1. **Use meaningful task IDs**: Task IDs should clearly indicate their purpose
2. **Handle errors gracefully**: Always return appropriate `TaskResult` objects
3. **Sanitize inputs**: Validate and sanitize all user inputs
4. **Document your plugin**: Provide clear documentation with examples
5. **Version your context**: Make context data classes backwards-compatible
6. **Use semantic versioning**: Follow semantic versioning for plugin releases

## Examples

See the official plugins for real-world examples:
- [docs-architected](../../plugins/docs-architected/)
- [git-architected](../../plugins/git-architected/)
- [github-architected](../../plugins/github-architected/)

## Contributing

Contributions are welcome! Please ensure backwards compatibility is maintained.

## License

Apache License 2.0
