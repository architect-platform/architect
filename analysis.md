# Architect Platform — Design Analysis

## What It Is

Architect is a **plugin-based task execution framework** written in Kotlin. It defines a universal project automation layer that sits above your existing tooling — git, gradle, npm, docker, whatever — and gives every project a unified, phase-ordered lifecycle.

The system has four primary components:

- **API** (`architect-api`) — the published contract library. Defines `Task`, `Phase`, `ArchitectPlugin`, `Environment`, `ProjectContext`. This is what plugin authors depend on.
- **Engine** (`architect-engine`) — a Micronaut HTTP daemon running on port 9292. Loads projects, downloads plugins from GitHub Releases at runtime via JAR + SPI, resolves task dependencies, executes tasks with async event streaming.
- **CLI** (`architect-cli`) — a thin Picocli client. Registers the current directory with the engine and delegates everything via HTTP. Streams SSE events to the terminal.
- **Plugins** (`plugins/`) — independently versioned JARs published to GitHub Releases, each implementing `ArchitectPlugin<Context>` and registering tasks into the lifecycle phases.

The lifecycle is defined by three phase enums in the API:

```
CoreWorkflow: INIT → LINT → VERIFY → BUILD → [RUN | TEST] → RELEASE → PUBLISH
CodeWorkflow: CODE-CLEAN → CODE-LINT → ... (mirrors CoreWorkflow, language-focused)
HooksWorkflow: PRE_COMMIT, PRE_PUSH, COMMIT_MSG
```

Tasks declare a phase and automatically inherit that phase's dependencies. There is no need to manually wire `build` to depend on `verify` — the phase system handles it.

---

## What It Gets Right

### The Phase Model Is a Genuine Insight

The biggest intellectual contribution of Architect is separating **what a task does** from **when it belongs**. A Gradle build task declares itself as `CodeWorkflow.BUILD`. A docs build task declares `CoreWorkflow.BUILD`. The engine resolves transitive phase dependencies automatically. This is Maven's lifecycle without being tied to Maven, Gradle, or Java.

This is the right abstraction. Project automation does have a natural topology: you can't release before you build, you can't build before you verify. Encoding this as a first-class concept — not just a convention or documentation — is the right move.

### Plugin Distribution Model

Downloading versioned JARs from GitHub Releases at runtime is clever. It means:
- Plugins are independently versioned and released
- The engine is stable; plugins evolve separately
- Projects pin exact plugin versions in `architect.yml`
- No monorepo coupling between engine and plugins

This mirrors how `terraform` handles providers or how `gradle` handles plugins from Gradle Plugin Portal.

### Typed Configuration per Plugin

Each plugin declares a `ctxClass`. The engine uses Jackson to deserialize the plugin's YAML section into a typed POJO. This means plugin authors get compile-time safety on their configuration, and IDE support works naturally. This is significantly better than untyped string maps.

### Clean Separation of API, Engine, and CLI

The API is a small, stable contract. The engine is a runtime. The CLI is replaceable. A VS Code extension, a web UI, or a GitHub Actions runner could all talk to the same engine API. The architecture correctly identifies that the CLI is not the product — the execution model is.

### Real-Time Event Streaming

SSE from the engine to the CLI means execution feels live. You see which task started, which completed, which failed, with project context. This is the right UX for long-running builds.

### Monorepo-First

Auto-discovery of subprojects (any directory with `architect.yml`) and concurrent subproject execution is correct default behavior. Monorepos are the norm, not the exception, and the engine handles them without configuration.

### Security-Conscious Plugin Code

The git plugin includes shell argument escaping and config key validation. The docs plugin has a `InputSanitizer` for path and domain sanitization. This discipline is rare and important given that plugins execute shell commands.

---

## What It Gets Wrong

### The Daemon Is a Barrier, Not a Feature

Every interaction requires the engine to be running. `architect build` fails silently or confusingly if the daemon is down. This is the biggest friction point.

Most developers don't think of their build tool as a server. When `architect build` fails because the engine isn't running, the mental model breaks immediately and trust is lost. The daemon pattern has real benefits (caching, remote access, web UI) but they are opt-in benefits, not default requirements.

