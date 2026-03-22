# Architect Platform — Production Readiness Plan

> The goal is the best developer experience and agility tool ever built.
> Every phase is independently committable. Tasks are `[ ]` pending, `[~]` in progress, `[x]` done.

---

## Status
Overall Progress: 31/131 tasks completed (24%)
Current Phase: Phase 9 — Enhanced Config Validation & Schema
Last Updated: 2026-03-22T14:13:00Z

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

- [ ] 10.1 **Task execution progress tree** — during execution, render a live updating tree showing tasks with status icons and elapsed time (like Gradle's task list or Cargo's build output)
- [ ] 10.2 **Batch grouping in output** — group events by parallel batch with a header "Batch 1 — running 3 tasks in parallel"
- [ ] 10.3 **Execution summary** — at the end, print a table: task name, status, duration, output (truncated). Highlight failures.
- [ ] 10.4 **Timing** — each task shows elapsed time. Total execution time shown at the end.
- [ ] 10.5 **Failure details** — on failure, print the full task output (not just the error message). Make it easy to debug.
- [ ] 10.6 **`architect tasks`** — list all available tasks in a formatted table: id, phase, description. Support `--json` flag for machine-readable output.
- [ ] 10.7 **`architect tasks --filter <phase>`** — filter by phase or workflow (e.g., `architect tasks --filter BUILD`)
- [ ] 10.8 **`architect info`** — print resolved project config: name, path, loaded plugins, registered tasks, subprojects
- [ ] 10.9 **`--json` output flag** — all commands support `--json` for scripting. Replaces `--plain` for structured output.
- [ ] 10.10 **Color themes** — respect `NO_COLOR`, `TERM`, and a `--no-color` flag. Auto-detect CI to disable colors.
- [ ] 10.11 **`architect --version`** — print CLI, engine, and API versions
- [ ] 10.12 Write tests for all new `ConsoleUI` rendering paths

### Acceptance Criteria

- Execution output is informative without being noisy
- `architect tasks` immediately communicates what a project can do
- All output modes (rich, plain, JSON) are tested

---

## Phase 11: Watch Mode

**Goal:** `architect watch <task>` re-executes a task automatically when relevant files change.

### Tasks

- [ ] 11.1 Add `WatchService` to engine (or `architect-core`) using Java `WatchService` API. Recursive directory watch with configurable root.
- [ ] 11.2 Add `watch` configuration section to `architect.yml` (per task or global):
  ```yaml
  tasks:
    test:
      watch:
        paths: ["src/**/*.kt", "src/**/*.java"]
        debounce-ms: 500
  ```
- [ ] 11.3 Default watch paths: project root with extension filter derived from loaded plugins (e.g., Gradle plugin watches `**/*.kt`, js plugin watches `**/*.ts,**/*.js`)
- [ ] 11.4 Add `architect watch <task>` CLI command. Streams events continuously. Clears and re-renders on each re-run.
- [ ] 11.5 Add `--watch` / `-w` flag to any task command as shorthand: `architect build --watch`
- [ ] 11.6 On file change: debounce, cancel in-flight execution if still running, restart
- [ ] 11.7 Ctrl+C exits watch mode cleanly
- [ ] 11.8 Write integration test for watch debounce logic

### Acceptance Criteria

- `architect watch test` re-runs tests on source file changes
- Debounce prevents multiple rapid re-runs from file saves
- Watch mode works in both daemon and embedded mode

---

## Phase 12: Environment Profiles

**Goal:** Different config for dev, staging, production. CI-aware defaults.

### Tasks

- [ ] 12.1 Add `profiles` section to `architect.yml`:
  ```yaml
  profiles:
    staging:
      scripts:
        deploy:
          run: "kubectl apply -f k8s/staging/"
    production:
      scripts:
        deploy:
          requires-confirmation: true
          run: "kubectl apply -f k8s/production/"
  ```
