# Versioned Project Example

This example demonstrates how to pin CLI and Engine versions in an Architect project for reproducible builds.

## Overview

Version pinning ensures that all team members and CI/CD pipelines use the same versions of Architect components, preventing "works on my machine" issues.

## Configuration

The `architect.yml` file includes:

```yaml
architect:
  cliVersion: "1.1.0"
  engineVersion: "1.6.1"
```

## When to Use Version Pinning

- **Production deployments**: Ensure stable, predictable behavior
- **Team projects**: Keep everyone on the same versions
- **CI/CD pipelines**: Reproducible builds across runs
- **Long-term projects**: Prevent unexpected breaking changes

## Testing This Example

1. Install the specified CLI version (if not already):
   ```bash
   curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-cli/.installers/bash | bash
   ```

2. Start the Engine:
   ```bash
   architect engine start
   ```

3. Run any command:
   ```bash
   architect
   ```

4. You should see no version warnings if your versions match.

5. To test version mismatch warnings, temporarily change the version in `architect.yml` to a different value and run a command again.

## Learn More

See the [Version Management Documentation](../../docs/versioning.md) for more details.
