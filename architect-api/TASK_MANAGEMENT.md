# Task Management System - Enhanced Features

This document describes the enhanced task management system in Architect, which provides flexible and powerful ways to organize and execute tasks.

## Overview

The enhanced task management system supports:

1. **Standalone Tasks** - Tasks without phase membership
2. **Phase Tasks** - Tasks organized into lifecycle phases
3. **Composite Tasks** - Tasks that contain and orchestrate child tasks
4. **Configurable Tasks** - Tasks with customizable configuration options
5. **Flexible Dependencies** - Custom dependencies beyond phase inheritance

## Core Concepts

### Task Types

#### 1. SimpleTask (Enhanced)

A basic task implementation that executes a lambda function. Now supports optional phases and custom dependencies.

**Key Features:**
- Can be standalone (no phase) or belong to a phase
- Supports custom dependencies beyond phase dependencies
- Convention over configuration with sensible defaults

**Example:**
```kotlin
// Standalone task
val task = SimpleTask(
  id = "verify-setup",
  description = "Verify project setup",
  phase = null,  // Standalone
  customDependencies = listOf("init-config")
) { env, ctx ->
  // Task logic
  TaskResult.success("Setup verified")
}

// Phase task
val buildTask = SimpleTask(
  id = "compile",
  description = "Compile source code",
  phase = CoreWorkflow.BUILD
) { env, ctx ->
  // Task logic
  TaskResult.success("Compiled successfully")
}
```

#### 2. TaskWithArgs (Enhanced)

A task that accepts command-line arguments. Now supports optional phases and custom dependencies.

**Key Features:**
- Handles command-line arguments
- Can be standalone or belong to a phase
- Flexible dependency management

**Example:**
```kotlin
val deployTask = TaskWithArgs(
  id = "deploy",
  description = "Deploy to environment",
  phase = null,
  customDependencies = listOf("build", "test")
) { env, ctx, args ->
  val environment = args.firstOrNull() ?: "staging"
  println("Deploying to $environment")
  TaskResult.success("Deployed to $environment")
}
```

#### 3. CompositeTask (NEW)

A task that can contain and orchestrate multiple child tasks, enabling hierarchical task organization.

**Key Features:**
- Defines parent-child task relationships
- Executes children in dependency order
- Supports before and after hooks
- Can be nested (composite tasks can have composite children)
- Combines phase and custom dependencies

**Example:**
```kotlin
val ciPipeline = CompositeTask(
  id = "ci-pipeline",
  description = "Complete CI pipeline",
  phase = null,  // Standalone workflow
  children = listOf("lint", "test", "build", "verify"),
  beforeChildren = { env, ctx ->
    println("Starting CI pipeline...")
    TaskResult.success("Pipeline started")
  },
  afterChildren = { env, ctx, childResults ->
    val allSuccess = childResults.all { it.success }
    if (allSuccess) {
      TaskResult.success("Pipeline completed", childResults)
    } else {
      TaskResult.failure("Pipeline failed", childResults)
    }
  }
)
```

#### 4. ConfigurableTask (NEW)

A task that supports configuration through a key-value map, enabling parameterization without code changes.

**Key Features:**
- Configuration via map of key-value pairs
- Helper methods for config access (getConfig, getRequiredConfig)
- Can read from project configuration files
- Supports both phase and custom dependencies

**Example:**
```kotlin
val notifyTask = ConfigurableTask(
  id = "notify",
  description = "Send build notification",
  phase = CoreWorkflow.PUBLISH,
  config = mapOf(
    "channel" to "slack",
    "webhook" to "https://hooks.slack.com/...",
    "enabled" to "true"
  )
) { env, ctx, cfg, args ->
  val enabled = cfg["enabled"]?.toBoolean() ?: true
  if (!enabled) {
    return@ConfigurableTask TaskResult.success("Notifications disabled")
  }
  
  val channel = cfg["channel"] ?: "email"
  val webhook = cfg.getOrElse("webhook") { 
    throw IllegalArgumentException("Webhook required")
  }
  
  // Send notification
  TaskResult.success("Notification sent to $channel")
}
```

