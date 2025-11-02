# Architect Platform

Welcome to the Architect Platform documentation! This comprehensive guide covers all components of the Architect ecosystem.

## Overview

Architect is a powerful, plugin-based task execution framework for automating project workflows, CI/CD pipelines, and development operations. It brings **convention over configuration** to your development workflow with an extensible plugin architecture.

## Components

The Architect Platform consists of several core components:

### Core Components

- **[Architect API](architect-api/index.md)** - Core abstractions and interfaces for plugin development
- **[Architect CLI](architect-cli/index.md)** - Interactive command-line tool for developers
- **[Architect Engine](architect-engine/index.md)** - RESTful API server managing task execution
- **[Architect Cloud](architect-cloud/index.md)** - Cloud platform for Architect (UI + Backend)

### Official Plugins

- **[docs-architected](plugins/docs-architected/index.md)** - Documentation management with multi-framework support
- **[git-architected](plugins/git-architected/index.md)** - Git version control integration
- **[github-architected](plugins/github-architected/index.md)** - GitHub automation (CI/CD, releases, dependencies)
- **[gradle-architected](plugins/gradle-architected/index.md)** - Gradle build automation integration
- **[javascript-architected](plugins/javascript-architected/index.md)** - JavaScript/Node.js package manager integration
- **[pipelines-architected](plugins/pipelines-architected/index.md)** - Pipeline workflow management
- **[scripts-architected](plugins/scripts-architected/index.md)** - Custom shell script execution

## Quick Start

### Prerequisites

- Java 17 or higher
- Gradle 8.x (included via wrapper)
- Git

### Installation

\`\`\`bash
curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-cli/.installers/bash | bash
\`\`\`

### Install & Run the Engine

\`\`\`bash
architect engine install
architect engine start
\`\`\`

### Your First Project

1. **Create a project configuration** (\`architect.yml\`):

\`\`\`yaml
project:
  name: my-awesome-project
  description: "My first Architect project"

plugins:
  - name: docs-architected
    repo: architect-platform/architect

docs:
  build:
    framework: "mkdocs"
    siteName: "My Project Documentation"
  publish:
    enabled: true
    githubPages: true
\`\`\`

2. **Initialize and build:**

\`\`\`bash
architect docs-init
architect docs-build
architect docs-publish
\`\`\`

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
