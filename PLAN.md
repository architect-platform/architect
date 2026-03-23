# Architect Platform — Production Readiness Plan

> The goal is the best developer experience and agility tool ever built.
> Every phase is independently committable. Tasks are `[ ]` pending, `[~]` in progress, `[x]` done.

---

## Status
Overall Progress: 157/217 tasks completed (72%)
Current Phase: Phase 24 — Security Hardening
Last Updated: 2026-03-23T16:25:00Z

---

## Guiding Principles

- **Zero friction** — works out of the box, no setup ceremony
- **Convention over configuration** — sensible defaults, opt-in complexity
- **Composable** — every piece is independently useful
- **Transparent** — always show what is happening and why
- **Fast** — sub-second startup, parallel execution, smart caching
- **Trustworthy** — >85% test coverage, secure, deterministic
- **Extensible** — any language, any registry, any workflow

---

## Current State (March 2026)

| Phase | Feature | Status |
|-------|---------|--------|
| 1 | Plan mode (`architect plan <task>`) | ✅ done |
| 2 | Parallel task batch execution | ✅ done |
| 3 | Inline task definitions in `architect.yml` | ✅ done |
| 4 | Local execution history | ✅ done |
| 5 | Config validation at project load | ✅ done |
| 6 | Auto-start engine from CLI | `[~]` in progress |

---

## Phase 6: Auto-Start Engine — Complete

**Goal:** `architect build` works with no running daemon. The CLI auto-starts it, waits for readiness, then executes.

### Tasks

- [x] 6.1 `EngineHealthChecker` — implemented. Verified: uses `@Property(name = "micronaut.http.services.engine.url")` correctly. | Finished: 2026-03-22T00:00:00Z
- [x] 6.2 `ensureEngineRunning()` in `ArchitectLauncher` — binary path resolution fixed: checks `~/.architect/bin/architect-engine` first, falls back to `which architect-engine` on PATH, emits clear install instructions if not found. Also replaced `Runtime.exec()` with `ProcessBuilder`. | Finished: 2026-03-22T00:00:00Z
- [x] 6.3 `--no-daemon` flag — verified: `ensureEngineRunning()` returns immediately when `noDaemon = true`, bypassing all health checks. | Finished: 2026-03-22T00:00:00Z
- [x] 6.4 Status messages — fixed: startup messages (⚙️ Starting / ✅ Engine ready) suppressed when `--plain` is set. | Finished: 2026-03-22T00:00:00Z
- [x] 6.5 Configurable startup timeout — added `@Property(name = "architect.engine.startup-timeout-seconds", defaultValue = "30")` to `ArchitectLauncher`; `application.yml` documents the property. | Finished: 2026-03-22T00:00:00Z
- [x] 6.6 `EngineHealthCheckerTest` — written: HTTP 200 path, timeout path, connection refused path. All pass. | Finished: 2026-03-22T00:00:00Z
- [x] 6.7 `ArchitectLauncherTest` — written: `resolveEngineBinary()` (local bin, non-executable, absent), `--no-daemon` health-check bypass. All pass. | Finished: 2026-03-22T00:00:00Z

### Acceptance Criteria

- `architect build` from a clean terminal (no daemon) starts the engine and executes
- `architect --no-daemon build` fails clearly if engine is not running
- `architect --plain build` suppresses startup UI (for CI)

---

## Phase 7: Bug Fixes & Tech Debt

**Goal:** Fix all known defects before building more features. Clean foundations make everything faster.

### Tasks

- [x] 7.1 **API version mismatch** — `architect-engine/engine/build.gradle.kts` updated from `api:1.2.0` to `api:2.1.0`. Engine still compiles. | Finished: 2026-03-22T00:01:00Z
- [x] 7.2 **SSE stream termination** — replaced `error(...)` + `try/catch` with `sharedFlow.filter { }.transformWhile { }` that completes cleanly on root COMPLETED/FAILED events. | Finished: 2026-03-22T00:02:00Z
- [x] 7.3 **ConfigValidator false warnings** — validation now happens after plugin loading; plugin `contextKey` values are collected and passed as `pluginContextKeys` so `gradle`, `git`, `docs`, etc. no longer produce false warnings. | Finished: 2026-03-22T00:03:00Z
- [x] 7.4 **`ScriptsPlugin` phase resolution** — `parsePhase()` now resolves all three workflows (`CoreWorkflow`, `CodeWorkflow`, `HooksWorkflow`) in priority order, matching `InlineTaskPlugin`. Replaced `println` with idiomatic `runCatching` chain. | Finished: 2026-03-22T00:04:00Z
- [x] 7.5 **Duplicate GitHub tag resolution** — extraction verified complete: shared `GitHubReleaseResolver` is used by both `ProjectPluginLoader` and `GitHubPluginSource` in `architect-engine` and mirrored in `architect-core`; `compileKotlin` passes for engine main sources. | Finished: 2026-03-22T13:21:46Z
- [x] 7.6 **`architect history` without engine** — `HistoryService` writes files to `~/.architect/history/`. `architect history` routes through the engine daemon unnecessarily. Add a direct `LocalHistoryReader` in the CLI that reads these files without an HTTP call, and use it as the primary path (engine endpoint as fallback for remote scenarios)
- [x] 7.7 **Root `architect.yml` missing plugin fields** — document and fix the root config: add `type: local` and `asset` fields, or establish that the root config is a documentation-only example
- [x] 7.8 **`ArchitectLauncher` `Runtime.exec()` fragility** — replace `Runtime.getRuntime().exec(arrayOf("architect-engine"))` with `ProcessBuilder` using the resolved binary path from task 6.2

### Acceptance Criteria

- All existing tests continue to pass after each fix
- `ConfigValidator` produces zero false-positive warnings on any official plugin
- `ScriptsPlugin` phase resolution matches `InlineTaskPlugin` behavior
- `architect history` works without a running engine

---

## Phase 8: Embedded Execution Mode

**Goal:** `architect --embedded build` runs tasks directly in-process, with no daemon. Eliminates the biggest adoption friction for new users and CI pipelines.

### Architecture

Introduce `EmbeddedTaskExecutor` that uses `ProjectService` + `TaskExecutor` as library code, not over HTTP. The CLI detects `--embedded` (or absence of a reachable daemon) and routes to this executor.

```
ArchitectLauncher
  ├─ DaemonMode  →  EngineCommandClient  →  HTTP  →  Engine
  └─ EmbeddedMode → EmbeddedTaskExecutor → ProjectService → TaskExecutor
```

The engine becomes optional, not required. The same `TaskExecutor`, `TaskDependencyResolver`, `PluginLoader`, `HistoryService` are reused without modification.

### Tasks

- [x] 8.1 Extract `architect-engine` execution domain into a separate Gradle sub-project `architect-core` (no Micronaut, no HTTP). Depends on `architect-api`. Contains: `TaskExecutor`, `TaskDependencyResolver`, `ProjectService`, `PluginLoader`, `HistoryService`, `ConfigValidator`, `BashCommandExecutor`, `InlineTaskPlugin`, `CorePlugin`, etc. | Finished: 2026-03-22T13:34:40Z | Notes: Completed standalone `architect-core/core` Gradle project and extraction of core execution domain classes; removed all `io.micronaut` imports and direct HTTP usage from core by introducing core abstractions (`EventBus`, `ProjectRegistrationReporter`, `RemoteContentFetcher`); verified with `./gradlew -q compileKotlin`.
- [x] 8.2 `architect-engine` and `architect-cli` both depend on `architect-core` | Finished: 2026-03-22T13:36:27Z | Notes: added `io.github.architectplatform:architect-core:1.6.1` dependency to both modules and wired `includeBuild("../../architect-core/core")` dependency substitution in `architect-engine/engine/settings.gradle.kts` and `architect-cli/cli/settings.gradle.kts`; verified with `./gradlew -q compileKotlin` in both modules.
- [x] 8.3 Add `EmbeddedEventBus` — an in-process event publisher that replaces Micronaut's `ApplicationEventPublisher` for embedded mode | Finished: 2026-03-22T13:37:10Z | Notes: added `EmbeddedEventBus<T>` in `architect-core/core` with subscribe/unsubscribe support and synchronous in-process dispatch; verified with `./gradlew -q compileKotlin`.
- [x] 8.4 Add `EmbeddedExecutionContext` — wires core services without a Micronaut container | Finished: 2026-03-22T13:38:53Z | Notes: added `EmbeddedExecutionContext.create(...)` in `architect-core/core` to wire `ProjectService`, `TaskExecutor`, `HistoryService`, plugin loader/downloader/resolver, source registry, and default core plugins with `EmbeddedEventBus` and `RemoteContentFetcher`; verified via `./gradlew -q compileKotlin`.
- [x] 8.5 Add `--embedded` flag to `ArchitectLauncher` | Finished: 2026-03-22T13:39:49Z | Notes: added `--embedded` Picocli option to `ArchitectLauncher` with default `false`; verified with `./gradlew -q compileKotlin` in `architect-cli/cli`.
- [x] 8.6 When `--embedded` is set (or engine is not reachable and `--no-daemon` is set): use `EmbeddedTaskExecutor` | Finished: 2026-03-22T13:44:15Z | Notes: added CLI `EmbeddedTaskExecutor` and `JdkRemoteContentFetcher`, integrated `ArchitectLauncher` routing to embedded mode when `--embedded` is set or when `--no-daemon` is used and engine health check fails; embedded path now supports list/plan/validate/task execution with local history recording; verified via `./gradlew -q compileKotlin` in `architect-cli/cli`.
- [x] 8.7 `EmbeddedConsoleUI` — same output contract as `ConsoleUI` but driven by in-process events | Finished: 2026-03-22T13:45:30Z | Notes: added `EmbeddedConsoleUI` delegating to `ConsoleUI` with identical completion/failure contract and wired embedded execution path to stream in-process `ArchitectEvent` objects into it; verified with `./gradlew compileKotlin --console=plain` in `architect-cli/cli`.
- [x] 8.8 Update `architect engine install` to make the engine optional for basic usage; document that embedded mode exists | Finished: 2026-03-22T13:47:27Z | Notes: updated CLI/docs guidance to present engine install as optional for basic workflows, added embedded-mode quick-start examples (`--embedded`, `--no-daemon`), and updated install command messaging in `ArchitectLauncher`; verified CLI compile via `./gradlew -q compileKotlin`.
- [x] 8.9 Write comprehensive tests for `EmbeddedExecutionContext` wiring | Finished: 2026-03-22T13:49:19Z | Notes: added `EmbeddedExecutionContextTest` covering service wiring, plugin source registry wiring, environment service availability, and embedded inline-task execution with emitted events; updated context wiring to expose `CommandExecutor` for inline tasks; verified with `./gradlew test --tests '*EmbeddedExecutionContextTest'` in `architect-core/core`.
- [x] 8.10 Write end-to-end embedded-mode integration test: load a real plugin, run a task, assert result | Finished: 2026-03-22T13:50:57Z | Notes: added `EmbeddedModeIntegrationTest` that creates a real project config, verifies real `inline-tasks` plugin loading via `ProjectService`, executes an inline task through `TaskExecutor` in embedded mode, and asserts successful completion; verified with `./gradlew test --tests '*Embedded*Test'` in `architect-core/core`.

