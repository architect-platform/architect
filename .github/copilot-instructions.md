# Copilot Instructions for Architect Platform

This document provides guidance for AI assistants working in the Architect Platform repository.

## Project Overview

**Architect** is a plugin-based task execution framework written in Kotlin that automates project workflows, CI/CD pipelines, and development operations. It follows "convention over configuration" principles and provides a unified way to manage documentation, releases, builds, tests, and deployment across diverse technology stacks.

### Core Architecture

The project consists of four main components:

```
Architect CLI (Interface)
        ↓
Architect Engine (REST API Server)
        ↓
Architect API (Core Library)
        ↓
Plugins (Extensions)
```

Each component is independently buildable via Gradle. The **Architect API** is published to GitHub Packages and consumed by the Engine and Plugins.

## Build, Test, and Lint Commands

### Build All Components
```bash
# Full build of all modules
./gradlew build

# Build specific component
cd architect-cli/cli && ./gradlew build
cd architect-engine/engine && ./gradlew build
cd architect-api/api && ./gradlew build
cd plugins/PLUGIN_NAME/app && ./gradlew build
```

### Test Commands
```bash
# Run all tests across all modules
./gradlew test

# Run tests for a specific module
cd architect-engine/engine && ./gradlew test

# Run a specific test class
cd architect-engine/engine && ./gradlew test --tests TaskServiceTest

# Run with code coverage
./gradlew test jacocoTestReport
```

### Linting
```bash
# Lint API project (uses ktlint)
cd architect-api/api && ./gradlew ktlintCheck

# Auto-fix lint issues in API
cd architect-api/api && ./gradlew ktlintFormat
```

### Documentation
```bash
# Install MkDocs dependencies
pip install mkdocs mkdocs-material mkdocs-monorepo-plugin

# Build documentation
mkdocs build

# Serve documentation locally
mkdocs serve
```

## High-Level Architecture

### Component Breakdown

#### 1. **Architect API** (`architect-api/api/`)
- **Role**: Core abstraction library
- **Language**: Kotlin
- **Key exports**:
  - `Task` interface: Base unit of work
  - `Phase` interface: Lifecycle stages
  - `ArchitectPlugin` interface: Plugin contract
  - Built-in phases: `CoreWorkflow`, `CodeWorkflow`, `HooksWorkflow`
  - `TaskRegistry`, `Environment`, `ProjectContext`
- **Versioning**: Published to GitHub Packages as dependency for Engine and Plugins
- **Important**: Contains the fundamental contracts that plugins must implement

#### 2. **Architect Engine** (`architect-engine/engine/`)
- **Role**: REST API server that orchestrates task execution
- **Language**: Kotlin (Micronaut framework)
- **Responsibilities**:
  - Project registration and lifecycle management
  - Task discovery and dependency resolution
  - Execution orchestration and result aggregation
  - Server-Sent Events (SSE) for real-time execution streaming
- **Key services**: `TaskService`, `ProjectService`, `ExecutionService`, `ExecutionEventService`
- **API Endpoints**:
  - `GET /api/projects` - List projects
  - `POST /api/projects/{projectName}/tasks/{taskName}` - Execute task
  - `GET /api/executions/{executionId}/events` - Stream execution events
- **Dependencies**: Depends on `architect-api` from Maven Central (GitHub Packages)

#### 3. **Architect CLI** (`architect-cli/cli/`)
- **Role**: Interactive command-line interface for end users
- **Language**: Kotlin (Micronaut + PicoCLI)
- **Responsibilities**:
  - Command parsing and routing
  - HTTP communication with Engine
  - Formatted output and user interaction
- **Architecture**: Picocli-based command structure with HTTP client to Engine
- **Entry point**: `ArchitectLauncher`

#### 4. **Plugins** (`plugins/`)
Available official plugins:
- `git-architected/` - Git integration (status, commit, push, etc.)
- `github-architected/` - GitHub CI/CD and release automation
- `gradle-architected/` - Gradle build integration
- `javascript-architected/` - npm/yarn/pnpm integration
- `docs-architected/` - Multi-framework documentation (MkDocs, Docusaurus, VuePress)
- `scripts-architected/` - Custom shell script execution
- `architecture-architected/` - Architecture validation
- `pipelines-architected/` - Pipeline management

Each plugin:
- Implements `ArchitectPlugin<ContextType>` interface
- Registers tasks in `register(registry: TaskRegistry)` method
- Is discovered via Java SPI (`META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin`)
- Can have its own configuration section in `architect.yml`

### Task System Architecture

**Task Types** (defined in API):
1. **SimpleTask**: Basic synchronous task with no arguments
2. **TaskWithArgs**: Task that accepts command-line arguments
3. **CompositeTask**: Parent task with child tasks (hierarchical workflows)
4. **ConfigurableTask**: Task with key-value configuration

**Task Lifecycle**:
```
Phase → Task Registration → Dependency Resolution → Execution → Result Aggregation
```

**Phases** (CoreWorkflow):
- `INIT` → `LINT` → `VERIFY` → `BUILD` → `TEST` / `RUN` → `RELEASE` → `PUBLISH`

Tasks can:
- Belong to a phase (implicit ordering)
- Have custom dependencies on other tasks
- Be part of composite tasks
- Be standalone (no phase)

### Configuration Model

