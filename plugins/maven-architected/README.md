# maven-architected

> **Status**: Experimental (incubating tier) — see [STATUS.md](STATUS.md)

Maven build and lifecycle automation for Architect projects.

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
./gradlew build
./gradlew test
```

## Limitations

This plugin is at incubating tier. Tests and documentation are minimal. See [STATUS.md](STATUS.md) for graduation criteria.