### Acceptance Criteria

- `architect --embedded build` runs with no engine process
- Plugin loading, inline tasks, parallel execution, and history all work in embedded mode
- `architect --embedded plan <task>` outputs the plan without running (plan mode in embedded)
- First-run experience: `architect build` works with zero setup (embedded fallback if engine not installed)

---

## Phase 9: Enhanced Config Validation & Schema

**Goal:** `architect.yml` is self-documenting. Editors show completions and inline errors. Invalid configs produce precise, line-numbered diagnostics.

### Tasks

- [x] 9.1 Generate **JSON Schema** for `architect.yml` from the Kotlin domain model (use `jackson-module-jsonSchema` or a custom generator). Publish schema to `https://architect.dev/schema/architect.yml.json` | Finished: 2026-03-22T13:58:51Z | Notes: Created `ArchitectSchemaGenerator` in architect-core with programmatic draft-07 JSON Schema generation covering project, plugins, tasks, all workflow phases; generated `docs/schema/architect.yml.json`; wrote 8 unit tests (all passing).
- [x] 9.2 Add `$schema` field support: if `architect.yml` contains `$schema:`, validate against declared schema version | Finished: 2026-03-22T14:00:49Z | Notes: Added `$schema` to `BASE_KNOWN_KEYS`; when present, `ConfigValidator` validates the config against the built-in JSON Schema using `networknt/json-schema-validator`; added dependency to `architect-core/core/build.gradle.kts`.
- [x] 9.3 Extend `ConfigValidator` to validate plugin configuration sections against each loaded plugin's declared schema. Each `ArchitectPlugin` gains an optional `configSchema(): JsonNode?` method (default: null = no validation) | Finished: 2026-03-22T14:03:50Z | Notes: Added `configSchema(): Map<String, Any>? = null` to `ArchitectPlugin` interface; `ConfigValidator.validate()` now accepts `plugins` list and validates each plugin's config section against its declared schema using networknt json-schema-validator; added `includeBuild` for API in architect-core settings.
- [x] 9.4 Produce diagnostics with **YAML line numbers** — use SnakeYAML marks for precise location | Finished: 2026-03-22T14:06:32Z | Notes: Created `YamlLineTracker` using SnakeYAML `compose()` API to map key paths to 1-based line numbers; updated `ConfigValidator.validate()` to accept `lineMap` parameter; errors and warnings now include `line N:` prefix; `ConfigLoader.loadWithRaw()` returns raw YAML alongside parsed config; `ProjectService` wires line tracking into validation.
- [x] 9.5 Produce actionable error messages: not just "missing field" but "Add `project.name: your-project` to fix this" | Finished: 2026-03-22T14:08:04Z | Notes: Updated `ConfigValidator` to produce actionable hints: missing `project.name` suggests exact YAML to add; unknown keys suggest nearest known keys; plugin validation errors reference the section name to check.
- [x] 9.6 **VS Code extension** `architect-vscode`:
  - YAML language server integration for `architect.yml` auto-complete and inline error highlighting
  - Task panel showing all registered tasks with run/plan buttons
  - Output panel showing live execution events
  | Finished: 2026-03-22T14:10:32Z | Notes: Scaffolded `architect-vscode/` extension with `package.json` (yamlValidation, commands, views, configuration), `extension.ts` (activation, command registration, process spawning), `taskTreeProvider.ts` (TreeDataProvider parsing inline tasks from architect.yml), README, tsconfig; depends on redhat.vscode-yaml for YAML language server.
- [x] 9.7 **IntelliJ plugin** `architect-intellij`:
  - JSON Schema association for `architect.yml`
  - Run configurations for tasks
  - Gutter icons to run tasks from `architect.yml`
  | Finished: 2026-03-22T14:13:00Z | Notes: Scaffolded `architect-intellij/` with Gradle IntelliJ Platform plugin, `plugin.xml` registering `JsonSchemaProviderFactory`, `ConfigurationType` run config, `RunLineMarkerContributor` for gutter icons on task definitions; bundled JSON Schema in resources.
- [x] 9.8 Write `ConfigValidatorTest` — covers all error and warning cases, including per-plugin validation (18 tests)
- [x] 9.9 Update docs with the JSON Schema URL and IDE setup instructions

### Acceptance Criteria

- Opening `architect.yml` in VS Code shows completions and inline validation
- `architect validate` reports line numbers in all error messages
- Invalid plugin config (wrong types, missing required fields) is caught with `architect validate`

---

## Phase 10: Rich CLI Output & Developer UX

**Goal:** Every interaction feels polished. Real-time progress, timing, summaries, colors, and structured output.

### Tasks

- [x] 10.1 **Task execution progress tree** — during execution, render a live updating tree showing tasks with status icons and elapsed time (like Gradle's task list or Cargo's build output)
- [x] 10.2 **Batch grouping in output** — group events by parallel batch with a header "Batch 1 — running 3 tasks in parallel"
- [x] 10.3 **Execution summary** — at the end, print a table: task name, status, duration, output (truncated). Highlight failures.
- [x] 10.4 **Timing** — each task shows elapsed time. Total execution time shown at the end.
- [x] 10.5 **Failure details** — on failure, print the full task output (not just the error message). Make it easy to debug.
- [x] 10.6 **`architect tasks`** — list all available tasks in a formatted table: id, phase, description. Support `--json` flag for machine-readable output.
- [x] 10.7 **`architect tasks --filter <phase>`** — filter by phase or workflow (e.g., `architect tasks --filter BUILD`)
- [x] 10.8 **`architect info`** — print resolved project config: name, path, loaded plugins, registered tasks, subprojects
- [x] 10.9 **`--json` output flag** — all commands support `--json` for scripting. Replaces `--plain` for structured output.
- [x] 10.10 **Color themes** — respect `NO_COLOR`, `TERM`, and a `--no-color` flag. Auto-detect CI to disable colors.
- [x] 10.11 **`architect --version`** — print CLI, engine, and API versions
- [x] 10.12 Write tests for all new `ConsoleUI` rendering paths (17 tests)

### Acceptance Criteria

- Execution output is informative without being noisy
- `architect tasks` immediately communicates what a project can do
- All output modes (rich, plain, JSON) are tested

---

## Phase 11: Watch Mode

**Goal:** `architect watch <task>` re-executes a task automatically when relevant files change.

### Tasks

- [x] 11.1 Add `WatchService` to engine (or `architect-core`) using Java `WatchService` API. Recursive directory watch with configurable root. | Finished: 2026-03-22T15:00:00Z
- [x] 11.2 Add `watch` configuration section to `architect.yml` (per task or global): `WatchConfig` with `fromMap()` parser and `resolve()` chain. | Finished: 2026-03-22T15:00:00Z
- [x] 11.3 Default watch paths: project root with extension filter derived from loaded plugins (e.g., Gradle plugin watches `**/*.kt`, js plugin watches `**/*.ts,**/*.js`). `WatchConfig.DEFAULT_PATTERNS` map. | Finished: 2026-03-22T15:00:00Z
- [x] 11.4 Add `architect watch <task>` CLI command. Streams events continuously. Clears and re-renders on each re-run. | Finished: 2026-03-22T15:00:00Z
- [x] 11.5 Add `--watch` / `-w` flag to any task command as shorthand: `architect build --watch` | Finished: 2026-03-22T15:00:00Z
- [x] 11.6 On file change: debounce, cancel in-flight execution if still running, restart | Finished: 2026-03-22T15:00:00Z
- [x] 11.7 Ctrl+C exits watch mode cleanly via shutdown hook | Finished: 2026-03-22T15:00:00Z
- [x] 11.8 Write integration test for watch debounce logic — 10 tests: file detection, debounce, glob filtering, stop lifecycle, hidden dir skip, WatchConfig parsing | Finished: 2026-03-22T15:00:00Z