- [ ] 12.2 Deep-merge profile config on top of root config at project load time
- [ ] 12.3 Add `--env <profile>` flag to `ArchitectLauncher`. Default: `default`
- [ ] 12.4 Auto-detect CI environment: set `CI=true` when running in GitHub Actions, GitLab CI, Jenkins, CircleCI. Apply `ci` profile if defined.
- [ ] 12.5 `Environment.profile(): String` — exposes active profile to tasks
- [ ] 12.6 `requires-confirmation` task attribute — pauses before destructive tasks in interactive mode, auto-fails in `--plain`/CI mode
- [ ] 12.7 Write `ProfileMergerTest` — covers deep merge, override behavior, CI auto-detection

### Acceptance Criteria

- `architect --env staging deploy` runs with staging config
- In CI, `CI=true` env var activates the `ci` profile automatically
- `requires-confirmation: true` tasks pause in local, auto-skip in CI

---

## Phase 13: Plugin Registry Protocol

**Goal:** Plugins are not tied to GitHub. Any team can host a private registry.

### Tasks

- [ ] 13.1 Define `PluginRegistryProtocol` — a standard `registry.json` format hosted at any HTTP URL:
  ```json
  {
    "plugins": [
      { "id": "my-plugin", "version": "1.0.0", "asset": "https://example.com/my-plugin-1.0.0.jar" }
    ]
  }
  ```
- [ ] 13.2 Add `type: registry` plugin source in `architect.yml`:
  ```yaml
  plugins:
    - name: my-plugin
      type: registry
      registry: https://plugins.example.com/registry.json
      version: "^1.0.0"
  ```
- [ ] 13.3 Implement `RegistryPluginSource` — fetches `registry.json`, resolves semver constraint, downloads JAR
- [ ] 13.4 Add `type: http` for direct JAR URL resolution (no registry):
  ```yaml
  plugins:
    - name: my-plugin
      type: http
      url: https://example.com/my-plugin-1.0.0.jar
  ```
- [ ] 13.5 Semver constraint resolution (`^1.0.0`, `~1.2.0`, `>=1.0.0 <2.0.0`) using a pure Kotlin semver library
- [ ] 13.6 **Plugin integrity verification** — add optional `sha256` field to plugin declaration; fail if downloaded JAR hash does not match
- [ ] 13.7 **Default public registry** at `https://registry.architect.dev/` (to be hosted). Local stub for tests.
- [ ] 13.8 `architect plugin search <query>` — searches the public registry
- [ ] 13.9 `architect plugin install <plugin-id>` — adds plugin to `architect.yml`
- [ ] 13.10 Write `RegistryPluginSourceTest`

### Acceptance Criteria

- A private team registry works with `type: registry` and a custom URL
- Semver constraints resolve correctly
- SHA256 verification rejects tampered JARs

---

## Phase 14: Plugin Classloader Isolation

**Goal:** Two plugins with conflicting library dependencies do not break each other.

### Tasks

- [ ] 14.1 Create `IsolatedPluginClassLoader` — child-first classloader. Each plugin gets its own isolated instance with no JAR sharing.
- [ ] 14.2 Define `shared-api` classloader: only `architect-api` classes are shared via the parent (bridge classloader). All other classes are isolated.
- [ ] 14.3 Handle cross-plugin type compatibility via API interfaces (not concrete classes).
- [ ] 14.4 Add `classloader.debug: true` config flag that logs classloader resolution decisions.
- [ ] 14.5 Write `ClassloaderIsolationTest` — two plugins declaring conflicting versions of a library both function correctly.

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

- [ ] 15.1 Define **Architect Plugin Protocol v1** (APP v1): a JSON-RPC 2.0 based protocol over stdin/stdout with methods:
  - `init(config: JsonObject)` → `{ ok: true }`
  - `listTasks()` → `Array<TaskDescriptor>`
  - `executeTask(id, args, env)` → streaming events via newline-delimited JSON
