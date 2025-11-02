# pipelines-architected

The Architect plugin for managing and executing pipelines of Architect tasks.

## Overview

The `pipelines-architected` plugin allows you to create and configure workflows composed of Architect commands. It provides a powerful way to orchestrate complex task execution with support for:

- **Workflow Templates**: Use predefined templates or create custom workflows
- **Step Dependencies**: Define execution order through step dependencies
- **Conditional Execution**: Run steps based on conditions
- **Parallel Execution**: Execute independent tasks in parallel (when supported by the engine)
- **Error Handling**: Continue execution even when steps fail with `continueOnError`

## Installation

Add the plugin to your `architect.yml`:

```yaml
plugins:
  - name: pipelines-architected
    repo: architect-platform/architect
```

## Usage

### Initialize Pipeline Templates

Create the `.architect/pipelines` directory with default workflow templates:

```bash
architect pipelines-init
```

This creates three template workflows:
- `ci-standard.yml`: Standard CI workflow (init → lint → build → test)
- `release-standard.yml`: Release workflow (build → test → release)
- `docs-publish.yml`: Documentation workflow (init → build → publish)

### Define Workflows

Define workflows in your `architect.yml`:

```yaml
pipelines:
  workflows:
    # Use a template as-is
    - name: ci
      extends: ci-standard
    
    # Extend a template with custom steps
    - name: custom-ci
      extends: ci-standard
      steps:
        - name: security-scan
          task: security-check
          dependsOn:
            - build
    
    # Create a workflow from scratch
    - name: deploy
      description: Deploy application
      steps:
        - name: build
          task: build
        - name: test
          task: test
          dependsOn:
            - build
        - name: deploy-staging
          task: deploy-to-staging
          dependsOn:
            - test
          condition: "ENVIRONMENT == staging"
```

### Execute Workflows

Run a workflow by name:

```bash
# Execute the CI workflow
architect pipelines-execute -- ci

# Execute custom workflow
architect pipelines-execute -- deploy
```

### List Available Workflows

See all configured workflows:

```bash
architect pipelines-list
```

## Configuration

### Workflow Definition

A workflow consists of:

```yaml
pipelines:
  workflows:
    - name: workflow-name              # Required: Unique workflow name
      description: "Description"       # Optional: Workflow description
      extends: template-name           # Optional: Template to extend
      env:                             # Optional: Environment variables
        KEY: value
      steps:                           # Required: List of steps
        - name: step-name              # Required: Unique step name
          task: task-id                # Required: Architect task to execute
          args:                        # Optional: Task arguments
            - arg1
            - arg2
          dependsOn:                   # Optional: Step dependencies
            - previous-step
          continueOnError: false       # Optional: Continue on failure
          condition: "ENV_VAR == value" # Optional: Execution condition
```

### Step Dependencies

Define execution order using `dependsOn`:

```yaml
steps:
  - name: compile
    task: build
  
  - name: test-unit
    task: test-unit
    dependsOn:
      - compile
  
  - name: test-integration
    task: test-integration
    dependsOn:
      - compile
  
  - name: package
    task: package
    dependsOn:
      - test-unit
      - test-integration
```

In this example:
- `compile` runs first
- `test-unit` and `test-integration` can run in parallel after `compile`
- `package` waits for both tests to complete

### Conditional Execution

Use conditions to control step execution:

```yaml
steps:
  - name: deploy-production
    task: deploy
    condition: "ENVIRONMENT == production"
  
  - name: deploy-staging
    task: deploy-staging
    condition: "ENVIRONMENT == staging"
  
  - name: skip-if-no-token
    task: publish
    condition: "API_TOKEN"  # Checks if API_TOKEN exists
```

Supported condition formats:
- `VAR == value`: Check if variable equals value
- `VAR != value`: Check if variable doesn't equal value
- `VAR`: Check if variable exists

### Error Handling

Control failure behavior with `continueOnError`:

```yaml
steps:
  - name: optional-lint
    task: lint
    continueOnError: true  # Workflow continues even if lint fails
  
  - name: required-build
    task: build
    continueOnError: false  # Workflow stops if build fails (default)
```

## Templates

### Built-in Templates

#### ci-standard
Standard continuous integration workflow:
```yaml
steps:
  - init → lint → build → test
```

#### release-standard
Release workflow with testing:
```yaml
steps:
  - build → test → github-release-task
```

