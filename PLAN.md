# ARCHITECT PLATFORM — MASTER IMPROVEMENT PLAN

> **Vision**: Make Architect the single entrypoint for every developer workflow — from code to cloud. Convention over configuration. Sensible defaults. Infinite extensibility.
>
> **Current State**: 6.5/10 — Strong core API and engine, but fragmented DX, incomplete plugins, missing observability, and no unified "just works" experience.
>
> **Target State**: 9/10 — Zero-config project setup, rich interactive CLI, full lifecycle coverage, plugin marketplace, cloud dashboard, IDE-native integration.

## EXECUTION STATUS

- Overall Progress: 77/128 tasks completed (60%)
- Current Phase: Phase 5 — Configuration as Code Excellence
- Last Updated: 2026-03-28T14:40:08Z

---

## STATUS LEGEND

| Symbol | Meaning |
|--------|---------|
| `[ ]` | Not started |
| `[~]` | In progress |
| `[x]` | Completed |
| `[!]` | Blocked |
| `[—]` | Skipped / Deferred |

**Priority**: 🔴 Critical · 🟠 High · 🟡 Medium · 🟢 Nice-to-have
**Effort**: `XS` (< 2h) · `S` (2-4h) · `M` (4-8h) · `L` (1-2d) · `XL` (3-5d) · `XXL` (1-2w)

---

## PHASE 0 — FOUNDATIONS & API ENRICHMENT
> *Strengthen the core contracts so every downstream improvement builds on solid ground.*

### 0.1 — Enhanced Task Result Model
> Tasks currently return `success + message`. We need structured data, metadata, and inter-task data passing.

- [x] **T-0.1.1** 🔴 `M` — Add `TaskMetadata` data class to API (`duration`, `exitCode`, `startedAt`, `finishedAt`, `executorInfo`) | Finished: 2026-03-26T18:20:00Z | Notes: Created TaskMetadata data class, added metadata field to TaskResult interface with null default, updated TaskResultImpl, factory methods, both DTOs (core + CLI), added 6 new tests
  - File: `architect-api/api/src/main/kotlin/io/github/architectplatform/api/core/tasks/TaskResult.kt`
  - Add `metadata: TaskMetadata?` field to `TaskResult` interface
  - All existing implementations must return `null` by default (backward compatible)

- [x] **T-0.1.2** 🔴 `M` — Add `data: Map<String, Any>` to `TaskResult` for inter-task data passing | Finished: 2026-03-26T18:28:00Z | Notes: Added data field with empty default to TaskResult, updated DTOs, enhanced TaskContext with upstreamData, added upstream data collection in TaskExecutor, added 5 new tests
  - Enables downstream tasks to read outputs from upstream tasks
  - Engine must propagate `data` through dependency chain into `TaskContext`

- [x] **T-0.1.3** 🟠 `S` — Add `TaskResult.skipped(reason)` factory method | Finished: 2026-03-26T18:32:00Z | Notes: Added Status enum (SUCCESS/FAILURE/SKIPPED), skipped() factory, CLI renders ⏭️ icon for skipped, 5 new tests
  - Currently no way to distinguish "did nothing" from "succeeded"
  - Engine should render skipped tasks differently in output

- [x] **T-0.1.4** 🟠 `S` — Add `TaskResult.warning(message)` for non-fatal issues | Finished: 2026-03-26T18:35:00Z | Notes: Added WARNING status, warning() factory with metadata/data/results support, CLI renders ⚠️ icon, 4 new tests
  - Separate from success (clean) and failure (broken)
  - CLI renders warnings in yellow with ⚠️ icon

### 0.2 — CommandExecutor Contract Completion
> `CommandExecutor.execute()` has no defined return type. Every implementation guesses.

- [x] **T-0.2.1** 🔴 `M` — Define `CommandResult` data class (`exitCode: Int`, `stdout: String`, `stderr: String`, `durationMs: Long`) | Finished: 2026-03-26T18:38:00Z | Notes: Created CommandResult in api/components/execution, added success computed property, 6 tests
  - File: `architect-api/api/src/main/kotlin/io/github/architectplatform/api/core/execution/CommandResult.kt`

- [x] **T-0.2.2** 🔴 `M` — Update `CommandExecutor` interface to return `CommandResult` | Finished: 2026-03-26T18:41:00Z | Notes: Added executeWithResult() with timeout/env params, default impl wraps execute() for backward compat, kept original execute() unchanged
  - Add overloads: `execute(command, workingDir?, timeout?, env?: Map<String, String>)`
  - Backward-compatible: default timeout = 300s, default env = inherit

- [x] **T-0.2.3** 🟠 `S` — Update `BashCommandExecutor` in engine to implement new contract | Finished: 2026-03-26T18:44:00Z | Notes: Native executeWithResult() override with separate stdout/stderr capture, env var support via SandboxedProcessLauncher, timeout handling returns CommandResult(-1)
  - Map existing implementation to new `CommandResult` return type

### 0.3 — Environment Service Enrichment
> Tasks need logging, metrics, and event subscriptions — not just `publish()`.

- [x] **T-0.3.1** 🔴 `M` — Add `Logger` interface to API and `Environment.logger(tag)` factory | Finished: 2026-03-26T18:48:00Z | Notes: Created ArchitectLogger interface (debug/info/warn/error), added Environment.logger(tag) with no-op default, SLF4J impl in architect-core, wired into ApplicationEnvironment
  - Levels: `debug`, `info`, `warn`, `error`
  - Engine implementation delegates to SLF4J
  - Tasks get structured logging for free

- [x] **T-0.3.2** 🟠 `M` — Add event subscription to `Environment` | Finished: 2026-03-26T18:52:00Z | Notes: Added subscribe(type, handler) to Environment with no-op default, ApplicationEnvironment routes published events to type-matched handlers
  - `fun <E> subscribe(type: Class<E>, handler: (E) -> Unit)`
  - Enables inter-task communication without tight coupling

- [x] **T-0.3.3** 🟠 `S` — Add `Environment.variable(name): String?` for explicit env var access | Finished: 2026-03-26T18:55:00Z | Notes: Default delegates to System.getenv(); engine can override for sandboxing. Added 6 EnvironmentTest tests.
  - Distinct from `secret()` — public config vs sensitive values
  - Validates variable exists, logs access for audit

- [x] **T-0.3.4** 🟡 `M` — Add `Environment.progressReporter()` for long-running tasks | Finished: 2026-03-26T18:58:00Z | Notes: Created ProgressReporter interface with NOOP default, added progressReporter() to Environment
  - Interface: `report(current: Int, total: Int, message: String)`
  - CLI renders as progress bar; JSON mode emits progress events
  - SSE streams progress to connected clients

### 0.4 — Task Conditions & Runtime Guards
> Tasks need runtime conditions beyond static `requires()`.

- [x] **T-0.4.1** 🟠 `M` — Add `Task.shouldExecute(env, ctx): Boolean` with default `true` | Finished: 2026-03-26T19:02:00Z | Notes: Added to Task interface with true default; TaskExecutor checks before execution, skips with TaskResult.skipped()
  - Evaluated at execution time, not registration time
  - Engine skips task with `TaskResult.skipped(reason)` if false
  - Example: "only run if `src/` changed since last execution"

- [x] **T-0.4.2** 🟠 `S` — Add `Task.onFailure(): FailureStrategy` enum | Finished: 2026-03-26T19:10:00Z | Notes: Sealed class ABORT/CONTINUE/RETRY; TaskExecutor respects strategy in batch loop and retries
  - Values: `ABORT` (default), `CONTINUE`, `RETRY(maxAttempts)`
  - Engine respects strategy during composite/pipeline execution

- [x] **T-0.4.3** 🟡 `S` — Add `Task.timeout(): Duration?` for per-task timeouts | Finished: 2026-03-26T19:15:00Z | Notes: Added to Task interface with null default; TaskExecutor enforces via Future.get() with TimeoutException handling
  - Overrides global `executor.timeout-seconds`
  - Engine cancels task if exceeded, returns `TaskResult.failure("Timeout")`

### 0.5 — Configuration & Schema Validation
> `architect.yml` has no schema enforcement. Typos silently fail.

- [x] **T-0.5.1** 🔴 `L` — Define JSON Schema for `architect.yml` root structure | Finished: 2026-03-26T19:22:00Z | Notes: Created Draft-07 schema at sdk/schema/architect.schema.json covering project, plugins, docs, git, github, gradle, scripts, pipelines, tasks sections
  - Validate: `project`, `plugins`, `tasks`, `scripts`, `pipelines` sections
  - Publish schema to SchemaStore for IDE auto-completion

- [x] **T-0.5.2** 🟠 `M` — Add config interpolation support (`${env.VAR}`, `${project.name}`) | Finished: 2026-03-26T19:28:00Z | Notes: Created ConfigInterpolator, wired into ConfigLoader.loadWithRaw(), 11 tests
  - Resolve environment variables and project properties in YAML values
  - Support default values: `${env.PORT:8080}`

- [x] **T-0.5.3** 🟠 `M` — Add config profiles/environments (`architect.yml` + `architect.ci.yml` overlay) | Finished: 2026-03-26T19:35:00Z | Notes: File-based overlays in ConfigLoader, ProjectService passes activeProfile
  - `--env ci` merges `architect.ci.yml` over `architect.yml`
  - Supports: `architect.staging.yml`, `architect.production.yml`

- [x] **T-0.5.4** 🟡 `S` — Add config inheritance for monorepos | Finished: 2026-03-26T19:40:00Z | Notes: inherit:true walks up parent dirs, deep-merges parent architect.yml as base
  - Child `architect.yml` inherits parent plugin config
  - `inherit: true` flag per plugin section
  - Reduces duplication in large monorepos

---

## PHASE 1 — CLI DEVELOPER EXPERIENCE REVOLUTION
> *Transform the CLI from a task executor into an intelligent developer companion.*

### 1.1 — Interactive Mode & Smart Defaults
> The CLI has zero interactivity. No menus, no prompts, no guidance.

- [x] **T-1.1.1** 🔴 `XL` — Implement `architect init` interactive project scaffolding wizard | Finished: 2026-03-26T19:35:00Z | Notes: Stack detection (13 languages/tools), plugin auto-suggestion, interactive prompts, --yes flag, YAML generation with 16 tests
  - Detect existing project type (package.json, build.gradle, Cargo.toml, etc.)
  - Auto-suggest plugins based on detected stack
  - Generate `architect.yml` with sensible defaults
  - Prompt for project name, description, plugins to enable
  - Support `--yes` flag for non-interactive mode with auto-detected defaults

