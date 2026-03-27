# Architect Platform

Welcome to the Architect Platform documentation! This comprehensive guide covers all components of the Architect ecosystem.

## Overview

Architect is a powerful, plugin-based task execution framework for automating project workflows, CI/CD pipelines, and development operations. It brings **convention over configuration** to your development workflow with an extensible plugin architecture.

## Components

The Architect Platform includes active, beta, and incubating modules.
For the authoritative module support set, see the
**[Repository Status Matrix](architecture/status-matrix.md)**.

### Core Platform

- **[Architect API](architect-api/index.md)** - Active contracts and plugin interfaces
- **Architect Core** (`architect-core/core`) - Active shared runtime library
- **[Architect Engine](architect-engine/index.md)** - Incubating execution host and REST API server
- **[Architect CLI](architect-cli/index.md)** - Incubating command-line interface

### Secondary Products

- **[Architect Cloud](architect-cloud/index.md)** - Backend is beta; UI is incubating
- **Architect VS Code** (`architect-vscode/`) - Incubating editor extension
- **Architect IntelliJ** (`architect-intellij/`) - Incubating IDE integration

### Official Plugins

- **[docs-architected](plugins/docs-architected/index.md)** - Documentation management with multi-framework support
- **[git-architected](plugins/git-architected/index.md)** - Git version control integration
- **[github-architected](plugins/github-architected/index.md)** - GitHub automation (CI/CD, releases, dependencies)
- **[gradle-architected](plugins/gradle-architected/index.md)** - Gradle build automation integration
- **[javascript-architected](plugins/javascript-architected/index.md)** - JavaScript/Node.js package manager integration
- **[security-architected](reference/plugins/security-architected.md)** - Security scanning, dependency auditing, and SBOM generation
- **[testing-architected](reference/plugins/testing-architected.md)** - Cross-language unit, integration, e2e, and coverage orchestration
- **[pipelines-architected](plugins/pipelines-architected/index.md)** - Pipeline workflow management
- **[scripts-architected](plugins/scripts-architected/index.md)** - Custom shell script execution

## Guides

- **[Plugin Authoring Guide](guides/authoring-plugins.md)** - Create, test, document, validate, and publish Architect plugins across JVM and process-plugin workflows

## Quick Start

### Prerequisites

- Java 17 or higher (for JVM-based plugins)
- Git

### Installation

```bash
brew tap architect-platform/tap
brew install architect    # macOS

# Linux / macOS (binary)
curl -sSfL https://github.com/architect-platform/architect/releases/latest/download/install.sh -o install.sh
# Verify the checksum published on the Releases page before running
bash install.sh
```

See the full [Installation guide](getting-started/installation.md) for Windows and CI/CD environments.

### Your First Project

1. **Create `architect.yml`:**

```yaml
project:
  name: my-project

tasks:
  hello:
    run: echo "Hello from Architect!"
```

2. **Run your first task:**

```bash
architect hello
```

→ **[Full Getting Started guide](getting-started/installation.md)**

## Key Features

- 🔌 **Plugin Architecture**: Extensible system with support for custom plugins
- 📋 **Task Management**: Organize work into phases with dependency resolution
- 🔄 **Workflow Automation**: Pre-built workflows for common development tasks
- 🚀 **CI/CD Integration**: Seamless integration with GitHub Actions and other CI platforms
- 📚 **Documentation Management**: Multi-framework documentation building and publishing
- 🔐 **Security First**: Built-in security validation and best practices
- 🎯 **Convention Based**: Sensible defaults with full customization options

## Architecture

\`\`\`
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
\`\`\`

### Language-Agnostic Plugins

Architect process plugins use the versioned [Architect Plugin Protocol v1](plugin-protocol.md), a JSON-RPC 2.0 protocol over stdin/stdout that allows plugins to be implemented in TypeScript, Go, Python, or any other language that can spawn a process.

## Workflow Phases

Architect organizes tasks into workflow phases:

\`\`\`
INIT → LINT → VERIFY → BUILD → TEST/RUN → RELEASE → PUBLISH
\`\`\`

- **INIT**: Initialize project structure and configuration
- **LINT**: Code quality checks and linting
- **VERIFY**: Security scans and validation
- **BUILD**: Compile and build artifacts
- **TEST/RUN**: Execute tests or run application
- **RELEASE**: Version tagging and release preparation
- **PUBLISH**: Deploy and publish artifacts

## Use Cases

### Documentation Automation

\`\`\`bash
architect docs-init
architect docs-build
architect docs-publish
\`\`\`

### Release Management

\`\`\`bash
architect github-release-task
\`\`\`

### CI/CD Integration

\`\`\`bash
architect github-init-pipelines
\`\`\`

## JSON Schema for `architect.yml`

Architect provides a [JSON Schema](https://json-schema.org/) for `architect.yml` to give you auto-completion, inline documentation, and validation in any editor that supports YAML schemas.

**Schema URL:**

```
https://architect.dev/schema/architect.yml.json
```

### Using the Schema

Add `$schema` to the top of your `architect.yml`:

```yaml
$schema: https://architect.dev/schema/architect.yml.json

project:
  name: my-project
```

When the `$schema` field is present, `architect validate` will automatically validate your configuration against the schema.

## IDE Setup

### VS Code

1. Install the **Architect Platform** extension from the VS Code Marketplace (or from `architect-vscode/`).
2. Install the [YAML extension](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-yaml) by Red Hat (installed automatically as a dependency).
3. The extension automatically associates `architect.yml` with the JSON Schema — you get auto-completion and inline validation out of the box.

**Features:**
- Auto-completion and validation for `architect.yml`
- Task panel showing all registered tasks with run buttons
- Output panel for execution output
- Commands: *Architect: Run Task*, *Architect: Validate Config*, *Architect: Refresh Tasks*

### IntelliJ IDEA / JetBrains IDEs

1. Install the **Architect Platform** plugin from the JetBrains Marketplace (or build from `architect-intellij/`).
2. The plugin automatically maps `architect.yml` to the bundled JSON Schema.

**Features:**
- Auto-completion and validation for `architect.yml`
- Run configurations for Architect tasks
- Gutter icons to run tasks directly from `architect.yml`

### Other Editors

For editors that support YAML schema associations (Sublime Text, Neovim with `yaml-language-server`, etc.), point your YAML language server at:

```
https://architect.dev/schema/architect.yml.json
```

Or reference the local copy at `docs/schema/architect.yml.json`.

## Contributing

We welcome contributions! Please see our [contributing guidelines](https://github.com/architect-platform/architect/blob/main/CONTRIBUTING.md).

### Commit Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

\`\`\`
<type>(<scope>): <subject>
\`\`\`

**Types:** \`feat\`, \`fix\`, \`docs\`, \`style\`, \`refactor\`, \`test\`, \`chore\`

## Community

- **Issues**: [GitHub Issues](https://github.com/architect-platform/architect/issues)
- **Discussions**: [GitHub Discussions](https://github.com/architect-platform/architect/discussions)

## License

This project is licensed under the MIT License - see the [LICENSE](https://github.com/architect-platform/architect/blob/main/LICENSE) file for details.

---

**Made with ❤️ by the Architect Platform Team**