### Acceptance Criteria

- `architect watch test` re-runs tests on source file changes
- Debounce prevents multiple rapid re-runs from file saves
- Watch mode works in both daemon and embedded mode

---

## Phase 12: Environment Profiles

**Goal:** Different config for dev, staging, production. CI-aware defaults.

### Tasks

- [x] 12.1 Add `profiles` section to `architect.yml` — `ProfileMerger.merge()` supports deep-merge of profile over root config | Finished: 2026-03-22T15:30:00Z
- [x] 12.2 Deep-merge profile config on top of root config at project load time — integrated into `ProjectService.loadProject()` | Finished: 2026-03-22T15:30:00Z
- [x] 12.3 Add `--env <profile>` flag to `ArchitectLauncher`. Default: `default` | Finished: 2026-03-22T15:30:00Z
- [x] 12.4 Auto-detect CI environment: `ProfileMerger.detectProfile()` checks CI, GITHUB_ACTIONS, GITLAB_CI, JENKINS_URL, CIRCLECI, BUILDKITE env vars. Apply `ci` profile if defined. | Finished: 2026-03-22T15:30:00Z
- [x] 12.5 `Environment.profile(): String` — added to interface with default "default", implemented in ApplicationEnvironment | Finished: 2026-03-22T15:30:00Z
- [x] 12.6 `requires-confirmation` task attribute — added `requiresConfirmation(): Boolean` to Task interface with default false | Finished: 2026-03-22T15:30:00Z
- [x] 12.7 Write `ProfileMergerTest` — 14 tests: deep merge (disjoint, scalar override, recursive, non-map replace), merge with profiles (null, default, matching, nonexistent, no-section, base preservation), detectProfile (explicit, null, blank), production scenario | Finished: 2026-03-22T15:30:00Z

### Acceptance Criteria

- `architect --env staging deploy` runs with staging config
- In CI, `CI=true` env var activates the `ci` profile automatically
- `requires-confirmation: true` tasks pause in local, auto-skip in CI

---

## Phase 13: Plugin Registry Protocol

**Goal:** Plugins are not tied to GitHub. Any team can host a private registry.

### Tasks

- [x] 13.1 Define `PluginRegistryProtocol` — a standard `registry.json` format hosted at any HTTP URL:
  ```json
  {
    "plugins": [
      { "id": "my-plugin", "version": "1.0.0", "asset": "https://example.com/my-plugin-1.0.0.jar" }
    ]
  }
  ```
- [x] 13.2 Add `type: registry` plugin source in `architect.yml`:
  ```yaml
  plugins:
    - name: my-plugin
      type: registry
      registry: https://plugins.example.com/registry.json
      version: "^1.0.0"
  ```
- [x] 13.3 Implement `RegistryPluginSource` — fetches `registry.json`, resolves semver constraint, downloads JAR
- [x] 13.4 Add `type: http` for direct JAR URL resolution (no registry):
  ```yaml
  plugins:
    - name: my-plugin
      type: http
      url: https://example.com/my-plugin-1.0.0.jar
  ```
- [x] 13.5 Semver constraint resolution (`^1.0.0`, `~1.2.0`, `>=1.0.0 <2.0.0`) using a pure Kotlin semver library
- [x] 13.6 **Plugin integrity verification** — add optional `sha256` field to plugin declaration; fail if downloaded JAR hash does not match
- [x] 13.7 **Default public registry** at `https://registry.architect.dev/` (to be hosted). Local stub for tests.
- [x] 13.8 `architect plugin search <query>` — searches the public registry
- [x] 13.9 `architect plugin install <plugin-id>` — adds plugin to `architect.yml`
- [x] 13.10 Write `RegistryPluginSourceTest`

### Acceptance Criteria

- A private team registry works with `type: registry` and a custom URL
- Semver constraints resolve correctly
- SHA256 verification rejects tampered JARs

---

## Phase 14: Plugin Classloader Isolation

**Goal:** Two plugins with conflicting library dependencies do not break each other.

### Tasks

- [x] 14.1 Create `IsolatedPluginClassLoader` — child-first classloader. Each plugin gets its own isolated instance with no JAR sharing.
- [x] 14.2 Define `shared-api` classloader: only `architect-api` classes are shared via the parent (bridge classloader). All other classes are isolated.
- [x] 14.3 Handle cross-plugin type compatibility via API interfaces (not concrete classes).
- [x] 14.4 Add `classloader.debug: true` config flag that logs classloader resolution decisions.
- [x] 14.5 Write `ClassloaderIsolationTest` — two plugins declaring conflicting versions of a library both function correctly.

### Acceptance Criteria

- Plugins with different Jackson/Kotlin/Guava versions coexist without `ClassCastException` or `NoSuchMethodError`
- Engine startup time does not regress by more than 200ms due to additional classloaders

---

## Phase 15: Language-Agnostic Plugin Protocol

**Goal:** Plugins can be written in any language (TypeScript, Go, Python, Rust). Not JVM-only.

### Architecture

Introduce a subprocess-based plugin protocol. The engine launches a plugin process, communicates via stdin/stdout JSON-RPC, and the process implements the plugin interface in any language.

```
Engine  ←→  JSON-RPC over stdin/stdout  ←→  Plugin Process (Go, Python, TS, etc.)
```

### Tasks

- [x] 15.1 Define **Architect Plugin Protocol v1** (APP v1): a JSON-RPC 2.0 based protocol over stdin/stdout with methods: | Finished: 2026-03-22T15:31:00Z | Notes: defined versioned APP v1 request and event contracts in `PluginProtocol.kt`, added standalone protocol reference in `docs/plugin-protocol.md`, linked it from root docs, and verified with `PluginProtocolTest`.
  - `init(config: JsonObject)` → `{ ok: true }`
  - `listTasks()` → `Array<TaskDescriptor>`
  - `executeTask(id, args, env)` → streaming events via newline-delimited JSON
- [x] 15.2 Add `type: process` to plugin declaration: | Finished: 2026-03-22T15:32:00Z | Notes: added `command` to plugin declaration models, updated generated schema and IDE schema bundles to require `command` for `type: process`, documented the YAML shape, and verified with `ArchitectSchemaGeneratorTest` and `ConfigValidatorTest`.
  ```yaml
  plugins:
    - name: my-go-plugin
      type: process
      command: "./my-go-plugin"
  ```
- [x] 15.3 Implement `ProcessPluginAdapter` in engine — launches the process, speaks APP v1, bridges to `ArchitectPlugin<Any>` | Finished: 2026-03-22T15:33:00Z | Notes: wired `ProjectPluginLoader` to instantiate process plugins from the declared command, added shell-command support plus JSON-RPC execute ack handling and stderr draining in `ProcessPluginAdapter`, and verified end-to-end behavior with a fresh `cleanTest test --tests '*ProcessPluginAdapterTest'` run.
- [x] 15.4 Publish **TypeScript SDK** (`@architect-platform/plugin-sdk`) with full APP v1 implementation. npm-installable. | Finished: 2026-03-22T15:34:00Z | Notes: added `sdk/typescript/plugin-sdk` with typed APP v1 protocol contracts, `PluginServer`/`runPlugin` runtime, package metadata for `@architect-platform/plugin-sdk`, README usage docs, and Node-based package tests verified via `npm test`.
- [x] 15.5 Publish **Go SDK** (`github.com/architect-platform/plugin-sdk-go`) implementing APP v1. | Finished: 2026-03-22T15:35:00Z | Notes: added `sdk/go/plugin-sdk-go` with APP v1 protocol types, a JSON-RPC stdin/stdout server, optional init support, README usage docs, and verified the package with `gofmt -w *.go && go test ./...`.
- [x] 15.6 Publish **Python SDK** (`architect-plugin-sdk` on PyPI) implementing APP v1. | Finished: 2026-03-22T15:36:00Z | Notes: added `sdk/python/architect-plugin-sdk` as a standard `src`-layout package with APP v1 constants, protocol models, `PluginServer`/`run_plugin`, README usage docs, and verified the package with `PYTHONPATH=src ... python -m unittest discover -s tests`.
- [x] 15.7 Implement `type: npm` shorthand — downloads and runs an npm package as a plugin: | Finished: 2026-03-22T16:10:43Z | Notes: added npm shorthand support in `ProjectPluginLoader` via `npx --yes <package>@<version>`, added `package` field mapping in plugin config, extended schema generation and bundled schema files to include `type: npm` and required `package`, added/updated validator+schema tests, and documented npm plugin declaration in protocol docs.
  ```yaml
  plugins:
    - name: my-ts-plugin
      type: npm
      package: "@my-org/architect-plugin"
      version: "^1.0.0"
  ```
