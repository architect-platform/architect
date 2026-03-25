# maven-architected

> **Status**: Experimental (incubating tier) — see [STATUS.md](STATUS.md)

## Overview

maven-architected integrates Apache Maven into the Architect task lifecycle, providing tasks for building, testing, and packaging Maven projects. It enables teams to manage Java/Maven builds through standardized Architect commands.

## Tasks

| Task ID | Description |
|---|---|
| `mvn-verify` | Run Maven verify lifecycle |
| `mvn-package` | Run Maven package lifecycle |
| `mvn-deploy` | Deploy artifacts to the configured repository |

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: maven-architected
    repo: architect-platform/maven-architected

maven:
  profiles:
    - production
  settings: .mvn/settings.xml
  skipTests: false
```

## Local Build and Test

```bash
cd plugins/maven-architected/app
gradle build
gradle test
```

## Limitations

This plugin is at incubating tier. Tests and documentation are minimal. See [STATUS.md](STATUS.md) for graduation criteria.
