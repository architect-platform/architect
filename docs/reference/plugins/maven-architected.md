# maven-architected

Apache Maven build tool integration for Architect.

**Source**: `architect-platform/maven-architected`

## Installation

```yaml
plugins:
  - name: maven-architected
    type: github
    repo: architect-platform/maven-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `mvn-package` | `BUILD` | Compile and package (`mvn package`) |
| `mvn-verify` | `TEST` | Run full verify lifecycle including tests |
| `mvn-deploy` | `PUBLISH` | Deploy artefacts to a Maven repository |

## Configuration

Configuration key: `maven`

```yaml
maven-architected:
  mavenPath: mvn
  settingsFile: .mvn/settings.xml
  profiles:
    - release
    - ci
  args:
    - -DskipTests=false
    - -Dmaven.compiler.failOnWarning=true
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `mavenPath` | `string` | `mvn` | Path to the Maven executable or wrapper |
| `settingsFile` | `string` | — | Custom `settings.xml` path |
| `profiles` | `string[]` | `[]` | Maven profiles to activate |
| `args` | `string[]` | `[]` | Additional arguments appended to all Maven commands |

## Usage examples

```bash
# Package
architect mvn-package

# Run full verify (includes tests)
architect mvn-verify

# Deploy to remote repository
architect mvn-deploy
```