The current design forces you to say `architect engine start` before you can say `architect build`. This is worse than any competing tool.

### No Embedded / Local Execution Mode

There is no way to run tasks without the engine. The CLI has no task execution logic of its own. The API has all the interfaces, the engine has the executor — but there's no path to `./architect build` that doesn't involve an HTTP roundtrip.

This makes `architect` harder to adopt locally, in CI, in containers, and in environments where running a persistent daemon is not practical.

### GitHub-Only Plugin Distribution

You cannot distribute a plugin as a Maven artifact, from a local registry, from a private artifact store, or from an npm package. The only supported sources are GitHub Releases and local paths.

This creates ecosystem lock-in. A team at a company that doesn't use GitHub cannot participate. A plugin author cannot use their existing artifact repository. The "latest" version resolution requires a live GitHub API call, making builds non-deterministic and fragile on GitHub rate limits.

### No Schema Validation for `architect.yml`

The engine silently accepts any YAML and only discovers misconfiguration at runtime — when Jackson fails to deserialize a plugin context, or when a task tries to access a missing config value. There is no IDE support, no schema file, no `architect validate` command.

Plugin authors have to document their config schema as prose. Users have to guess-and-check.

### Task Execution Is Sequential Within a Project

The topological sort produces a strict linear order. Tasks without dependency relationships between them still execute one at a time. A project with 10 independent lint tasks waits for each one before starting the next.

Nx, Gradle, and Bazel all exploit the DAG for parallelism. Architect has the DAG but doesn't use it.

### No Dry-Run / Plan Mode

There is no way to ask "what would happen if I ran `architect build`?" without actually running it. No `--dry-run`, no `--plan`, no task graph visualization.

This matters especially for workflows like `RELEASE` and `PUBLISH` where running the wrong thing has consequences.

### Execution History Is Ephemeral

Task results exist only as SSE events streamed to the terminal during execution. Restarting the engine erases all history. There is no `architect history`, no log file, no way to see what happened during last night's CI run without the Cloud component.

### The Cloud Component Is the Escape Hatch for Basic Features

Execution history, multi-project visibility, and dashboards require standing up `architect-cloud`. This is a significant operational burden for what should be basic developer tooling.

The right model: basic history and observability locally (SQLite or file-based), upgraded to centralized with Cloud as an explicit opt-in.

### Plugin Loading Has No Isolation

All plugins share the same `URLClassLoader` chain. If two plugins depend on different versions of the same library, one will win silently. There is no classloader isolation between plugins. This is the classic JAR hell problem.

### Task Arguments Are Untyped

Tasks that accept arguments receive a raw `List<String>`. There is no argument parsing, no named arguments, no validation, no `--help` per task. The `scripts-architected` plugin's tasks are entirely opaque from the CLI perspective.

### The `curl | bash` Installer Is a Trust Problem

`architect engine install` executes `curl | bash`. This is a well-known security anti-pattern. At minimum, the download should be verified against a checksum or signature before execution. For a tool that runs arbitrary plugins on your machine, the installation story matters.

---

## What It's Trying to Be

The system is trying to be three things simultaneously, which creates tension:

**1. A universal project lifecycle manager** — like Maven's lifecycle or `npm run` convention, but language-agnostic. Define phases once, let plugins fill them in.

**2. A shareable task library** — like Terraform providers or GitHub Actions, but for local and CI workflows. Version your automation the same way you version your code.

**3. A platform for project observability** — the Cloud component suggests ambition toward SaaS, multi-tenant monitoring, and centralized execution dashboards.

These are compatible goals but they have different shapes. The lifecycle manager wants to be frictionless and embedded. The shareable task library wants a rich discovery ecosystem. The platform wants a persistent backend and identity layer.

The current design has chosen the platform path (daemon, HTTP API, Cloud) but applied it to the lifecycle manager use case (developers running `architect build` locally). The friction is the collision between these two shapes.

---

## What It's Missing

### A Task Composition Language

Right now, if you want to run three tasks in sequence without writing a plugin, you can't. `architect.yml` has no inline task definitions. You either write a plugin (Kotlin, JVM, published to GitHub) or configure `scripts-architected` with shell commands.

