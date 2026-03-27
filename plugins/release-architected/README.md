# release-architected

> **Status**: Incubating — see [STATUS.md](STATUS.md)

## Overview

`release-architected` provides a pragmatic release orchestration layer for Architect projects. It analyzes conventional commits, prepares release metadata, updates version and changelog files inside the project, and coordinates multi-artifact publishing across npm, Docker, and GitHub Releases.

## Tasks

| Task ID | Phase | Description |
|---|---|---|
| `release-prepare` | `RELEASE` | Analyze commits, determine the next version, and optionally update version/changelog files |
| `release-publish` | `PUBLISH` | Publish configured release artifacts such as npm packages, Docker images, and GitHub Releases |
| `release-rollback` | `RELEASE` | Safely roll back local release state by deleting tags and restoring prepared files |

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: release-architected
    repo: architect-platform/release-architected

release:
  enabled: true
  strategy: semantic        # semantic | calendar | manual
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
```

### Configuration notes

- `strategy: semantic` analyzes conventional commits since the last matching tag and recommends a `major`, `minor`, or `patch` bump.
- `strategy: calendar` generates `yyyy.MM.dd` versions and adds a numeric suffix when multiple releases happen on the same day.
- `strategy: manual` uses `release.manualVersion` or the first task argument as the explicit version to prepare/publish.
- `versionFiles` is intentionally pragmatic: plain `VERSION` files are overwritten, while common `version=` / `version:` / `"version": "..."` shapes are updated in place.
- `artifacts[].assets` are expanded relative to the project root and passed to the GitHub Release command.

## Release behavior

### `release-prepare`

- Reads the latest tag with the configured prefix
- Collects commit messages since that tag
- Computes the next version for the selected strategy
- Generates release notes and optionally prepends a new section to `CHANGELOG.md`
- Updates configured version files and returns structured task data

### `release-publish`

- Executes optional pre/post publish hooks
- Publishes configured artifact types using local CLIs through `CommandExecutor`
- Supports:
  - `npm publish`
  - `docker build` + `docker push`
  - `gh release create`
- Returns command, artifact, version, and notes metadata in `TaskResult.data`

### `release-rollback`

- Executes optional pre/post rollback hooks
- Deletes the local tag when present
- Restores configured version files and changelog via `git checkout -- ...`
- Avoids destructive history rewrites by default

## Local build and test

```bash
cd plugins/release-architected/app
./gradlew test
./gradlew build
```