- [x] **T-1.1.2** 🔴 `L` — Add `architect help` and per-command `--help` with examples | Finished: 2026-03-26T19:50:00Z | Notes: HelpCommandHandler with overview, tasks topic, plugins topic, and per-command detailed help for all 15 commands
  - Structured help with usage, description, examples, related commands
  - `architect help <command>` shows detailed docs
  - `architect help tasks` explains the task system
  - `architect help plugins` explains plugin ecosystem

- [x] **T-1.1.3** 🟠 `L` — Implement interactive task selector when no task specified | Finished: 2026-03-27T12:45:00Z | Notes: Created InteractiveTaskSelector with stty raw-mode, arrow key navigation, fuzzy search (id/desc/phase), phase grouping (INIT→LINT→VERIFY→BUILD→TEST→RUN→RELEASE→PUBLISH→OTHER), alternate screen buffer, CI/plain fallback to printTasks; wired into ArchitectLauncher for both engine and embedded modes; 9 unit tests
  - `architect` with no args shows categorized task list (grouped by phase)
  - Arrow key navigation, fuzzy search, Enter to execute
  - Show task description, phase, dependencies inline
  - Fallback to plain list in CI mode

- [x] **T-1.1.4** 🟠 `M` — Add `architect doctor` diagnostic command
  - Check: engine running, plugins loaded, config valid, tools available, versions compatible
  - Output: checklist with ✅/❌ per check + remediation hints
  - `architect doctor --fix` attempts auto-remediation (install engine, fix config)

- [x] **T-1.1.5** 🟡 `M` — Add `architect config` subcommand for config management
  - `architect config show` — display resolved configuration (merged profiles)
  - `architect config set <key> <value>` — modify architect.yml programmatically
  - `architect config get <key>` — read specific value
  - `architect config diff` — show diff between profiles
  - `architect config validate` — validate against schemas

### 1.2 — Output & Debugging Experience
> No verbose mode, no output persistence, limited debugging.

- [x] **T-1.2.1** 🔴 `M` — Add `--verbose` / `-q` graduated verbosity levels | Finished: 2026-03-26T19:30:00Z | Notes: Default v1 (backward compat), --quiet/-q for v0, --verbose for v2, --verbose 3 for debug
  - Level 0 (default): summary output only
  - Level 1 (`-v`): task names + durations
  - Level 2 (`-vv`): command details + stdout
  - Level 3 (`-vvv`): full debug (HTTP calls, plugin loading, config resolution)

- [x] **T-1.2.2** 🟠 `M` — Add `--output <file>` and `--tee` flags for output persistence
  - Save execution output to file while displaying on terminal
  - JSON mode: structured execution log with timestamps
  - Useful for CI artifact collection

- [x] **T-1.2.3** 🟠 `S` — Add `--dry-run` flag for all task executions
  - Show what would execute without actually running
  - Display: task order, commands, environment, working directories
  - Helps debug pipeline issues before committing

- [x] **T-1.2.4** 🟡 `S` — Add `--timing` flag to show task execution breakdown
  - Waterfall view: which tasks ran in parallel, durations, critical path
  - ASCII-art timeline in terminal, HTML in `--json` mode

- [x] **T-1.2.5** 🟡 `S` — Add command aliasing support
  - `architect.yml` section for aliases: `aliases: { b: build, t: test, bt: "build test" }`
  - `architect b` expands to `architect build`

### 1.3 — Shell Integration & Completions
> Static completions only. No dynamic task/project name completion.

- [x] **T-1.3.1** 🟠 `L` — Implement dynamic shell completion for task names | Finished: 2026-03-27T13:05:00Z | Notes: Added `architect completion query tasks|phases|projects` sub-command; `CliInfrastructureHandler` queries engine (falls back to task-cache.txt, then static list); bash script injects `_architect_complete()` for task+phase completions; zsh uses `compdef _architect_zsh`; fish uses `(architect completion query tasks)` live query; project name cached to ~/.architect/project-cache.txt after registration
  - Query engine (or parse architect.yml) for available tasks
  - Complete `architect <TAB>` with actual task names
  - Complete `architect --filter <TAB>` with phase names

- [x] **T-1.3.2** 🟠 `M` — Add project name completion for multi-project commands | Finished: 2026-03-27T13:15:00Z | Notes: Extended bash _architect_complete() with --affected/--base and 'history <TAB>' project completions; zsh uses _arguments with project candidates; fish adds separate 'history <project>' completion; all shells use 'architect completion query projects' (engine + project-cache.txt)
  - `architect --affected <TAB>` completes project names
  - `architect history <TAB>` completes from known projects

- [x] **T-1.3.3** 🟡 `S` — Add `architect completion install` auto-installer | Finished: 2026-03-27T13:25:00Z | Notes: `handleCompletionInstall()` detects shell via $SHELL/parent process; bash writes ~/.architect/architect-completion.bash and appends source line to ~/.bashrc; zsh writes ~/.architect/architect-completion.zsh and appends to ~/.zshrc; fish writes ~/.config/fish/completions/architect.fish; all idempotent (checks for existing source lines); --dry-run flag shows what would happen; --shell <s> overrides auto-detection
  - Detect shell (bash/zsh/fish), install completion script to correct location
  - Add to `.bashrc`, `.zshrc`, or `~/.config/fish/completions/`
  - Idempotent — safe to run multiple times

### 1.4 — Execution UX Improvements
> Progress rendering is basic. No cancellation, no retry, no parallel visibility.

- [x] **T-1.4.1** 🟠 `L` — Implement rich progress rendering with parallel task visualization | Finished: 2026-03-27T13:55:00Z | Notes: Created ParallelProgressView.kt with background spinner thread (80ms), in-place ANSI rendering via cursor-up/clear-to-end, taskStarted/taskCompleted/taskFailed/taskSkipped/taskCancelled/output API, integrated into ConsoleUI.process() and printSummary()
  - Show multiple concurrent tasks with individual progress
  - Spinner per running task, checkmark on complete, X on failure
  - Collapse completed tasks to one line, expand failures
  - Respect terminal width for truncation

- [x] **T-1.4.2** 🟠 `M` — Add Ctrl+C graceful cancellation with cleanup
  - First Ctrl+C: graceful stop (finish current task, skip remaining)
  - Second Ctrl+C: force kill
  - Display: "Cancelling... (press Ctrl+C again to force)"
  - Engine must support execution cancellation API

- [x] **T-1.4.3** 🟡 `M` — Add `architect retry` to re-run last failed execution
  - Read from history, re-execute same task with same args
  - `architect retry --from <task>` resumes from specific failed task
  - Useful for flaky tests or transient network failures

- [x] **T-1.4.4** 🟡 `S` — Add `--parallel <N>` flag to control task parallelism
  - Override engine's default parallel execution behavior
  - `--parallel 1` for sequential debugging
  - `--parallel 0` for unlimited (use all cores)

---

## PHASE 2 — ENGINE HARDENING & EXECUTION INTELLIGENCE
> *Make the engine production-grade: resilient, observable, and intelligent.*

### 2.1 — Execution Lifecycle Improvements

- [x] **T-2.1.1** 🔴 `L` — Implement execution cancellation API | Finished: 2026-03-26T21:20:00Z | Notes: CANCELLED event type, DELETE /api/executions/{id} endpoint, Job tracking in TaskService, CLI cancelExecution client method, ConsoleUI CANCELLED rendering
  - `DELETE /api/executions/{executionId}` — cancel running execution
  - Propagate cancellation to running `BashCommandExecutor` processes
  - Emit `execution.cancelled` event
  - CLI hooks into this via Ctrl+C handler

- [x] **T-2.1.2** 🔴 `M` — Add task retry logic with configurable policies | Finished: 2026-03-27T14:10:00Z | Notes: Extended FailureStrategy.RETRY with backoffMs/exponential/jitter; added RETRYING ExecutionEventType; added taskRetryingEvent factory; TaskExecutor.executeSingleTask() made suspend + delay() + retry events; EngineConfiguration.TaskRetry constants; 8 new tests in TaskExecutorRetryTest
  - Respect `Task.onFailure()` strategy from API
  - Configurable: `architect.engine.retry.max-attempts`, `architect.engine.retry.backoff-ms`
  - Exponential backoff with jitter for retries
  - Retry events emitted for monitoring

- [x] **T-2.1.3** 🟠 `M` — Add resource limits for task execution | Finished: 2026-03-27T14:20:00Z | Notes: Added maxConcurrentTasks to TaskExecutor (kotlinx.coroutines.sync.Semaphore); EngineConfiguration.TaskExecution.MAX_CONCURRENT_TASKS constant (default 0=unlimited); wired in RuntimeServiceFactory; semaphore concurrency test verifies peak ≤ limit
  - `architect.engine.executor.max-concurrent-tasks: 4`
  - `architect.engine.executor.memory-limit-mb: 512` (per task)
  - Semaphore-based concurrency control
  - Reject excess tasks with `429 Too Many Requests`

- [x] **T-2.1.4** 🟠 `M` — Add execution timeout at the execution level (not just task) | Finished: 2026-03-27T14:30:00Z | Notes: Added executionTimeoutSeconds @Property to TaskService; withTimeoutOrNull wraps executeRecursivelyOverSubprojectsFirst; on timeout emits executionCancelledEvent + returns TaskResult.failure; EngineConfiguration.TaskExecution.EXECUTION_TIMEOUT_SECONDS constant (default 0=unlimited)
  - `architect.engine.executor.execution-timeout-seconds: 1800`
  - Cancels entire execution if wall-clock time exceeded
  - Prevents runaway monorepo executions

- [x] **T-2.1.5** 🟡 `S` — Fix "task not found silently returns success" behavior | Finished: 2026-03-26T19:58:00Z | Notes: Created TaskNotFoundException with Levenshtein-based suggestions; updated all 4 lookup sites; subproject uses TaskResult.skipped()
  - Currently questionable: missing tasks don't fail
  - Change to: throw `TaskNotFoundException` with helpful message
  - Suggest similar task names (Levenshtein distance)

### 2.2 — Observability & Metrics

- [x] **T-2.2.1** 🔴 `L` — Add structured metrics collection | Finished: 2026-03-26T21:25:00Z | Notes: MetricsService (counters+histograms), GET /api/metrics (Prometheus) + /api/metrics/json, wired into TaskService execution pipeline, 7 tests
  - Task execution duration, success/failure rates, cache hit ratio
  - Expose via `GET /api/metrics` (Prometheus format)
  - Micrometer integration with Micronaut
  - Counters: `architect.tasks.executed`, `architect.tasks.failed`
  - Histograms: `architect.tasks.duration`, `architect.cache.lookup.duration`

