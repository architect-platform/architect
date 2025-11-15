# Architect Engine

The Architect Engine is a plugin-based task execution engine built with Kotlin and Micronaut. It provides a flexible framework for managing projects and executing tasks through a RESTful API, with built-in support for AI agents via the Model Context Protocol (MCP).

## Overview

Architect Engine allows you to:
- Register and manage projects with custom configurations
- Load and execute plugins that provide domain-specific tasks
- Execute tasks with event-driven feedback
- Support nested project structures (subprojects)
- **NEW**: Expose capabilities to AI agents via Model Context Protocol (MCP)

## Architecture

### Core Components

- **Project Service**: Manages project registration, loading, and configuration
- **Task Service**: Handles task discovery and execution across projects
- **Plugin System**: Extensible plugin architecture using SPI (Service Provider Interface)
- **Event System**: Real-time event streaming for task execution monitoring
- **MCP Integration**: Built-in Model Context Protocol support for AI agents (see [MCP.md](engine/MCP.md))

### Plugins

The engine comes with several built-in plugins:

1. **Workflows Plugin**: Manages workflow phases and execution
   - Core Plugin: Core workflow functionality
   - Code Plugin: Code-related workflows
   - Hooks Plugin: Git hooks management

2. **Commits Plugin**: Validates commit messages and enforces commit conventions

3. **Installers Plugin**: Manages installation scripts and resources

## Prerequisites

- Java 17 or higher
- Gradle 8.x (wrapper included)

## Building

To build the project locally:

```bash
cd engine
./gradlew build
```

To run without tests:

```bash
./gradlew build -x test
```

## Running

Start the engine server:

```bash
./gradlew run
```

The server will start on port 9292 (configurable in `src/main/resources/application.yml`).

## API Endpoints

### Projects API

- `GET /api/projects` - List all registered projects
- `POST /api/projects` - Register a new project
  ```json
  {
    "name": "my-project",
    "path": "/path/to/project"
  }
  ```
- `GET /api/projects/{name}` - Get project details
- `GET /api/projects/{projectName}/config` - Get project configuration

### Tasks API

- `GET /api/projects/{projectName}/tasks` - List all tasks for a project
- `GET /api/projects/{projectName}/tasks/{taskName}` - Get task details
- `POST /api/projects/{projectName}/tasks/{taskName}` - Execute a task
  ```json
  ["arg1", "arg2"]
  ```

### Execution API

- `GET /api/executions/{executionId}/events` - Stream execution events (SSE)

## Configuration

The engine is configured through `application.yml`:

```yaml
micronaut:
  application:
    name: architect-engine
  server:
    port: 9292

engine:
  project:
    cache:
      enabled: false  # Enable/disable project caching
  plugin:
    cache:
      enabled: false  # Enable/disable plugin caching
```

## Project Configuration

Projects are configured using an `architect.yml` file in the project root:

```yaml
project:
  name: my-project
  description: "Project description"

plugins:
  - name: plugin-name
    # Plugin-specific configuration
```

## Development

### Project Structure

```
engine/
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── io/github/architectplatform/engine/
│       │       ├── Application.kt          # Main entry point
│       │       ├── core/                   # Core functionality
│       │       │   ├── execution/          # Command execution
│       │       │   ├── plugin/             # Plugin system
│       │       │   ├── project/            # Project management
│       │       │   └── tasks/              # Task execution
│       │       ├── domain/                 # Domain events
│       │       └── plugins/                # Built-in plugins
│       └── resources/
│           ├── application.yml             # Configuration
│           ├── hooks/                      # Hook scripts
│           └── installers/                 # Installer scripts
├── build.gradle.kts                        # Build configuration
└── settings.gradle.kts
```

### Adding a New Plugin

1. Implement the `Plugin` interface from architect-api
2. Create a `PluginProvider` using SPI
3. Add plugin metadata in `META-INF/services/`
4. Register tasks in the `register()` method

### Running Tests

```bash
./gradlew test
```

## Docker

Build a Docker image:

```bash
./gradlew dockerBuild
```

Build a native image with GraalVM:

```bash
./gradlew dockerBuildNative
```

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please ensure:
- Code follows Kotlin coding conventions
- All tests pass
- New features include appropriate tests and documentation