There should be a way to define lightweight tasks directly in `architect.yml`:

```yaml
tasks:
  deploy:
    description: "Build and push to staging"
    depends: [build, test]
    phase: PUBLISH
    run: "kubectl apply -f k8s/"
```

This closes the gap between "I need a quick script" and "I need a full plugin."

### Plugin Discovery

There is no way to search for plugins. You have to know the exact GitHub repository path. There is no `architect plugin search`, no registry, no curated list beyond the official plugins in this repo.

### Task Conditioning

Tasks cannot declare what they require before running. A task that requires `DOCKER_TOKEN` to be set, or requires `docker` to be in PATH, or only runs on Linux — these conditions must be implemented inside the task itself or not at all.

```yaml
tasks:
  docker-push:
    phase: PUBLISH
    requires:
      env: [DOCKER_TOKEN]
      tools: [docker]
      platform: linux
```

This kind of declaration enables better error messages, faster failure, and dry-run support.

### Affected Task Detection

For large monorepos, running all tasks on every commit is wasteful. There is no mechanism to detect which projects changed and limit execution to affected subprojects. Nx calls this "affected commands" and it is one of the primary reasons teams adopt Nx.

### IDE / Language Server Integration

`architect.yml` has no schema. There is no JSON Schema, no language server, no VS Code extension for autocompletion of phase names, plugin config keys, or task IDs. Developers write YAML in the dark.

### Plugin Authoring Tooling

There is no `architect plugin new` scaffolding command. No testing framework for plugins. No local plugin hot-reload for development. Writing a plugin requires understanding SPI, fat JAR packaging, GitHub Releases — a high barrier to community contribution.

---

## Directions Worth Pursuing

### 1. Embedded Execution Without a Daemon

Add an embedded execution mode. When the engine is not running, `architect build` launches an in-process engine, executes, and exits. The daemon becomes optional — useful for IDEs, web UIs, and remote access, but not required for `architect build`.

This single change removes the biggest adoption barrier.

### 2. A First-Party Task DSL in `architect.yml`

Allow projects to define tasks inline. This is the "scripts-architected" plugin done right — as a first-class citizen of the config format, not a plugin. Plugins remain the mechanism for reusable, complex, language-specific automation. Inline tasks are for project-specific wiring.

### 3. Plugin Registry Protocol

Decouple plugin distribution from GitHub. Define a simple protocol: a registry entry is a YAML/JSON document pointing to a versioned artifact. Implement the default registry as a GitHub-hosted file. Allow private registries via config.

This mirrors how `terraform` has a public registry and supports private registries with the same protocol.

### 4. DAG Visualization and Dry Run

`architect plan build` should print the execution graph — which tasks will run, in what order, with which dependencies. This is table stakes for a serious build tool. Bazel calls it `--explain`, Pulumi calls it `preview`.

### 5. Parallel Task Execution

Use the topological sort output to identify batches of tasks with no remaining dependencies and execute them concurrently. The infrastructure (coroutines, SSE events) is already in place. This is an extension of what subprojects already do.

### 6. Local Execution History

Write execution results to `~/.architect/history.db` (SQLite) or append to `~/.architect/logs/`. Enable `architect history` and `architect log <task>`. The Cloud component becomes an aggregation layer over these local logs, not the only way to see history.

### 7. Schema-First Configuration

Define a JSON Schema or Kotlin `@Schema` annotation for each plugin's context class. Generate documentation and IDE integrations from it. Validate `architect.yml` at registration time with useful error messages pointing to the exact YAML path and what was expected.

### 8. Language-Agnostic Plugin Protocol

The current plugin interface requires Kotlin/JVM. Add a secondary protocol: a plugin can be an executable (any language) that implements a simple stdin/stdout protocol — receive a JSON task invocation, return a JSON result. This would allow plugins written in Go, Python, Rust, or shell.

The JAR/SPI path remains for performance-critical or JVM-ecosystem plugins. The subprocess path enables the long tail of the ecosystem.

### 9. The `architect-x` / `architect-server` / `architect-data` Direction

These scaffolded modules point somewhere intentional. `architect-data` likely means persistence. `architect-server` likely means a more capable server than the current engine (API gateway? multi-tenant?). `architect-x` could be extended tooling or a paid tier.

