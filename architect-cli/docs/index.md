# Architect CLI

Interactive command-line tool for developers to interact with the Architect platform.

## Overview

The Architect CLI provides a powerful command-line interface for managing projects, executing tasks, and interacting with the Architect Engine. It's designed to be developer-friendly with intuitive commands and helpful output.

## Installation

### One-Line Installer (Bash)

```bash
curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-cli/.installers/bash | bash
```

### Manual Installation

1. Download the latest release from GitHub
2. Extract the archive
3. Add the `bin` directory to your PATH

## Getting Started

### Initialize a Project

```bash
# Create architect.yml in your project
cat > architect.yml << 'EOF'
project:
  name: my-project
  description: "My awesome project"

plugins:
  - name: docs-architected
    repo: architect-platform/architect
EOF
```

### Install and Start the Engine

```bash
architect engine install
architect engine start
```

### Run Tasks

```bash
# List available tasks
architect

# Run a specific task
architect docs-build

# Run task with arguments
architect git-commit -- -m "Initial commit"

# Run in plain mode (for CI)
architect --plain docs-build
```

## Core Commands

### Engine Management

```bash
# Install the Architect Engine
architect engine install

# Start the engine
architect engine start

# Stop the engine
architect engine stop

# Clean engine data
architect engine clean

# Check engine status
architect engine status
```

### Task Execution

```bash
# List all available tasks
architect

# Execute a task
architect <task-name>

# Execute task with arguments
architect <task-name> -- arg1 arg2

# Execute in plain mode (no colors, for CI)
architect --plain <task-name>
```

## Configuration

The CLI reads configuration from:
1. `architect.yml` in the current directory
2. `~/.architect/config.yml` (global settings)
3. Environment variables

### architect.yml Example

```yaml
project:
  name: my-project
  description: "Project description"

plugins:
  - name: git-architected
    repo: architect-platform/architect
  - name: github-architected
    repo: architect-platform/architect
  - name: docs-architected
    repo: architect-platform/architect

docs:
  build:
    framework: "mkdocs"
    siteName: "My Documentation"
  publish:
    enabled: true
    githubPages: true
```

## Features

### Colored Output

The CLI provides colored output for better readability:
- 🟢 Green: Success messages
- 🔴 Red: Error messages
- 🟡 Yellow: Warnings
- 🔵 Blue: Information
- ⚪ Gray: Debug messages

### Progress Indicators

Long-running tasks show progress indicators:
```
⠋ Building documentation...
✓ Documentation built successfully!
```

### Error Handling

Clear error messages with actionable suggestions:
```
❌ Error: Task 'docs-build' failed
   Cause: MkDocs not installed
   Suggestion: Run 'pip install mkdocs' or set installDeps: true in config
```

### Auto-completion

The CLI supports shell auto-completion for bash and zsh:

```bash
# Enable for bash
architect completion bash >> ~/.bashrc

# Enable for zsh
architect completion zsh >> ~/.zshrc
```

## Common Workflows

### Documentation Workflow

```bash
# Initialize docs structure
architect docs-init

# Build documentation
architect docs-build

# Publish to GitHub Pages
architect docs-publish
```

### Git Workflow

```bash
# Check status
architect git-status

# Add files
architect git-add -- .

# Commit changes
architect git-commit -- -m "feat: add new feature"

# Push to remote
architect git-push
```

### Release Workflow

```bash
# Build the project
architect gradle-build

# Run tests
architect gradle-test

# Create release
architect github-release-task
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `ARCHITECT_ENGINE_URL` | Engine API URL | `http://localhost:9292` |
| `ARCHITECT_LOG_LEVEL` | Log level | `INFO` |
| `ARCHITECT_NO_COLOR` | Disable colored output | `false` |
| `ARCHITECT_TIMEOUT` | Command timeout (seconds) | `300` |

## Building from Source

```bash
cd architect-cli/cli
./gradlew build

# Run locally
./gradlew run --args="<command>"
```

## Testing

```bash
cd architect-cli/cli
./gradlew test
```

## Troubleshooting

### Engine Not Responding

```bash
# Check if engine is running
architect engine status

# Restart the engine
architect engine stop
architect engine start
```

### Task Not Found

```bash
# List all tasks
architect

# Ensure plugin is configured in architect.yml
cat architect.yml
```

### Permission Denied

```bash
# Make sure CLI is executable
chmod +x $(which architect)
```

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](../../CONTRIBUTING.md) for guidelines.

## License

MIT License - see [LICENSE](../../LICENSE) for details
