# architecture-architected

> **Status**: Experimental (incubating tier) — see [STATUS.md](STATUS.md)

Architecture validation workflow automation for Architect projects.

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
  onViolation: warn
  reportFormat: text
  strict: false
  rulesets:
    layered:
      enabled: true
      description: "Example layered architecture rules"
      rules:
        - id: no-direct-repository-access
          description: "Controllers should not depend directly on repositories"
          type: dependency
          pattern: ".*Controller.*"
          forbidden:
            - ".*Repository.*"
          severity: error
```

## Local Build and Test

```bash
cd plugins/architecture-architected/app
./gradlew build
./gradlew test
```

## Limitations

This plugin is at incubating tier. Implementation depth, docs, and automated coverage are still below the active plugin standard. See [STATUS.md](STATUS.md) for graduation criteria.
