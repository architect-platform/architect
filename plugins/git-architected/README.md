# Git Architected Plugin

An Architect plugin that integrates Git version control operations into the Architect workflow system, enabling seamless Git management through the Architect CLI.

## Overview

The Git Architected plugin provides integration between Git commands and the Architect platform. It allows you to:
- Configure Git settings (user.name, user.email, etc.) through Architect configuration
- Execute Git commands within the Architect workflow
- Proxy common Git operations through a unified `architect` command interface
- Apply conventions over configuration for common Git operations

## Features

### 1. Git Configuration Management

Automatically configure Git settings in your repository:

```yaml
git:
  config:
    user.name: "Your Name"
    user.email: "your.email@example.com"
    core.editor: "vim"
    core.autocrlf: "input"
```

### 2. Git Command Proxying

Execute Git commands through the Architect CLI. All common Git commands are available as Architect tasks:

| Git Command | Architect Task | Purpose |
|------------|----------------|---------|
| `git status` | `git-status` | Show working tree status |
| `git add` | `git-add` | Add file contents to the index |
| `git commit` | `git-commit` | Record changes to the repository |
| `git push` | `git-push` | Update remote refs |
| `git pull` | `git-pull` | Fetch and integrate changes |
| `git fetch` | `git-fetch` | Download objects and refs |
| `git checkout` | `git-checkout` | Switch branches or restore files |
| `git branch` | `git-branch` | List, create, or delete branches |
| `git log` | `git-log` | Show commit logs |
| `git diff` | `git-diff` | Show changes between commits |
| `git merge` | `git-merge` | Join development histories |
| `git reset` | `git-reset` | Reset current HEAD |
| `git stash` | `git-stash` | Stash changes |
| `git tag` | `git-tag` | Create, list, delete tags |
| `git remote` | `git-remote` | Manage tracked repositories |

### 3. Workflow Integration

The plugin integrates with Architect workflow phases:

| Architect Phase | Git Tasks | Purpose |
|----------------|-----------|---------|
| `INIT` | `git-config` | Configure Git settings |
| `BUILD` | Most Git commands | Development operations |
| `PUBLISH` | `git-push` | Publishing operations |

### 4. Conventions Over Configuration

The plugin follows sensible defaults:
- Enabled by default
- Uses existing Git configuration if no custom config is provided
- All standard Git commands are available
- No configuration required for basic Git operations

## Architecture

### Core Components

- **GitPlugin**: Main plugin class implementing `ArchitectPlugin`
- **GitTask**: Task executor for Git commands
- **GitContext**: Configuration container for Git settings
- **GitConfigTask**: Specialized task for applying Git configuration

### Configuration Model

```kotlin
GitContext(
  config: Map<String, String>,  // Git configuration key-value pairs
  enabled: Boolean              // Whether the plugin is enabled
)
```

## Tasks

### `git-config` (Init)
- **Phase**: `INIT`
- **Purpose**: Apply Git configuration settings
- **Command**: `git config --local <key> <value>` for each config entry

### Git Command Tasks
- **Phase**: `BUILD` or `PUBLISH`
- **Purpose**: Proxy Git commands through Architect
- **Command**: `git <command> <args>`

## Building

```bash
cd app
./gradlew build
```

**Note**: Building requires access to the Architect API package. Set the following environment variables:
- `GITHUB_USER` or `githubUser` property
- `REGISTRY_TOKEN` or `GITHUB_TOKEN` environment variable

## Usage

### Basic Configuration

Add the plugin configuration to your `architect.yml`:

```yaml
git:
  config:
    user.name: "Architect User"
    user.email: "architect@example.com"
```

### Minimal Configuration (Use Existing Git Settings)

If you don't specify any configuration, the plugin will use your existing Git configuration:

```yaml
git:
  enabled: true
```

### Disable the Plugin

```yaml
git:
  enabled: false
```

### Advanced Configuration

Configure multiple Git settings:

