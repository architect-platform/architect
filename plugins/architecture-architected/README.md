# architecture-architected

> **Status**: Experimental (incubating tier) — see [STATUS.md](STATUS.md)

## Overview

architecture-architected integrates architecture validation into the Architect task lifecycle, providing tasks for enforcing architectural rules and boundaries. It enables teams to validate architecture decisions through standardized Architect commands.

## Tasks

| Task ID | Description |
|---|---|
| `architecture-validate` | Validate the project against configured architecture rules |

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: architecture-architected
    repo: architect-platform/architecture-architected

architecture:
  enabled: true
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
    plugins/*:
      - api
  onViolation: warn
  reportFormat: text
  strict: false
  rulesets:
    monorepo-conventions:
      enabled: true
    custom:
      enabled: true
      description: "Custom architecture rules"
      rules:
        - id: no-cycles
          type: import
          severity: error
        - id: public-kdoc
          type: convention
          convention: kdoc-required
          paths:
            - "src/main/.*\\.kt"
          severity: warning
          suggestion: "Add KDoc to public Kotlin declarations."
```

## Built-in rule types

- `dependency` — forbid or require imports for matching declarations
- `naming` — enforce file naming conventions by path
- `structure` — assert required files/directories exist
- `import` — detect circular imports and enforce module boundaries
- `convention` — validate KDoc/Javadoc and production-to-test coverage
- `custom` — delegate to a custom `RuleValidator` implementation

## Built-in preset rulesets

- `layered-architecture`
- `hexagonal-architecture`
- `clean-architecture`
- `monorepo-conventions`

Violations now include file, optional line number, severity, and remediation suggestions in both text and JSON reports.

For monorepos, `architecture.boundaries` enforces which modules may import which other modules.

You can also run structure-only checks via:

```bash
architect validate --structure
```

## Local Build and Test

```bash
cd plugins/architecture-architected/app
./gradlew build
./gradlew test
```

## Limitations

This plugin is at incubating tier. Implementation depth, docs, and automated coverage are still below the active plugin standard. See [STATUS.md](STATUS.md) for graduation criteria.