- [x] 15.8 Example plugins in each language demonstrating all protocol features | Finished: 2026-03-22T16:13:15Z | Notes: added full-featured APP v1 examples for TypeScript (`sdk/typescript/plugin-sdk/examples/full-featured-plugin.ts`), Go (`sdk/go/plugin-sdk-go/examples/full-featured/main.go`), and Python (`sdk/python/architect-plugin-sdk/examples/full_featured_plugin.py`) covering init metadata, task descriptors with phase/dependencies/requires-confirmation, output/progress/error events, and success/failure execution paths; documented examples in each SDK README and verified SDK tests (`npm test`, `go test ./...`, `python3 -m unittest discover -s tests`).
- [x] 15.9 Write `ProcessPluginAdapterTest` with a mock subprocess | Finished: 2026-03-22T16:30:55Z | Notes: verified dedicated `ProcessPluginAdapterTest` includes a mock subprocess script that exercises APP v1 request/response flow (`init`, `listTasks`, `executeTask`, `shutdown`) and success/failure event handling; re-ran focused suite with `./gradlew -q test --tests '*ProcessPluginAdapterTest'`.

### Acceptance Criteria

- Writing a plugin in TypeScript requires only `npm install @architect-platform/plugin-sdk`
- Go plugin runs as a compiled binary; no JVM required on the host
- APP v1 protocol is documented and versioned

---

## Phase 16: Affected Task Detection

**Goal:** In a monorepo, only run tasks for modules that have changed since the last successful run.

### Tasks

- [x] 16.1 Build **project dependency graph** from `architect.yml` `subprojects` declarations and inferred relationships (plugin shared config, shared `build.gradle.kts`, etc.) | Finished: 2026-03-22T16:33:37Z | Notes: added `ProjectDependencyGraph` model and `ProjectDependencyGraphBuilder` in `architect-core` with support for declared `subprojects` entries (`name`/`path` and `dependsOn`/`dependencies`) plus inferred parent-child and shared-build-file relationships; exposed graph construction through `ProjectService.buildDependencyGraph(...)`; added `ProjectDependencyGraphBuilderTest` (4 tests) and verified with `./gradlew -q test --tests '*ProjectDependencyGraphBuilderTest'` and `./gradlew -q compileKotlin`.
- [x] 16.2 **`git diff` integration** — compare against a base ref (default: `HEAD~1`, configurable to `origin/main`):
  - Map changed files to source roots
  - Walk the dependency graph to find all transitively affected projects
  | Finished: 2026-03-22T17:00:00Z | Notes: added `AffectedProjectResolver` in architect-core with git diff integration, file-to-project mapping via longest-prefix matching, transitive dependent expansion via BFS, `AffectedConfig` with always-include/never-include support, and `parseConfig()` for YAML parsing; added `transitiveDependentsOf()` to `ProjectDependencyGraph`; verified with `./gradlew -q compileKotlin`.
- [x] 16.3 Add `architect build --affected` — executes tasks only for affected projects | Finished: 2026-03-22T17:10:00Z | Notes: added `--affected` flag to `ArchitectLauncher` with affected project resolution in both embedded and engine execution paths; prints affected project list before execution; skips execution when no projects are affected.
- [x] 16.4 Add `architect build --affected --base origin/main` — compare against base branch (ideal for PR workflows) | Finished: 2026-03-22T17:10:00Z | Notes: added `--base` flag to `ArchitectLauncher` (default: `HEAD~1`); passed to `AffectedProjectResolver.resolve()` for configurable base ref comparison.
- [x] 16.5 Add `affected` configuration to `architect.yml`: | Finished: 2026-03-22T17:15:00Z | Notes: added `affected` section (`always-include`, `never-include`) to project schema in `ArchitectSchemaGenerator`, updated bundled JSON schemas in `docs/schema/` and `architect-intellij/`; `AffectedProjectResolver.parseConfig()` parses from config map; all schema tests pass.
  ```yaml
  project:
    affected:
      always-include: ["shared-lib"]   # always run these regardless
      never-include: ["docs"]          # never run these in affected mode
  ```
- [x] 16.6 `architect affected` — prints the list of affected projects without running anything | Finished: 2026-03-22T17:20:00Z | Notes: added `architect affected` command to `ArchitectLauncher` with `printAffected()` rendering (formatted table and JSON output support via `--json`); uses `resolveAffectedProjects()` with `--base` flag support; verified with `./gradlew -q compileKotlin`.
- [x] 16.7 **Cache invalidation integration**: cacheValidator hook applied in AffectedProjectResolver.resolve(), wired in CLI with LocalOutputCache to filter out projects with valid cached outputs when --no-cache is not set
- [x] 16.8 Write `AffectedProjectResolverTest` — covers: no changes, root-only change, transitive dependency chain, always-include, never-include | Finished: 2026-03-22T18:00:00Z | Notes: 16 tests covering: no changes, git failure, root-only changes (with/without dependents), direct child changes, transitive dependency chain, diamond dependency, always-include (valid and nonexistent), never-include, never-include overriding always-include, parseConfig (null/missing/valid), longest-prefix file mapping, root fallback, cacheValidator identity.

### Acceptance Criteria

- `architect test --affected` in a 50-project monorepo only tests changed modules
- `--base origin/main` works in PR CI pipeline

---

## Phase 17: Task Output Caching

**Goal:** Skip tasks whose inputs have not changed. Gradle-style incremental computation, but protocol-agnostic.

### Architecture

Each task declares `inputs` (files, config values, env vars). The engine hashes all inputs to produce a `cacheKey`. If a matching output exists locally (or in a remote cache), the task is skipped.

### Tasks

- [x] 17.1 Add `TaskCacheDescriptor` to `architect-api`: tasks optionally return `CacheDescriptor(inputs: List<CacheInput>, outputs: List<CacheOutput>)` | Finished: 2026-03-22T18:05:00Z | Notes: Created `CacheDescriptor`, `CacheInput` (sealed: FileSet, ConfigValue, EnvVar, CommandOutput), `CacheOutput` (sealed: FileSet, Stdout) in `api/core/tasks/cache/`; added `cacheDescriptor(): CacheDescriptor? = null` to `Task` interface.
- [x] 17.2 `CacheInput` types: `FileSet(glob)`, `ConfigValue(key)`, `EnvVar(name)`, `CommandOutput(cmd)` — each produces a deterministic hash | Finished: 2026-03-22T18:05:00Z | Notes: Implemented as sealed class hierarchy in CacheDescriptor.kt; CacheOutput types also defined (FileSet, Stdout).
- [x] 17.3 `LocalOutputCache` — stores serialized task states in `~/.architect/cache/{cacheKey}/`. Stores stdout, exit code, output files. | Finished: 2026-03-22T18:10:00Z | Notes: Created `LocalOutputCache` with file-system storage (result.json + stdout.txt per cache key), get/store/contains/clear/sizeBytes/entryCount API; created `CacheKeyComputer` with SHA-256 hashing over FileSet (glob walk), ConfigValue, EnvVar, CommandOutput inputs.
- [x] 17.4 `TaskExecutor` cache integration: compute key → check cache → skip if hit → execute and store on miss | Finished: 2026-03-22T18:15:00Z | Notes: Added `outputCache` and `outputCacheEnabled` params to TaskExecutor; integrated CacheKeyComputer + LocalOutputCache into executeSingleTask: checks output cache before execution using cacheDescriptor(), stores on successful miss; fixed pre-existing EmbeddedExecutionContextTest assertion for expanded plugin source types.
- [x] 17.5 `RemoteOutputCache` interface — `storeResult(key, result)`, `fetchResult(key): Result?`. Writable provider: HTTP cache server. | Finished: 2026-03-22T18:20:00Z | Notes: Defined `RemoteOutputCache` interface with `fetchResult`/`storeResult` methods and `CachedTaskResult` transport type; integrated into TaskExecutor with local→remote fallback on cache miss and remote push on store.
- [x] 17.6 Implement HTTP remote cache backend (simple REST API: `GET /cache/{key}`, `PUT /cache/{key}`). Can be self-hosted or use architect-cloud. | Finished: 2026-03-22T18:25:00Z | Notes: Created `HttpRemoteOutputCache` implementing `RemoteOutputCache` using `RemoteContentFetcher`; uses simple text-based protocol (success flag + message + stdout separated by `---`).
- [x] 17.7 `architect cache clear` — wipes local cache. `architect cache info` — shows cache size, hit rate from last session. | Finished: 2026-03-22T18:30:00Z | Notes: Added `handleCacheCommand()` to `ArchitectLauncher` with `clear` and `info` subcommands; info shows entry count and human-readable size; supports `--json` output.
- [x] 17.8 `--no-cache` flag to bypass cache for a run | Finished: 2026-03-22T18:35:00Z | Notes: Added `--no-cache` CLI flag in ArchitectLauncher; wired through EmbeddedTaskExecutor → EmbeddedExecutionContext → TaskExecutor `outputCacheEnabled` parameter; added `outputCacheEnabled` parameter to EmbeddedExecutionContext.create().
- [x] 17.9 Write `TaskOutputCacheTest` — 15 tests: LocalOutputCache (miss/hit/convert/clear/info), CacheKeyComputer (deterministic/diff/fileset/envvar/outputs), invalidation, RemoteOutputCache (hit/miss/convert) with InMemoryRemoteCache

