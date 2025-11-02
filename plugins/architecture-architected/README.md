# Architecture Architected Plugin

The Architecture Architected plugin for the Architect platform provides comprehensive architectural rule management and validation capabilities. This plugin enables software projects to define, extend, and enforce architectural rules, ensuring consistency with adopted architectural patterns and templates.

## Features

- **Ruleset Management**: Define collections of related architectural rules
- **Multiple Rule Types**:
  - **Dependency Rules**: Enforce or forbid dependencies between components
  - **Naming Rules**: Enforce naming conventions for files and classes
  - **Structure Rules**: Ensure required directories and files exist
  - **Custom Rules**: Extend with your own validation logic
- **Flexible Validation**: Configure violation handling (warn/fail)
- **Multiple Report Formats**: Text, JSON, and HTML output
- **Strict Mode**: Treat warnings as errors for stricter validation
- **Language Support**: Works with Java, Kotlin, JavaScript, TypeScript, Python, Go, Rust, and more

## Installation

Add the plugin to your `architect.yml`:

```yaml
project:
  name: my-project
  description: "My awesome project"

plugins:
  - name: architecture-architected
    repo: architect-platform/architect
```

## Configuration

### Basic Configuration

```yaml
architecture:
  enabled: true
  onViolation: warn  # Options: warn, fail
  reportFormat: text  # Options: text, json, html
  strict: false      # If true, warnings are treated as errors
```

### Defining Rulesets

Rulesets are collections of related architectural rules:

```yaml
architecture:
  enabled: true
  rulesets:
    layered:
      enabled: true
      description: "Enforce layered architecture principles"
      rules:
        - id: "no-direct-db-access"
          description: "Controllers should not directly access database"
          type: "dependency"
          pattern: ".*Controller.*"
          forbidden: [".*Repository.*", ".*DAO.*"]
          severity: "error"
```

### Rule Types

#### Dependency Rules

Dependency rules enforce or forbid dependencies between components:

```yaml
- id: "service-layer-required"
  description: "Controllers must use service layer"
  type: "dependency"
  pattern: ".*Controller.*"
  required: [".*Service.*"]
  severity: "warning"
  enabled: true
```

- `pattern`: Regex pattern to match class/module names
- `forbidden`: List of regex patterns for forbidden dependencies
- `required`: List of regex patterns for required dependencies
- `severity`: `error`, `warning`, or `info`

#### Naming Rules

Naming rules enforce naming conventions:

```yaml
- id: "controller-naming"
  description: "Controllers must end with 'Controller'"
  type: "naming"
  pattern: ".*Controller"
  paths: ["src/main/.*Controller\\..*"]
  severity: "warning"
  enabled: true
```

- `pattern`: Regex pattern that file/class names must match
- `paths`: List of file path patterns to apply this rule to

#### Structure Rules

Structure rules ensure required directories and files exist:

```yaml
- id: "required-directories"
  description: "Required directories must exist"
  type: "structure"
  paths: ["src/main", "src/test", "docs"]
  severity: "error"
  enabled: true
```

### Complete Example

```yaml
architecture:
  enabled: true
  onViolation: warn
  reportFormat: text
  strict: false
  
  rulesets:
    # Layered Architecture
    layered:
      enabled: true
      description: "Enforce layered architecture principles"
      rules:
        - id: "no-direct-db-access"
          description: "Controllers should not directly access database"
          type: "dependency"
          pattern: ".*Controller.*"
          forbidden: [".*Repository.*", ".*DAO.*", ".*Entity.*"]
          severity: "error"
        
        - id: "service-layer-required"
          description: "Controllers must use service layer"
          type: "dependency"
          pattern: ".*Controller.*"
          required: [".*Service.*"]
          severity: "warning"
    
    # Naming Conventions
    naming:
      enabled: true
      description: "Enforce naming conventions"
      rules:
        - id: "controller-naming"
          description: "Controllers must end with 'Controller'"
          type: "naming"
          pattern: ".*Controller"
          paths: ["src/main/.*Controller\\..*"]
          severity: "warning"
        
        - id: "service-naming"
          description: "Services must end with 'Service'"
          type: "naming"
          pattern: ".*Service"
          paths: ["src/main/.*Service\\..*"]
          severity: "warning"
    
    # Project Structure
    structure:
      enabled: true
      description: "Enforce project structure"
      rules:
        - id: "required-directories"
          description: "Required directories must exist"
          type: "structure"
          paths: ["src/main", "src/test"]
          severity: "error"
```

## Tasks

### architecture-init

Initializes architecture configuration with a sample template.

```bash
architect architecture-init
```

This creates a `.architect/architecture.yml` file with sample rules that you can customize.