```yaml
git:
  config:
    user.name: "John Doe"
    user.email: "john.doe@company.com"
    core.editor: "code --wait"
    core.autocrlf: "input"
    pull.rebase: "false"
    init.defaultBranch: "main"
    commit.gpgsign: "true"
    user.signingkey: "ABC123"
```

## Command Examples

### Using Architect CLI with Git Commands

Once the plugin is installed, you can use Git commands through the Architect CLI:

```bash
# Check status
architect run git-status

# Add files
architect run git-add -- .

# Commit changes
architect run git-commit -- -m "feat: add new feature"

# Push changes
architect run git-push -- origin main

# Pull changes
architect run git-pull -- origin main

# Create a branch
architect run git-branch -- feature/new-feature

# Checkout a branch
architect run git-checkout -- feature/new-feature

# View commit history
architect run git-log -- --oneline

# Show differences
architect run git-diff -- HEAD~1

# Stash changes
architect run git-stash

# Apply stash
architect run git-stash -- apply
```

### Arguments and Options

All Git command arguments can be passed after `--`:

```bash
# Git add with specific files
architect run git-add -- src/main.kt README.md

# Git commit with multiple flags
architect run git-commit -- -m "fix: bug fix" --amend

# Git log with formatting
architect run git-log -- --oneline --graph --all

# Git push with force
architect run git-push -- --force-with-lease
```

## Execution Flow

1. **Configuration Phase (INIT)**: Git configuration settings are applied to the repository
2. **Command Execution**: Git commands are proxied through the CommandExecutor
3. **Result Handling**: Success/failure status is returned to the Architect system
4. **Error Handling**: Exceptions are caught and reported with detailed messages

## Technical Stack

- **Language**: Kotlin 1.9.25
- **JVM**: Java 17
- **Dependencies**: Architect API 1.1.2
- **Build Tool**: Gradle 8.14.3

## Development

### Adding New Git Commands

To add a new Git command to the plugin:

```kotlin
registry.add(GitTask("newcommand", WorkflowPhase.BUILD, context))
```

### Customizing Command Execution

The `CommandExecutor` service is used to run Git commands:

```kotlin
commandExecutor.execute(
    "git command ${args.joinToString(" ")}",
    workingDir = projectContext.dir.toString()
)
```

### Adding Custom Configuration Validators

You can extend the `GitConfigTask` to validate configuration values before applying them.

## Error Handling

The plugin provides robust error handling:
- **Configuration Failures**: Each config setting failure is captured separately
- **Command Failures**: Git command errors are caught and reported
- **Detailed Messages**: Exceptions include full context
- **Graceful Degradation**: Plugin can be disabled without affecting other components

## Task Results

Task results provide detailed feedback:

```
✅ Git configuration completed successfully
  ├── ✅ Git config user.name set to John Doe
  ├── ✅ Git config user.email set to john.doe@example.com
  └── ✅ Git config core.editor set to vim
```

## Best Practices

1. **Use Configuration Files**: Store Git configuration in `architect.yml` for consistency across team members
2. **Conventions Over Configuration**: Only configure what differs from defaults
3. **Proxy Through Architect**: Use `architect run git-*` commands for consistency and logging
4. **Use Descriptive Commit Messages**: Follow conventional commits format
5. **Leverage Workflow Phases**: Use appropriate phases for different Git operations

## Limitations

- Git must be installed on the system
- Commands are executed in the project directory
- Some interactive Git commands may not work well through the proxy
- Git configuration is applied with `--local` scope only

## Security Considerations

- Configuration values are passed as command arguments
- Credentials should not be stored in configuration files
- Use Git credential helpers for authentication
- Be cautious with force push and destructive operations

## Comparison with Direct Git Usage

### Using Git Directly
```bash
cd project-directory
git config --local user.name "John Doe"
git add .
git commit -m "feat: new feature"
git push
```

### Using Git Architected Plugin
```bash
# Configuration is automatic from architect.yml
architect run git-add -- .
architect run git-commit -- -m "feat: new feature"
architect run git-push
```

Benefits of using the plugin:
- Centralized configuration management
- Consistent logging and error handling
- Integration with other Architect plugins
- Team-wide configuration sharing
- Workflow automation capabilities