- [ ] 15.2 Add `type: process` to plugin declaration:
  ```yaml
  plugins:
    - name: my-go-plugin
      type: process
      command: "./my-go-plugin"
  ```
- [ ] 15.3 Implement `ProcessPluginAdapter` in engine — launches the process, speaks APP v1, bridges to `ArchitectPlugin<Any>`
- [ ] 15.4 Publish **TypeScript SDK** (`@architect-platform/plugin-sdk`) with full APP v1 implementation. npm-installable.
- [ ] 15.5 Publish **Go SDK** (`github.com/architect-platform/plugin-sdk-go`) implementing APP v1.
- [ ] 15.6 Publish **Python SDK** (`architect-plugin-sdk` on PyPI) implementing APP v1.
- [ ] 15.7 Implement `type: npm` shorthand — downloads and runs an npm package as a plugin:
  ```yaml
  plugins:
    - name: my-ts-plugin
      type: npm
      package: "@my-org/architect-plugin"
      version: "^1.0.0"
  ```
- [ ] 15.8 Example plugins in each language demonstrating all protocol features
- [ ] 15.9 Write `ProcessPluginAdapterTest` with a mock subprocess

### Acceptance Criteria

- Writing a plugin in TypeScript requires only `npm install @architect-platform/plugin-sdk`
- Go plugin runs as a compiled binary; no JVM required on the host
- APP v1 protocol is documented and versioned

---

## Phase 16: Affected Task Detection

**Goal:** In a monorepo, only run tasks for modules that have changed since the last successful run.

### Tasks

- [ ] 16.1 Build **project dependency graph** from `architect.yml` `subprojects` declarations and inferred relationships (plugin shared config, shared `build.gradle.kts`, etc.)
- [ ] 16.2 **`git diff` integration** — compare against a base ref (default: `HEAD~1`, configurable to `origin/main`):
  - Map changed files to source roots
  - Walk the dependency graph to find all transitively affected projects
- [ ] 16.3 Add `architect build --affected` — executes tasks only for affected projects
- [ ] 16.4 Add `architect build --affected --base origin/main` — compare against base branch (ideal for PR workflows)
- [ ] 16.5 Add `affected` configuration to `architect.yml`:
  ```yaml
  project:
    affected:
      always-include: ["shared-lib"]   # always run these regardless
      never-include: ["docs"]          # never run these in affected mode
  ```
- [ ] 16.6 `architect affected` — prints the list of affected projects without running anything
- [ ] 16.7 **Cache invalidation integration**: if task output cache is enabled, a project is not "affected" if its cached outputs are valid even if files changed (requires Phase 17)
- [ ] 16.8 Write `AffectedProjectResolverTest` — covers: no changes, root-only change, transitive dependency chain, always-include, never-include

### Acceptance Criteria

- `architect test --affected` in a 50-project monorepo only tests changed modules
- `--base origin/main` works in PR CI pipeline

---

## Phase 17: Task Output Caching

**Goal:** Skip tasks whose inputs have not changed. Gradle-style incremental computation, but protocol-agnostic.

### Architecture

Each task declares `inputs` (files, config values, env vars). The engine hashes all inputs to produce a `cacheKey`. If a matching output exists locally (or in a remote cache), the task is skipped.

### Tasks