### architecture-validate

Validates the project against defined architectural rules.

```bash
architect architecture-validate
```

This task:
1. Analyzes all source files in the project
2. Applies all enabled architectural rules
3. Generates a validation report
4. Fails the build if configured to do so

## Usage Examples

### Example 1: Enforce Layered Architecture

```yaml
architecture:
  enabled: true
  rulesets:
    layered:
      enabled: true
      rules:
        - id: "controllers-use-services"
          type: "dependency"
          pattern: ".*Controller.*"
          required: [".*Service.*"]
          forbidden: [".*Repository.*"]
          severity: "error"
```

### Example 2: Enforce Clean Architecture

```yaml
architecture:
  enabled: true
  rulesets:
    clean:
      enabled: true
      rules:
        - id: "domain-independence"
          description: "Domain layer must not depend on infrastructure"
          type: "dependency"
          pattern: ".*domain.*"
          paths: ["src/main/.*/domain/.*"]
          forbidden: [".*infrastructure.*", ".*framework.*"]
          severity: "error"
```

### Example 3: Naming Conventions

```yaml
architecture:
  enabled: true
  rulesets:
    naming:
      enabled: true
      rules:
        - id: "test-naming"
          type: "naming"
          pattern: ".*Test"
          paths: ["src/test/.*Test\\..*"]
          severity: "warning"
```

## Validation Reports

### Text Format

```
================================================================================
Architecture Validation Report
================================================================================

Summary:
  Rules checked: 5
  Files analyzed: 23
  Violations found: 2

Violations:

Rule: no-direct-db-access [ERROR]
  Description: Controllers should not directly access database
  Violations: 1

    • UserController.kt
      File contains forbidden dependency: com.example.UserRepository (matched pattern: .*Repository.*)

Rule: service-layer-required [WARNING]
  Description: Controllers must use service layer
  Violations: 1

    • OrderController.kt
      File is missing required dependency matching pattern: .*Service.*

================================================================================
```

### JSON Format

Set `reportFormat: json` to get machine-readable output:

```json
{
  "summary": {
    "rulesChecked": 5,
    "filesAnalyzed": 23,
    "violationsFound": 2,
    "hasErrors": true,
    "hasWarnings": true
  },
  "violations": [
    {
      "rule": "no-direct-db-access",
      "severity": "error",
      "file": "src/main/kotlin/UserController.kt",
      "line": null,
      "message": "File contains forbidden dependency: com.example.UserRepository"
    }
  ]
}
```

## Integration with CI/CD

### GitHub Actions

```yaml
- name: Validate Architecture
  run: architect architecture-validate
```

### Fail on Violations

Set `onViolation: fail` to fail the build on any violation:

```yaml
architecture:
  enabled: true
  onViolation: fail
```

### Strict Mode

Enable strict mode to treat warnings as errors:

```yaml
architecture:
  enabled: true
  strict: true
```

## Best Practices

1. **Start with Warnings**: Begin with `onViolation: warn` to identify violations without breaking builds
2. **Gradual Adoption**: Enable rules incrementally as you refactor your codebase
3. **Document Rules**: Use descriptive IDs and descriptions for all rules
4. **Team Agreement**: Ensure the team agrees on architectural rules before enforcing them
5. **Regular Validation**: Run validation in CI/CD pipelines to catch violations early
6. **Customize Rulesets**: Create rulesets that match your specific architectural patterns

## Advanced Usage

### Custom Validators

For advanced validation logic, you can implement custom validators:

```yaml
architecture:
  customRules:
    - id: "custom-rule"
      description: "Custom validation logic"
      type: "custom"
      validator: "com.example.MyCustomValidator"
      severity: "error"
```

### Path Filtering

Use path patterns to apply rules to specific parts of your codebase:

```yaml
- id: "api-layer-rules"
  type: "dependency"
  pattern: ".*Controller.*"
  paths: ["src/main/.*/api/.*"]
  forbidden: [".*internal.*"]
```

## Troubleshooting

### No Rules Configured

If you see "No architectural rules configured", either:
- Run `architect architecture-init` to create a configuration
- Add rules directly in your `architect.yml`

### Rules Not Applied

Check that:
- The plugin is enabled (`enabled: true`)
- Rulesets are enabled (`rulesets.*.enabled: true`)
- Individual rules are enabled (`rules.*.enabled: true`)

### Pattern Not Matching

- Use online regex testers to validate your patterns
- Check that file paths match your `paths` patterns
- Ensure class/module names match your `pattern`

## License

MIT License - see LICENSE file for details

## Contributing

Contributions are welcome! Please submit issues and pull requests to the [Architect repository](https://github.com/architect-platform/architect).