## Task Relationships

### Dependencies

Tasks can depend on other tasks. Dependencies are resolved transitively and executed before the task.

```kotlin
val task = SimpleTask(
  id = "integration-test",
  description = "Run integration tests",
  phase = CoreWorkflow.TEST,  // Inherits: init, lint, verify, build
  customDependencies = listOf("start-database", "seed-data")
) { env, ctx ->
  // This runs after: init, lint, verify, build, start-database, seed-data
  TaskResult.success("Tests passed")
}
```

### Children (Parent-Child)

Composite tasks can have child tasks. Children are executed as part of the parent task.

```kotlin
val parentTask = CompositeTask(
  id = "build-all",
  description = "Build all components",
  children = listOf("build-backend", "build-frontend", "build-docs")
)

// When build-all is executed:
// 1. Parent task's beforeChildren hook (if any)
// 2. build-backend
// 3. build-frontend  
// 4. build-docs
// 5. Parent task's afterChildren hook (if any)
```

### Dependencies vs Children

| Aspect | Dependencies | Children |
|--------|-------------|----------|
| **When executed** | Before the task starts | As part of the task |
| **Purpose** | Prerequisites that must complete first | Subtasks that make up the work |
| **Visibility** | Executed separately in task list | Executed within parent |
| **Result handling** | Failures block task execution | Failures included in parent result |
| **Example** | "test" depends on "build" | "ci-pipeline" contains "lint", "test", "build" |

## Execution Model

### Basic Execution Flow

1. **Resolve dependencies**: Gather all transitive dependencies
2. **Resolve children**: For composite tasks, gather all child tasks
3. **Topological sort**: Order tasks respecting dependencies
4. **Execute in order**: Run each task, handling success/failure
5. **Execute children**: For each task with children, run child tasks
6. **Aggregate results**: Combine parent and child results

### Example Execution

Given these tasks:
```kotlin
registry.add(SimpleTask("init", "Initialize", CoreWorkflow.INIT) { ... })
registry.add(SimpleTask("lint", "Lint code", CoreWorkflow.LINT) { ... })
registry.add(SimpleTask("compile", "Compile", CoreWorkflow.BUILD) { ... })
registry.add(SimpleTask("test-unit", "Unit tests", null) { ... })
registry.add(SimpleTask("test-integration", "Integration tests", null) { ... })

registry.add(CompositeTask(
  id = "test-all",
  description = "All tests",
  phase = CoreWorkflow.TEST,
  children = listOf("test-unit", "test-integration")
))
```

When executing `test-all`:
1. Resolve phase dependencies: init → lint → verify → build
2. Execute: init, lint, verify, build
3. Execute test-all (composite):
   - Before hook (if any)
   - Execute children: test-unit, test-integration
   - After hook (if any)

## Best Practices

### 1. Choose the Right Task Type

- **SimpleTask**: For basic tasks without arguments or configuration
- **TaskWithArgs**: When you need to accept command-line arguments
- **CompositeTask**: To group related tasks into a logical unit
- **ConfigurableTask**: When behavior should be customizable via configuration

### 2. Use Phases for Lifecycle Organization

```kotlin
// Good: Standard lifecycle phases
SimpleTask("compile", "Compile source", CoreWorkflow.BUILD)
SimpleTask("run-tests", "Run tests", CoreWorkflow.TEST)
SimpleTask("package", "Create package", CoreWorkflow.RELEASE)
```

### 3. Use Composite Tasks for Workflows

```kotlin
// Good: Workflow as composite task
CompositeTask(
  id = "ci-workflow",
  children = listOf("lint", "test", "build", "verify")
)
```

### 4. Use Custom Dependencies Carefully

```kotlin
// Good: Specific dependency requirement
SimpleTask(
  id = "integration-test",
  phase = CoreWorkflow.TEST,
  customDependencies = listOf("start-services")
)

// Avoid: Too many custom dependencies (consider refactoring)
SimpleTask(
  id = "complex-task",
  customDependencies = listOf("a", "b", "c", "d", "e", "f")
)
```

### 5. Leverage Configuration for Flexibility

