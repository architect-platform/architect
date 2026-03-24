# Architect

A powerful, plugin-based task execution framework for automating project workflows, CI/CD pipelines, and development operations.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-purple.svg)](https://kotlinlang.org/)

## Overview

Architect is a comprehensive automation platform that brings **convention over configuration** to your development workflow. It provides a unified way to manage documentation, releases, builds, tests, and deployment across diverse technology stacks through an extensible plugin architecture.

### Key Features

- 🔌 **Plugin Architecture**: Extensible system with support for custom plugins
- 📋 **Task Management**: Organize work into phases with dependency resolution
- 🔄 **Workflow Automation**: Pre-built workflows for common development tasks
- 🚀 **CI/CD Integration**: Seamless integration with GitHub Actions and other CI platforms
- 📚 **Documentation Management**: Multi-framework documentation building and publishing
- 🔐 **Security First**: Built-in security validation and best practices
- 🎯 **Convention Based**: Sensible defaults with full customization options

## Architecture

Architect consists of three main components:

```
┌─────────────────────────────────────────────────────────────┐
│                      Architect CLI                           │
│  Command-line interface for project interaction              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Architect Engine                          │
│  REST API server for task execution and project management   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                     Architect API                            │
│  Core abstractions and interfaces for plugin development     │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
            ┌────────────┴────────────┐
            ▼                          ▼
    ┌──────────────┐          ┌──────────────┐
    │   Built-in   │          │    Custom    │
    │   Plugins    │          │   Plugins    │
    └──────────────┘          └──────────────┘
```

### Components

- **[Architect CLI](architect-cli/)**: Interactive command-line tool for developers
- **[Architect Engine](architect-engine/)**: RESTful API server managing task execution
- **[Architect API](architect-api/)**: Core library for building plugins
- **[Plugins](plugins/)**: Extensible plugins for various technologies and platforms

## Quick Start

### Prerequisites

- Java 17 or higher
- Gradle 8.x (included via wrapper)
- Git

###  One‑Line Installer Script

```bash
curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-cli/.installers/bash | bash
```

## Optional: Install & Run the Engine
```bash
architect engine install
architect engine start
# architect engine stop/clean
```

## Embedded Mode (No Engine Required)
```bash
architect --embedded build
# or automatic fallback when daemon is unavailable
architect --no-daemon build
```

### Your First Project

1. **Create a project configuration** (`architect.yml`):

```yaml
project:
  name: my-awesome-project
  description: "My first Architect project"

plugins:
  - name: git-architected
    repo: architect-platform/architect
  - name: github-architected
    repo: architect-platform/architect
  - name: docs-architected
    repo: architect-platform/architect

git:
  config:
    user.name: "Your Name"
    user.email: "your.email@example.com"

docs:
  build:
    framework: "mkdocs"
    siteName: "My Project Documentation"
    siteAuthor: "Your Name"
  publish:
    enabled: true
    githubPages: true
```

2. **Initialize your project:**
```bash
architect init
```

3. **Run tasks:**
```bash
# Build documentation
architect docs-build

# Publish to GitHub Pages
architect docs-publish
```

## Available Plugins

Architect comes with several official plugins ready to use:

### Core Plugins

#### [git-architected](plugins/git-architected/)
Integrates Git version control operations with Architect workflows.

**Features:**
- Configure Git settings through Architect
- Execute Git commands via Architect CLI
- Workflow integration for Git operations

**Example:**
```yaml
git:
  config:
    user.name: "John Doe"
    user.email: "john@example.com"
```

#### [github-architected](plugins/github-architected/)
Provides GitHub-specific automation for CI/CD pipelines, releases, and dependency management.

**Features:**
- Automated release management with semantic-release
- GitHub Actions workflow initialization
- Renovate configuration for dependency updates

**Example:**
```yaml
github:
  release:
    enabled: true
    assets:
      - name: "app.jar"
        path: "build/libs/app.jar"
  pipelines:
    - name: "ci"
      type: "standard"
      branch: "main"
```

#### [gradle-architected](plugins/gradle-architected/)
Integrates Gradle build automation with Architect workflows.

**Features:**
- Multi-project Gradle builds
- Task execution and lifecycle management
- Build configuration through Architect

#### [javascript-architected](plugins/javascript-architected/)
Integrates JavaScript/Node.js package managers with Architect workflows.

**Features:**
- Support for npm, yarn, and pnpm
- Standard JavaScript workflows (install, build, test, lint)
- Custom working directory configuration

**Example:**
```yaml
javascript:
  packageManager: "npm"
  workingDirectory: "."
```

#### [docs-architected](plugins/docs-architected/)
Comprehensive documentation management with multi-framework support.

**Features:**
- Multiple frameworks: MkDocs, Docusaurus, VuePress
- GitHub Pages publishing
- Custom domain support
- Template-based configuration
- Automated workflow generation

**Example:**
```yaml
docs:
  build:
    framework: "mkdocs"
    siteName: "My Documentation"
    siteDescription: "Complete project guide"
    siteAuthor: "Dev Team"
    repoUrl: "https://github.com/user/repo"
    primaryColor: "blue"
  publish:
    enabled: true
    githubPages: true
    domain: "docs.myproject.com"
```

#### [scripts-architected](plugins/scripts-architected/) ⭐ NEW
Execute custom shell scripts with full workflow integration.

**Features:**
- Define custom scripts in configuration
- Attach scripts to workflow phases (INIT, BUILD, TEST, etc.)
- Standalone script execution
- Environment variable support
- Custom working directory configuration
- Command-line argument passing

**Example:**
```yaml
scripts:
  scripts:
    build:
      command: "npm run build"
      description: "Build the application"
      phase: "BUILD"
    deploy:
      command: "./deploy.sh"
      description: "Deploy to production"
      phase: "PUBLISH"
      environment:
        ENV: "production"
        REGION: "us-east-1"
    custom:
      command: "echo 'Custom task'"
      description: "Standalone custom script"
```

## Use Cases

### Documentation Automation

Architect makes documentation a first-class citizen:

```bash
# Initialize documentation structure
architect docs-init

# Write your docs in docs/

# Build and preview
architect docs-build

# Publish to GitHub Pages
architect docs-publish
```

### Release Management

Automate your release process:

```yaml
github:
  release:
    enabled: true
    message: "chore(release): ${nextRelease.version}"
    assets:
      - name: "distribution.zip"
        path: "build/distributions/*.zip"
```

```bash
architect github-release-task
```

### CI/CD Integration

Generate GitHub Actions workflows:

```yaml
github:
  pipelines:
    - name: "build-and-test"
      type: "standard"
      branch: "main"
      path: "src/**"
```

```bash
architect github-init-pipelines
```

### Multi-Project Management

Handle complex project structures:

```yaml
gradle:
  projects:
    - name: backend
      path: backend/
    - name: frontend
      path: frontend/
    - name: shared
      path: shared/
```

## Configuration

### Project Configuration File

Every Architect project uses an `architect.yml` file:

```yaml
project:
  name: project-name
  description: "Project description"

plugins:
  - name: plugin-name
    repo: owner/repository

# Plugin-specific configuration
plugin-name:
  setting: value
```

### Global Settings

Configure the engine through `architect-engine/src/main/resources/application.yml`:

```yaml
micronaut:
  server:
    port: 9292

engine:
  project:
    cache:
      enabled: true
```

## Workflows

Architect organizes tasks into workflow phases:

### Core Workflow

```
INIT → LINT → VERIFY → BUILD → TEST/RUN → RELEASE → PUBLISH
```

**Phases:**
- **INIT**: Initialize project structure and configuration
- **LINT**: Code quality checks and linting
- **VERIFY**: Security scans and validation
- **BUILD**: Compile and build artifacts
- **TEST/RUN**: Execute tests or run application
- **RELEASE**: Version tagging and release preparation
- **PUBLISH**: Deploy and publish artifacts

### Hooks Workflow

Git hook integration:

```
PRE_COMMIT → PREPARE_COMMIT_MSG → COMMIT_MSG → POST_COMMIT → PRE_PUSH
```

## Development

### Project Structure

```
architect/
├── architect-api/          # Core API and interfaces
├── architect-cli/          # Command-line interface
├── architect-engine/       # Execution engine (REST API)
├── architect-cloud/        # Secondary product (backend beta, UI incubating)
├── architect-vscode/       # Incubating VS Code extension
├── architect-intellij/     # Incubating IntelliJ plugin
├── plugins/                # Official plugins
│   ├── git-architected/
│   ├── github-architected/
│   ├── gradle-architected/
│   └── docs-architected/
├── architect.yml           # Root project configuration
└── README.md              # This file
```

Incubating areas are intentionally called out above so they are not mistaken
for production-stable surfaces.

### Building Components

> There is no root Gradle wrapper or supported root build/test orchestrator. Each module is independently built from its own directory.

```bash
# Build the API library
cd architect-api/api && ./gradlew build

# Build the core runtime
cd architect-core/core && ./gradlew build

# Build the engine
cd architect-engine/engine && ./gradlew build

# Build the CLI
cd architect-cli/cli && ./gradlew build

# Run tests for a specific module
cd architect-engine/engine && ./gradlew test

# Run tests with coverage
cd architect-api/api && ./gradlew test jacocoTestReport
```

The root `architect.yml` configures documentation and automation metadata; it is
not a repository-wide build runner.

### Creating a Custom Plugin

1. **Create plugin structure:**
```
my-plugin/
├── app/
│   ├── src/main/kotlin/com/example/MyPlugin.kt
│   └── src/main/resources/
│       └── META-INF/services/
│           └── io.github.architectplatform.api.core.plugins.ArchitectPlugin
└── architect.yml
```

2. **Implement the plugin:**
```kotlin
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
        // Task implementation
        return TaskResult.success("Task completed!")
    }
}
```

3. **Register the plugin:**
```
# In META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin
com.example.MyPlugin
```

## CLI Commands

### Project Commands

```bash
# Show available tasks
architect

# Execute a task
architect <task-name> [args...]

# Run in plain mode (for CI)
architect --plain <task-name>
```

### Engine Management

```bash
# Install engine (optional)
architect engine install

# Start engine
architect engine start

# Stop engine
architect engine stop

# Clean engine data
architect engine clean
```

### Plugin-Specific Commands

```bash
# Git commands
architect git-status
architect git-add -- .
architect git-commit -- -m "message"

# Documentation
architect docs-init
architect docs-build
architect docs-publish

# GitHub
architect github-init-pipelines
architect github-release-task
```

## REST API

The Architect Engine exposes a RESTful API:

### Projects

- `GET /api/projects` - List all projects
- `POST /api/projects` - Register a project
- `GET /api/projects/{name}` - Get project details

### Tasks

- `GET /api/projects/{projectName}/tasks` - List tasks
- `POST /api/projects/{projectName}/tasks/{taskName}` - Execute task

### Execution

- `GET /api/executions/{executionId}/events` - Stream execution events (SSE)

## Examples

### Example 1: Simple Documentation Project

```yaml
project:
  name: docs-only

plugins:
  - name: docs-architected
    repo: architect-platform/architect

docs:
  build:
    framework: "mkdocs"
    siteName: "Simple Docs"
  publish:
    enabled: true
    githubPages: true
```

### Example 2: Full-Stack Project

```yaml
project:
  name: fullstack-app

plugins:
  - name: git-architected
    repo: architect-platform/architect
  - name: github-architected
    repo: architect-platform/architect
  - name: gradle-architected
    repo: architect-platform/architect
  - name: docs-architected
    repo: architect-platform/architect

gradle:
  projects:
    - name: backend
      path: backend/
    - name: frontend
      path: frontend/

github:
  pipelines:
    - name: ci
      type: standard
      branch: main
  release:
    enabled: true

docs:
  build:
    framework: "docusaurus"
    siteName: "Full Stack App"
  publish:
    enabled: true
```

### Example 3: Open Source Project

```yaml
project:
  name: open-source-project
  description: "An awesome open source project"

plugins:
  - name: git-architected
    repo: architect-platform/architect
  - name: github-architected
    repo: architect-platform/architect
  - name: docs-architected
    repo: architect-platform/architect

git:
  config:
    user.name: "OSS Bot"
    user.email: "bot@project.org"

github:
  pipelines:
    - name: ci
      type: standard
      branch: main
    - name: docs
      type: docs
      branch: main
  deps:
    type: renovate
    enabled: true
  release:
    enabled: true
    assets:
      - name: "release.jar"
        path: "build/libs/*.jar"

docs:
  build:
    framework: "mkdocs"
    siteName: "Project Documentation"
    siteDescription: "Comprehensive project guide"
    repoUrl: "https://github.com/user/project"
    repoName: "user/project"
    primaryColor: "green"
  publish:
    enabled: true
    githubPages: true
    domain: "docs.project.org"
```

## Contributing

We welcome contributions! Please see our contributing guidelines:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit your changes** (`git commit -m 'feat: add amazing feature'`)
4. **Push to the branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

### Commit Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

## Community

- **Issues**: [GitHub Issues](https://github.com/architect-platform/architect/issues)
- **Discussions**: [GitHub Discussions](https://github.com/architect-platform/architect/discussions)
- **Documentation**: Check individual component READMEs

## Roadmap

- [ ] Additional plugins (Maven, npm, Docker, Kubernetes)
- [ ] Web UI for engine management
- [ ] Plugin marketplace
- [ ] Enhanced CI/CD integrations
- [ ] Native binary distributions

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

Built with:
- [Kotlin](https://kotlinlang.org/) - Modern programming language
- [Micronaut](https://micronaut.io/) - Lightweight framework
- [Gradle](https://gradle.org/) - Build automation
- [PicoCLI](https://picocli.info/) - CLI framework

## Support

- 📖 **Documentation**: See individual component READMEs
- 💬 **Community**: GitHub Discussions
- 🐛 **Issues**: GitHub Issues
- ✉️ **Contact**: Open an issue for questions

---

**Made with ❤️ by the Architect Platform Team**
