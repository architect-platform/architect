# `architect.yml` Reference

> **JSON Schema**: `https://architect.dev/schema/architect.yml.json`
>
> Add `$schema: "https://architect.dev/schema/architect.yml.json"` to your file for IDE auto-complete and validation.

---

## Top-level structure

```yaml
$schema: "https://architect.dev/schema/architect.yml.json"

project:    # required — project metadata
  ...

plugins:    # optional — list of plugins to load
  - ...

tasks:      # optional — inline task definitions
  task-id:
    ...

# Plugin-specific configuration blocks
# The key matches the plugin `name`
git-architected:
  ...
```

---

## `project`

**Required.** Project metadata.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | `string` | ✅ | Unique project identifier. Used as the key in the Engine's project registry. |
| `description` | `string` | | Human-readable project description. |
| `affected` | `object` | | Affected-task detection for monorepos (see below). |

### `project.affected`

Controls which projects are always or never included in `architect affected` runs.

| Field | Type | Description |
|-------|------|-------------|
| `always-include` | `string[]` | Projects always included regardless of changes |
| `never-include` | `string[]` | Projects always excluded regardless of changes |

```yaml
project:
  name: monorepo-root
  affected:
    always-include:
      - shared-libs
    never-include:
      - legacy-module
```

---

## `plugins`

A list of plugin declarations. Each entry tells Architect where to load the plugin JAR from.

```yaml
plugins:
  - name: git-architected
    type: github
    repo: architect-platform/git-architected
    version: "1.2.0"
```

### Plugin fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `name` | `string` | — | **Required.** Plugin identifier. Also used as key for the plugin configuration block. |
| `type` | `enum` | `github` | Plugin source type. See [plugin types](#plugin-types) below. |
| `version` | `string` | `latest` | Semver version or `latest`. |
| `repo` | `string` | — | GitHub repo in `owner/name` format. Required for `type: github`. |
| `owner` | `string` | `architect-platform` | Override GitHub owner. |
| `asset` | `string` | — | Exact release asset filename. Overrides `pattern`. |
| `assetType` | `enum` | `jar` | Asset format. Currently only `jar` is supported. |
| `pattern` | `string` | — | Release asset filename prefix pattern (e.g. `git-architected-`). |
| `path` | `string` | `.` | Local filesystem path. Required for `type: local`. |
| `registry` | `string` | — | Registry index URL. Required for `type: registry`. |
| `url` | `string` | — | Direct asset download URL. Required for `type: http`. |
| `command` | `string` | — | Command to execute. Required for `type: process`. |
| `package` | `string` | — | npm package name. Required for `type: npm`. |

### Plugin types

| `type` | Loads from | Required field |
|--------|-----------|----------------|
| `github` | GitHub Releases | `repo` |
| `local` | Local filesystem JAR | `path` |
| `registry` | Architect plugin registry | `registry` |
| `http` | Arbitrary URL | `url` |
| `process` | A subprocess (sidecar) | `command` |
| `npm` | npm package | `package` |

---

## `tasks`

A map of inline task definitions. Keys are task IDs (lowercase, hyphens recommended).

```yaml
tasks:
  compile:
    description: "Compile the project"
    run: ./gradlew compileKotlin
    phase: BUILD

  test:
    description: "Run tests"
    run: ./gradlew test
    phase: TEST
    depends:
      - compile
```

### Inline task fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `description` | `string` | | Human-readable task description shown in `architect tasks`. |
| `run` | `string` | | Shell command to execute. |
| `phase` | `enum` | | Lifecycle phase. See [phases](#phases) below. |
| `depends` | `string[]` | | List of task IDs that must complete before this task runs. |
| `permissions` | `string[]` | | Explicit permissions required by the task (`file-system:read`, `file-system:write`, `network:outbound`, `process:exec`). |
| `requires` | `object` | | Runtime prerequisites checked before execution (`tools`, `min-tool-versions`, `env`, `platform`). |
| `condition` | `string` | | Runtime condition for skipping the task, for example `env.BRANCH == 'main'`. |
| `timeout` | `string` | | Per-task timeout such as `300s`, `5m`, or `1h`. |
| `onFailure` | `enum` | | Failure strategy: `ABORT`, `CONTINUE`, or `RETRY`. |
| `retryAttempts` | `integer` | | Number of retry attempts when `onFailure: RETRY`. |

### Phases

Tasks assigned to a phase participate in the standard build lifecycle. Phases execute in this order:

| Phase | Code Workflow | Hooks Workflow | Description |
|-------|---------------|----------------|-------------|
| `INIT` | `CODE-init` | | Project initialization, dependency install |
| `LINT` | `CODE-lint` | | Static analysis, style checks |
| `VERIFY` | `CODE-verify` | | Schema and contract validation |
| `BUILD` | `CODE-build` | | Compilation and artefact generation |
| `RUN` | `CODE-run` | | Local execution / dev server |
| `TEST` | `CODE-test` | | Unit and integration tests |
| `RELEASE` | `CODE-release` | | Tag creation, changelog |
| `PUBLISH` | `CODE-publish` | | Registry push, deployment |
| — | — | `pre-commit` | Git pre-commit hook |
| — | — | `pre-push` | Git pre-push hook |
| — | — | `commit-msg` | Git commit-msg hook |

`CODE-*` phases mirror `CoreWorkflow` phases but operate at a code-module level in monorepos where both infrastructure and code tasks coexist.

---

## Plugin configuration blocks

After declaring a plugin under `plugins:`, add a top-level block with the same key to configure it:

```yaml
plugins:
  - name: git-architected
    type: github
    repo: architect-platform/git-architected

git-architected:
  remote: origin
  defaultBranch: main
  signCommits: false
```

Refer to the individual [plugin pages](../reference/plugins/index.md) for each plugin's configuration schema.

---

## Complete example

```yaml
$schema: "https://architect.dev/schema/architect.yml.json"

project:
  name: my-app
  description: "Production web application"
  affected:
    always-include:
      - shared

plugins:
  - name: git-architected
    type: github
    repo: architect-platform/git-architected

  - name: gradle-architected
    type: github
    repo: architect-platform/gradle-architected
    version: "2.1.0"

  - name: docs-architected
    type: github
    repo: architect-platform/docs-architected

tasks:
  smoke-test:
    description: "Quick sanity check"
    run: curl -f http://localhost:8080/health
    phase: TEST
    depends:
      - gradle-build

git-architected:
  remote: origin
  defaultBranch: main

gradle-architected:
  tasks:
    build: [build, -x, test]
    test: [test, jacocoTestReport]
    lint: [ktlintCheck]

docs-architected:
  framework: mkdocs
  source: docs/
  outputDir: site/
```