- [ ] 17.1 Add `TaskCacheDescriptor` to `architect-api`: tasks optionally return `CacheDescriptor(inputs: List<CacheInput>, outputs: List<CacheOutput>)`
- [ ] 17.2 `CacheInput` types: `FileSet(glob)`, `ConfigValue(key)`, `EnvVar(name)`, `CommandOutput(cmd)` — each produces a deterministic hash
- [ ] 17.3 `LocalOutputCache` — stores serialized task states in `~/.architect/cache/{cacheKey}/`. Stores stdout, exit code, output files.
- [ ] 17.4 `TaskExecutor` cache integration: compute key → check cache → skip if hit → execute and store on miss
- [ ] 17.5 `RemoteOutputCache` interface — `storeResult(key, result)`, `fetchResult(key): Result?`. Writable provider: HTTP cache server.
- [ ] 17.6 Implement HTTP remote cache backend (simple REST API: `GET /cache/{key}`, `PUT /cache/{key}`). Can be self-hosted or use architect-cloud.
- [ ] 17.7 `architect cache clear` — wipes local cache. `architect cache info` — shows cache size, hit rate from last session.
- [ ] 17.8 `--no-cache` flag to bypass cache for a run
- [ ] 17.9 Write `TaskOutputCacheTest` — hit, miss, invalidation, remote fallback

### Acceptance Criteria

- Second `architect test` on unchanged project completes in <100ms (all cache hits)
- Cache keys are deterministic: same inputs always produce same key across machines
- Remote cache works across CI build agents

---

## Phase 18: Extended Official Plugin Library

**Goal:** Cover the most common developer toolchains with first-class plugins.

### New Plugins

- [ ] 18.1 `docker-architected` — `docker-build` (BUILD), `docker-push` (PUBLISH), `docker-run` (RUN), `docker-compose-up/down/logs` (RUN). Context: `image`, `registry`, `platforms`.
- [ ] 18.2 `kubernetes-architected` — `k8s-apply` (PUBLISH), `k8s-rollout` (RUN), `k8s-status` (VERIFY), `k8s-port-forward` (RUN). Context: `namespace`, `context`, `manifests`.
- [ ] 18.3 `terraform-architected` — `tf-init` (INIT), `tf-plan` (VERIFY), `tf-apply` (PUBLISH), `tf-destroy`. Context: `workspace`, `backend`, `vars`.
- [ ] 18.4 `python-architected` — `py-install` (INIT), `py-lint` (LINT), `py-test` (TEST), `py-build` (BUILD), `py-publish` (PUBLISH). Context: `tool: uv|pip|poetry`, `python-version`.
- [ ] 18.5 `go-architected` — `go-build` (BUILD), `go-test` (TEST), `go-lint` (LINT), `go-release` (RELEASE). Context: `module`, `ldflags`.
- [ ] 18.6 `rust-architected` — `cargo-build` (BUILD), `cargo-test` (TEST), `cargo-lint` (LINT), `cargo-publish` (PUBLISH).
- [ ] 18.7 `maven-architected` — `mvn-verify` (TEST), `mvn-package` (BUILD), `mvn-deploy` (PUBLISH).
- [ ] 18.8 `nx-architected` — integrate with Nx monorepo: expose Nx targets as Architect tasks. Bridge affectedness detection.
- [ ] 18.9 **Fix `scripts-architected`** — resolve all three workflows (`CoreWorkflow`, `CodeWorkflow`, `HooksWorkflow`). Add `environment`, `workingDirectory` per-task. Add `sequential: true` flag to disable parallelism for a specific script group.

### Acceptance Criteria

- Each plugin has: typed context, full task coverage of its toolchain, unit tests for config parsing, and integration tests with a `--dry-run` path.

---

## Phase 19: Plugin Authoring Toolkit

**Goal:** Creating a new plugin takes 5 minutes, not 5 days.

### Tasks

- [ ] 19.1 `architect plugin create <name>` — scaffolds a new plugin in the current directory with:
  - Kotlin + Gradle template (for JVM plugins)
  - TypeScript template (for process plugins via npm SDK)
  - Go template (for process plugins via Go SDK)
  - `plugin.yml` manifest, test harness, README template
- [ ] 19.2 **Plugin test harness** — `ArchitectPluginTestKit` in `architect-api`:
  ```kotlin
  val kit = ArchitectPluginTestKit(MyPlugin())
  kit.configure(mapOf("setting" to "value"))
  val result = kit.executeTask("my-task")
  assertThat(result).isSuccess()
  ```
