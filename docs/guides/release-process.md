# Release Process

## Overview

The Architect Platform uses an automated, convention-driven release process.
Every module in the monorepo follows the same pipeline pattern: code merges to
`main`, a reusable CI workflow builds and tests the module, and — for modules
that publish artifacts — the pipeline creates a release and publishes to the
appropriate registry.

All release orchestration is performed through the **Architect CLI** itself,
keeping the process consistent regardless of which module is being released.

---

## Release Types

### API Releases

`architect-api` is the published contract consumed by plugins and downstream
modules. Releases are published to **GitHub Packages (Maven)** so that any
Gradle or Maven project can depend on them.

### Plugin Releases

Each plugin (`architecture-architected`, `git-architected`, `gradle-architected`,
etc.) is packaged as a **shadow JAR** and attached as an asset to a
**GitHub Release** on the monorepo.

### CLI Releases

`architect-cli` has the widest distribution surface:

| Channel               | Artifact                              |
|-----------------------|---------------------------------------|
| GitHub Releases       | GraalVM native images (macOS, Linux)  |
| Homebrew              | Tap formula auto-updated              |
| Docker Hub            | Container image                       |
| Linux packages        | `.deb` / `.rpm` via dedicated workflow |
| Windows installer     | `.msi` via dedicated workflow          |

### Engine Releases

`architect-engine` is published as a **shadow JAR** to GitHub Releases and as a
**Docker image** for containerised execution.

---

## Release Workflow

1. A pull request is merged into the `main` branch.
2. The module's generated pipeline (e.g. `architect-cli-pipeline.yml`) triggers.
3. The reusable workflow (`reusable-kotlin-pipeline.yml`) executes:
   - **Build** — compile and run unit tests.
   - **Release** — run `architect release --plain` to create a Git tag and
     generate release notes.
   - **Publish** — run `architect publish --plain` to push artifacts to the
     configured registries.
4. Release artifacts are created automatically — no manual steps required for
   standard releases.

---

## Release Commands

All release operations are driven by the Architect CLI:

```bash
# Create a release tag and generate release notes
architect release --plain

# Publish artifacts to the configured registries
architect publish --plain
```

Both commands read their configuration from the module's `architect.yml` file,
which declares the publish targets and versioning strategy.

---

## Module-Specific Release Channels

| Module               | Release Channel(s)                                                     |
|----------------------|------------------------------------------------------------------------|
| `architect-api`      | GitHub Packages (Maven)                                                |
| `architect-cli`      | GitHub Releases (native images) · Homebrew · Docker · Linux pkgs · MSI |
| `architect-engine`   | GitHub Releases (shadow JAR) · Docker image                            |
| Plugins              | GitHub Releases (JAR assets)                                           |
| `architect-core`     | Internal module — no published artifacts                               |
| `architect-cloud/*`  | Internal modules — no published artifacts                              |

---

## Manual Release Triggers

Several workflows support `workflow_dispatch` for on-demand releases:

- **linux-packages** — build `.deb` and `.rpm` packages.
- **native-image** — build GraalVM native images for all platforms.
- **windows-installer** — build the `.msi` Windows installer.

Trigger them from the GitHub Actions UI or via the GitHub CLI:

```bash
gh workflow run native-image.yml
```

---

## Pre-Release Checklist

Before cutting a release, verify:

- [ ] **Convention checks pass** — `bash scripts/convention-check.sh`
- [ ] **All tests pass** — every module's pipeline is green on `main`.
- [ ] **Version is bumped** — the module's `architect.yml` contains the
      intended version.
- [ ] **No workflow drift** — run `python3 .github/scripts/gen_workflows.py`
      and confirm no uncommitted changes in `.github/workflows/`.
- [ ] **CHANGELOG updated** — release notes reflect the changes being shipped.
