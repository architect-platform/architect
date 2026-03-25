# Architect Commands

Architect uses a plugin-based task execution model. Each plugin registers named tasks
that you invoke through the CLI. This guide covers every built-in task organised by
the plugin that provides it.

## Running Tasks

```bash
# Run a single task
architect <task-name>

# Run a task with arguments
architect <task-name> -- --flag value

# List all registered tasks for the current project
architect tasks

# Run tasks from a specific module directory
cd architect-engine && architect gradle-build
```

Tasks are resolved from the `plugins` section of the nearest `architect.yml`.

---

## Documentation Tasks — `docs-architected`

Manages documentation sites (MkDocs, Docusaurus, VuePress, or manual builds).

| Task | Phase | Description |
|------|-------|-------------|
| `docs-init` | INIT | Scaffold `docs/` structure, `mkdocs.yml`, and initial pages |
| `docs-build` | BUILD | Build the documentation site into `site/` |
| `docs-publish` | PUBLISH | Deploy the built site to GitHub Pages (`gh-pages` branch) |

```bash
architect docs-init       # create skeleton docs
architect docs-build      # generate HTML
architect docs-publish    # push to GitHub Pages
```

---

## Git Tasks — `git-architected`

Wraps common Git operations so they can participate in pipelines and lifecycle phases.

| Task | Phase | Description |
|------|-------|-------------|
| `git-config` | INIT | Apply repository-level Git settings from `architect.yml` |
| `git-status` | BUILD | Show working-tree status |
| `git-add` | BUILD | Stage files for the next commit |
| `git-commit` | BUILD | Record staged changes to the repository |
| `git-push` | PUBLISH | Push local commits to the remote |
| `git-pull` | BUILD | Fetch and merge changes from the remote |
| `git-fetch` | BUILD | Download objects and refs without merging |
| `git-checkout` | BUILD | Switch branches or restore working-tree files |
| `git-branch` | BUILD | List, create, or delete branches |
| `git-log` | BUILD | Display commit history |
| `git-diff` | BUILD | Show changes between commits, index, or working tree |
| `git-merge` | BUILD | Join two or more development histories |
| `git-reset` | BUILD | Reset current HEAD to a specified state |
| `git-stash` | BUILD | Stash and restore uncommitted changes |
| `git-tag` | PUBLISH | Create, list, or delete tags |
| `git-remote` | INIT | Manage remote repository references |

---

## GitHub Tasks — `github-architected`

Integrates with GitHub for CI pipelines and releases.

| Task | Phase | Description |
|------|-------|-------------|
| `github-init-dependencies` | INIT | Validate required GitHub environment variables |
| `github-init-pipelines` | INIT | Synchronise pipeline definitions to `.github/workflows/` |
| `github-release-task` | RELEASE | Create a GitHub Release with an auto-generated changelog |

---

## Build Tasks — `gradle-architected`

Drives Gradle builds for Kotlin/JVM modules.

| Task | Phase | Description |
|------|-------|-------------|
| `gradle-build` | BUILD | Compile and assemble all configured Gradle projects |
| `gradle-test` | TEST | Run the test suites for all configured projects |
| `gradle-run` | RUN | Start the application via `./gradlew run` |
| `gradle-publish*` | PUBLISH | Publish artifacts to GitHub Packages |

Pass `-p <project-path>` to target a single sub-project:

```bash
architect gradle-build -- -p architect-api/api
architect gradle-test  -- -p architect-engine/engine
```

---

## Script Tasks — `scripts-architected`

Runs user-defined shell scripts declared in `architect.yml`. Each entry under
`scripts.scripts` becomes a task prefixed with `scripts-`.

| Task | Phase | Description |
|------|-------|-------------|
| `scripts-convention-check` | LINT | Run repository convention checks |
| `scripts-release-readiness` | VERIFY | Run release-readiness verification |
| `scripts-detekt` | LINT | Run Detekt static analysis across modules |
| `scripts-ktlint-check` | LINT | Run ktlint code-style checks |

Custom scripts are declared like this:

```yaml
scripts:
  enabled: true
  scripts:
    my-task:
      command: "./scripts/my-task.sh"
      description: "Does something useful"
      phase: "BUILD"
```

---

## Pipeline Tasks — `pipelines-architected`

Orchestrates multi-step workflows with dependency ordering.

| Task | Phase | Description |
|------|-------|-------------|
| `pipelines-init` | INIT | Load and validate pipeline definitions |
| `pipelines-list` | BUILD | List all registered pipelines |
| `pipelines-execute` | BUILD | Execute a named pipeline by name |

Each declared workflow also generates a dynamic task:

```bash
architect pipelines-execute -- --name build-all
architect pipelines-execute -- --name test-all
architect pipelines-execute -- --name full-validation
```

---

## Usage Examples

### Build everything

```bash
architect pipelines-execute -- --name build-all
```

### Run all tests

```bash
architect pipelines-execute -- --name test-all
```

### Full validation (lint → build → test)

```bash
architect pipelines-execute -- --name full-validation
```

### Publish documentation

```bash
architect docs-build && architect docs-publish
```

### Create a release

```bash
architect github-release-task
```

### Check code quality

```bash
architect scripts-detekt
architect scripts-ktlint-check
architect scripts-convention-check
```
