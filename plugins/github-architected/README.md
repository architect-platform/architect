# GitHub Architected Plugin

An Architect plugin that provides GitHub-specific automation for CI/CD pipelines, releases, and dependency management.

## Overview

The GitHub Architected plugin integrates with the Architect platform to provide:
- Automated GitHub release management with semantic-release
- GitHub Actions workflow initialization
- Dependency management tool configuration (Renovate)
- Git repository detection and operations

## Features

### 1. Release Management

Automates the release process using semantic-release with customizable configuration:

- **Automatic Version Bumping**: Based on commit messages
- **Changelog Generation**: Automatic changelog from commits
- **Asset Publishing**: Upload release artifacts to GitHub
- **Git Tagging**: Automatic git tag creation and push

#### Configuration

```yaml
github:
  release:
    enabled: true
    message: "chore(release): ${nextRelease.version} [skip ci]"
    assets:
      - name: "app.jar"
        path: "build/libs/app.jar"
    git_assets:
      - "**/*.gradle.kts"
```

### 2. Pipeline Initialization

Creates and configures GitHub Actions workflows:

- **Template-based Generation**: Uses predefined workflow templates
- **Customizable Triggers**: Configure branch and path filters
- **Multiple Pipeline Types**: Support for different workflow types

#### Configuration

```yaml
github:
  pipelines:
    - name: "ci"
      type: "standard"
      path: "**"
      branch: "main"
```

### 3. Dependency Management

Initializes dependency management tools in your repository:

- **Renovate Support**: Automatic dependency updates
- **Configurable Format**: JSON or other formats
- **Repository-level Configuration**: Placed in `.github/` directory

#### Configuration

```yaml
github:
  deps:
    enabled: true
    type: "renovate"
    format: "json"
```

## Architecture

### Core Components

- **GithubPlugin**: Main plugin class implementing `ArchitectPlugin`
- **GithubTask**: Task wrapper for GitHub operations
- **GithubContext**: Configuration context for the plugin

### Data Transfer Objects

- **GithubContext**: Top-level configuration
- **GithubReleaseContext**: Release-specific settings
- **PipelineContext**: Pipeline configuration
- **DepsContext**: Dependency management settings
- **Asset**: Release asset definition

## Tasks

The plugin registers the following tasks:

### `github-release-task`
- **Phase**: `RELEASE`
- **Purpose**: Execute semantic-release process
- **Requires**: Git repository, release configuration

### `github-init-pipelines`
- **Phase**: `INIT`
- **Purpose**: Create GitHub Actions workflow files
- **Requires**: Git repository, pipeline configuration

### `github-init-dependencies`
- **Phase**: `INIT`
- **Purpose**: Set up dependency management configuration
- **Requires**: Git repository

## Resource Files

The plugin includes embedded resources:

- `releases/run.sh`: Script to execute semantic-release
- `releases/update-version.sh`: Script to update version in files
- `releases/.releaserc.json`: semantic-release configuration template
- `pipelines/*.yml`: GitHub Actions workflow templates
- `dependencies/renovate/renovate.json`: Renovate configuration

## Building

```bash
cd app
./gradlew build
```

**Note**: Building requires access to the Architect API package. Set the following environment variables:
- `GITHUB_USER` or `githubUser` property
- `REGISTRY_TOKEN` or `GITHUB_TOKEN` environment variable

## Usage

1. Add the plugin to your Architect project
2. Configure the plugin in your `architect.yml`:

```yaml
github:
  release:
    enabled: true
    assets:
      - name: "my-app.jar"
        path: "build/libs/my-app.jar"
  pipelines:
    - name: "build"
      type: "standard"
      branch: "main"
  deps:
    enabled: true
    type: "renovate"
```

3. Execute Architect tasks to trigger plugin functionality

## Technical Stack

- **Language**: Kotlin 1.9.25
- **JVM**: Java 17
- **Dependencies**: Architect API 1.1.2, Jackson
- **Build Tool**: Gradle 8.14.3

## Development

### Adding New Pipeline Types

1. Create a new template in `resources/pipelines/`
2. Reference it in your `architect.yml` configuration
3. Use `{{name}}`, `{{path}}`, and `{{branch}}` placeholders

### Customizing Release Process

1. Modify `resources/releases/.releaserc.json` template
2. Update `releaseTask()` method to handle new configuration
3. Add new shell scripts to `resources/releases/` if needed

### Extending Task Types

1. Add new task to `register()` method
2. Implement task function following the pattern
3. Register with appropriate workflow phase

## Error Handling

The plugin provides comprehensive error handling:
- Git repository validation
- Resource extraction failure handling
- Command execution error capture
- Detailed error messages in task results

## Security Considerations

- Shell scripts are executed with proper permissions
- Temporary files are cleaned up after execution
- Git operations respect repository boundaries
- Resource extraction validates paths
