# architecture-architected

Architecture validation and dependency constraint enforcement for Architect.

**Source**: `architect-platform/architecture-architected`

## Installation

```yaml
plugins:
  - name: architecture-architected
    type: github
    repo: architect-platform/architecture-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `architecture-validate` | `VERIFY` | Validate code structure against declared rules |

## Configuration

Configuration key: `architecture`

```yaml
architecture-architected:
  enabled: true
  rules:
    - name: "No circular dependencies"
      type: no-circular
    - name: "API must not depend on Engine"
      type: no-dependency
      from: architect-api
      to: architect-engine
    - name: "Plugins must only depend on API"
      type: allowed-dependencies
      module: plugins/*
      allowedModules:
        - architect-api
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | `boolean` | `true` | Enable or disable validation |
| `rules` | `object[]` | `[]` | List of architecture rules (see below) |

### Rule types

| `type` | Description |
|--------|-------------|
| `no-circular` | Fail if any circular dependency is detected |
| `no-dependency` | Prohibit `from` module from depending on `to` module |
| `allowed-dependencies` | Restrict which modules a given module may depend on |

## Usage examples

```bash
# Validate architecture rules
architect architecture-validate

# Run as part of VERIFY phase
architect --phase VERIFY
```
