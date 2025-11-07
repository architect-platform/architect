# Version Management

Architect provides version management for both the CLI and Engine components. This allows projects to:
- Pin specific versions for reproducible builds
- Get notified about available updates
- Ensure version compatibility across team members

## Version Pinning

To pin specific versions of the CLI and Engine in your project, add the `architect` section to your `architect.yml`:

```yaml
project:
  name: my-project
  description: "My Architect project"

architect:
  cliVersion: "1.1.0"
  engineVersion: "1.6.1"

plugins:
  - name: docs-architected
    repo: architect-platform/architect
```

### When to Pin Versions

**Pin versions when:**
- You need reproducible builds in CI/CD environments
- Your project relies on specific features or behavior
- You're working in a team and need consistent tooling
- You're deploying to production and want stability

**Don't pin versions when:**
- You want to automatically get bug fixes and improvements
- You're experimenting or in early development
- You trust the Architect team's semantic versioning

## Version Validation

When versions are pinned, Architect validates them on every command execution:

### CLI Version Validation
If your project requires CLI version `1.1.0` but you're running `1.0.0`, you'll see:

```
⚠️  WARNING: CLI version mismatch
  Required version: 1.1.0
  Current version:  1.0.0

This project requires CLI version 1.1.0.
You are running version 1.0.0.
```

### Engine Version Validation
Similarly, if the Engine version doesn't match:

```
⚠️  WARNING: Engine version mismatch
  Required version: 1.6.1
  Current version:  1.5.0

This project requires Engine version 1.6.1.
The running engine is version 1.5.0.
```

## Update Checking

For projects **without pinned versions**, Architect checks for updates periodically (once per 24 hours).

### How it Works

1. **Automatic Check**: The CLI checks for new versions once per day
2. **Non-Intrusive**: Checks happen in the background and don't slow down commands
3. **Smart Notifications**: You'll only be notified once per new version
4. **Easy Upgrades**: Clear instructions are provided for upgrading

### Update Notification Example

When a new version is available:

```
════════════════════════════════════════════════════════════════════════════════
⚠️  UPDATE AVAILABLE
════════════════════════════════════════════════════════════════════════════════

A new version of Architect CLI is available!
  Current version: 1.1.0
  Latest version:  1.2.0

To upgrade, run:
  curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-cli/.installers/bash | bash

════════════════════════════════════════════════════════════════════════════════
```

## Version Information

### Check Your CLI Version

The CLI version is shown in the version manager and can be retrieved programmatically.

### Check Your Engine Version

The Engine exposes its version via the REST API:

```bash
curl http://localhost:8080/api/version
```

Response:
```json
{
  "version": "1.6.1",
  "component": "engine"
}
```

## Upgrading

### Upgrade the CLI

```bash
curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-cli/.installers/bash | bash
```

### Upgrade the Engine

```bash
architect engine stop
architect engine clean
architect engine install
architect engine start
```

## Best Practices

1. **Pin in Production**: Always pin versions for production deployments
2. **Test Before Pinning**: Test new versions in development before pinning them
3. **Document Reasons**: Add comments in `architect.yml` explaining why specific versions are required
4. **Update Regularly**: Don't let pinned versions get too far behind
5. **CI/CD Strategy**: Consider pinning in CI but not in local development

## Example Configurations

### Development Project (No Pinning)
```yaml
project:
  name: dev-project
  description: "Development project with latest versions"

plugins:
  - name: docs-architected
    repo: architect-platform/architect
```

### Production Project (Pinned)
```yaml
project:
  name: prod-project
  description: "Production project with pinned versions"

# Pin versions for reproducibility
architect:
  cliVersion: "1.1.0"
  engineVersion: "1.6.1"

plugins:
  - name: docs-architected
    repo: architect-platform/architect
  - name: github-architected
    repo: architect-platform/architect
```

### Mixed Environment
```yaml
project:
  name: team-project
  description: "Team project with engine pinned but CLI flexible"

# Pin engine but allow CLI updates
architect:
  engineVersion: "1.6.1"
  # cliVersion is not pinned - CLI can auto-update

plugins:
  - name: docs-architected
    repo: architect-platform/architect
```

## Troubleshooting

### Version Mismatch Warnings

If you see version mismatch warnings but want to proceed anyway, you can either:
1. Upgrade/downgrade to the required version
2. Update the pinned version in `architect.yml` if you control the project
3. Continue with the warning (functionality may be affected)

### Update Checks Not Working

Update checks are stored in `~/.architect-cli/version-check.json`. If checks aren't working:
1. Verify the file exists and is writable
2. Delete the file to force a fresh check
3. Ensure you have network connectivity (for GitHub API calls)

### Can't Find Engine Version

If the CLI can't determine the engine version:
1. Ensure the engine is running (`architect engine start`)
2. Check that the engine is accessible at the configured URL
3. Verify the engine version is 1.6.1 or later (which includes the version endpoint)
