# git-architected

Git version control integration for Architect.

**Source**: `architect-platform/git-architected`

## Installation

```yaml
plugins:
  - name: git-architected
    type: github
    repo: architect-platform/git-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `git-config` | `INIT` | Apply Git configuration settings from context |
| `git-remote` | `INIT` | Manage remote repositories |
| `git-status` | `BUILD` | Show working tree status |
| `git-add` | `BUILD` | Stage files for commit |
| `git-commit` | `BUILD` | Record staged changes |
| `git-pull` | `BUILD` | Fetch and merge from remote |
| `git-fetch` | `BUILD` | Download objects and refs from remote |
| `git-checkout` | `BUILD` | Switch branches or restore files |
| `git-branch` | `BUILD` | List, create, or delete branches |
| `git-log` | `BUILD` | Show commit history |
| `git-diff` | `BUILD` | Show changes between commits / working tree |
| `git-merge` | `BUILD` | Join development histories |
| `git-reset` | `BUILD` | Reset HEAD to a specified state |
| `git-stash` | `BUILD` | Save and restore dirty working directory state |
| `git-push` | `PUBLISH` | Push commits to remote |
| `git-tag` | `PUBLISH` | Create, list, or delete tags |

## Configuration

Configuration key: `git`

```yaml
git-architected:
  enabled: true
  config:
    user.name: "My CI Bot"
    user.email: "ci@example.com"
    core.autocrlf: "false"
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | `boolean` | `true` | Enable or disable the plugin |
| `config` | `map<string,string>` | `{}` | Key-value pairs passed to `git config --local` during INIT |

## Usage examples

```bash
# Check status
architect git-status

# Stage and commit
architect git-add -- .
architect git-commit -- -m "chore: release prep"

# Push and tag as part of publish phase
architect --phase PUBLISH
```

## Security notes

Configuration keys are validated against an allowlist before being applied. Values are shell-escaped. Do not use this plugin to pass arbitrary user input as config values.