#### docs-publish
Documentation workflow:
```yaml
steps:
  - docs-init → docs-build → docs-publish
```

### Custom Templates

Create custom templates in `.architect/pipelines/`:

```yaml
# .architect/pipelines/my-template.yml
name: my-template
description: Custom template
steps:
  - name: step1
    task: task1
  - name: step2
    task: task2
    dependsOn:
      - step1
```

Use your template:

```yaml
pipelines:
  workflows:
    - name: my-workflow
      extends: my-template
      steps:
        - name: additional-step
          task: additional-task
          dependsOn:
            - step2
```

## Examples

### Example 1: Simple CI Workflow

```yaml
project:
  name: my-app

plugins:
  - name: pipelines-architected
    repo: architect-platform/architect
  - name: gradle-architected
    repo: architect-platform/architect

pipelines:
  workflows:
    - name: ci
      extends: ci-standard
```

Run with:
```bash
architect pipelines-execute -- ci
```

### Example 2: Multi-Stage Deployment

```yaml
pipelines:
  workflows:
    - name: deploy-pipeline
      description: Multi-stage deployment workflow
      env:
        REGION: us-east-1
      steps:
        - name: build
          task: gradle-build
        
        - name: test
          task: gradle-test
          dependsOn:
            - build
        
        - name: deploy-staging
          task: deploy-staging
          dependsOn:
            - test
          condition: "STAGE == staging"
        
        - name: smoke-test
          task: smoke-test
          dependsOn:
            - deploy-staging
          continueOnError: true
        
        - name: deploy-production
          task: deploy-production
          dependsOn:
            - smoke-test
          condition: "STAGE == production"
```

### Example 3: Parallel Testing

```yaml
pipelines:
  workflows:
    - name: test-suite
      steps:
        - name: compile
          task: build
        
        - name: unit-tests
          task: test-unit
          dependsOn:
            - compile
        
        - name: integration-tests
          task: test-integration
          dependsOn:
            - compile
        
        - name: e2e-tests
          task: test-e2e
          dependsOn:
            - compile
        
        - name: report
          task: generate-report
          dependsOn:
            - unit-tests
            - integration-tests
            - e2e-tests
```

In this workflow, all three test suites run in parallel after compilation, and the report is generated after all tests complete.

## GitHub Actions Integration

Use the plugin to simplify GitHub Actions workflows:

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  ci:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Architect
        run: |
          curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-cli/.installers/bash | bash
          architect engine install
          architect engine start
      
      - name: Run CI Pipeline
        run: architect pipelines-execute -- ci
```

Instead of defining all steps in GitHub Actions, you define them once in `architect.yml` and can use them locally and in CI.

## Advanced Usage

### Extending Multiple Templates

While you can only directly extend one template, you can create a custom template that combines others:

```yaml
# .architect/pipelines/full-pipeline.yml
name: full-pipeline
steps:
  # Include steps from ci-standard manually
  - name: init
    task: init
  - name: lint
    task: lint
    dependsOn: [init]
  - name: build
    task: build
    dependsOn: [lint]
  - name: test
    task: test
    dependsOn: [build]
  # Add release steps
  - name: release
    task: github-release-task
    dependsOn: [test]
```

### Programmatic Workflow Execution

The plugin can be used programmatically by other plugins or tools that need to execute complex task sequences.

## Tasks Reference

### pipelines-init
- **Phase**: INIT
- **Description**: Initialize pipeline templates and configuration
- **Usage**: `architect pipelines-init`

### pipelines-execute
- **Phase**: BUILD
- **Description**: Execute a workflow by name
- **Usage**: `architect pipelines-execute -- <workflow-name>`
- **Arguments**: Workflow name (required)

### pipelines-list
- **Phase**: BUILD
- **Description**: List all configured workflows
- **Usage**: `architect pipelines-list`

## Architecture

The plugin follows these design principles:

1. **Declarative Configuration**: Workflows are defined declaratively in YAML
2. **Composability**: Workflows can extend templates and be composed of reusable steps
3. **Dependency Resolution**: Automatic resolution of step dependencies
4. **Fail-Fast**: By default, workflows stop on first failure
5. **Flexibility**: Support for conditional execution and error handling

## Contributing

Contributions are welcome! Please see the main Architect contributing guidelines.

## License

MIT License - see the main Architect LICENSE file for details.