If the direction is SaaS/platform, the right move is:
- `architect-data`: Shared persistence layer (projects, executions, artifacts) with DB-agnostic adapters
- `architect-server`: Multi-engine coordination (multiple engines report to one server, triggering cross-engine workflows)
- `architect-x`: Feature gates, RBAC, team management, audit logging

The engine should be designed as a node in this larger system from the start, not retrofitted.

---

## The Reframing

Architect is most precisely described as: **a phase-typed task registry with a plugin ecosystem and a remote execution protocol**.

The word "phase-typed" is the core concept. It means task scheduling is governed by a lifecycle topology, not by arbitrary script names. This is the right way to think about project automation at scale.

Everything else — the daemon, the SSE streaming, the plugin downloads, the Cloud component — is infrastructure serving this idea.

The tool should own this identity explicitly. The README should open with the phase/lifecycle mental model, not with "unified way to manage documentation, releases, and builds." The goals should be:

1. **Every project, regardless of language or tooling, has the same lifecycle vocabulary.** `architect build` means the same thing across a Go service, a Python library, and a React app.
2. **Lifecycle phases are typed contracts, not naming conventions.** A task in `BUILD` depends on `VERIFY`. This is enforced, not documented.
3. **Workflow automation is composable and versioned.** Plugins are artifacts. They can be audited, pinned, upgraded. Not scripts in a scripts/ folder.
4. **The runtime is optional infrastructure.** The model stands on its own. Running it embedded or as a daemon is an implementation choice.

---

## Summary

| Area | Assessment |
|------|------------|
| Phase/lifecycle model | Strong — the core right idea |
| Plugin versioning via GitHub Releases | Good direction, too narrow |
| Typed plugin config with Jackson | Correct, should add schema validation |
| SSE event streaming | Good UX, infrastructure is sound |
| Monorepo subproject support | Works, needs affected-detection |
| Required daemon for CLI | Critical friction — needs embedded mode |
| Task execution parallelism | Missing — topology is there, parallelism is not |
| Dry-run / plan mode | Missing |
| Local execution history | Missing |
| Plugin ecosystem discoverability | Missing |
| Schema-first config | Missing |
| Language-agnostic plugins | Missing |
| `architect-x/data/server` scaffolding | Promising direction, undefined |

---

## Cognitive Complexity Analysis

### Module Statistics

| Module | Files | Est. Lines | Risk |
|---|---|---|---|
| architect-api/api | 27 | ~2,013 | ✅ Healthy |
| architect-core/core | 102 | ~6,110 | ⚠️ High |
| architect-engine/engine | 25 | ~1,425 | ✅ Healthy |
| architect-cli/cli | 26 | ~3,877 | 🔴 Critical |
| plugins/git-architected | 4 | ~288 | ✅ Healthy |
| plugins/github-architected | 7 | ~362 | ✅ Healthy |

### Highest Cognitive Load Classes

| Rank | File | Lines | Branches | Domains | Risk |
|---|---|---|---|---|---|
| 1 | ArchitectLauncher.kt (CLI) | 1609 | 157 | 8 | 🔴 Critical |
| 2 | PluginScaffolder.kt (core) | 410 | 7 | 3+ | 🟠 High |
| 3 | ProjectService.kt (engine) | 312 | 25 | 6 | 🟠 High |
| 4 | ConsoleUI.kt (CLI) | 300 | 24 | 4 | 🟠 High |
| 5 | ProcessPluginAdapter.kt (core) | 266 | 19 | 5 | 🟠 High |
| 6 | TaskExecutor.kt (core) | 220 | 19 | 6 | 🟠 High |

### Recommended Refactoring Priority

1. **ArchitectLauncher** → Extract 8 command handlers (Strategy pattern)
2. **ProjectService** → Split into ProjectLoaderService, PluginDiscoveryService, ProjectWatchingService
3. **ProcessPluginAdapter** → Separate ProcessLauncher, JsonRpcClient, PluginProcessAdapter
4. **TaskExecutor** → Extract TaskCacheLayer, TaskEventPublisher
5. **ConsoleUI** → Extract formatters per output concern
