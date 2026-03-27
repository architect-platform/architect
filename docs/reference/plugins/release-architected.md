# release-architected

Unified release preparation, publishing, and rollback automation for Architect projects.

**Source**: `architect-platform/release-architected`

## Installation

```yaml
plugins:
  - name: release-architected
    type: github
    repo: architect-platform/release-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `release-prepare` | `RELEASE` | Analyze commits, determine the next version, and update local release files |
| `release-publish` | `PUBLISH` | Publish configured npm, Docker, and GitHub Release artifacts |
| `release-rollback` | `RELEASE` | Restore prepared files and delete the local release tag when present |

## Configuration

Configuration key: `release`

```yaml
release:
  enabled: true
  strategy: semantic
  workingDirectory: .
  changelog: true
  changelogPath: CHANGELOG.md
  releaseNotesPath: build/release-notes.md
  versionFiles: [VERSION, package.json]
  tagPrefix: v
  artifacts:
    - type: npm
      registry: https://registry.npmjs.org
    - type: docker
      registry: ghcr.io
      image: architect-platform/architect
      tags: [latest]
    - type: github-release
      assets: [dist/*.tar.gz]
  publish:
    beforeCommands: []
    afterCommands: []
  rollback:
    deleteTag: true
    restoreVersionFiles: true
    restoreChangelog: true
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | `boolean` | `true` | Enable or disable all release tasks |
| `workingDirectory` | `string` | `.` | Directory used for git and publish orchestration |
| `strategy` | `string` | `semantic` | Versioning strategy: `semantic`, `calendar`, or `manual` |
| `changelog` | `boolean` | `true` | When enabled, prepend a release section to `CHANGELOG.md` during `release-prepare` |
| `changelogPath` | `string` | `CHANGELOG.md` | Changelog file path relative to `workingDirectory` |
| `releaseNotesPath` | `string` | `build/release-notes.md` | Generated release notes path consumed by `release-publish` |
| `versionFiles` | `string[]` | `[VERSION]` | Files updated with the prepared version |
| `tagPrefix` | `string` | `v` | Prefix used when reading and creating release tags |
| `currentVersion` | `string` | `null` | Explicit current version override when no version file or tag is available |
| `manualVersion` | `string` | `null` | Explicit version used by `strategy: manual` |
| `artifacts[].type` | `string` | — | Artifact type: `npm`, `docker`, or `github-release` |
| `artifacts[].registry` | `string` | `null` | Registry or host used by npm/Docker artifacts |
| `artifacts[].directory` | `string` | `null` | Working directory override for npm publishing |
| `artifacts[].image` | `string` | `null` | Docker image repository name |
| `artifacts[].context` | `string` | `null` | Docker build context path |
| `artifacts[].dockerfile` | `string` | `null` | Dockerfile path |
| `artifacts[].assets` | `string[]` | `[]` | Asset glob patterns for GitHub Releases |
| `artifacts[].notesPath` | `string` | `null` | Artifact-specific notes path override |
| `artifacts[].tags` | `string[]` | `[]` | Extra Docker tags appended in addition to the release version |
| `publish.beforeCommands` | `string[]` | `[]` | Commands executed before artifact publishing |
| `publish.afterCommands` | `string[]` | `[]` | Commands executed after artifact publishing |
| `rollback.deleteTag` | `boolean` | `true` | Delete the local tag when rolling back |
| `rollback.restoreVersionFiles` | `boolean` | `true` | Restore configured version files with `git checkout -- ...` |
| `rollback.restoreChangelog` | `boolean` | `true` | Restore the changelog file with `git checkout -- ...` |

## Versioning strategies

- **semantic** — analyze conventional commits since the last matching tag:
  - breaking change → major
  - `feat:` → minor
  - all other commits → patch
- **calendar** — generate `yyyy.MM.dd` and add `.N` when a same-day version already exists
- **manual** — use `release.manualVersion` or pass the version as the first task argument

## Publish behavior

- `npm` artifacts run `npm publish` with an optional registry override.
- `docker` artifacts run `docker build` and push each generated tag.
- `github-release` artifacts run `gh release create`, optionally attaching generated notes and resolved asset globs.

## Usage examples

```bash
# Prepare the next release from conventional commits
architect release-prepare

# Publish the prepared version across configured artifacts
architect release-publish

# Roll back the prepared local release state
architect release-rollback
```