### Acceptance Criteria

- Second `architect test` on unchanged project completes in <100ms (all cache hits)
- Cache keys are deterministic: same inputs always produce same key across machines
- Remote cache works across CI build agents

---

## Phase 18: Extended Official Plugin Library

**Goal:** Cover the most common developer toolchains with first-class plugins.

### New Plugins

- [x] 18.1 `docker-architected` — 6 tasks: docker-build/push/run/compose-up/compose-down/compose-logs. Context: image, registry, platforms, dockerfile, buildArgs, composeFile. Tests: DockerContextTest + DockerPluginTest.
- [x] 18.2 `kubernetes-architected` — 4 tasks: k8s-apply/rollout/status/port-forward. Context: namespace, context, manifests. Tests pass.
- [x] 18.3 `terraform-architected` — 4 tasks: tf-init/plan/apply/destroy. Context: workspace, backend, vars, varFile, autoApprove. Tests pass.
- [x] 18.4 `python-architected` — 5 tasks: py-install/lint/test/build/publish. Context: tool (uv|pip|poetry), pythonVersion, testRunner, linter. Tests pass.
- [x] 18.5 `go-architected` — 4 tasks: go-build/test/lint/release. Context: module, ldflags, outputBinary. Tests pass.
- [x] 18.6 `rust-architected` — 4 tasks: cargo-build/test/lint/publish. Context: profile, features, target. Tests pass.
- [x] 18.7 `maven-architected` — 3 tasks: mvn-verify/package/deploy. Context: profiles, settings, skipTests. Tests pass.
- [x] 18.8 `nx-architected` — integrate with Nx monorepo: expose Nx targets as Architect tasks. Bridge affectedness detection. | Finished: 2026-03-22T19:03:56Z | Notes: completed the Nx plugin module scaffold, wired CLI `--affected` runs to pass Architect-computed affected projects into `nx-*` tasks, taught `NxTask` to translate those bridge args into `nx run-many` or `nx affected` commands, and verified with focused Nx plugin and CLI tests.
- [x] 18.9 **Fix `scripts-architected`** — resolve all three workflows (`CoreWorkflow`, `CodeWorkflow`, `HooksWorkflow`). Add `environment`, `workingDirectory` per-task. Add `sequential: true` flag to disable parallelism for a specific script group. | Finished: 2026-03-22T19:08:03Z | Notes: verified per-task `environment` and `workingDirectory` support with execution tests, implemented `sequential: true` as same-phase dependency chaining to prevent parallel execution without cross-phase cycles, and added workflow-resolution coverage for core and hook phases plus script execution tests.

### Acceptance Criteria

- Each plugin has: typed context, full task coverage of its toolchain, unit tests for config parsing, and integration tests with a `--dry-run` path.

---

## Phase 19: Plugin Authoring Toolkit

**Goal:** Creating a new plugin takes 5 minutes, not 5 days.

### Tasks

- [x] 19.1 `architect plugin create <name>` — scaffolds a new plugin in the current directory with: | Finished: 2026-03-22T19:13:41Z | Notes: added `architect plugin create` to the CLI with Kotlin, TypeScript, and Go templates generated by a dedicated `PluginScaffolder`; each scaffold includes `plugin.yml`, README, and a template-specific test harness; verified with focused CLI tests covering all three templates.
  - Kotlin + Gradle template (for JVM plugins)
  - TypeScript template (for process plugins via npm SDK)
  - Go template (for process plugins via Go SDK)
  - `plugin.yml` manifest, test harness, README template
- [x] 19.2 **Plugin test harness** — `ArchitectPluginTestKit` in `architect-api`: | Finished: 2026-03-22T19:16:18Z | Notes: added `ArchitectPluginTestKit` to `architect-api` with in-memory task registration, service injection, published-event capture, project-config support, and reflective `configure(mapOf(...))` context hydration via `kotlin-reflect`; verified with focused API tests covering config mapping, task execution, service access, and event capture.
  ```kotlin
  val kit = ArchitectPluginTestKit(MyPlugin())
  kit.configure(mapOf("setting" to "value"))
  val result = kit.executeTask("my-task")
  assertThat(result).isSuccess()
  ```
- [x] 19.3 **Local plugin dev loop** — `type: local` plugin source reloads the plugin JAR on every execution (no engine restart). Add `architect engine reload-plugins` endpoint. | Finished: 2026-03-22T19:20:46Z | Notes: engine project registration now reloads cached projects when they declare `type: local` plugins, added `POST /api/projects/{projectName}/reload-plugins`, and exposed it in the CLI as `architect engine reload-plugins`; verified with focused CLI and engine tests.
- [x] 19.4 **Plugin documentation generator** — `architect plugin docs <path>` — reads plugin metadata and generates a Markdown reference doc | Finished: 2026-03-22T19:40:13Z | Notes: added CLI `architect plugin docs <path>` with `PluginDocumentationGenerator` that reads `plugin.yml`, emits `PLUGIN_REFERENCE.md`, infers scaffolded task metadata for Kotlin, TypeScript, and Go templates, and verified with focused CLI tests.
- [x] 19.5 **Plugin validation** — `architect plugin validate <path>` — validates a plugin JAR: checks SPI file, verifies `ArchitectPlugin` implementation, tests config deserialization | Finished: 2026-03-22T19:45:54Z | Notes: added CLI `architect plugin validate <path>` with `PluginJarValidator` that inspects the JAR SPI descriptor, loads implementations through the isolated classloader and SPI loader, verifies plugin metadata, and checks config initialization via `ArchitectPluginTestKit`; verified with focused validator and CLI tests.
- [x] 19.6 Write plugin authoring guide (see Phase 25) | Finished: 2026-03-22T19:47:33Z | Notes: added a root plugin authoring guide at `docs/guides/authoring-plugins.md` covering JVM and process plugin workflows, testing with `ArchitectPluginTestKit`, local reload flow, and the new docs and validation commands; linked it from the root docs index.

### Acceptance Criteria

- A developer with no prior Architect knowledge can create, test, and publish a plugin in under 30 minutes
- `ArchitectPluginTestKit` allows testing plugins without a running engine

---

## Phase 20: Project Graph & Visualization

**Goal:** Understand the full task dependency graph at a glance.

### Tasks

- [x] 20.1 `architect graph` — outputs a DOT format directed graph of the task DAG for the current project | Finished: 2026-03-22T19:51:03Z | Notes: added CLI `architect graph` DOT output for the full current-project task DAG by aggregating planned task dependencies and rendering them through `TaskGraphDotRenderer`; verified with focused CLI launcher tests.
- [x] 20.2 `architect graph --open` — renders the graph as an SVG or HTML page and opens in browser (uses D3.js or Mermaid) | Finished: 2026-03-22T19:53:03Z | Notes: extended `architect graph` with `--open` to generate a temporary Mermaid-based HTML page through `TaskGraphHtmlRenderer`, print the file location, and open it through the desktop browser when supported; verified with focused renderer and launcher tests.
- [x] 20.3 `architect graph <task>` — subgraph for a specific task and its dependencies | Finished: 2026-03-22T19:54:44Z | Notes: extended graph argument parsing so `architect graph <task>` renders only that task's dependency subgraph for both DOT and `--open` HTML output; verified with focused launcher tests using task-specific graph fixtures.
- [x] 20.4 `architect graph --projects` — shows the monorepo project graph (project ↔ project dependency relationships) | Finished: 2026-03-22T19:58:00Z | Notes: added `--projects` graph mode backed by the existing embedded `ProjectDependencyGraph` builder, with DOT and Mermaid HTML renderers plus focused renderer and launcher test coverage.
- [x] 20.5 `architect plan <task> --tree` — ASCII tree rendering in the terminal (already partially done in plan mode; make it richer with batch groups and timing estimates) | Finished: 2026-03-22T20:05:04Z | Notes: added `--tree` flag to plan command with `TaskPlanTreeRenderer` that renders dependency hierarchy as an indented ASCII tree with batch numbers; wired into both engine and embedded modes with `parsePlanOptions()`; verified with focused renderer and launcher tests.
- [x] 20.6 Integrate graph rendering into the VS Code extension (see Phase 9.6) — a panel that renders the live task graph | Finished: 2026-03-22T20:05:04Z | Notes: added `architect.showGraph` command with `GraphPanel` webview that runs `architect graph`, converts DOT to Mermaid, and renders in a side panel; auto-refreshes on `architect.yml` changes via file watcher; accessible from command palette and task tree view toolbar.

### Acceptance Criteria

- `architect graph --open` opens a navigable HTML page with the full DAG
- Graph updates live in VS Code as `architect.yml` is edited

---

## Phase 21: Testing — Coverage to >85%

**Goal:** The platform is trustworthy. Every component has comprehensive automated tests.

### architect-api