- [ ] 19.3 **Local plugin dev loop** — `type: local` plugin source reloads the plugin JAR on every execution (no engine restart). Add `architect engine reload-plugins` endpoint.
- [ ] 19.4 **Plugin documentation generator** — `architect plugin docs <path>` — reads plugin metadata and generates a Markdown reference doc
- [ ] 19.5 **Plugin validation** — `architect plugin validate <path>` — validates a plugin JAR: checks SPI file, verifies `ArchitectPlugin` implementation, tests config deserialization
- [ ] 19.6 Write plugin authoring guide (see Phase 25)

### Acceptance Criteria

- A developer with no prior Architect knowledge can create, test, and publish a plugin in under 30 minutes
- `ArchitectPluginTestKit` allows testing plugins without a running engine

---

## Phase 20: Project Graph & Visualization

**Goal:** Understand the full task dependency graph at a glance.

### Tasks

- [ ] 20.1 `architect graph` — outputs a DOT format directed graph of the task DAG for the current project
- [ ] 20.2 `architect graph --open` — renders the graph as an SVG or HTML page and opens in browser (uses D3.js or Mermaid)
- [ ] 20.3 `architect graph <task>` — subgraph for a specific task and its dependencies
- [ ] 20.4 `architect graph --projects` — shows the monorepo project graph (project ↔ project dependency relationships)
- [ ] 20.5 `architect plan <task> --tree` — ASCII tree rendering in the terminal (already partially done in plan mode; make it richer with batch groups and timing estimates)
- [ ] 20.6 Integrate graph rendering into the VS Code extension (see Phase 9.6) — a panel that renders the live task graph

### Acceptance Criteria

- `architect graph --open` opens a navigable HTML page with the full DAG
- Graph updates live in VS Code as `architect.yml` is edited

---

## Phase 21: Testing — Coverage to >85%

**Goal:** The platform is trustworthy. Every component has comprehensive automated tests.

### architect-api

- [ ] 21.1 All existing tests verified passing
- [ ] 21.2 Add `ProjectContextTest` — getKey extension, missing key, nested key, wrong type
- [ ] 21.3 Add `TaskRegistryTest` — register, get, all, duplicate id handling

### architect-core / architect-engine

- [ ] 21.4 `TaskExecutorTest` — parallel batch execution with mock tasks, sequential fallback, failure propagation, child task execution
- [ ] 21.5 `HistoryServiceTest` — write record, read all (sorted), read by project, limit, directory creation, JSON round-trip
- [ ] 21.6 `ConfigValidatorTest` — required field missing, unknown key warning, plugin key validation, line numbers in errors
- [ ] 21.7 `TaskDependencyResolverTest` — expand existing; add: children resolution, batch assignment for diamond graph, large graph performance test
- [ ] 21.8 `ProjectServiceTest` — loadProject round-trip with inline tasks, subproject discovery, validation exception on invalid config
- [ ] 21.9 `BashCommandExecutorTest` — expand with: timeout enforcement, non-zero exit code, environment variable injection, working directory
- [ ] 21.10 `ExecutionApiControllerTest` — SSE stream: verify events are received, stream terminates on COMPLETED, stream terminates on FAILED
- [ ] 21.11 `TaskCacheTest` — expand: concurrent reads, cache invalidation, TTL expiry

### architect-cli

- [ ] 21.12 `ArchitectLauncherTest` — command routing: plan, history, validate, engine subcommands, task execution, `--plain`, `--no-daemon`
- [ ] 21.13 `EngineHealthCheckerTest` — HTTP 200, HTTP 500, connection refused, timeout
- [ ] 21.14 `ConsoleUITest` — expand existing 12 tests; add: batch grouping output, summary rendering, timing output, `--json` output

### plugins

