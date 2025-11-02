# scripts-architected

The Architect plugin for custom script execution. This plugin enables you to define and execute custom shell scripts through the Architect framework with full workflow integration.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](../LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-purple.svg)](https://kotlinlang.org/)

## Overview

The `scripts-architected` plugin provides a flexible and powerful way to integrate custom scripts into your Architect workflows. Define scripts once in your `architect.yml` configuration and execute them through the Architect CLI with full support for:

- **Workflow Phase Integration**: Attach scripts to any workflow phase (INIT, BUILD, TEST, RUN, RELEASE, PUBLISH)
- **Standalone Scripts**: Create scripts that run independently without phase dependencies
- **Environment Variables**: Configure environment variables for script execution
- **Working Directory Control**: Specify custom working directories for each script
- **CLI Arguments**: Pass arguments to scripts via the command line
- **Full Configurability**: Complete control over script behavior through YAML configuration

## Installation

Add the plugin to your `architect.yml`:

```yaml
plugins:
  - name: scripts-architected
    repo: architect-platform/architect
```

## Configuration

### Basic Configuration

```yaml
scripts:
  scripts:
    hello:
      command: "echo 'Hello, Architect!'"
      description: "Prints a greeting message"
```

Execute with:
```bash
architect scripts-hello
```

### Full Configuration Example

```yaml
scripts:
  enabled: true  # Enable/disable all scripts (default: true)
  scripts:
    # Build script attached to BUILD phase
    build-app:
      command: "npm run build"
      description: "Builds the application"
      phase: "BUILD"
      workingDirectory: "."
      
    # Test script with custom working directory
    run-tests:
      command: "pytest tests/"
      description: "Runs Python tests"
      phase: "TEST"
      workingDirectory: "backend"
      
    # Deployment script with environment variables
    deploy-prod:
      command: "./deploy.sh"
      description: "Deploys to production"
      phase: "PUBLISH"
      workingDirectory: "scripts"
      environment:
        ENV: "production"
        REGION: "us-east-1"
        LOG_LEVEL: "info"
    
    # Standalone script (no phase)
    clean-cache:
      command: "rm -rf .cache && echo 'Cache cleaned'"
      description: "Cleans the cache directory"
      
    # Script that accepts arguments
    generate-docs:
      command: "mkdocs build"
      description: "Generates documentation"
      phase: "BUILD"
      workingDirectory: "docs"
```

## Configuration Reference

### ScriptsContext

The root configuration for the scripts plugin.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | boolean | `true` | Global enable/disable for script execution |
| `scripts` | map | `{}` | Map of script name to script configuration |

### ScriptConfig

Configuration for individual scripts.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `command` | string | *required* | The shell command to execute |
| `description` | string | `"Custom script"` | Human-readable description of the script |
| `phase` | string | `null` | Workflow phase to attach to (see phases below) |
| `workingDirectory` | string | `"."` | Working directory relative to project root |
| `environment` | map | `{}` | Environment variables to set for the script |

### Supported Phases

Scripts can be attached to any of these workflow phases:

- **INIT**: Initialization and setup tasks
- **LINT**: Code quality and linting
- **VERIFY**: Verification and validation
- **BUILD**: Building and compilation
- **TEST**: Testing
- **RUN**: Running the application
- **RELEASE**: Release preparation
- **PUBLISH**: Publishing and deployment

If no phase is specified, the script becomes a standalone task that can be executed independently.

## Usage

### Execute a Script

```bash
# Execute a named script
architect scripts-<script-name>

# Examples
architect scripts-build-app
architect scripts-deploy-prod
architect scripts-clean-cache
```

### Pass Arguments to Scripts

```bash
# Arguments after -- are passed to the script
architect scripts-generate-docs -- --strict
architect scripts-deploy-prod -- --verbose --dry-run
```

### Disable Script Execution

Temporarily disable all script execution:

```yaml
scripts:
  enabled: false
  scripts:
    # ... scripts still defined but won't execute
```

### List Available Scripts

```bash
# Show all available tasks (including scripts)
architect
```

## Examples

### Example 1: Simple Build and Deploy

```yaml
project:
  name: my-app

plugins:
  - name: scripts-architected
    repo: architect-platform/architect

scripts:
  scripts:
    build:
      command: "npm run build"
      description: "Build the application"
      phase: "BUILD"
    
    deploy:
      command: "./deploy.sh production"
      description: "Deploy to production"
      phase: "PUBLISH"
      environment:
        DEPLOY_ENV: "production"
```

Workflow execution:
```bash
# Runs build script in BUILD phase, then deploy in PUBLISH phase
architect build deploy
```

### Example 2: Multi-Environment Deployment

```yaml
scripts:
  scripts:
    deploy-dev:
      command: "./deploy.sh"
      description: "Deploy to development"
      environment:
        ENV: "dev"
        API_URL: "https://dev-api.example.com"
    
    deploy-staging:
      command: "./deploy.sh"
      description: "Deploy to staging"
      environment:
        ENV: "staging"
        API_URL: "https://staging-api.example.com"
    
    deploy-prod:
      command: "./deploy.sh"
      description: "Deploy to production"
      environment:
        ENV: "production"
        API_URL: "https://api.example.com"
```

Usage:
```bash
architect scripts-deploy-dev
architect scripts-deploy-staging
architect scripts-deploy-prod
```

### Example 3: Testing Scripts

```yaml
scripts:
  scripts:
    unit-tests:
      command: "npm test"
      description: "Run unit tests"
      phase: "TEST"
    
    integration-tests:
      command: "npm run test:integration"
      description: "Run integration tests"
      phase: "TEST"
      environment:
        TEST_DB: "test-db"
    
    e2e-tests:
      command: "npm run test:e2e"
      description: "Run end-to-end tests"
      phase: "TEST"
      workingDirectory: "e2e"
```

### Example 4: Maintenance Scripts

```yaml
scripts:
  scripts:
    clean:
      command: "rm -rf node_modules dist .cache && echo 'Cleaned'"
      description: "Clean build artifacts"
    
    reset-db:
      command: "psql -f scripts/reset.sql"
      description: "Reset development database"
      workingDirectory: "database"
    
    generate-types:
      command: "npm run codegen"
      description: "Generate TypeScript types from GraphQL"
```

### Example 5: Complex Build Pipeline

```yaml
scripts:
  scripts:
    install:
      command: "npm ci"
      description: "Install dependencies"
      phase: "INIT"
    
    lint:
      command: "npm run lint && npm run format:check"
      description: "Run linting and format checks"
      phase: "LINT"
    
    type-check:
      command: "npm run type-check"
      description: "Run TypeScript type checking"
      phase: "VERIFY"
    
    build-client:
      command: "npm run build:client"
      description: "Build client application"
      phase: "BUILD"
      workingDirectory: "packages/client"
    
    build-server:
      command: "npm run build:server"
      description: "Build server application"
      phase: "BUILD"
      workingDirectory: "packages/server"
    
    test-unit:
      command: "npm run test:unit"
      description: "Run unit tests"
      phase: "TEST"
    
    test-integration:
      command: "npm run test:integration"
      description: "Run integration tests"
      phase: "TEST"
      environment:
        NODE_ENV: "test"
```

## Security Considerations

The scripts-architected plugin implements multiple layers of security to prevent command injection vulnerabilities:

### Built-in Security Measures

1. **Argument Escaping**: All command-line arguments passed to scripts are automatically escaped using single-quote wrapping, preventing shell interpretation of special characters.

2. **Environment Variable Validation**: Environment variable keys are strictly validated to only allow uppercase letters, numbers, and underscores (matching standard shell conventions).

3. **Environment Variable Escaping**: Environment variable values are escaped using double-quote wrapping with proper escaping of special characters ($, `, ", \, !).

4. **Configuration Source**: Script commands come from the trusted `architect.yml` configuration file, not from user input at runtime.

### Security Implementation

The plugin uses the `ScriptUtils` utility class that provides:

- `escapeShellArg()`: Wraps arguments in single quotes and escapes any single quotes within
- `validateEnvKey()`: Ensures environment variable keys follow shell naming conventions
- `escapeEnvValue()`: Properly escapes special characters in environment variable values

### Best Practices

While the plugin implements security measures, users should still follow these best practices:

1. **Review Configuration**: Audit `architect.yml` script definitions in code reviews
2. **Use absolute paths**: Prefer absolute paths for critical scripts
3. **Limit permissions**: Run with minimal required permissions
4. **Protect sensitive data**: Don't hardcode secrets in scripts or environment variables
5. **Validate custom commands**: Ensure script commands don't contain malicious content

### Example of Safe Configuration

```yaml
scripts:
  scripts:
    # Good: Simple, clear command
    build:
      command: "npm run build"
      phase: "BUILD"
    
    # Good: Using environment variables for configuration
    deploy:
      command: "/usr/local/bin/deploy.sh"
      phase: "PUBLISH"
      environment:
        ENVIRONMENT: "production"
        LOG_LEVEL: "info"
```

### What to Avoid

```yaml
scripts:
  scripts:
    # Bad: Command injection attempt (but will be escaped safely)
    bad-example:
      command: "echo test; rm -rf /"  # Still executed as intended due to escaping
      
    # Bad: Hardcoded secrets (use environment or secret management instead)
    bad-deploy:
      command: "./deploy.sh"
      environment:
        API_KEY: "hardcoded-secret-key"  # Don't do this!
```

### Environment Variables

Environment variables are prepended to commands in the format:
```bash
KEY1="escaped-value1" KEY2="escaped-value2" your-command
```

All values are properly escaped to prevent injection attacks.

## Integration with Other Plugins

The scripts plugin works seamlessly with other Architect plugins:

```yaml
plugins:
  - name: git-architected
    repo: architect-platform/architect
  - name: scripts-architected
    repo: architect-platform/architect
  - name: docs-architected
    repo: architect-platform/architect

git:
  config:
    user.name: "Build Bot"

scripts:
  scripts:
    pre-commit:
      command: "npm run lint-staged"
      description: "Run pre-commit checks"
    
    build-docs:
      command: "npm run docs:generate"
      description: "Generate API documentation"
      phase: "BUILD"

docs:
  build:
    framework: "mkdocs"
```

## Troubleshooting

### Script Not Found

**Problem**: `architect scripts-<name>` says task not found

**Solution**: 
- Verify the script is defined in `architect.yml` under `scripts.scripts`
- Check the script name matches exactly (case-sensitive)
- Ensure the scripts plugin is listed in the `plugins` section

### Script Fails to Execute

**Problem**: Script exits with an error

**Solution**:
- Check that the command is valid and executable
- Verify the working directory exists and is correct
- Ensure required environment variables are set
- Test the command manually in a terminal first

### Environment Variables Not Working

**Problem**: Environment variables aren't available in the script

**Solution**:
- Verify environment variable syntax in the configuration
- Check that your script actually uses the variables
- Try echoing the variable in the script to confirm it's set

## Development

### Building the Plugin

```bash
cd plugins/scripts-architected/app
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Publishing

```bash
./gradlew publishToMavenLocal
```

## Contributing

Contributions are welcome! Please see the main [CONTRIBUTING.md](../../CONTRIBUTING.md) for guidelines.

## License

This plugin is part of the Architect project and is licensed under the MIT License. See [LICENSE](../LICENSE) for details.

## Support

- 📖 **Documentation**: See the main [Architect README](../../README.md)
- 💬 **Community**: [GitHub Discussions](https://github.com/architect-platform/architect/discussions)
- 🐛 **Issues**: [GitHub Issues](https://github.com/architect-platform/architect/issues)

---

**Made with ❤️ by the Architect Platform Team**
