# Architect CLI

A command-line interface for interacting with the Architect Engine, enabling project management and task execution.

## Overview

The Architect CLI is a Kotlin-based command-line tool built with Micronaut and PicoCLI. It provides an intuitive interface for:
- Registering projects with the Architect Engine
- Executing tasks within projects
- Managing the Architect Engine lifecycle
- Monitoring task execution with real-time feedback

## Architecture

The CLI consists of several key components:

### Core Components

- **ArchitectLauncher**: Main entry point and command parser
- **ConsoleUI**: Terminal user interface with rich formatting and progress tracking
- **TerminalUI**: Low-level terminal rendering utilities
- **EngineCommandClient**: HTTP client for communicating with the Architect Engine

### Data Transfer Objects (DTOs)

- **ProjectDTO**: Represents a project registered with the engine
- **TaskDTO**: Represents an executable task
- **RegisterProjectRequest**: Request payload for project registration
- **TaskResultDTO**: Hierarchical task execution results
- **ExecutionTaskEvent**: Events emitted during task execution

## Features

### Project Management

```bash
# Register and list tasks for current project
architect

# Execute a specific task
architect <task-name> [args...]

# Scaffold a new plugin in the current directory
architect plugin create my-plugin
architect plugin create my-process-plugin typescript
```

### Engine Management

```bash
# Install the Architect Engine (optional for basic task execution)
architect engine install

# Install the Architect Engine for CI environments
architect engine install-ci

# Start the engine
architect engine start

# Stop the engine
architect engine stop

# Clean engine data
architect engine clean
```

### Embedded Mode (No Daemon)

```bash
# Run directly in-process (no engine required)
architect --embedded <task-name>

# Fallback to embedded mode when daemon is unavailable
architect --no-daemon <task-name>
```

### Output Modes

- **Interactive Mode**: Rich terminal UI with progress tracking (default)
- **Plain Mode**: Simple output for CI environments (use `-p` or `--plain`)

```bash
# Run in plain mode
architect --plain <task-name>
```

## Building

```bash
cd cli
./gradlew build
```

## Running

```bash
cd cli
./gradlew run --args="<command>"
```

## Configuration

The CLI supports two execution modes:
- Embedded mode (default fallback): runs tasks in-process with no daemon requirement
- Engine mode: communicates with the Architect Engine over HTTP

When engine mode is used, the endpoint is configured via Micronaut HTTP client settings (`/api` by default).

## Technical Stack

- **Language**: Kotlin 1.9.25
- **JVM**: Java 17
- **Framework**: Micronaut 4.6.1
- **CLI Framework**: PicoCLI
- **Build Tool**: Gradle 8.14.3
- **Serialization**: Micronaut Serde (Jackson)
- **Async**: Kotlin Coroutines

## Development

### Adding New Commands

1. Add command parameters to `ArchitectLauncher`
2. Implement command logic in the `run()` method
3. Update `handleEngineCommand()` for engine-specific commands

### Customizing the UI

- Modify `ConsoleUI` for terminal UI behavior
- Adjust `TerminalUI` for rendering utilities
- Update `AnsiColors` for color scheme changes

## Dependencies

- Micronaut Kotlin Runtime
- PicoCLI for command-line parsing
- Kotlin Coroutines for async operations
- Jackson for JSON serialization
- Logback for logging