- [x] 21.1 All existing tests verified passing | Finished: 2026-03-22T20:08:26Z | Notes: All 10 API test files pass — ConfigTest, CompositeTaskTest, TaskResultTest, ConfigurableTaskTest, SimpleTaskTest, TaskWithArgsTest, ArchitectPluginTestKitTest, HooksWorkflowTest, CodeWorkflowTest, CoreWorkflowTest.
- [x] 21.2 Add `ProjectContextTest` — getKey extension, missing key, nested key, wrong type | Finished: 2026-03-22T20:08:26Z | Notes: 8 tests covering dir/config access, missing key, nested traversal, partial path, type erasure behavior, primitive traversal error, list element access, and data class equality.
- [x] 21.3 Add `TaskRegistryTest` — register, get, all, duplicate id handling | Finished: 2026-03-22T20:10:00Z | Notes: 6 tests covering add/get, unknown id, empty all, insertion order, duplicate id rejection, and multi-task retrieval.

### architect-core / architect-engine

- [x] 21.4 `TaskExecutorTest` — parallel batch execution with mock tasks, sequential fallback, failure propagation, child task execution | Finished: 2026-03-22T20:10:00Z | Notes: 6 tests covering single task success, parallel batch with independent tasks, sequential fallback ordering, failure propagation stopping remaining batches, CompositeTask child execution, and exception handling.
- [x] 21.5 `HistoryServiceTest` — write record, read all (sorted), read by project, limit, directory creation, JSON round-trip | Finished: 2026-03-22T20:10:00Z | Notes: 7 tests covering single record write/read, sorted retrieval (newest first), limit enforcement, project filtering, JSON field round-trip, empty results, and non-matching project.
- [x] 21.6 `ConfigValidatorTest` — required field missing, unknown key warning, plugin key validation, line numbers in errors | Finished: 2026-03-22T20:20:40Z | Notes: verified focused coverage for required and blank `project.name`, unknown top-level key warnings, plugin schema validation and context-key recognition, plus line-number diagnostics; confirmed with `./gradlew test --tests '*ConfigValidatorTest'` in `architect-core/core`.
- [x] 21.7 `TaskDependencyResolverTest` — expand existing; add: children resolution, batch assignment for diamond graph, large graph performance test | Finished: 2026-03-22T20:21:53Z | Notes: added coverage for resolving child tasks and composite child ordering, explicit parallel batch assignment on a diamond dependency graph, and a bounded 1,000-task topological-sort performance check; confirmed with `./gradlew test --tests '*TaskDependencyResolverTest'` in `architect-engine/engine`.
- [x] 21.8 `ProjectServiceTest` — loadProject round-trip with inline tasks, subproject discovery, validation exception on invalid config | Finished: 2026-03-22T21:00:41Z | Notes: added core-level ProjectService coverage for inline-task plugin round-trip loading, recursive subproject discovery, and invalid-config rejection; tightened ProjectService in both core and engine to throw `ConfigValidationException` when validation errors are present; confirmed with focused `ProjectServiceTest` runs in both `architect-core/core` and `architect-engine/engine`.
- [x] 21.9 `BashCommandExecutorTest` — expand with: timeout enforcement, non-zero exit code, environment variable injection, working directory | Finished: 2026-03-22T21:02:12Z | Notes: tightened the engine executor suite to assert timeout failure messaging, non-zero exit code plus stderr capture, environment variable propagation into a shell command, and correct working-directory execution with macOS-safe path normalization; confirmed with `./gradlew test --tests '*BashCommandExecutorTest'` in `architect-engine/engine`.
- [x] 21.10 `ExecutionApiControllerTest` — SSE stream: verify events are received, stream terminates on COMPLETED, stream terminates on FAILED | Finished: 2026-03-22T21:03:49Z | Notes: added a focused controller unit suite with mocked `TaskService` flows to verify SSE event delivery and clean stream termination after root-level `COMPLETED` and `FAILED` execution events while still emitting the terminal event itself; confirmed with `./gradlew test --tests '*ExecutionApiControllerTest'` in `architect-engine/engine`.
- [x] 21.11 `TaskCacheTest` — expand: concurrent reads, cache invalidation, TTL expiry | Finished: 2026-03-22T21:06:33Z | Notes: added TTL-based expiry support to the simple in-memory `TaskCache` in both core and engine, introduced cache TTL configuration constants, and expanded the engine test suite to verify concurrent reads on enabled cache entries, cache invalidation via `clear()`, and expiry after a short TTL; confirmed with `./gradlew test --tests '*TaskCacheTest'` in `architect-engine/engine` plus `./gradlew test --tests '*TaskExecutorTest'` in `architect-core/core`.

### architect-cli

- [x] 21.12 `ArchitectLauncherTest` — command routing: plan, history, validate, engine subcommands, task execution, `--plain`, `--no-daemon` | Finished: 2026-03-23T10:00:00Z | Notes: expanded to 35 tests covering history (empty, local files, project filter), plan output, validate (valid with warnings), tasks (list, --filter, --json), info (rich and --json), --version (plain and --json), --plain, cache (info, info --json, clear), engine (no subcommand, unknown subcommand), plugin create, --no-daemon fallback, embedded mode routing.
- [x] 21.13 `EngineHealthCheckerTest` — HTTP 200, HTTP 500, connection refused, timeout | Finished: 2026-03-23T10:05:00Z | Notes: added HTTP 500 test; 4 tests total covering 200, 500, connection refused, read timeout.
- [x] 21.14 `ConsoleUITest` — expand existing 12 tests; add: batch grouping output, summary rendering, timing output, `--json` output | Finished: 2026-03-23T10:15:00Z | Notes: expanded to 24 tests; added batch grouping (same-batch assignment, batch boundary detection, no-advance while running), summary rendering (total duration, skipped count, per-task status+duration, empty summary no-op).

### plugins

- [x] 21.15 All plugins: add `execute()` level tests using hand-rolled test infrastructure (Phase 19.2 prerequisite)
- [x] 21.16 `GitPluginTest` — git-config secure escaping with adversarial inputs
- [x] 21.17 `ScriptsPluginTest` — phase resolution for all three workflows
- [x] 21.18 `DocsPluginTest` — path traversal prevention, all three builders

### architect-cloud

- [x] 21.19 `EngineServiceTest`, `ProjectServiceTest`, `ExecutionServiceTest` — all use case operations
- [x] 21.20 `ExecutionEventServiceTest` — event broadcasting via Reactor Sink
- [x] 21.21 `EventsWebSocketServerTest` — WebSocket event delivery
- [x] 21.22 Add persistence adapter integration tests with H2

### Coverage Gates

- [x] 21.23 Add JaCoCo to all modules with a minimum coverage gate of 80% (enforced in CI, target 85%)
- [x] 21.24 Add mutation testing via PIT to `architect-api` and `architect-core`

---

## Phase 22: Integration & End-to-End Tests

**Goal:** Confidence that all components work together, not just in isolation.

### Tasks

- [x] 22.1 **CLI ↔ Engine integration tests** — use `@MicronautTest` to spin up the real engine, execute commands via the real CLI HTTP client, assert events via SSE | Finished: 2026-03-22T21:11:09Z | Notes: added a real CLI-to-engine integration suite in `architect-engine/engine` by wiring the CLI `EngineCommandClient` into the engine test module through a composite-build test dependency and fixed test-service URL; covered successful and failing inline-task execution over HTTP plus SSE event collection; integration exposed and fixed an SSE bug where `ExecutionApiController` terminated streams on root `task.failed` before `execution.failed`.
- [x] 22.2 **Embedded mode end-to-end** — load a real plugin JAR locally, execute a real task, assert the result and history record | Finished: 2026-03-22T21:14:02Z | Notes: added a CLI integration test that builds a valid local plugin JAR from compiled test classes (including synthetic Kotlin class files), executes the plugin task through `EmbeddedTaskExecutor`, captures emitted events, and verifies the persisted history record through `LocalHistoryReader`.
- [x] 22.3 **Plugin contract tests** — a shared test suite that any `ArchitectPlugin` implementation can run to verify protocol compliance | Finished: 2026-03-23T10:57:43Z | Notes: added published `ArchitectPluginContractTestSuite` coverage in `architect-api`, exposed JUnit to plugin consumers, added an API smoke test for map-based config conversion, and adopted the shared suite in Gradle, Docker, Git, and Scripts plugin modules with focused passing contract-test runs.
- [x] 22.4 **Monorepo end-to-end** — create a temporary multi-project workspace, run a task across all subprojects, assert parallel execution and result aggregation | Finished: 2026-03-23T11:07:15Z | Notes: added `MonorepoExecutionIntegrationTest` in `architect-engine` using the real `ProjectService`, `TaskService`, inline tasks, execution-event collector, and history service; verifies two subprojects execute in parallel before the root task, successful runs persist history, and failing subprojects aggregate into a root-level `Some subprojects failed` result without executing the root task.
- [x] 22.5 **CI simulation test** — run the full pipeline (`init → lint → verify → build → test`) on the project itself using Architect | Finished: 2026-03-23T11:10:12Z | Notes: added `CiPipelineSimulationIntegrationTest` in `architect-engine` with the real `TaskService`, built-in core workflow phase tasks, and inline tasks for each core phase; verifies the `test` phase drives the full `init → lint → verify → build → test` pipeline in order, emits a successful terminal execution event, and persists a successful history record.