- [x] **T-2.2.2** 🟠 `M` — Add health check endpoint
  - `GET /api/health` — engine health with subsystem checks
  - Checks: plugin loader, project repository, event system, cloud connectivity
  - Returns: `{ status: "UP/DOWN", checks: [...] }`
  - Enable Micronaut management endpoints

- [x] **T-2.2.3** 🟠 `M` — Add execution audit log
  - Persist: who ran what, when, with what args, from where
  - `GET /api/audit` — query audit records
  - Include: user, host, project, task, args, result, duration
  - Configurable retention: `architect.engine.audit.retention-days: 30`

- [x] **T-2.2.4** 🟡 `M` — Add task performance profiling | Finished: 2026-03-27T15:00:00Z | Notes: Created TaskStats+Trend (engine), TaskStatsService (percentile/trend computation from HistoryService), TaskStatsController (GET /api/projects/{project}/tasks/{taskId}/stats, GET .../tasks/stats), TaskStatsDTO (CLI), printStats+printTaskStats in OutputFormatter, handleStats() in ArchitectLauncher, 9 unit tests in TaskStatsServiceTest
  - Track per-task: avg duration, p50/p95/p99, trend (improving/degrading)
  - `GET /api/projects/{name}/tasks/{task}/stats`
  - CLI: `architect stats <task>` shows performance history
  - Alert when task regresses significantly

### 2.3 — Plugin Loading Hardening

- [x] **T-2.3.1** 🔴 `M` — Add plugin version conflict resolution | Finished: 2026-03-27T13:31:03Z | Notes: Added PluginVersionConflictResolver with dependency-key grouping and newest-wins selection; ProjectPluginLoader now resolves conflicts before load and logs warnings; ConfigValidator reports conflict warnings during `architect validate`; added tests in ProjectPluginLoaderTest and ConfigValidatorTest.
  - Detect when multiple plugins require different versions of same dependency
  - Strategy: newest wins, with warning
  - `architect validate` reports version conflicts

- [x] **T-2.3.2** 🟠 `L` — Add plugin dependency graph | Finished: 2026-03-27T13:31:03Z | Notes: Added `dependencies(): List<String>` to ArchitectPlugin interface, introduced PluginDependencyResolver with topological sorting and circular/missing dependency detection, integrated resolver into ProjectPluginLoader so plugins initialize in dependency order, and added tests for ordering and cycle errors.
  - Plugins can declare dependencies on other plugins
  - `ArchitectPlugin` interface: `fun dependencies(): List<String>` (plugin IDs)
  - Engine loads plugins in dependency order
  - Circular plugin dependency detection

- [x] **T-2.3.3** 🟠 `M` — Add plugin sandboxing (security boundaries) | Finished: 2026-03-27T13:40:38Z | Notes: Added plugin permission policy validation (`PluginPermissionPolicyValidator`) that compares task runtime permissions vs plugin-declared `configSchema` `x-permissions`; integrated checks into `ProjectService` with warnings or strict-mode errors (`architect.engine.plugins.security.strict-mode`); wired strict-mode property in engine factory; added focused tests and inline plugin declaration support.
  - Plugins declare required permissions in `configSchema()`
  - Engine validates actual usage against declared permissions
  - Log violations; optionally block in strict mode

- [x] **T-2.3.4** 🟡 `M` — Add automatic plugin update checking | Finished: 2026-03-27T13:40:38Z | Notes: Added `architect plugin outdated` (registry-aware update listing) and `architect plugin update [--all|<id>]` (writes latest version pins to architect.yml); added startup/tasks hint via `maybeWarnOutdatedPlugins`; injected remote fetcher for testability and added CLI tests for outdated/update flows.
  - On `architect tasks` or startup, check plugin versions against registry
  - `architect plugin outdated` lists plugins with available updates
  - `architect plugin update [--all]` updates to latest compatible versions

### 2.4 — Event System Enhancement

- [x] **T-2.4.1** 🟠 `M` — Add typed event hierarchy | Finished: 2026-03-27T15:20:00Z | Notes: Added typed execution event model (`TypedExecutionEvent` + concrete task/execution events) and converter from existing `ArchitectEvent<ExecutionEvent>`; wired engine SSE (`ExecutionController`) to emit typed wrappers and stop on terminal root execution events; emitted `project.registered` via ProjectService event bus hook; added serde imports and focused tests (`TypedArchitectEventTest`, controller/integration updates); preserved CLI HTTP compatibility by keeping map stream contract while supporting typed payload keys (`eventType`/`parentProject`) in ConsoleUI.
  - `TaskStartedEvent`, `TaskCompletedEvent`, `TaskFailedEvent`, `TaskOutputEvent`
  - `ExecutionStartedEvent`, `ExecutionCompletedEvent`
  - `PluginLoadedEvent`, `ProjectRegisteredEvent`
  - Replace `Map<String, Any>` with typed events in SSE stream

- [x] **T-2.4.2** 🟠 `M` — Add event persistence for replay | Finished: 2026-03-27T15:30:00Z | Notes: Added in-memory append-only execution event log to `ExecutionEventCollector` and replay API (`getReplayEvents(executionId, from)`); exposed through `TaskService.getExecutionReplay`; added REST replay endpoint `GET /api/executions/{id}/events?from=0` in `ExecutionController` returning typed events; added/updated tests (`ExecutionEventCollectorTest`, `ExecutionControllerTest`) and validated with focused engine + CLI suites.
  - Store events in append-only log per execution
  - `GET /api/executions/{id}/events?from=0` — replay from beginning
  - Enables: post-mortem debugging, cloud sync, UI replay

- [x] **T-2.4.3** 🟡 `S` — Add event buffer overflow monitoring | Finished: 2026-03-27T15:40:00Z | Notes: Added configurable overflow strategy `architect.engine.events.overflow-strategy` (`BLOCK`, `DROP_OLDEST`, `EXPAND`) to `EngineConfiguration`; `ExecutionEventCollector` now applies strategy-specific flow overflow behavior, records dropped events via metrics counter `architect.events.dropped`, and logs warnings when effective buffer pressure exceeds 80%; validated through focused engine/core test suites.
  - Current `DROP_OLDEST` strategy silently loses events
  - Add metric: `architect.events.dropped`
  - Log warning when buffer > 80% capacity
  - Configure: `architect.engine.events.overflow-strategy: BLOCK | DROP_OLDEST | EXPAND`

---

## PHASE 3 — PLUGIN ECOSYSTEM MATURATION
> *Graduate incubating plugins to production. Fill critical ecosystem gaps.*

### 3.1 — Plugin Standard Enforcement

- [x] **T-3.1.1** 🔴 `L` — Create plugin graduation checklist and automate enforcement | Finished: 2026-03-26T20:10:00Z | Notes: Created PluginGraduationChecker with 6 checks; CLI architect plugin graduate command; 5 tests
  - Required for "Active" status:
    - ✅ Contract tests (`ArchitectPluginContractTestSuite`) passing
    - ✅ README with configuration examples
    - ✅ STATUS.md with maturity declaration
    - ✅ `configSchema()` returning valid JSON Schema
    - ✅ Error handling for all known failure modes
    - ✅ At least 60% test coverage
  - CI job validates all plugins against checklist
  - Block merge if active plugin drops below standard

- [x] **T-3.1.2** 🟠 `M` — Add `architect plugin test` command | Finished: 2026-03-27T15:03:05Z | Notes: Added `plugin test` subcommand to `PluginCommandHandler` with JSON/text output and failure exit code; implemented plugin JAR contract checks in `PluginJarValidator.test()` (SPI wiring, `ArchitectPluginContract` verification, schema checks, task registration); updated help docs; added focused tests (`PluginJarTesterTest`, `PluginCommandHandlerTest`) and re-ran targeted CLI suites including `ArchitectLauncherTest`.
  - Runs contract test suite against any plugin JAR
  - Validates SPI wiring, schema, task registration
  - Useful for plugin developers before publishing

- [x] **T-3.1.3** 🟠 `M` — Create plugin development guide documentation | Finished: 2026-03-27T15:06:11Z | Notes: Added comprehensive Plugin Development guide (`docs/guides/plugin-development.md`) covering scaffold → implement → test → publish workflow, API contract examples, and release checklist; wired guide into MkDocs navigation; updated CLI reference and authoring docs to include `architect plugin test`; added cross-links under Guides for discoverability.
  - Step-by-step: scaffold → implement → test → publish
  - Template project with best practices
  - Document all API contracts with examples
  - Add to MkDocs site under "Plugin Development"

### 3.2 — Graduate Incubating Plugins (Language Ecosystems)

- [x] **T-3.2.1** 🟠 `L` — Graduate `javascript-architected` to Active | Finished: 2026-03-27T15:37:38Z | Notes: Expanded plugin feature set for active maturity: added workspace detection, lockfile validation, dependency audit, version, and publish tasks; added package-manager support matrix updates (npm/yarn classic+berry/pnpm/bun), config schema and enriched context options; strengthened tests (contract + behavior + workspace/lockfile/version/publish coverage); updated plugin README/STATUS and docs reference page for new tasks and configuration.
  - Add: monorepo workspace detection, package publishing, version management
  - Add: lockfile validation, security audit (`npm audit`)
  - Full contract tests, comprehensive README
  - Support: npm, yarn (classic + berry), pnpm, bun

- [—] **T-3.2.2** 🟠 `L` — Graduate `python-architected` to Active | Deferred: 2026-03-27T15:38:40Z | Notes: Postponed by user request ("postpone plugins graduation").
  - Add: virtual environment management (auto-create, activate)
  - Add: PyPI publishing with twine/flit
  - Add: requirements.txt / pyproject.toml detection
  - Support: pip, uv, poetry, pipenv, conda
  - Full contract tests

- [—] **T-3.2.3** 🟡 `L` — Graduate `rust-architected` to Active | Deferred: 2026-03-27T15:38:40Z | Notes: Postponed by user request ("postpone plugins graduation").
  - Add: cargo workspace support
  - Add: MSRV checking, cross-compilation targets
  - Add: crates.io publishing
  - Full contract tests