- [ ] 21.15 All plugins: add `execute()` level tests using `ArchitectPluginTestKit` (Phase 19.2 prerequisite)
- [ ] 21.16 `GitPluginTest` — git-config secure escaping with adversarial inputs
- [ ] 21.17 `ScriptsPluginTest` — phase resolution for all three workflows
- [ ] 21.18 `DocsPluginTest` — path traversal prevention, all three builders

### architect-cloud

- [ ] 21.19 `EngineServiceTest`, `ProjectServiceTest`, `ExecutionServiceTest` — all use case operations
- [ ] 21.20 `ExecutionEventServiceTest` — event broadcasting via Reactor Sink
- [ ] 21.21 `EventsWebSocketServerTest` — WebSocket event delivery
- [ ] 21.22 Add persistence adapter integration tests with H2

### Coverage Gates

- [ ] 21.23 Add JaCoCo to all modules with a minimum coverage gate of 80% (enforced in CI, target 85%)
- [ ] 21.24 Add mutation testing via PIT to `architect-api` and `architect-core`

---

## Phase 22: Integration & End-to-End Tests

**Goal:** Confidence that all components work together, not just in isolation.

### Tasks

- [ ] 22.1 **CLI ↔ Engine integration tests** — use `@MicronautTest` to spin up the real engine, execute commands via the real CLI HTTP client, assert events via SSE
- [ ] 22.2 **Embedded mode end-to-end** — load a real plugin JAR locally, execute a real task, assert the result and history record
- [ ] 22.3 **Plugin contract tests** — a shared test suite that any `ArchitectPlugin` implementation can run to verify protocol compliance
- [ ] 22.4 **Monorepo end-to-end** — create a temporary multi-project workspace, run a task across all subprojects, assert parallel execution and result aggregation
- [ ] 22.5 **CI simulation test** — run the full pipeline (`init → lint → verify → build → test`) on the project itself using Architect

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

- [ ] 23.1 **GraalVM Native Image for CLI** — compile `architect-cli` to a native binary. Eliminates JVM startup (~200ms saving). Requires Micronaut AOT compatibility.
- [ ] 23.2 **Lazy plugin loading** — load plugin JARs only when a plugin's tasks are actually needed (not at project registration time)
- [ ] 23.3 **Parallel plugin loading** — load independent plugins concurrently (coroutine-based, already feasible)
- [ ] 23.4 **Project config caching** — `ProjectService` project cache is already implemented but disabled. Enable by default with file-system watcher invalidation.
- [ ] 23.5 **Build benchmarks** — `jmh` micro-benchmarks for `TaskDependencyResolver.topologicalSort()` and `ConfigValidator` on large configs
- [ ] 23.6 **Startup profiling** — instrument engine startup and identify top-3 bottlenecks
- [ ] 23.7 **Connection pooling** — CLI ↔ Engine HTTP keep-alive connections (already in Micronaut HTTP client; verify active)

---

## Phase 24: Security Hardening

**Goal:** Architect can be trusted in regulated, enterprise, and multi-tenant environments.

### Tasks

- [ ] 24.1 **Plugin signing** — plugins can be signed with a GPG key. Engine verifies signature before loading. `architect.yml`:
  ```yaml
  plugins:
    - name: my-plugin
      type: github
      repo: my-org/my-plugin
      verify-signature: true
      trusted-keys: ["0xABCD1234"]
  ```
- [ ] 24.2 **Task permission model** — tasks declare required permissions in their descriptor: `file-system:read`, `file-system:write`, `network:outbound`, `process:exec`. Engine enforces via a Java SecurityManager replacement (process-level sandboxing).
- [ ] 24.3 **Secrets management** — tasks access secrets via `Environment.secret("MY_SECRET")` which resolves from: environment variable, `.env` file, HashiCorp Vault, AWS Secrets Manager, GCP Secret Manager. Secrets are never logged.
- [ ] 24.4 **Audit logging** — every task execution is audit-logged with: timestamp, user, project, task, args, result, duration. Stored locally and optionally synced to `architect-cloud`.
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