---

## Phase 23: Performance Optimization

**Goal:** Fast enough that developers never wait unnecessarily.

### Targets

| Metric | Target |
|--------|--------|
| CLI startup to first task event | <500ms (daemon mode) |
| CLI startup to first task event | <300ms (embedded mode) |
| Plugin loading (10 plugins) | <2s |
| Task graph resolution (100 tasks) | <50ms |
| Cache hit task skip | <10ms |

### Tasks

- [x] 23.1 **GraalVM Native Image for CLI** — compile `architect-cli` to a native binary. Eliminates JVM startup (~200ms saving). Requires Micronaut AOT compatibility. | Finished: 2026-03-23T13:38:00Z | Notes: installed GraalVM CE 17 with `native-image`, configured CLI native builds with `-J-Xmx4g` to avoid native-image OOMs, added Micronaut bean factories/fixes for `HttpClient` and embedded executor startup, and verified `architect-cli/cli:./gradlew nativeCompile` plus native `./build/native/nativeCompile/architect-cli --version` startup.
- [x] 23.2 **Lazy plugin loading** — load plugin JARs only when a plugin's tasks are actually needed (not at project registration time) | Finished: 2026-03-23T14:05:00Z | Notes: `ProjectService` in both `architect-core` and `architect-engine` now registers projects with deferred plugin loading, and `Project` wraps plugin state behind a lazy task registry so plugin download/init/register only happens on first task access or explicit validation; added focused project-service regressions plus embedded and monorepo execution verification to confirm registration stays cheap without breaking execution.
- [x] 23.3 **Parallel plugin loading** — load independent plugins concurrently (coroutine-based, already feasible) | Finished: 2026-03-23T14:18:00Z | Notes: `ProjectPluginLoader` now fans out configured plugin resolution with ordered coroutines on `Dispatchers.IO`, preserving config order in the returned plugin list while overlapping independent downloads/classloading; added `ProjectPluginLoaderTest` to prove concurrent download overlap and reran embedded execution context coverage with the real loader.
- [x] 23.4 **Project config caching** — `ProjectService` project cache is already implemented but disabled. Enable by default with file-system watcher invalidation. | Finished: 2026-03-23T14:29:00Z | Notes: wired `FileWatchService` into both core and engine `ProjectService` implementations so registered projects keep cached state until filesystem changes mark them stale; the next `getProject()` transparently reloads config and tasks, and focused watcher-driven cache invalidation tests now cover both modules.
- [x] 23.5 **Build benchmarks** — `jmh` micro-benchmarks for `TaskDependencyResolver.topologicalSort()` and `ConfigValidator` on large configs | Finished: 2026-03-23T14:41:00Z | Notes: added JMH support to `architect-core/core` with a small default harness, created `ProjectCoreBenchmarks` covering `TaskDependencyResolver.topologicalSort()` on a 100-task graph and `ConfigValidator` on a large synthetic config, and verified the suite with `./gradlew jmh`, which produced initial results of roughly `0.006 ms/op` and `0.395 ms/op` respectively.
- [x] 23.6 **Startup profiling** — instrument engine startup and identify top-3 bottlenecks | Finished: 2026-03-23T14:56:00Z | Notes: added `StartupProfileRecorder` plus Micronaut lifecycle listeners for bootstrap, server-startup, and service-ready checkpoints, wrapped cloud registration timing in the recorder, verified ranking coverage with `StartupProfileRecorderTest`, and confirmed live engine startup logs reported the top bottlenecks as `service-ready` (~2.772 ms), `server-startup` (~0.312 ms), and `micronaut-bootstrap` (~0.042 ms) during a `487 ms` startup.
- [x] 23.7 **Connection pooling** — CLI ↔ Engine HTTP keep-alive connections (already in Micronaut HTTP client; verify active) | Finished: 2026-03-23T15:05:00Z | Notes: made the CLI engine client pool explicit in `application.yml` with `micronaut.http.services.engine.pool.enabled: true` and added `EngineCommandClientConnectionPoolingTest`, which boots the declarative client against a local HTTP server and verifies two sequential `/api/projects` calls reuse the same TCP remote port, confirming HTTP/1.1 keep-alive pooling is active.

---

## Phase 24: Security Hardening

**Goal:** Architect can be trusted in regulated, enterprise, and multi-tenant environments.

### Tasks

- [x] 24.1 **Plugin signing** — plugins can be signed with a GPG key. Engine verifies signature before loading. `architect.yml`: | Finished: 2026-03-23T15:28:00Z | Notes: added `verify-signature` and `trusted-keys` support to plugin declarations, wired detached `.asc` signature downloads/sidecar lookup into both core and engine plugin loaders, and verify signatures through `gpg --verify` before classloading, matching signer fingerprints against configured trusted key IDs; covered with focused core loader/schema/verifier tests and an engine-side loader regression.
  ```yaml
  plugins:
    - name: my-plugin
      type: github
      repo: my-org/my-plugin
      verify-signature: true
      trusted-keys: ["0xABCD1234"]
  ```
- [x] 24.2 **Task permission model** — tasks declare required permissions in their descriptor: `file-system:read`, `file-system:write`, `network:outbound`, `process:exec`. Engine enforces via a Java SecurityManager replacement (process-level sandboxing). | Finished: 2026-03-23T15:45:00Z | Notes: added shared `TaskPermission` declarations in `architect-api` with permission-aware task constructors/defaults, propagated `permissions` through inline tasks plus APP v1 `TaskDescriptor`/SDKs/docs, and enforced task-scoped subprocess launches in both core and engine via `TaskPermissionScope` plus `SandboxedProcessLauncher`; focused API/core/engine regression suites now cover explicit permission metadata and `process:exec` denial paths.
- [x] 24.3 **Secrets management** — tasks access secrets via `Environment.secret("MY_SECRET")` which resolves from: environment variable, `.env` file, HashiCorp Vault, AWS Secrets Manager, GCP Secret Manager. Secrets are never logged. | Finished: 2026-03-23T16:05:00Z | Notes: added `Environment.secret(name)` to the shared API plus `ArchitectPluginTestKit.withSecret(...)`, implemented default secret resolver chains in both core and engine (`env` → project `.env` → Vault HTTP API → AWS CLI → GCP CLI), threaded project directories through task execution scope so `.env` resolution stays task-local, and added focused API/core/engine tests for secret access, resolver precedence, and engine environment delegation without logging secret values.
- [x] 24.4 **Audit logging** — every task execution is audit-logged with: timestamp, user, project, task, args, result, duration. Stored locally and optionally synced to `architect-cloud`. | Finished: 2026-03-23T16:25:00Z | Notes: enriched `ExecutionRecord`/CLI history DTOs with `user`, `args`, and `result`, updated both engine and embedded CLI execution paths to persist the fuller audit record locally, added optional cloud audit sync through `CloudReporterService.reportAuditRecord(...)` and a new `CloudClient` audit endpoint, and verified the behavior with focused engine `TaskServiceTest` plus CLI history compatibility tests.
- [ ] 24.5 **Path traversal prevention** — all user-provided paths are validated against the project root (already partially done in `SecurityUtils`; apply uniformly).
- [ ] 24.6 **Shell injection prevention** — all user-provided values passed to shell commands are escaped (already done in `GitUtils`; apply uniformly to `BashCommandExecutor.buildCommand()`).
- [ ] 24.7 **Dependency vulnerability scanning** — add `trivy` or OWASP Dependency Check to CI for all components
- [ ] 24.8 **CI security review** — replace `curl | bash` installer in CI workflows with a checksummed binary download

---

## Phase 25: Documentation

**Goal:** Any developer can go from zero to productive in 15 minutes. No tribal knowledge required.

### Structure

```
docs/
├── getting-started/
│   ├── installation.md
│   ├── first-project.md
│   ├── adding-plugins.md
│   └── inline-tasks.md
├── concepts/
│   ├── phases-and-workflows.md
│   ├── task-dag.md
│   ├── plugin-system.md
│   └── monorepo.md
├── reference/
│   ├── architect-yml.md       ← full config reference, auto-generated from schema
│   ├── cli-commands.md        ← all commands, flags, examples
│   ├── api/                   ← KDoc API reference
│   └── plugins/               ← one page per official plugin
├── guides/
│   ├── authoring-plugins.md
│   ├── ci-cd-integration.md
│   ├── migrating-from-make.md
│   ├── migrating-from-nx.md
│   └── enterprise-setup.md
└── architecture/
    ├── overview.md
    ├── engine-internals.md
    ├── plugin-protocol.md
    └── decision-log.md
```

### Tasks

