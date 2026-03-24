# Contributing to Architect Platform

Thank you for your interest in contributing to the Architect Platform! This document provides guidelines and instructions for contributing.

## Code of Conduct

Please be respectful and constructive in all interactions. We're building a welcoming community.

## How Can I Contribute?

### Reporting Bugs

Before creating a bug report, please check if the issue already exists. When creating a bug report, include:

- **Clear title and description**
- **Steps to reproduce** the behavior
- **Expected behavior**
- **Actual behavior**
- **Environment details** (OS, Java version, Architect version)
- **Stack traces or logs** if applicable

### Suggesting Enhancements

Enhancement suggestions are welcome! Please provide:

- **Clear use case** for the enhancement
- **Proposed solution** or API design
- **Alternatives considered**
- **Examples** of how it would be used

### Pull Requests

1. **Fork the repository** and create your branch from `main`
2. **Make your changes** with clear, focused commits
3. **Add tests** for new functionality
4. **Update documentation** if needed
5. **Ensure tests pass** locally
6. **Submit a pull request** with a clear description

## Development Setup

### Prerequisites

- Java 17 or higher
- Gradle 8.x
- Git
- Node.js 18+ (for documentation)
- Python 3.x (for MkDocs documentation)

### Clone and Build

> There is no root Gradle wrapper or supported root build/test task. Each module must be built from its own directory.

```bash
# Clone the repository
git clone https://github.com/architect-platform/architect.git
cd architect

# Build specific components
cd architect-cli/cli && ./gradlew build
cd architect-engine/engine && ./gradlew build
cd architect-api/api && ./gradlew build
cd architect-core/core && ./gradlew build
```

### Running Tests

> Run tests per module — there is no root-level test task.

```bash
# Run tests for a specific module
cd architect-engine/engine && ./gradlew test

# Run tests for a specific test class
cd architect-engine/engine && ./gradlew test --tests TaskServiceTest

# Run with coverage (architect-api supports jacocoTestReport)
cd architect-api/api && ./gradlew test jacocoTestReport
```

The root `architect.yml` is repository metadata for docs/git/GitHub automation,
not a monorepo build orchestrator.

### Building Documentation

```bash
# Install MkDocs
pip install mkdocs mkdocs-material mkdocs-monorepo-plugin

# Build documentation
mkdocs build

# Serve locally
mkdocs serve
```

## Coding Standards

### Kotlin Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use 2 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful variable and function names

### Code Quality

- Write clear, self-documenting code
- Add comments for complex logic
- Keep functions small and focused
- Follow SOLID principles
- Avoid code duplication

### Testing

- Write unit tests for all new functionality
- Aim for >80% code coverage
- Use descriptive test names
- Follow Arrange-Act-Assert pattern

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

## Commit Guidelines

We follow [Conventional Commits](https://www.conventionalcommits.org/):

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `perf`: Performance improvements
- `ci`: CI/CD changes

### Examples

```bash
feat(docs): add monorepo documentation support

- Implemented mkdocs-monorepo-plugin integration
- Added component documentation configuration
- Updated workflow templates

Closes #123
```

```bash
fix(engine): resolve memory leak in task executor

Fixed issue where task contexts were not being properly cleaned up
after execution, causing memory to grow over time.

Fixes #456
```

## Plugin Development

### Creating a New Plugin

1. **Create plugin structure**:
```
my-plugin/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/
│   │   │   └── resources/
│   │   └── test/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── docs/
│   ├── index.md
│   └── mkdocs.yml
├── architect.yml
└── README.md
```

2. **Implement ArchitectPlugin interface**:
```kotlin
class MyPlugin : ArchitectPlugin<MyContext> {
    override val id = "my-plugin"
    override val contextKey = "myplugin"
    override val ctxClass = MyContext::class.java
    override var context: MyContext = MyContext()
    
    override fun register(registry: TaskRegistry) {
        // Register your tasks
    }
}
```

3. **Register via SPI**:
Create `META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin`:
```
com.example.MyPlugin
```

4. **Add documentation**:
- Create README.md with plugin description
- Add docs/index.md with usage guide
- Include configuration examples

5. **Write tests**:
- Unit tests for plugin logic
- Integration tests for task execution
- Configuration validation tests

## Documentation

- Update relevant documentation for code changes
- Add code examples for new features
- Keep README files up to date
- Document breaking changes

## Review Process

1. Automated checks must pass (tests, linting)
2. Code review by at least one maintainer
3. Documentation review
4. Security review for sensitive changes

## Release Process

Releases follow semantic versioning:
- **Major**: Breaking changes
- **Minor**: New features (backwards compatible)
- **Patch**: Bug fixes (backwards compatible)

## Getting Help

- **Issues**: [GitHub Issues](https://github.com/architect-platform/architect/issues)
- **Discussions**: [GitHub Discussions](https://github.com/architect-platform/architect/discussions)
- **Documentation**: Check component READMEs

## License

By contributing, you agree that your contributions will be licensed under the project's **Apache 2.0 License**.

---

Thank you for contributing to Architect Platform! 🎉
