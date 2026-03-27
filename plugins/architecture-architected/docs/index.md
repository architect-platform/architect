# Architecture Architected Plugin

Architectural rule management and validation for the Architect platform.

## Overview

The Architecture Architected plugin provides tools for defining, managing, and validating architectural rules and constraints in your projects.

## Features

- Dependency, naming, structure, import, and convention rule types
- Built-in rulesets for layered, hexagonal, clean, and monorepo conventions
- Text and JSON reports with file, line, severity, and suggestion output
- Custom validator hooks for advanced rules
- Integration with build workflows

## Getting Started

Add the plugin to your `architect.yml`:

```yaml
plugins:
  - name: architecture-architected
    repo: architect-platform/architect
```

## Configuration

Configure architectural rules in your project:

```yaml
architecture:
  presetRulesets:
    - layered-architecture
  structure:
    required:
      - src/main/kotlin
      - docs
    forbidden:
      - .env
      - "**/*.tmp"
  boundaries:
    api:
      - core
    engine:
      - api
      - core
  customRules:
    - id: no-cycles
      type: import
      severity: error
    - id: public-kdoc
      type: convention
      convention: kdoc-required
      paths:
        - "src/main/.*\\.kt"
      severity: warning
```

## Examples

### Define Architectural Rules

```yaml
architecture:
  rulesets:
    layered-architecture:
      enabled: true
    monorepo-conventions:
      enabled: true
  customRules:
    - id: bounded-modules
      type: import
      moduleBoundaries:
        api: [core]
        engine: [api, core]
    - id: controllers-need-services
      type: dependency
      pattern: ".*Controller.*"
      required:
        - ".*Service.*"
```

## API Reference

See the plugin source code for detailed API documentation.

## Structure Validation

Use `architect validate --structure` to run only the structure checks derived from
`architecture.structure.required` and `architecture.structure.forbidden`.

## Monorepo Boundaries

Use `architecture.boundaries` to declare which modules may import other modules.
These boundaries are enforced through the import-rule engine and show up as
cross-module violations in architecture validation reports.

## Contributing

Contributions are welcome! Please follow the project's contributing guidelines.