```kotlin
// Good: Configurable behavior
ConfigurableTask(
  id = "deploy",
  config = mapOf(
    "environment" to "production",
    "region" to "us-east-1",
    "timeout" to "300"
  )
) { env, ctx, cfg, args ->
  val environment = args.firstOrNull() ?: cfg["environment"]
  // Deploy logic
}
```

### 6. Use Hooks in Composite Tasks

```kotlin
CompositeTask(
  id = "build-all",
  children = listOf("build-backend", "build-frontend"),
  beforeChildren = { env, ctx ->
    // Clean build directories
    TaskResult.success("Prepared build")
  },
  afterChildren = { env, ctx, results ->
    if (results.all { it.success }) {
      // Copy artifacts to common location
      TaskResult.success("Build completed", results)
    } else {
      TaskResult.failure("Build failed", results)
    }
  }
)
```

## Migration Guide

### From Old to New API

#### Making Tasks Standalone

**Before:**
```kotlin
// Tasks had to have a phase
SimpleTask("my-task", "Description", CoreWorkflow.BUILD)
```

**After:**
```kotlin
// Tasks can be standalone
SimpleTask("my-task", "Description", phase = null)
```

#### Adding Custom Dependencies

**Before:**
```kotlin
// Only phase dependencies were available
// Had to override depends() method
class MyTask : Task {
  override fun depends() = listOf("custom-dep")
}
```

**After:**
```kotlin
// Custom dependencies built-in
SimpleTask(
  "my-task", 
  "Description",
  customDependencies = listOf("custom-dep")
)
```

#### Creating Composite Tasks

**Before:**
```kotlin
// Had to manually execute children
class BuildAll : Task {
  override fun execute(...) {
    val children = listOf("build-a", "build-b")
    // Manual child execution logic
  }
}
```

**After:**
```kotlin
// Built-in composite support
CompositeTask(
  "build-all",
  "Build everything",
  children = listOf("build-a", "build-b")
)
```

## Advanced Patterns

### Pattern 1: Hierarchical Workflows

```kotlin
// Top-level workflow
CompositeTask(
  id = "full-pipeline",
  children = listOf("prepare", "build-all", "test-all", "deploy")
)

// Nested workflows
CompositeTask(
  id = "build-all",
  children = listOf("build-backend", "build-frontend", "build-docs")
)

CompositeTask(
  id = "test-all",
  children = listOf("test-unit", "test-integration", "test-e2e")
)
```

### Pattern 2: Conditional Execution

```kotlin
ConfigurableTask(
  id = "optional-task",
  config = mapOf("enabled" to "true")
) { env, ctx, cfg, args ->
  if (cfg["enabled"]?.toBoolean() != true) {
    return@ConfigurableTask TaskResult.success("Task disabled")
  }
  // Actual task logic
  TaskResult.success("Task completed")
}
```

### Pattern 3: Dynamic Task Configuration

```kotlin
// Read from project context
ConfigurableTask(
  id = "environment-aware",
  config = projectContext.config.getMap("deployment")
) { env, ctx, cfg, args ->
  val targetEnv = cfg["target"] ?: "development"
  // Environment-specific logic
  TaskResult.success("Configured for $targetEnv")
}
```

### Pattern 4: Error Recovery

```kotlin
CompositeTask(
  id = "resilient-build",
  children = listOf("build-step-1", "build-step-2", "build-step-3"),
  afterChildren = { env, ctx, results ->
    val failures = results.filter { !it.success }
    if (failures.isNotEmpty()) {
      // Attempt recovery
      println("${failures.size} steps failed, attempting recovery...")
      // Recovery logic
    }
    TaskResult.success("Build completed with recovery", results)
  }
)
```

## Summary

The enhanced task management system provides:

- **Flexibility**: Tasks can be standalone, phase-based, or composite
- **Power**: Hierarchical organization with parent-child relationships
- **Configuration**: Parameterization through configuration maps
- **Convention**: Sensible defaults with customization options
- **Compatibility**: Backward compatible with existing code

For more examples, see `ExampleFlexiblePlugin.kt`.