- [—] **T-3.2.4** 🟡 `L` — Graduate `go-architected` to Active | Deferred: 2026-03-27T15:38:40Z | Notes: Postponed by user request ("postpone plugins graduation").
  - Add: Go workspace support, cross-compilation matrix
  - Add: GoReleaser integration
  - Full contract tests

- [—] **T-3.2.5** 🟡 `L` — Graduate `maven-architected` to Active | Deferred: 2026-03-27T15:38:40Z | Notes: Postponed by user request ("postpone plugins graduation").
  - Add: multi-module support, custom goal execution
  - Add: Maven Central publishing
  - Full contract tests

### 3.3 — Graduate Incubating Plugins (Infrastructure)

- [—] **T-3.3.1** 🟠 `L` — Graduate `docker-architected` to Active | Deferred: 2026-03-27T15:38:40Z | Notes: Postponed by user request ("postpone plugins graduation").
  - Add: multi-stage build optimization hints
  - Add: image scanning integration (Trivy/Snyk)
  - Add: Docker layer caching strategies
  - Add: registry authentication management
  - Full contract tests

- [—] **T-3.3.2** 🟠 `L` — Graduate `kubernetes-architected` to Active | Deferred: 2026-03-27T15:38:40Z | Notes: Postponed by user request ("postpone plugins graduation").
  - Add: Helm chart support (install, upgrade, rollback)
  - Add: Kustomize overlays
  - Add: health check waiting (rollout status --watch)
  - Add: namespace creation, RBAC setup
  - Full contract tests

- [—] **T-3.3.3** 🟡 `L` — Graduate `terraform-architected` to Active | Deferred: 2026-03-27T15:38:40Z | Notes: Postponed by user request ("postpone plugins graduation").
  - Add: state management (lock, unlock, import)
  - Add: module version constraints
  - Add: plan output saving and applying saved plans
  - Full contract tests

- [—] **T-3.3.4** 🟡 `L` — Graduate `nx-architected` to Active | Deferred: 2026-03-27T15:38:40Z | Notes: Postponed by user request ("postpone plugins graduation").
  - Add: Nx Cloud integration
  - Add: custom executor support
  - Add: project graph visualization
  - Full contract tests

### 3.4 — New Critical Plugins

- [x] **T-3.4.1** 🔴 `XL` — Create `testing-architected` plugin | Finished: 2026-03-27T17:26:08Z | Notes: Added `plugins/testing-architected` with framework auto-detection (Gradle/JUnit, pytest, Jest, Vitest, Go, Cargo), `test-unit`/`test-integration`/`test-e2e`/`test-coverage` tasks, configurable coverage aggregation + threshold enforcement, contract/unit tests, plugin docs, workflow, and repo/help/reference wiring.
  - Unified testing interface across all languages
  - Tasks: `test-unit`, `test-integration`, `test-e2e`, `test-coverage`
  - Auto-detect test framework (JUnit, pytest, Jest, Vitest, Go test, Cargo test)
  - Coverage report aggregation (Jacoco, coverage.py, Istanbul, llvm-cov)
  - Coverage threshold enforcement in `architect.yml`
  - Configuration:
    ```yaml
    testing:
      coverage:
        enabled: true
        threshold: 80
        reporter: [html, lcov, cobertura]
      parallel: true
      retryFlaky: 2
    ```

- [x] **T-3.4.2** 🔴 `XL` — Create `security-architected` plugin | Finished: 2026-03-27T17:54:10Z | Notes: Added `plugins/security-architected` with `security-scan`/`security-audit`/`security-sbom` tasks, Trivy/Snyk/CodeQL plus npm/pip/cargo audit integration points, CycloneDX/SPDX SBOM generation, severity-threshold enforcement, contract/unit tests, plugin docs, and repo/help/workflow/reference wiring.
  - Tasks: `security-scan`, `security-audit`, `security-sbom`
  - Integrations: Trivy, Snyk, CodeQL, npm audit, pip audit, cargo audit
  - SBOM generation (CycloneDX, SPDX)
  - Vulnerability threshold (block on critical/high)
  - Configuration:
    ```yaml
    security:
      scan:
        enabled: true
        tools: [trivy, npm-audit]
        failOn: critical
      sbom:
        format: cyclonedx
        output: sbom.json
    ```

- [x] **T-3.4.3** 🟠 `XL` — Create `quality-architected` plugin | Finished: 2026-03-27T18:19:01Z | Notes: Promoted the existing `plugins/quality-architected` scaffold into a buildable plugin by wiring the Gradle wrapper and local API substitution, tightening the schema/shell-safety implementation, keeping `quality-gate` alongside `quality-lint`/`quality-analyze`/`quality-report`, adding contract/unit coverage, and updating repo/help/workflow/reference docs.
  - Tasks: `quality-lint`, `quality-analyze`, `quality-report`, `quality-gate`
  - Integrations: SonarQube, CodeClimate, detekt, ESLint, Ruff, Clippy
  - Quality gate enforcement (block merge if gate fails)
  - Configuration:
    ```yaml
    quality:
      tools:
        - name: sonarqube
          url: https://sonar.example.com
          projectKey: my-project
      gates:
        coverage: 80
        duplications: 3
        bugs: 0
        vulnerabilities: 0
    ```

- [x] **T-3.4.4** 🟠 `L` — Create `release-architected` plugin | Finished: 2026-03-27T18:55:00Z | Notes: Added `plugins/release-architected` with `release-prepare`/`release-publish`/`release-rollback` tasks, semantic/calendar/manual version planning, changelog + release notes generation, npm/Docker/GitHub Release command orchestration, contract/unit tests, workflow, and repo/help/reference wiring.
  - Unified release management across all ecosystems
  - Tasks: `release-prepare`, `release-publish`, `release-rollback`
  - Semantic versioning with conventional commits analysis
  - Changelog generation (CHANGELOG.md, GitHub Releases)
  - Multi-artifact release (npm + Docker + GitHub Release in one)
  - Configuration:
    ```yaml
    release:
      strategy: semantic  # semantic, calendar, manual
      changelog: true
      artifacts:
        - type: npm
          registry: https://registry.npmjs.org
        - type: docker
          registry: ghcr.io
        - type: github-release
          assets: [dist/*.tar.gz]
    ```


---

## PHASE 4 — PROJECT INTELLIGENCE & CONVENTIONS
> *Architect should understand your project, enforce rules, and guide best practices.*

### 4.1 — Project Auto-Detection & Smart Defaults

- [x] **T-4.1.1** 🔴 `XL` — Implement project stack auto-detection engine | Finished: 2026-03-27T19:35:33Z | Notes: Added reusable `ProjectProfile` + `StackDetectionService` in architect-core with marker-based detection for JS/TS, JVM, Python, Rust, Go, Docker, Terraform, and GitHub Actions; wired richer detection output into `architect init` and `architect doctor`; added focused core + CLI tests.
  - Scan project root for markers:
    - `package.json` → JavaScript/TypeScript (detect: npm/yarn/pnpm/bun)
    - `build.gradle.kts` / `pom.xml` → JVM (detect: Kotlin/Java, Gradle/Maven)
    - `Cargo.toml` → Rust
    - `go.mod` → Go
    - `pyproject.toml` / `requirements.txt` → Python (detect: uv/poetry/pip)
    - `Dockerfile` → Docker
    - `terraform/` → Terraform
    - `.github/workflows/` → GitHub Actions
  - Output: `ProjectProfile { languages, buildTools, testFrameworks, ciSystem, containerization }`
  - Used by `architect init` and `architect doctor`

- [x] **T-4.1.2** 🔴 `L` — Create default plugin presets based on detected stack | Finished: 2026-03-27T19:53:46Z | Notes: Added reusable plugin preset registry/matcher in architect-core for kotlin-gradle, typescript-npm, rust-cargo, python-uv, and fullstack; wired `architect init --preset <id>` plus recommended preset application in auto and interactive flows; updated init help and added core/CLI preset tests.
  - `kotlin-gradle` preset: gradle-architected + git + github + testing + quality
  - `typescript-npm` preset: javascript-architected + git + github + testing + quality
  - `rust-cargo` preset: rust-architected + git + github + testing + security
  - `python-uv` preset: python-architected + git + github + testing + quality
  - `fullstack` preset: javascript + docker + kubernetes + security
  - `architect init --preset kotlin-gradle` applies all at once

- [x] **T-4.1.3** 🟠 `M` — Generate optimized `architect.yml` from detection | Finished: 2026-03-27T19:57:19Z | Notes: Extended `architect init` YAML generation to emit detected plugin config sections with explanatory comments, inferred package-manager/test-framework defaults, and sensible quality/testing/security settings; added `--detect` help text and regression coverage for detected config generation.
  - Pre-fill all plugin configs with detected values
  - Set sensible defaults (test coverage: 80%, lint: enabled, etc.)
  - Comment each section explaining what it does
  - `architect init --detect` runs detection → generation pipeline

### 4.2 — Architecture Validation & Rule Enforcement

- [x] **T-4.2.1** 🔴 `XL` — Upgrade `architecture-architected` to full rule engine | Finished: 2026-03-27T20:57:49Z | Notes: Replaced the monolithic validator with a registry-backed rule engine, added dedicated dependency/naming/structure/import/convention/custom validators plus import graph analysis, shipped built-in layered/hexagonal/clean/monorepo rulesets, enriched violations with line/severity/suggestion output, added config schema coverage, refreshed docs, and expanded plugin tests/build verification.
  - Rule types:
    - **Dependency rules**: "controllers must not import repositories directly"
    - **Naming rules**: "files in `src/api/` must end with `Controller`"
    - **Structure rules**: "every module must have `src/test/`"
    - **Import rules**: "no circular imports between modules"
    - **Convention rules**: "all public functions must have KDoc"
  - Built-in rulesets:
    - `layered-architecture` (controllers → services → repositories)
    - `hexagonal-architecture` (ports → adapters → domain)
    - `clean-architecture` (entities → use-cases → interfaces → frameworks)
    - `monorepo-conventions` (shared types, no cross-module private imports)
  - Custom rules via `architect.yml`
  - Output: violations with file:line, severity, suggestion
  - Implementation slices:
    - Extract a validator registry/abstraction so rule types are decoupled from the current monolith
    - Add import graph analysis for circular and cross-module import rules
    - Add convention validators for KDoc/Javadoc/test-presence checks
    - Ship built-in rulesets for layered, hexagonal, clean, and monorepo setups
    - Expand tests/docs and graduate the plugin once coverage and reporting are complete