Projects use `architect.yml` (YAML) which specifies:
```yaml
project:
  name: project-name
  description: "..."

plugins:
  - name: plugin-name
    repo: owner/repository

# Plugin-specific sections
plugin-name:
  setting: value
```

The Engine:
1. Loads `architect.yml` from project
2. Instantiates registered plugins
3. Builds task registry from all plugin registrations
4. Resolves dependencies
5. Executes on demand or via hooks

## Key Conventions

### Naming Conventions

- **Task IDs**: Lowercase with hyphens (e.g., `git-status`, `docs-build`)
- **Plugin IDs**: Lowercase with hyphens (e.g., `git-architected`)
- **Package structure**: `io.github.architectplatform.<component>.<feature>`
- **Test files**: `*Test.kt` (JUnit 5)

### Testing Patterns

All tests use **JUnit 5** and follow **Arrange-Act-Assert**:

```kotlin
@Test
fun `should build documentation successfully when config is valid`() {
    // Arrange
    val config = BuildContext(framework = "mkdocs")
    
    // Act
    val result = buildDocs(config)
    
    // Assert
    assertEquals(TaskResult.Status.SUCCESS, result.status)
}
```

**Test locations**:
- `src/test/kotlin/` - Unit tests alongside source code
- Example: `architect-engine/engine/src/test/kotlin/io/github/architectplatform/engine/.../TaskServiceTest.kt`

### Code Style

- **Language**: Kotlin 1.9.25
- **JVM Target**: Java 17
- **Indentation**: 2 spaces
- **Max line length**: 120 characters
- **Null safety**: Use Kotlin's nullable types (`Type?`)
- **API projects only**: ktlint enforcement (use `./gradlew ktlintFormat`)

### Commit Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `perf`, `ci`

**Examples**:
```
feat(engine): add task caching for improved performance
fix(api): resolve memory leak in task executor
docs(cli): update command reference
```

### Plugin Development Pattern

When creating or modifying plugins:

1. **Extend `ArchitectPlugin<Context>`**:
   ```kotlin
   class MyPlugin : ArchitectPlugin<MyContext> {
       override val id = "my-plugin"
       override val contextKey = "myplugin"
       override val ctxClass = MyContext::class.java
       override var context: MyContext = MyContext()
   }
   ```

2. **Register tasks in `register()` method**:
   ```kotlin
   override fun register(registry: TaskRegistry) {
       registry.add(SimpleTask(
           id = "my-task",
           description = "My custom task",
           phase = CoreWorkflow.BUILD,
           task = ::executeMyTask
       ))
   }
   ```

3. **Register via SPI**:
   Create `app/src/main/resources/META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin`:
   ```
   com.example.MyPlugin
   ```

4. **Test with integration tests**:
   Create test that verifies plugin registration and task execution

### Dependency Management

- **API projects** use Maven central and GitHub Packages (maven.pkg.github.com)
- **GitHub Packages authentication** requires:
  - `GITHUB_USER` environment variable (or `githubUser` project property)
  - `REGISTRY_TOKEN` or `GITHUB_TOKEN` environment variable
- **Kotlin version alignment**: Enforced at 1.9.25 across all modules
- **Coroutines version**: 1.10.2 (aligned with Kotlin 1.9.x compatibility)

### Important Files and Their Roles

- `architect.yml` - Root project configuration (uses plugins, docs, git, github settings)
- `architect-api/api/build.gradle.kts` - Publishes to GitHub Packages
- `architect-engine/engine/build.gradle.kts` - Consumes API from packages
- `plugins/*/app/build.gradle.kts` - Each plugin independently buildable
- `.github/workflows/` - CI/CD automation (triggered on push to main)
- `.github/agents/` - Agent configurations (Copilot, automation rules)
- `docs/` - MkDocs source with auto-discovery of component docs

### Avoiding Common Mistakes

1. **Don't** publish from non-API modules - only `architect-api` publishes to GitHub Packages
2. **Don't** bypass task dependency resolution - use `TaskRegistry` and `TaskDependencyResolver`
3. **Don't** hardcode file paths - use `Environment` and `ProjectContext` for project information
4. **Don't** mix phases with custom dependencies without understanding precedence - read `TaskDependencyResolver`
5. **Don't** forget SPI registration - plugins won't be discovered without the META-INF/services entry
6. **Don't** assume Engine uses classpath plugins - load from GitHub repositories as configured

## Frequently Needed Patterns

### Running a Single Module's Tests
```bash
cd architect-engine/engine && ./gradlew test --tests *YourTestName*
```

### Building a Plugin for Local Testing
```bash
cd plugins/git-architected/app && ./gradlew build
```

### Checking Task Dependencies
Look at `architect-engine/engine/src/main/kotlin/io/github/architectplatform/engine/core/tasks/domain/TaskDependencyResolver.kt`

### Adding a New Phase
Extend `Phase` interface in API, update `CoreWorkflow` or create new workflow in `architect-api/api/src/main/kotlin/io/github/architectplatform/api/core/workflows/`

### Debugging Task Execution
- Check `ExecutionService` in Engine for orchestration
- Check `TaskExecutor` for execution logic
- Use Server-Sent Events endpoint to observe real-time execution flow

---

**Last updated**: Part of the Architect Platform automation framework. For questions, refer to component-specific READMEs or GitHub Issues.