- [ ] 25.1 **Getting started guide** — from `brew install architect` to running first task in <15 minutes
- [ ] 25.2 **`architect.yml` reference** — auto-generated from JSON Schema (Phase 9.1). Every field documented with type, default, and example.
- [ ] 25.3 **CLI command reference** — all commands, all flags, usage examples, exit codes
- [ ] 25.4 **Plugin authoring guide** — full guide for JVM (Kotlin/Java), TypeScript, Go, and Python plugins
- [ ] 25.5 **KDoc API reference** — deployed alongside the docs site
- [ ] 25.6 **Official plugin pages** — one page per plugin with all tasks, config options, and worked examples
- [ ] 25.7 **Architecture decision log** — document key decisions: phase model, daemon architecture, plugin protocol, embedded mode
- [ ] 25.8 **CI/CD integration guides** — GitHub Actions, GitLab CI, Jenkins, CircleCI — copy-paste examples
- [ ] 25.9 **Migration guides** — from Make, from Gradle tasks, from Nx, from GitHub Actions scripts
- [ ] 25.10 Update MkDocs site to use `mkdocs-material` with: search, versioning, dark mode, code copy, task runner examples

---

## Phase 26: Installer & Distribution

**Goal:** Installing Architect is effortless on any platform.

### Tasks

- [ ] 26.1 **Homebrew tap** — `brew tap architect-platform/tap && brew install architect`. Auto-updated on release.
- [ ] 26.2 **Native installers** — GraalVM native image for macOS (arm64, x86_64), Linux (x86_64, arm64), Windows (x86_64). Built and published via CI.
- [ ] 26.3 **apt/yum packages** — `.deb` and `.rpm` packages published to a hosted repository
- [ ] 26.4 **Windows installer** — MSI installer with PATH registration and PowerShell completion
- [ ] 26.5 **Docker image** — `ghcr.io/architect-platform/architect:latest` — contains CLI + Engine in a single image for CI usage
- [ ] 26.6 **GitHub Action** — `architect-platform/setup-architect@v1` — installs CLI in a GitHub Actions workflow with one step
- [ ] 26.7 **Shell completion** — bash, zsh, fish completions for all commands and task IDs (Picocli generates these; wire to the installers)
- [ ] 26.8 **`architect upgrade`** — checks for a new version and self-updates
- [ ] 26.9 **Verify the CI `curl | bash` installer** — replace with a checksummed install script or GitHub Action (security fix from Phase 24.8)

---

## Phase 27: Embedded Task Conditioning

**Goal:** Tasks declare what they need to run. Architect checks preconditions before executing and gives clear instructions when requirements are not met.

### Tasks

- [ ] 27.1 Add `requires` section to task descriptor:
  ```yaml
  tasks:
    docker-build:
      requires:
        tools: [docker]
        min-tool-versions: { docker: "20.0.0" }
        env: [DOCKER_REGISTRY]
        platform: [linux, darwin]
  ```
- [ ] 27.2 Plugin tasks declare requirements programmatically:
  ```kotlin
  SimpleTask(
    id = "docker-build",
    requires = TaskRequirements(
      tools = listOf("docker"),
      env = listOf("DOCKER_REGISTRY"),
      platform = setOf(Platform.LINUX, Platform.DARWIN)
    ),
    task = ::buildImage
  )
  ```
- [ ] 27.3 `TaskConditionChecker` — before execution: checks tool existence (`which <tool>`), version, env vars, platform. On failure: prints a precise "install docker" or "set DOCKER_REGISTRY" message.
- [ ] 27.4 `architect check` — runs all task precondition checks without executing any task. Reports which tasks are runnable and which are blocked.
- [ ] 27.5 Write `TaskConditionCheckerTest`

---

## Phase 28: Enterprise Features

**Goal:** Architect scales to large organizations with security, compliance, and admin requirements.

### Tasks

- [ ] 28.1 **Multi-engine coordination** — `architect-cloud` can route task requests to multiple engine instances. Enables distributed monorepo builds.
- [ ] 28.2 **RBAC** — users have roles (`developer`, `lead`, `admin`). Roles govern which tasks can be executed and which projects can be accessed. Configured in `architect-cloud`.
- [ ] 28.3 **SSO integration** — OIDC/SAML authentication for `architect-cloud`. CLI authenticates via device flow.
- [ ] 28.4 **Centralized audit log** — all execution events shipped to `architect-cloud`. Queryable with time/user/project filters. Export to SIEM.
- [ ] 28.5 **Org-level plugin policy** — admin declares approved plugins and versions. Engine enforces; unapproved plugins are rejected.
- [ ] 28.6 **Secrets management integration** (extends Phase 24.3) — centralized secrets from cloud, with rotation support.
- [ ] 28.7 **Usage analytics** — team dashboards: most-run tasks, failure rates, average durations, cache hit rates.
- [ ] 28.8 **architect-cloud REST API v2** — fully documented, versioned API for integrations. OpenAPI spec published.

---

## Phase 29: SaaS Platform Foundation

**Goal:** Lay the foundation for architect-cloud to become a real SaaS product.

### Tasks

- [ ] 29.1 **`architect-data`** — define schema with Flyway migrations: `engines`, `projects`, `executions`, `events`, `users`, `orgs`, `roles`, `plugins`, `audit_log`. Replace in-memory/H2 with PostgreSQL as primary.
- [ ] 29.2 **`architect-server`** — API gateway that: authenticates requests, routes to engines, handles multi-tenancy, enforces rate limits, serves the OpenAPI spec.
- [ ] 29.3 **`architect-x`** — advanced daemon features: file watcher (for watch mode via daemon), distributed cache coordinator, plugin pre-fetch service.
- [ ] 29.4 **architect-cloud frontend** — Vue.js dashboard: execution timeline, task graph view, plugin marketplace, org settings, audit log viewer, usage analytics.
- [ ] 29.5 **Tenant isolation** — each org has isolated execution scope. Engine instances are org-scoped.
- [ ] 29.6 **Billing integration hooks** — usage metering API for SaaS billing.

---

## Phase 30: Code Quality Pass

**Goal:** Every file is clean, idiomatic Kotlin. No dead code, no duplication, no inconsistency.

### Tasks

- [ ] 30.1 **Enable ktlint on all modules** — currently only enforced in `architect-api`. Apply to `architect-engine`, `architect-cli`, all plugins, `architect-cloud`.
- [ ] 30.2 **Detekt static analysis** — add Detekt with a baseline. Fix all high-severity findings.
- [ ] 30.3 **Dead code removal** — identify and remove unused classes, methods, and dependencies across all modules.
- [ ] 30.4 **Consistent error handling** — audit all `try/catch` and `Result` usage. Establish a project-wide `ArchitectException` hierarchy.
- [ ] 30.5 **Dependency cleanup** — audit all `build.gradle.kts` for unused dependencies, mismatched versions, `implementation` vs `api` scoping.
- [ ] 30.6 **Logging consistency** — replace ad-hoc `println` calls (currently in `ScriptsPlugin.parsePhase()` and others) with proper SLF4J logging.
- [ ] 30.7 **Coroutine scoping** — audit all `GlobalScope`, `runBlocking`, and unstructured coroutine usage. Ensure all coroutines are launched in properly scoped contexts.

---

## Phase 31: Release Engineering

**Goal:** Releasing a new version of Architect is a single command.

### Tasks

- [ ] 31.1 **Unified versioning** — all modules share a single version number, bumped once per release (currently each module is versioned independently, causing drift like the `api:1.2.0` vs `2.1.0` issue)
- [ ] 31.2 **Release automation** — tag `vX.Y.Z` on `main` triggers: build all modules, run all tests, build native binaries for all platforms, publish to GitHub Packages, Homebrew, apt/yum, Docker Hub, GitHub Releases
- [ ] 31.3 **Changelog automation** — `CHANGELOG.md` generated from Conventional Commits using `git-cliff` or semantic-release
- [ ] 31.4 **Release candidate process** — `vX.Y.Z-rc.N` tags publish to a `--pre` channel. Users opt in with `brew install architect --HEAD`.
- [ ] 31.5 **Backwards compatibility tests** — on each release, run the integration suite against projects using the previous version of the API (plugin compatibility guarantee)

---

## Roadmap Summary

| Tier | Phases | Focus | Priority |
|------|--------|-------|----------|
| 0 | Phase 6, 7 | Complete current work, fix bugs | Immediate |
| 1 | Phases 8–12 | Core DX: embedded mode, rich output, watch, profiles, schema | 4–6 weeks |
| 2 | Phases 13–17 | Ecosystem: registry, isolation, languages, affected, caching | 6–10 weeks |
| 3 | Phases 18–20 | Breadth: more plugins, authoring toolkit, graph viz | 4–6 weeks |
| 4 | Phases 21–24 | Quality: tests, integration, perf, security | 4–6 weeks |
| 5 | Phases 25–27 | Polish: docs, distribution, conditioning | 3–4 weeks |
| 6 | Phases 28–31 | Scale: enterprise, SaaS, code quality, release eng | Ongoing |

---

## Success Metrics

| Signal | Target |
|--------|--------|
| Test coverage | >85% across all modules |
| CLI startup (embedded, first task event) | <300ms |
| First-run experience | `brew install architect && architect build` works in <5min |
| Plugin languages supported | JVM (Kotlin/Java), TypeScript, Go, Python, Rust |
| Official plugins | 12+ covering major toolchains |
| Documentation pages | Full reference, 5+ guides, architecture docs |
| Platform support | macOS (arm64/x86_64), Linux (arm64/x86_64), Windows |
| Security | No critical CVEs, all shell inputs escaped, plugin signing supported |
| Monorepo teams | Affected detection, remote caching, distributed execution |