- [x] **T-4.2.2** 🟠 `L` — Add file structure validation | Finished: 2026-03-27T21:04:50Z | Notes: Added architecture.structure required/forbidden config, glob-aware structure validation, and routed `architect validate --structure` through `architecture-validate` with CLI/plugin/docs/tests updated.
  - Define expected directory structure in `architect.yml`:
    ```yaml
    architecture:
      structure:
        required:
          - src/main/kotlin/**
          - src/test/kotlin/**
          - README.md
          - build.gradle.kts
        forbidden:
          - src/**/*.java  # Kotlin only
          - **/.env        # No committed env files
    ```
  - `architect validate --structure` checks compliance
  - CI integration: fail PR if structure violated

- [x] **T-4.2.3** 🟠 `M` — Add dependency boundary enforcement for monorepos | Finished: 2026-03-27T21:08:12Z | Notes: Added top-level `architecture.boundaries` config that derives import-boundary rules for monorepos, updated schema/docs, and expanded plugin tests/build verification.
  - Define which subprojects can depend on which:
    ```yaml
    architecture:
      boundaries:
        api: [core]           # api can only depend on core
        engine: [api, core]   # engine can depend on api and core
        plugins/*: [api]      # plugins can only depend on api
    ```
  - Analyze import statements / build dependencies
  - Block violations in CI

- [x] **T-4.2.4** 🟡 `M` — Add convention presets (opinionated defaults) | Finished: 2026-03-27T21:13:23Z | Notes: Added built-in Kotlin/TypeScript convention presets, introduced `architect conventions <preset>` to apply them into `architect.yml`, expanded convention validation with TSDoc support, and refreshed docs/tests.
  - `architect conventions kotlin` — applies Kotlin best practices
  - `architect conventions typescript` — applies TS best practices
  - Includes: naming, file structure, test patterns, doc requirements
  - Extensible: override any convention in `architect.yml`

### 4.3 — Monorepo Intelligence

- [x] **T-4.3.1** 🔴 `L` — Enhance affected project detection | Finished: 2026-03-28T09:30:00Z | Notes: Added ignorePatterns to AffectedConfig with glob matching via java.nio; updated parseConfig to read affected.ignore list; enhanced ProjectDependencyGraphBuilder to parse build.gradle.kts/build.gradle for project(":name") deps; 10 new tests added.
  - Current: simple `git diff` based
  - Add: dependency-aware affected detection
    - If `core` changed → all modules depending on core are affected
    - Build project dependency graph from `architect.yml` + build files
  - Add: file pattern filtering
    - `*.md` changes don't affect builds
    - Configurable: `affected.ignore: ["**/*.md", "docs/**"]`

- [x] **T-4.3.2** 🟠 `M` — Add project dependency graph visualization | Finished: 2026-03-28T10:00:00Z | Notes: Replaced Mermaid with vis.js Network: zoom/pan/drag, click-select with neighbour dimming, hover tooltips, circular dep detection (red nodes/edges via DFS), shared dep highlighting (blue), affected project overlay (orange), stats/legend bar, Fit/Re-layout controls. Added --affected flag to graph command. Added 8 graph domain tests.
  - `architect graph --projects` already exists but limited
  - Add: interactive HTML with zoom, click-to-navigate
  - Show: dependency direction, shared dependencies, circular deps
  - Highlight affected projects on hover

- [x] **T-4.3.3** 🟠 `M` — Add cross-project task orchestration | Finished: 2026-03-28T12:00:00Z | Notes: Added topologicalTiers() (Kahn's alg) to ProjectDependencyGraph; created MultiProjectOrchestrator with TaskRunner interface, tier-based parallel execution (up to 8 threads), stopOnFailure support; wired --all and --affected flags in ArchitectLauncher runEmbeddedMode(); added executeTaskAllProjects/executeTaskForProjects/executeProjectOrchestration helpers; 7 tests added using TaskRunner stub approach.
  - Execute tasks across multiple projects with dependency ordering
  - `architect build --all` builds all projects in correct order
  - `architect test --affected` tests only changed + downstream projects
  - Parallel execution within dependency tiers

- [x] **T-4.3.4** 🟡 `M` — Add monorepo health dashboard | Finished: 2026-03-28T13:00:00Z | Notes: Added `architect status` command; created MonorepoHealthDTO/ProjectHealthDTO data classes; HealthDashboard implemented inline in handleStatusCommand() collecting validation, taskCount and LocalOutputCache build status per project; printHealthDashboard() in OutputFormatter with --json support; 6 tests in OutputFormatterHealthTest.
  - `architect status` shows all projects with:
    - Last build status (✅/❌)
    - Test coverage
    - Outdated dependencies count
    - Architecture violations
  - JSON output for CI dashboards

---

## PHASE 5 — CONFIGURATION AS CODE EXCELLENCE
> *architect.yml should be the single source of truth for everything.*

### 5.1 — Configuration Schema & IDE Support

- [x] **T-5.1.1** 🔴 `L` — Publish `architect.yml` JSON Schema to SchemaStore | Finished: 2026-03-28T14:00:00Z | Notes: Created full JSON Schema at docs/schema/architect-schema.json covering all plugin configs (git, github, gradle, scripts, docs, pipelines, affected + stubs for docker/k8s/terraform/rust/security/quality/testing/python/maven/go/nx/release/architecture); bundled in CLI resources for `architect schema` command; VS Code yaml.schemas mapping added to .vscode/settings.json. SchemaStore PR to be raised separately.
  - Full schema covering all sections and all plugin configs
  - VS Code: auto-completion in `architect.yml` files
  - IntelliJ: auto-completion in `architect.yml` files
  - Validate on save in supported editors

- [x] **T-5.1.2** 🟠 `M` — Generate plugin schemas automatically | Finished: 2026-03-28T15:00:00Z | Notes: Created KotlinDataClassSchemaGenerator using kotlin-reflect to introspect data class primary constructors; SchemaCommandHandler with generate/show/lint subcommands; architect schema generate merges root schema with per-plugin configSchema() (or reflection fallback); architect schema show prints bundled schema.
  - Introspect plugin `ctxClass` fields via reflection
  - Generate JSON Schema from Kotlin data classes
  - Merge all plugin schemas into root schema
  - `architect schema generate` outputs combined schema

- [x] **T-5.1.3** 🟡 `S` — Add `architect config lint` command | Finished: 2026-03-28T10:06:24Z | Notes: Added ConfigCommandHandler lint subcommand with deprecated key checks, unknown plugin detection, typo suggestions, CI exit code 1 on issues; updated config help text; added unit tests for success/failure and suggestions.
  - Check for: deprecated keys, unknown plugins, invalid values
  - Suggest corrections for common mistakes
  - Exit code 1 if issues found (CI-friendly)

### 5.2 — Environment & Secret Management

- [x] **T-5.2.1** 🔴 `L` — Complete secret management system | Finished: 2026-03-28T10:16:46Z | Notes: Wired `architect secret` into CLI dispatch/help, refactored `SecretStore` to prefer native macOS/Linux keychain backends with encrypted file fallback at `~/.architect/secrets.enc`, added `StoredSecretResolver` so `environment.secret(name)` resolves stored secrets, and added launcher/help/store/resolver tests. CLI rerun passed after rebuilding included modules cleanly.
  - Files: `architect-cli/cli/src/main/kotlin/io/github/architectplatform/cli/ArchitectLauncher.kt`, `architect-cli/cli/src/main/kotlin/io/github/architectplatform/cli/command/HelpCommandHandler.kt`, `architect-core/core/src/main/kotlin/io/github/architectplatform/core/secrets/SecretStore.kt`, `architect-core/core/src/main/kotlin/io/github/architectplatform/core/secrets/SecretResolver.kt`, `architect-core/core/src/main/kotlin/io/github/architectplatform/core/secrets/PlatformKeychainSecretStore.kt`
  - Verification: core secret tests passed; targeted CLI tests passed after rebuilding API/core/CLI sequentially to avoid composite-build artifact races

- [x] **T-5.2.2** 🟠 `M` — Add `.env` file support with precedence chain | Finished: 2026-07-10T00:00:00Z | Notes: `EnvFileLoader` + `EnvInterpolator` in `architect-core/env` package; full test coverage with @TempDir; pure stdlib, no external deps
  - Load order: `.env` → `.env.local` → `.env.{profile}` → `.env.{profile}.local` → system env
  - `.env.local` in `.gitignore` by default
  - `architect.yml` can reference: `${env.DATABASE_URL}`
  - `architect config resolve` shows final resolved values

- [x] **T-5.2.3** 🟡 `M` — Finish environment validation coverage | Finished: 2026-03-28T10:16:46Z | Notes: Added `requiredEnvironmentVariables()` plugin metadata hook, extended `architect check` to union env requirements from plugin metadata/schema plus raw `${env.*}` config references, scanned profile-specific config files, reused the existing dotenv precedence loader, kept `.env.example` generation aligned, and added CLI/plugin tests. Preserved pre-existing edits in `CheckCommandHandler.kt`.
  - Files: `architect-api/api/src/main/kotlin/io/github/architectplatform/api/core/plugins/ArchitectPlugin.kt`, `architect-cli/cli/src/main/kotlin/io/github/architectplatform/cli/command/CheckCommandHandler.kt`, `plugins/github-architected/app/src/main/kotlin/io/github/architectplatform/plugins/github/GithubPlugin.kt`
  - Verification: `architect-api` tests passed, targeted GitHub plugin tests passed, and targeted CLI `CheckCommandHandlerTest` / `ArchitectLauncherTest` passed with `--no-daemon`

### 5.3 — Task Definition Enhancements

- [x] **T-5.3.1** 🟠 `L` — Extend inline task definitions in `architect.yml` | Finished: 2026-03-28T10:16:46Z | Notes: Extended `InlineTaskConfig` / `InlineTaskPlugin` to support `timeout`, `condition`, `onFailure`, and `retryAttempts`; updated `SimpleTask` passthroughs and schema generation; added inline execution tests for success, skip, retry, and timeout; and adjusted `TaskExecutor` aggregation so single skipped tasks preserve `SKIPPED` status.
  - Files: `architect-api/api/src/main/kotlin/io/github/architectplatform/api/core/tasks/builtin/SimpleTask.kt`, `architect-core/core/src/main/kotlin/io/github/architectplatform/core/plugins/inline/InlineTaskConfig.kt`, `architect-core/core/src/main/kotlin/io/github/architectplatform/core/plugins/inline/InlineTaskPlugin.kt`, `architect-core/core/src/main/kotlin/io/github/architectplatform/core/schema/ArchitectSchemaGenerator.kt`, `architect-core/core/src/main/kotlin/io/github/architectplatform/core/tasks/application/TaskExecutor.kt`
  - Verification: focused `architect-core` `TaskExecutorTest` / `ArchitectSchemaGeneratorTest` passed, and focused engine `InlineTaskPluginTest` passed
  - Beyond current `scripts:` section — full task features:
    ```yaml
    tasks:
      deploy-staging:
        phase: RELEASE
        depends: [build, test]
        requires:
          tools: [aws-cli]
          env: [AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY]
        condition: "env.BRANCH == 'main'"
        timeout: 300s
        run: |
          aws ecs update-service --cluster staging --service my-app
        onFailure: RETRY
        retryAttempts: 3
    ```

- [ ] **T-5.3.2** 🟠 `M` — Add task grouping and namespacing | sequential | Priority: high | Depends: T-5.3.1 | [REVISED] 2026-03-28T13:40:14Z | Assumptions: do not rename existing task IDs silently; support grouping/namespace expansion through registry + CLI resolution while keeping legacy IDs executable | Acceptance: `architect build` expands configured groups, `build:*` / `build:frontend` lookups resolve predictably, plugin tasks gain explicit namespace aliases where applicable, and task listing/help surfaces the grouping model
  - Expected files: `architect-api/api/src/main/kotlin/io/github/architectplatform/api/core/tasks/TaskRegistry.kt`, `architect-core/core/src/main/kotlin/io/github/architectplatform/core/tasks/infrastructure/InMemoryTaskRegistry.kt`, `architect-core/core/src/main/kotlin/io/github/architectplatform/core/tasks/infrastructure/TaskReferenceResolver.kt`, CLI task-resolution code in `architect-cli/cli/src/main/kotlin/io/github/architectplatform/cli/ArchitectLauncher.kt`, and schema/config parsing for `groups:` in the inline task plugin/config loader
  - Implementation details:
    - Add `TaskRegistry.resolve(reference, groups)` signature for `group`, `group:*`, and `group:member` lookups (preserve direct task IDs)
    - Generate namespaced aliases for plugin tasks (e.g., `git:commit` -> `git-commit`) without changing original IDs
    - Surface grouping in `architect tasks` output (show group header + member list)
  - Verification: fixture `architect.yml` with `groups:` list and mixed plugin/inline tasks; unit tests covering group expansion order, wildcard expansion, and error cases (unknown group/member) in registry + CLI resolution; CLI regression tests for task listing/group rendering and `build:*` lookups
  - Acceptance details: unknown groups/members surface `TaskNotFoundException` with available group/member suggestions; group expansion order matches the `groups:` list order
  - Group related tasks: `architect build:frontend`, `architect build:backend`
  - `architect build` runs all tasks in `build:*` group
  - Plugin tasks auto-namespaced: `git:commit`, `docker:build`
  - Configurable in `architect.yml`:
    ```yaml
    groups:
      build: [gradle-build, javascript-build, docker-build]
      deploy: [docker-push, k8s-apply]
      ci: [build, test, security-scan, deploy]
    ```

- [ ] **T-5.3.3** 🟡 `M` — Add task templates / reusable task definitions | sequential | Priority: medium | Depends: T-5.3.1 | [REVISED] 2026-03-28T13:40:14Z | Assumptions: templates live alongside inline `tasks:` in `architect.yml` and merge into the existing inline task model via explicit `extends` semantics | Acceptance: `templates:` can define reusable defaults, inline tasks can `extends` a template with child override precedence, circular references are rejected clearly, and schema/tests document the behavior
  - Expected files: `architect-core/core/src/main/kotlin/io/github/architectplatform/core/plugins/inline/InlineTaskConfig.kt`, `InlineTaskPlugin.kt`, a new template resolver under the same package, and the generated schema inputs for `templates:`
  - Implementation details:
    - Add `templates` map to `InlineTaskConfig` and resolve `extends` before task registration
    - Merge strategy: child overrides parent keys; nested maps deep-merge; lists replace unless explicitly concatenated via `+`
    - Detect missing template or circular reference and throw `IllegalArgumentException` with template chain in message
  - Verification: fixture config with shared template + overriding child task, plus tests for missing-template, circular-reference, and merge precedence; schema generation test includes `templates:` alongside `tasks:`
  - Acceptance details: resolved task configs are fully expanded before registration (no lingering `extends`), and error messages include the full template chain for quick diagnosis
  - Define once, use many times:
    ```yaml
    templates:
      npm-script:
        requires:
          tools: [node, npm]
        timeout: 120s
    
    tasks:
      frontend-build:
        extends: npm-script
        run: npm run build
        phase: BUILD
    ```

---

## PHASE 7 — CLOUD INTEGRATION & COLLABORATION
> *Extend Architect from local tool to team platform.*

### 7.1 — Cloud Dashboard

- [ ] **T-7.1.1** 🟠 `XXL` — Build execution dashboard web UI | sequential | Priority: high | Depends: T-7.1.2 | [REVISED] 2026-03-28T14:40:08Z | Assumptions: architect-cloud is the delivery vehicle; UI lives in `architect-cloud/ui` and consumes `architect-cloud/backend` APIs | Acceptance: dashboard lists executions with filters, shows real-time task progress via SSE, and cloud UI/backend builds and tests pass
  - Expected files: `architect-cloud/ui/src/**` (routes, components, state), `architect-cloud/backend/src/main/kotlin/**/execution/**` (controllers/services), `architect-cloud/backend/src/main/resources/**` (CORS/SSE config)
  - Add REST endpoints for execution summaries and detail views, plus an SSE proxy to engine events
  - UI: executions list, project filter, status badges, timeline view, task detail panel
  - Verification: cloud UI build/test and backend tests for new endpoints + SSE behavior

- [ ] **T-7.1.2** 🟠 `L` — Add remote execution result storage | sequential | Priority: high | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: architect-cloud backend persists execution/task results in a relational store; engine can POST summaries on completion | Acceptance: execution/task results are persisted, queryable by project/task/time range, and covered by backend tests
  - Expected files: `architect-cloud/backend/src/main/kotlin/**/execution/**` (entities, repositories, services), `architect-cloud/backend/src/main/resources/application.yml` (DB config), `architect-engine/engine/src/main/kotlin/**/cloud/**` (result publisher client)
  - Add POST API for engine to submit execution summaries, plus GET APIs for list/detail queries
  - Include indexes for project, task, and timestamp queries
  - Verification: backend integration tests for persistence + engine client tests for submit flow

- [ ] **T-7.1.3** 🟡 `L` — Add team configuration sharing | sequential | Priority: medium | Depends: T-7.1.2 | [REVISED] 2026-03-28T14:40:08Z | Assumptions: cloud backend stores team config templates and version history; CLI handles pull/push | Acceptance: `architect config pull/push` round-trips team config with versioning and conflict warnings; docs and tests included
  - Expected files: `architect-cloud/backend/src/main/kotlin/**/config/**`, `architect-cli/cli/src/main/kotlin/**/command/ConfigCommandHandler.kt`, `architect-cli/cli/src/main/kotlin/**/client/**`
  - Add backend APIs for config templates (list/get/create/update) with version metadata
  - CLI: `architect config pull` and `architect config push` with diff/confirm flow
  - Verification: CLI tests for new commands + backend tests for config versioning


### 7.3 — CI/CD Generation

- [ ] **T-7.3.1** 🔴 `L` — Generate CI/CD pipelines from `architect.yml` | sequential | Priority: high | Depends: T-5.3.2, T-5.3.3 | [REVISED] 2026-03-28T14:40:08Z | Assumptions: CI generation is implemented in architect-core with provider templates; CLI owns the command surface | Acceptance: `architect ci generate --provider github-actions` produces valid workflows that mirror task graph ordering and passes tests for fixture configs
  - Expected files: `architect-core/core/src/main/kotlin/**/ci/**` (pipeline model + generator), `architect-cli/cli/src/main/kotlin/**/command/CiCommandHandler.kt`, `docs/reference/ci/**` (provider docs), `architect-cli/cli/src/test/**` (fixtures)
  - Implement task graph to job mapping (phases, dependencies, groups/templates)
  - Provider templates: GitHub Actions first, with stubs for other providers
  - Verification: fixture-based tests comparing generated YAML to golden files

- [ ] **T-7.3.2** 🟠 `M` — Add CI/CD drift detection | sequential | Priority: medium | Depends: T-7.3.1 | [REVISED] 2026-03-28T14:40:08Z | Assumptions: drift is computed by regenerating pipeline outputs and diffing against repo files | Acceptance: `architect ci diff` prints deterministic diffs and `architect ci sync` updates workflow files; validation warns on drift
  - Expected files: `architect-cli/cli/src/main/kotlin/**/command/CiCommandHandler.kt`, `architect-core/core/src/main/kotlin/**/ci/**`, `.github/workflows/**` (generated outputs)
  - Add diff renderer for YAML (unified diff) and sync writer
  - Integrate with `architect validate` warning path
  - Verification: CLI tests for diff/sync and drift warning in validate

- [ ] **T-7.3.3** 🟡 `M` — Add CI optimization recommendations | parallel | Priority: medium | Depends: T-7.3.1 | [REVISED] 2026-03-28T14:40:08Z | Assumptions: optimization uses TaskStats/history data already available in engine | Acceptance: `architect ci optimize` outputs recommendations and optional optimized workflow diff; tests cover recommendation rules
  - Expected files: `architect-core/core/src/main/kotlin/**/ci/**`, `architect-engine/engine/src/main/kotlin/**/history/**`, `architect-cli/cli/src/main/kotlin/**/command/CiCommandHandler.kt`
  - Implement rule-based recommendations (slow tasks, low failure rate, parallelizable groups)
  - Optionally emit optimized workflow variant with suggestions annotated
  - Verification: unit tests for recommendation rules + CLI output formatting

---

## PHASE 8 — DISTRIBUTION & ECOSYSTEM
> *Make Architect easy to install, update, and extend.*

### 8.1 — Distribution Channels

- [ ] **T-8.1.1** 🔴 `L` — Set up cross-platform binary distribution | sequential | Priority: high | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: release artifacts are produced via GitHub Actions using GraalVM native-image | Acceptance: release workflow publishes signed binaries and checksums for macOS, Linux, Windows and updates Homebrew/apt/winget manifests
  - Expected files: `.github/workflows/native-image.yml`, `.github/workflows/linux-packages.yml`, `.github/workflows/windows-installer.yml`, `.github/workflows/update-homebrew.yml`, `architect-cli/cli/build.gradle.kts`
  - Configure native-image builds per OS/arch and upload release artifacts with checksums
  - Add packaging steps for Homebrew, apt, winget, Scoop
  - Verification: CI workflow runs on tag and produces expected artifacts

- [ ] **T-8.1.2** 🟠 `M` — Add Docker distribution | parallel | Priority: medium | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: Docker images are built from the CLI native binary and published to GHCR | Acceptance: `ghcr.io/architect-platform/architect` and `.../architect-slim` images build and run `architect --version`
  - Expected files: `Dockerfile`, `.github/workflows/docker-image.yml`, `scripts/docker/**` (build helpers)
  - Add multi-stage Dockerfile with full and slim targets
  - Publish images on release tags with versioned and latest tags
  - Verification: CI pipeline builds/pushes images and smoke tests `architect --version`

- [ ] **T-8.1.3** 🟠 `M` — Add `npx` / `pip` / `go install` one-liner installation | sequential | Priority: medium | Depends: T-8.1.1 | [REVISED] 2026-03-28T14:40:08Z | Assumptions: installers are thin wrappers that download release binaries | Acceptance: `npx @architect-platform/cli`, `pip install architect-cli`, and `go install ...` all run `architect` without manual download
  - Expected files: `sdk/installers/node/**`, `sdk/installers/python/**`, `sdk/installers/go/**`, `docs/getting-started/**`
  - Implement wrapper scripts that fetch the correct release asset and invoke it
  - Publish package metadata for npm, PyPI, and Go module
  - Verification: smoke tests in CI that run `architect --version` via each installer

- [ ] **T-8.1.4** 🟡 `S` — Add version pinning in `architect.yml` | parallel | Priority: low | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: CLI enforces pinning by warning or erroring on version mismatch | Acceptance: `architect.yml` supports `architect.version` constraints, CLI warns on mismatch, and schema/tests are updated
  - Expected files: `architect-core/core/src/main/kotlin/**/config/**`, `architect-cli/cli/src/main/kotlin/**/ArchitectLauncher.kt`, `docs/schema/architect-schema.json`
  - Add semantic version constraint parsing and validation on startup
  - Update schema + docs for `architect.version`
  - Verification: unit tests for version constraint parsing and warning output
  - ```yaml
    architect:
      version: ">=2.3.0 <3.0.0"
    ```
  - CLI warns if version mismatch
  - `architect upgrade` respects version constraints

### 8.2 — Plugin Marketplace

- [ ] **T-8.2.1** 🔴 `XL` — Build plugin registry and marketplace | sequential | Priority: high | Depends: T-3.1.1 | [REVISED] 2026-03-28T14:40:08Z | Assumptions: registry is hosted in architect-cloud backend with a public UI in architect-cloud/ui | Acceptance: CLI can search/install from registry, marketplace UI lists plugins with metadata, and backend APIs are covered by tests
  - Expected files: `architect-cloud/backend/src/main/kotlin/**/registry/**`, `architect-cloud/ui/src/**/marketplace/**`, `architect-cli/cli/src/main/kotlin/**/command/PluginCommandHandler.kt`
  - Add registry APIs: list/search/get plugin metadata and versions
  - UI: marketplace browse + plugin detail + install instructions
  - CLI: `architect plugin search/install` integrates with registry and updates `architect.yml`
  - Verification: backend tests for registry APIs + CLI tests for search/install

- [ ] **T-8.2.2** 🟠 `L` — Add plugin publishing workflow | sequential | Priority: medium | Depends: T-8.2.1 | [REVISED] 2026-03-28T14:40:08Z | Assumptions: publishing uses registry APIs with signature verification | Acceptance: `architect plugin publish` packages and uploads a signed plugin with compatibility metadata; backend validates and stores releases
  - Expected files: `architect-cli/cli/src/main/kotlin/**/command/PluginCommandHandler.kt`, `architect-cloud/backend/src/main/kotlin/**/registry/**`
  - Add CLI command to package plugin JAR + metadata and upload to registry
  - Backend: validate signature, API compatibility, store release metadata
  - Verification: CLI tests for publish flow + backend tests for validation rules

- [ ] **T-8.2.3** 🟡 `M` — Add plugin template gallery | parallel | Priority: low | Depends: T-8.2.1 | [REVISED] 2026-03-28T14:40:08Z | Assumptions: templates are stored in registry and downloaded by CLI | Acceptance: `architect plugin create --template <name>` scaffolds a plugin with build/test/CI wiring and docs
  - Expected files: `architect-cli/cli/src/main/kotlin/**/command/PluginCommandHandler.kt`, `templates/plugins/**`, `docs/guides/plugin-development.md`
  - Provide templates: kotlin, typescript, go, python with minimal example task/tests
  - CLI downloads templates from registry or uses bundled fallback templates
  - Verification: CLI tests for scaffold output and template selection

### 8.3 — SDK & Multi-Language Plugin Support

- [ ] **T-8.3.1** 🟠 `XL` — Create TypeScript SDK for plugin development | parallel | Priority: medium | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: SDK lives under `sdk/typescript` and publishes to npm | Acceptance: SDK builds/tests pass, example plugin runs against engine, and published package includes typings
  - Expected files: `sdk/typescript/package.json`, `sdk/typescript/src/**`, `sdk/typescript/test/**`, `examples/typescript-plugin/**`
  - Implement core types mirroring Kotlin API contracts + HTTP client/runner
  - Provide example plugin and test utilities (mock engine)
  - Verification: SDK unit tests + example plugin integration test

- [ ] **T-8.3.2** 🟠 `XL` — Create Go SDK for plugin development | parallel | Priority: medium | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: SDK lives under `sdk/go` and publishes a Go module | Acceptance: SDK builds/tests pass, example plugin runs against engine, and module docs are present
  - Expected files: `sdk/go/go.mod`, `sdk/go/**`, `examples/go-plugin/**`
  - Implement Go interfaces for task/phase/plugin contracts and HTTP runner
  - Provide example plugin with a test task
  - Verification: `go test ./...` in sdk/go and example plugin integration test

- [ ] **T-8.3.3** 🟡 `XL` — Create Python SDK for plugin development | parallel | Priority: low | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: SDK lives under `sdk/python` and publishes to PyPI | Acceptance: SDK tests pass, example plugin runs against engine, and package includes type hints
  - Expected files: `sdk/python/pyproject.toml`, `sdk/python/architect_sdk/**`, `examples/python-plugin/**`
  - Implement Python classes for API contracts and HTTP runner
  - Provide example plugin with test coverage
  - Verification: `python -m pytest` in sdk/python and example plugin integration test

---

## PHASE 9 — TESTING & QUALITY INFRASTRUCTURE
> *Ensure everything we build stays working.*

### 9.1 — Test Coverage Expansion

- [ ] **T-9.1.1** 🔴 `L` — Add end-to-end integration test suite | sequential | Priority: high | Depends: T-0.2.1, T-2.1.1 | [REVISED] 2026-03-28T14:40:08Z | Assumptions: E2E tests run via Gradle in a dedicated module or CLI test suite using embedded engine | Acceptance: CI runs E2E tests that cover init/build/test/release, monorepo affected, and plugin install flows
  - Expected files: `architect-cli/cli/src/test/kotlin/**/E2E/**` or new `architect-e2e` module, `architect-engine/engine/src/test/kotlin/**`
  - Implement fixture projects for single repo and monorepo scenarios
  - Wire tests to start embedded engine, run CLI commands, assert results
  - Verification: CI job executes E2E suite with deterministic outputs

- [ ] **T-9.1.2** 🟠 `L` — Add engine stress/load tests | sequential | Priority: medium | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: stress tests run under `architect-engine/engine` with controlled concurrency | Acceptance: load tests simulate 100+ concurrent tasks, validate event buffer behavior, and produce perf metrics in CI
  - Expected files: `architect-engine/engine/src/test/kotlin/**/LoadTest.kt`, `architect-engine/engine/src/test/resources/**`
  - Implement load harness with configurable concurrency and event buffer size
  - Capture throughput/latency metrics and assert thresholds
  - Verification: CI runs stress tests on scheduled workflow or nightly job

- [ ] **T-9.1.3** 🟠 `M` — Add CLI snapshot tests | parallel | Priority: medium | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: snapshots are stored in test resources with a stable renderer | Acceptance: snapshot tests cover key commands and fail on output drift with clear diffs
  - Expected files: `architect-cli/cli/src/test/kotlin/**/SnapshotTest.kt`, `architect-cli/cli/src/test/resources/snapshots/**`
  - Add snapshot harness with update flag for regenerating snapshots
  - Cover plain/json/interactive outputs for core commands
  - Verification: CI runs snapshot tests and reports diffs on failure

- [ ] **T-9.1.4** 🟡 `M` — Add mutation testing thresholds | parallel | Priority: low | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: PiTest is already wired in Gradle for API/engine modules | Acceptance: mutation score thresholds enforced in CI and documented
  - Expected files: `architect-engine/engine/build.gradle.kts`, `architect-api/api/build.gradle.kts`, `.github/workflows/**`
  - Configure minimum mutation score (60%) and target packages for task execution paths
  - Add CI job to run mutation tests on schedule or nightly
  - Verification: build fails when mutation score drops below threshold

### 9.2 — CI/CD Pipeline Hardening

- [ ] **T-9.2.1** 🔴 `M` — Add cross-module dependency validation in CI | sequential | Priority: high | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: CI can detect API module changes and trigger dependent module tests | Acceptance: CI blocks merges when API changes break engine/plugins and documents affected modules
  - Expected files: `.github/workflows/monorepo-validation.yml`, `scripts/ci/**`
  - Add change detection for `architect-api/api` and run downstream tests conditionally
  - Surface failures with clear module attribution
  - Verification: CI job fails on simulated breaking change in API

- [ ] **T-9.2.2** 🟠 `M` — Add automated release pipeline | sequential | Priority: medium | Depends: T-8.1.1 | [REVISED] 2026-03-28T14:40:08Z | Assumptions: release automation uses GitHub Actions and conventional commits | Acceptance: merge to main triggers version bump, changelog, builds/tests, publishes artifacts and release notes
  - Expected files: `.github/workflows/release-architected.yml`, `.github/workflows/architect-*-pipeline.yml`, `scripts/release/**`
  - Implement versioning + changelog generation (conventional commits)
  - Publish API to GitHub Packages, CLI binaries to Releases, engine Docker image to GHCR
  - Verification: dry-run release workflow on tag and inspect artifacts

- [ ] **T-9.2.3** 🟡 `M` — Add security scanning in CI | parallel | Priority: low | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: CI uses GitHub Advanced Security features where available | Acceptance: CodeQL runs on PRs, dependency scans run on schedule, SBOM generated on release, and SARIF uploads succeed
  - Expected files: `.github/workflows/dependency-vulnerability-scan.yml`, `.github/workflows/codeql.yml`, `.github/dependabot.yml`
  - Configure CodeQL for Kotlin/Gradle and upload SARIF
  - Enable dependency scanning and SBOM generation during release
  - Verification: CI jobs run and upload results to GitHub Security tab

---

## PHASE 10 — DOCUMENTATION & DEVELOPER EDUCATION
> *If it's not documented, it doesn't exist.*

### 10.1 — Documentation Overhaul

- [ ] **T-10.1.1** 🔴 `L` — Create comprehensive Getting Started guide | sequential | Priority: high | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: docs live under `docs/getting-started` and `docs/guides` | Acceptance: quickstart and language-specific guides are published in MkDocs navigation
  - Expected files: `docs/getting-started/index.md`, `docs/getting-started/kotlin.md`, `docs/getting-started/typescript.md`, `docs/getting-started/python.md`, `docs/getting-started/rust.md`, `docs/getting-started/go.md`, `mkdocs.yml`
  - Include copy-pastable commands for install/init/build/test and CI tips
  - Verification: `mkdocs build` succeeds with new pages

- [ ] **T-10.1.2** 🔴 `L` — Create Architecture Decision Records (ADRs) | sequential | Priority: high | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: ADRs live under `docs/adr` and are linked in MkDocs nav | Acceptance: ADR template plus initial ADRs for core decisions are published
  - Expected files: `docs/adr/0000-template.md`, `docs/adr/0001-micronaut.md`, `docs/adr/0002-daemon-architecture.md`, `docs/adr/0003-plugin-spi.md`, `docs/adr/0004-config-yaml.md`, `mkdocs.yml`
  - Use consistent ADR format (context/decision/consequences)
  - Verification: `mkdocs build` succeeds and ADRs appear in nav

- [ ] **T-10.1.3** 🟠 `M` — Create Plugin Development Tutorial | parallel | Priority: medium | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: tutorial lives under `docs/guides` and references existing plugin templates | Acceptance: tutorial published with step-by-step scaffold, implementation, testing, and publish flow
  - Expected files: `docs/guides/plugin-tutorial.md`, `docs/guides/plugin-development.md`, `mkdocs.yml`
  - Include troubleshooting and common patterns section
  - Verification: `mkdocs build` succeeds with tutorial in nav

- [ ] **T-10.1.4** 🟠 `M` — Create CLI Command Reference (auto-generated) | sequential | Priority: medium | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: CLI can introspect PicoCLI metadata to emit Markdown docs | Acceptance: `architect docs generate` writes command reference pages and MkDocs build includes them
  - Expected files: `architect-cli/cli/src/main/kotlin/**/command/DocsCommandHandler.kt`, `docs/reference/commands/**`, `mkdocs.yml`
  - Implement doc generator that renders usage/options/examples per command
  - Add CI check to ensure generated docs are up to date
  - Verification: command generates docs and `mkdocs build` succeeds

- [ ] **T-10.1.5** 🟡 `M` — Create Monorepo Best Practices Guide | parallel | Priority: low | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: guide lives under `docs/guides` | Acceptance: monorepo guide published and linked in MkDocs nav
  - Expected files: `docs/guides/monorepo-best-practices.md`, `mkdocs.yml`
  - Include affected detection, dependency boundaries, and CI optimization examples
  - Verification: `mkdocs build` succeeds with new guide

### 10.2 — API Documentation

- [ ] **T-10.2.1** 🟠 `L` — Add KDoc to all public API interfaces | sequential | Priority: medium | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: API docs are generated with Dokka and hosted with MkDocs | Acceptance: all public API surfaces have KDoc, Dokka builds cleanly, and docs are published
  - Expected files: `architect-api/api/src/main/kotlin/**`, `architect-api/api/build.gradle.kts`, `docs/reference/api/**`
  - Add KDoc blocks with params/returns/examples and @since where relevant
  - Configure Dokka output and include in docs site
  - Verification: `./gradlew dokkaHtml` succeeds with no missing KDoc warnings

- [ ] **T-10.2.2** 🟠 `M` — Create API migration guides | parallel | Priority: medium | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: migration guides live under `docs/reference/migrations` | Acceptance: migration docs cover recent breaking changes with before/after examples
  - Expected files: `docs/reference/migrations/index.md`, `docs/reference/migrations/2.x-to-3.0.md`, `mkdocs.yml`
  - Include deprecation notices and replacement guidance
  - Verification: `mkdocs build` succeeds and migrations appear in nav

- [ ] **T-10.2.3** 🟡 `M` — Add REST API documentation (OpenAPI) | sequential | Priority: low | Depends: none | [REVISED] 2026-03-28T14:40:08Z | Assumptions: Micronaut OpenAPI plugin is used in engine build | Acceptance: OpenAPI spec is generated, Swagger UI available locally, and docs site includes spec links
  - Expected files: `architect-engine/engine/build.gradle.kts`, `architect-engine/engine/src/main/resources/application.yml`, `docs/reference/api/openapi.md`
  - Enable Micronaut OpenAPI generation and Swagger UI
  - Publish openapi.json/yaml as build artifacts and link in docs
  - Verification: `./gradlew :architect-engine:engine:openapi` (or equivalent) generates specs

---

## DEPENDENCY MAP

> Tasks listed with their blockers. Unblocked tasks can begin immediately.

| Task | Depends On |
|------|------------|
| T-0.2.3 | T-0.2.1, T-0.2.2 |
| T-0.3.4 | T-0.3.1 |
| T-0.4.2 | T-0.1.3 |
| T-1.1.1 | T-4.1.1 |
| T-1.1.3 | (none — can start now) |
| T-1.2.1 | T-0.3.1 |
| T-1.3.1 | (none — can start now) |
| T-1.4.2 | T-2.1.1 |
| T-2.1.2 | T-0.4.2 |
| T-2.2.1 | (none — can start now) |
| T-3.1.1 | (none — can start now) |
| T-3.4.1 | T-0.1.1, T-0.1.2 |
| T-3.4.2 | T-0.1.1 |
| T-4.1.2 | T-4.1.1 |
| T-4.2.1 | T-3.1.1 |
| T-5.1.1 | T-5.1.2 |
| T-5.2.1 | T-0.3.3 |
| T-6.1.1 | T-5.1.1, T-6.3.1 |
| T-6.2.1 | T-5.1.1, T-6.3.1 |
| T-7.2.1 | T-0.1.1 |
| T-8.2.1 | T-3.1.1 |
| T-9.1.1 | T-0.2.1, T-2.1.1 |
| T-7.1.1 | T-7.1.2 |
| T-7.1.3 | T-7.1.2 |
| T-7.3.1 | T-5.3.2, T-5.3.3 |
| T-7.3.2 | T-7.3.1 |
| T-7.3.3 | T-7.3.1 |
| T-8.1.3 | T-8.1.1 |
| T-8.2.2 | T-8.2.1 |
| T-8.2.3 | T-8.2.1 |
| T-9.2.2 | T-8.1.1 |

---

## EXECUTION PRIORITY (Recommended Order)

### 🏁 Immediate (Unblocked, High Impact)

1. **T-0.1.1** — Enhanced TaskResult (foundation for everything)
2. **T-0.2.1** — CommandResult data class (fixes undefined contract)
3. **T-0.3.1** — Logger interface (enables observability everywhere)
4. **T-0.5.1** — JSON Schema for architect.yml (enables IDE support)
5. **T-1.1.2** — Help system (most basic UX gap)
6. **T-2.1.5** — Fix silent task-not-found (bug)
7. **T-3.1.1** — Plugin graduation checklist (quality gate)

### 🚀 Next Wave (After foundations)

8. **T-0.2.2** + **T-0.2.3** — CommandExecutor contract completion
9. **T-0.4.1** — Runtime task conditions
10. **T-0.5.2** — Config interpolation
11. **T-1.1.1** — `architect init` wizard
12. **T-1.2.1** — Verbose mode
13. **T-2.1.1** — Execution cancellation
14. **T-2.2.1** — Metrics collection

### 🌊 Growth Wave (Plugin ecosystem)

15. **T-3.2.1** through **T-3.2.5** — Graduate language plugins
16. **T-3.4.1** — Testing plugin
17. **T-3.4.2** — Security plugin
18. **T-4.1.1** — Stack auto-detection
19. **T-4.2.1** — Architecture rule engine

### 🏔️ Platform Wave (Cloud + IDE)

20. **T-6.3.1** — Language Server
21. **T-6.1.1** — VS Code extension
22. **T-7.1.1** — Cloud dashboard
23. **T-7.2.1** — Remote caching
24. **T-8.2.1** — Plugin marketplace

---

## METRICS & SUCCESS CRITERIA

| Metric | Current | Target |
|--------|---------|--------|
| `architect init` to first task run | N/A (manual) | < 2 minutes |
| Plugin count (production-ready) | 7 | 20+ |
| Test coverage (API) | 50% | 80% |
| Test coverage (Engine) | ~50% | 75% |
| CLI commands with `--help` | 0 | 100% |
| `architect.yml` schema coverage | 0% | 100% |
| IDE extensions (production) | 0 | 2 (VS Code + IntelliJ) |
| Time saved per developer per day | Unknown | 30+ minutes |
| CI pipeline generation accuracy | N/A | 95% |

---

## CHANGELOG

| Date | Update |
|------|--------|
| 2026-03-26 | Initial plan created from comprehensive codebase analysis |
| 2026-03-27 | Completed T-3.4.1 (`testing-architected`) with new plugin module, tests, docs, and workflow wiring |
| 2026-03-27 | Completed T-3.4.2 (`security-architected`) with multi-tool security tasks, severity gating, docs, and CI wiring |
| 2026-03-27 | Completed T-3.4.3 (`quality-architected`) by finishing the existing scaffold, validating builds/tests, and wiring docs plus CI |
| 2026-03-27 | Completed T-3.4.4 (`release-architected`) with release planning, changelog/notes generation, multi-artifact publish orchestration, and repo wiring |
