# ARCHITECT PLATFORM — MASTER IMPROVEMENT PLAN

> **Vision**: Make Architect the single entrypoint for every developer workflow — from code to cloud. Convention over configuration. Sensible defaults. Infinite extensibility.
>
> **Current State**: 6.5/10 — Strong core API and engine, but fragmented DX, incomplete plugins, missing observability, and no unified "just works" experience.
>
> **Target State**: 9/10 — Zero-config project setup, rich interactive CLI, full lifecycle coverage, plugin marketplace, cloud dashboard, IDE-native integration.

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

- [ ] **T-0.4.3** 🟡 `S` — Add `Task.timeout(): Duration?` for per-task timeouts
  - Overrides global `executor.timeout-seconds`
  - Engine cancels task if exceeded, returns `TaskResult.failure("Timeout")`

### 0.5 — Configuration & Schema Validation
> `architect.yml` has no schema enforcement. Typos silently fail.

- [ ] **T-0.5.1** 🔴 `L` — Define JSON Schema for `architect.yml` root structure
  - Validate: `project`, `plugins`, `tasks`, `scripts`, `pipelines` sections
  - Publish schema to SchemaStore for IDE auto-completion

- [ ] **T-0.5.2** 🟠 `M` — Add config interpolation support (`${env.VAR}`, `${project.name}`)
  - Resolve environment variables and project properties in YAML values
  - Support default values: `${env.PORT:8080}`

- [ ] **T-0.5.3** 🟠 `M` — Add config profiles/environments (`architect.yml` + `architect.ci.yml` overlay)
  - `--env ci` merges `architect.ci.yml` over `architect.yml`
  - Supports: `architect.staging.yml`, `architect.production.yml`

- [ ] **T-0.5.4** 🟡 `S` — Add config inheritance for monorepos
  - Child `architect.yml` inherits parent plugin config
  - `inherit: true` flag per plugin section
  - Reduces duplication in large monorepos

---

## PHASE 1 — CLI DEVELOPER EXPERIENCE REVOLUTION
> *Transform the CLI from a task executor into an intelligent developer companion.*

### 1.1 — Interactive Mode & Smart Defaults
> The CLI has zero interactivity. No menus, no prompts, no guidance.

- [ ] **T-1.1.1** 🔴 `XL` — Implement `architect init` interactive project scaffolding wizard
  - Detect existing project type (package.json, build.gradle, Cargo.toml, etc.)
  - Auto-suggest plugins based on detected stack
  - Generate `architect.yml` with sensible defaults
  - Prompt for project name, description, plugins to enable
  - Support `--yes` flag for non-interactive mode with auto-detected defaults

- [ ] **T-1.1.2** 🔴 `L` — Add `architect help` and per-command `--help` with examples
  - Structured help with usage, description, examples, related commands
  - `architect help <command>` shows detailed docs
  - `architect help tasks` explains the task system
  - `architect help plugins` explains plugin ecosystem

- [ ] **T-1.1.3** 🟠 `L` — Implement interactive task selector when no task specified
  - `architect` with no args shows categorized task list (grouped by phase)
  - Arrow key navigation, fuzzy search, Enter to execute
  - Show task description, phase, dependencies inline
  - Fallback to plain list in CI mode

- [ ] **T-1.1.4** 🟠 `M` — Add `architect doctor` diagnostic command
  - Check: engine running, plugins loaded, config valid, tools available, versions compatible
  - Output: checklist with ✅/❌ per check + remediation hints
  - `architect doctor --fix` attempts auto-remediation (install engine, fix config)

- [ ] **T-1.1.5** 🟡 `M` — Add `architect config` subcommand for config management
  - `architect config show` — display resolved configuration (merged profiles)
  - `architect config set <key> <value>` — modify architect.yml programmatically
  - `architect config get <key>` — read specific value
  - `architect config diff` — show diff between profiles
  - `architect config validate` — validate against schemas

### 1.2 — Output & Debugging Experience
> No verbose mode, no output persistence, limited debugging.

- [ ] **T-1.2.1** 🔴 `M` — Add `--verbose` / `-vvv` graduated verbosity levels
  - Level 0 (default): summary output only
  - Level 1 (`-v`): task names + durations
  - Level 2 (`-vv`): command details + stdout
  - Level 3 (`-vvv`): full debug (HTTP calls, plugin loading, config resolution)

- [ ] **T-1.2.2** 🟠 `M` — Add `--output <file>` and `--tee` flags for output persistence
  - Save execution output to file while displaying on terminal
  - JSON mode: structured execution log with timestamps
  - Useful for CI artifact collection

- [ ] **T-1.2.3** 🟠 `S` — Add `--dry-run` flag for all task executions
  - Show what would execute without actually running
  - Display: task order, commands, environment, working directories
  - Helps debug pipeline issues before committing

- [ ] **T-1.2.4** 🟡 `S` — Add `--timing` flag to show task execution breakdown
  - Waterfall view: which tasks ran in parallel, durations, critical path
  - ASCII-art timeline in terminal, HTML in `--json` mode

- [ ] **T-1.2.5** 🟡 `S` — Add command aliasing support
  - `architect.yml` section for aliases: `aliases: { b: build, t: test, bt: "build test" }`
  - `architect b` expands to `architect build`

### 1.3 — Shell Integration & Completions
> Static completions only. No dynamic task/project name completion.

- [ ] **T-1.3.1** 🟠 `L` — Implement dynamic shell completion for task names
  - Query engine (or parse architect.yml) for available tasks
  - Complete `architect <TAB>` with actual task names
  - Complete `architect --filter <TAB>` with phase names

- [ ] **T-1.3.2** 🟠 `M` — Add project name completion for multi-project commands
  - `architect --affected <TAB>` completes project names
  - `architect history <TAB>` completes from known projects

- [ ] **T-1.3.3** 🟡 `S` — Add `architect completion install` auto-installer
  - Detect shell (bash/zsh/fish), install completion script to correct location
  - Add to `.bashrc`, `.zshrc`, or `~/.config/fish/completions/`
  - Idempotent — safe to run multiple times

### 1.4 — Execution UX Improvements
> Progress rendering is basic. No cancellation, no retry, no parallel visibility.

- [ ] **T-1.4.1** 🟠 `L` — Implement rich progress rendering with parallel task visualization
  - Show multiple concurrent tasks with individual progress
  - Spinner per running task, checkmark on complete, X on failure
  - Collapse completed tasks to one line, expand failures
  - Respect terminal width for truncation

- [ ] **T-1.4.2** 🟠 `M` — Add Ctrl+C graceful cancellation with cleanup
  - First Ctrl+C: graceful stop (finish current task, skip remaining)
  - Second Ctrl+C: force kill
  - Display: "Cancelling... (press Ctrl+C again to force)"
  - Engine must support execution cancellation API

- [ ] **T-1.4.3** 🟡 `M` — Add `architect retry` to re-run last failed execution
  - Read from history, re-execute same task with same args
  - `architect retry --from <task>` resumes from specific failed task
  - Useful for flaky tests or transient network failures

- [ ] **T-1.4.4** 🟡 `S` — Add `--parallel <N>` flag to control task parallelism
  - Override engine's default parallel execution behavior
  - `--parallel 1` for sequential debugging
  - `--parallel 0` for unlimited (use all cores)

---

## PHASE 2 — ENGINE HARDENING & EXECUTION INTELLIGENCE
> *Make the engine production-grade: resilient, observable, and intelligent.*

### 2.1 — Execution Lifecycle Improvements

- [ ] **T-2.1.1** 🔴 `L` — Implement execution cancellation API
  - `DELETE /api/executions/{executionId}` — cancel running execution
  - Propagate cancellation to running `BashCommandExecutor` processes
  - Emit `execution.cancelled` event
  - CLI hooks into this via Ctrl+C handler

- [ ] **T-2.1.2** 🔴 `M` — Add task retry logic with configurable policies
  - Respect `Task.onFailure()` strategy from API
  - Configurable: `architect.engine.retry.max-attempts`, `architect.engine.retry.backoff-ms`
  - Exponential backoff with jitter for retries
  - Retry events emitted for monitoring

- [ ] **T-2.1.3** 🟠 `M` — Add resource limits for task execution
  - `architect.engine.executor.max-concurrent-tasks: 4`
  - `architect.engine.executor.memory-limit-mb: 512` (per task)
  - Semaphore-based concurrency control
  - Reject excess tasks with `429 Too Many Requests`

- [ ] **T-2.1.4** 🟠 `M` — Add execution timeout at the execution level (not just task)
  - `architect.engine.executor.execution-timeout-seconds: 1800`
  - Cancels entire execution if wall-clock time exceeded
  - Prevents runaway monorepo executions

- [ ] **T-2.1.5** 🟡 `S` — Fix "task not found silently returns success" behavior
  - Currently questionable: missing tasks don't fail
  - Change to: throw `TaskNotFoundException` with helpful message
  - Suggest similar task names (Levenshtein distance)

### 2.2 — Observability & Metrics

- [ ] **T-2.2.1** 🔴 `L` — Add structured metrics collection
  - Task execution duration, success/failure rates, cache hit ratio
  - Expose via `GET /api/metrics` (Prometheus format)
  - Micrometer integration with Micronaut
  - Counters: `architect.tasks.executed`, `architect.tasks.failed`
  - Histograms: `architect.tasks.duration`, `architect.cache.lookup.duration`

- [ ] **T-2.2.2** 🟠 `M` — Add health check endpoint
  - `GET /api/health` — engine health with subsystem checks
  - Checks: plugin loader, project repository, event system, cloud connectivity
  - Returns: `{ status: "UP/DOWN", checks: [...] }`
  - Enable Micronaut management endpoints

- [ ] **T-2.2.3** 🟠 `M` — Add execution audit log
  - Persist: who ran what, when, with what args, from where
  - `GET /api/audit` — query audit records
  - Include: user, host, project, task, args, result, duration
  - Configurable retention: `architect.engine.audit.retention-days: 30`

- [ ] **T-2.2.4** 🟡 `M` — Add task performance profiling
  - Track per-task: avg duration, p50/p95/p99, trend (improving/degrading)
  - `GET /api/projects/{name}/tasks/{task}/stats`
  - CLI: `architect stats <task>` shows performance history
  - Alert when task regresses significantly

### 2.3 — Plugin Loading Hardening

- [ ] **T-2.3.1** 🔴 `M` — Add plugin version conflict resolution
  - Detect when multiple plugins require different versions of same dependency
  - Strategy: newest wins, with warning
  - `architect validate` reports version conflicts

- [ ] **T-2.3.2** 🟠 `L` — Add plugin dependency graph
  - Plugins can declare dependencies on other plugins
  - `ArchitectPlugin` interface: `fun dependencies(): List<String>` (plugin IDs)
  - Engine loads plugins in dependency order
  - Circular plugin dependency detection

- [ ] **T-2.3.3** 🟠 `M` — Add plugin sandboxing (security boundaries)
  - Plugins declare required permissions in `configSchema()`
  - Engine validates actual usage against declared permissions
  - Log violations; optionally block in strict mode

- [ ] **T-2.3.4** 🟡 `M` — Add automatic plugin update checking
  - On `architect tasks` or startup, check plugin versions against registry
  - `architect plugin outdated` lists plugins with available updates
  - `architect plugin update [--all]` updates to latest compatible versions

### 2.4 — Event System Enhancement

- [ ] **T-2.4.1** 🟠 `M` — Add typed event hierarchy
  - `TaskStartedEvent`, `TaskCompletedEvent`, `TaskFailedEvent`, `TaskOutputEvent`
  - `ExecutionStartedEvent`, `ExecutionCompletedEvent`
  - `PluginLoadedEvent`, `ProjectRegisteredEvent`
  - Replace `Map<String, Any>` with typed events in SSE stream

- [ ] **T-2.4.2** 🟠 `M` — Add event persistence for replay
  - Store events in append-only log per execution
  - `GET /api/executions/{id}/events?from=0` — replay from beginning
  - Enables: post-mortem debugging, cloud sync, UI replay

- [ ] **T-2.4.3** 🟡 `S` — Add event buffer overflow monitoring
  - Current `DROP_OLDEST` strategy silently loses events
  - Add metric: `architect.events.dropped`
  - Log warning when buffer > 80% capacity
  - Configure: `architect.engine.events.overflow-strategy: BLOCK | DROP_OLDEST | EXPAND`

---

## PHASE 3 — PLUGIN ECOSYSTEM MATURATION
> *Graduate incubating plugins to production. Fill critical ecosystem gaps.*

### 3.1 — Plugin Standard Enforcement

- [ ] **T-3.1.1** 🔴 `L` — Create plugin graduation checklist and automate enforcement
  - Required for "Active" status:
    - ✅ Contract tests (`ArchitectPluginContractTestSuite`) passing
    - ✅ README with configuration examples
    - ✅ STATUS.md with maturity declaration
    - ✅ `configSchema()` returning valid JSON Schema
    - ✅ Error handling for all known failure modes
    - ✅ At least 60% test coverage
  - CI job validates all plugins against checklist
  - Block merge if active plugin drops below standard

- [ ] **T-3.1.2** 🟠 `M` — Add `architect plugin test` command
  - Runs contract test suite against any plugin JAR
  - Validates SPI wiring, schema, task registration
  - Useful for plugin developers before publishing

- [ ] **T-3.1.3** 🟠 `M` — Create plugin development guide documentation
  - Step-by-step: scaffold → implement → test → publish
  - Template project with best practices
  - Document all API contracts with examples
  - Add to MkDocs site under "Plugin Development"

### 3.2 — Graduate Incubating Plugins (Language Ecosystems)

- [ ] **T-3.2.1** 🟠 `L` — Graduate `javascript-architected` to Active
  - Add: monorepo workspace detection, package publishing, version management
  - Add: lockfile validation, security audit (`npm audit`)
  - Full contract tests, comprehensive README
  - Support: npm, yarn (classic + berry), pnpm, bun

- [ ] **T-3.2.2** 🟠 `L` — Graduate `python-architected` to Active
  - Add: virtual environment management (auto-create, activate)
  - Add: PyPI publishing with twine/flit
  - Add: requirements.txt / pyproject.toml detection
  - Support: pip, uv, poetry, pipenv, conda
  - Full contract tests

- [ ] **T-3.2.3** 🟡 `L` — Graduate `rust-architected` to Active
  - Add: cargo workspace support
  - Add: MSRV checking, cross-compilation targets
  - Add: crates.io publishing
  - Full contract tests

- [ ] **T-3.2.4** 🟡 `L` — Graduate `go-architected` to Active
  - Add: Go workspace support, cross-compilation matrix
  - Add: GoReleaser integration
  - Full contract tests

- [ ] **T-3.2.5** 🟡 `L` — Graduate `maven-architected` to Active
  - Add: multi-module support, custom goal execution
  - Add: Maven Central publishing
  - Full contract tests

### 3.3 — Graduate Incubating Plugins (Infrastructure)

- [ ] **T-3.3.1** 🟠 `L` — Graduate `docker-architected` to Active
  - Add: multi-stage build optimization hints
  - Add: image scanning integration (Trivy/Snyk)
  - Add: Docker layer caching strategies
  - Add: registry authentication management
  - Full contract tests

- [ ] **T-3.3.2** 🟠 `L` — Graduate `kubernetes-architected` to Active
  - Add: Helm chart support (install, upgrade, rollback)
  - Add: Kustomize overlays
  - Add: health check waiting (rollout status --watch)
  - Add: namespace creation, RBAC setup
  - Full contract tests

- [ ] **T-3.3.3** 🟡 `L` — Graduate `terraform-architected` to Active
  - Add: state management (lock, unlock, import)
  - Add: module version constraints
  - Add: plan output saving and applying saved plans
  - Full contract tests

- [ ] **T-3.3.4** 🟡 `L` — Graduate `nx-architected` to Active
  - Add: Nx Cloud integration
  - Add: custom executor support
  - Add: project graph visualization
  - Full contract tests

### 3.4 — New Critical Plugins

- [ ] **T-3.4.1** 🔴 `XL` — Create `testing-architected` plugin
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

- [ ] **T-3.4.2** 🔴 `XL` — Create `security-architected` plugin
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

- [ ] **T-3.4.3** 🟠 `XL` — Create `quality-architected` plugin
  - Tasks: `quality-lint`, `quality-analyze`, `quality-report`
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

- [ ] **T-3.4.4** 🟠 `L` — Create `release-architected` plugin
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

- [ ] **T-3.4.5** 🟡 `L` — Create `database-architected` plugin
  - Tasks: `db-migrate`, `db-rollback`, `db-status`, `db-seed`
  - Integrations: Flyway, Liquibase, Prisma, Alembic, golang-migrate
  - Configuration:
    ```yaml
    database:
      tool: flyway
      url: ${env.DATABASE_URL}
      locations: db/migrations
    ```

- [ ] **T-3.4.6** 🟡 `L` — Create `cloud-deploy-architected` plugin
  - Unified cloud deployment abstraction
  - Tasks: `deploy`, `rollback`, `status`, `logs`
  - Providers: AWS (ECS, Lambda, S3), GCP (Cloud Run, GKE), Azure (App Service, AKS)
  - Configuration:
    ```yaml
    deploy:
      provider: aws
      service: ecs
      cluster: production
      taskDefinition: my-app
      desiredCount: 2
    ```

---

## PHASE 4 — PROJECT INTELLIGENCE & CONVENTIONS
> *Architect should understand your project, enforce rules, and guide best practices.*

### 4.1 — Project Auto-Detection & Smart Defaults

- [ ] **T-4.1.1** 🔴 `XL` — Implement project stack auto-detection engine
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

- [ ] **T-4.1.2** 🔴 `L` — Create default plugin presets based on detected stack
  - `kotlin-gradle` preset: gradle-architected + git + github + testing + quality
  - `typescript-npm` preset: javascript-architected + git + github + testing + quality
  - `rust-cargo` preset: rust-architected + git + github + testing + security
  - `python-uv` preset: python-architected + git + github + testing + quality
  - `fullstack` preset: javascript + docker + kubernetes + database + security
  - `architect init --preset kotlin-gradle` applies all at once

- [ ] **T-4.1.3** 🟠 `M` — Generate optimized `architect.yml` from detection
  - Pre-fill all plugin configs with detected values
  - Set sensible defaults (test coverage: 80%, lint: enabled, etc.)
  - Comment each section explaining what it does
  - `architect init --detect` runs detection → generation pipeline

### 4.2 — Architecture Validation & Rule Enforcement

- [ ] **T-4.2.1** 🔴 `XL` — Upgrade `architecture-architected` to full rule engine
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

- [ ] **T-4.2.2** 🟠 `L` — Add file structure validation
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

- [ ] **T-4.2.3** 🟠 `M` — Add dependency boundary enforcement for monorepos
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

- [ ] **T-4.2.4** 🟡 `M` — Add convention presets (opinionated defaults)
  - `architect conventions kotlin` — applies Kotlin best practices
  - `architect conventions typescript` — applies TS best practices
  - Includes: naming, file structure, test patterns, doc requirements
  - Extensible: override any convention in `architect.yml`

### 4.3 — Monorepo Intelligence

- [ ] **T-4.3.1** 🔴 `L` — Enhance affected project detection
  - Current: simple `git diff` based
  - Add: dependency-aware affected detection
    - If `core` changed → all modules depending on core are affected
    - Build project dependency graph from `architect.yml` + build files
  - Add: file pattern filtering
    - `*.md` changes don't affect builds
    - Configurable: `affected.ignore: ["**/*.md", "docs/**"]`

- [ ] **T-4.3.2** 🟠 `M` — Add project dependency graph visualization
  - `architect graph --projects` already exists but limited
  - Add: interactive HTML with zoom, click-to-navigate
  - Show: dependency direction, shared dependencies, circular deps
  - Highlight affected projects on hover

- [ ] **T-4.3.3** 🟠 `M` — Add cross-project task orchestration
  - Execute tasks across multiple projects with dependency ordering
  - `architect build --all` builds all projects in correct order
  - `architect test --affected` tests only changed + downstream projects
  - Parallel execution within dependency tiers

- [ ] **T-4.3.4** 🟡 `M` — Add monorepo health dashboard
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

- [ ] **T-5.1.1** 🔴 `L` — Publish `architect.yml` JSON Schema to SchemaStore
  - Full schema covering all sections and all plugin configs
  - VS Code: auto-completion in `architect.yml` files
  - IntelliJ: auto-completion in `architect.yml` files
  - Validate on save in supported editors

- [ ] **T-5.1.2** 🟠 `M` — Generate plugin schemas automatically
  - Introspect plugin `ctxClass` fields via reflection
  - Generate JSON Schema from Kotlin data classes
  - Merge all plugin schemas into root schema
  - `architect schema generate` outputs combined schema

- [ ] **T-5.1.3** 🟡 `S` — Add `architect config lint` command
  - Check for: deprecated keys, unknown plugins, invalid values
  - Suggest corrections for common mistakes
  - Exit code 1 if issues found (CI-friendly)

### 5.2 — Environment & Secret Management

- [ ] **T-5.2.1** 🔴 `L` — Implement secret management system
  - `architect secret set <key> <value>` — store encrypted locally
  - `architect secret list` — show available secrets (names only)
  - `architect secret delete <key>` — remove secret
  - Storage: OS keychain (macOS Keychain, Linux secret-service, Windows Credential Manager)
  - Fallback: encrypted file at `~/.architect/secrets.enc`
  - Tasks access via `environment.secret(name)`

- [ ] **T-5.2.2** 🟠 `M` — Add `.env` file support with precedence chain
  - Load order: `.env` → `.env.local` → `.env.{profile}` → `.env.{profile}.local` → system env
  - `.env.local` in `.gitignore` by default
  - `architect.yml` can reference: `${env.DATABASE_URL}`
  - `architect config resolve` shows final resolved values

- [ ] **T-5.2.3** 🟡 `M` — Add environment validation
  - Plugins declare required env vars in config schema
  - `architect check` validates all required vars are set
  - `architect check --env ci` validates CI-specific requirements
  - Generate `.env.example` from all required variables

### 5.3 — Task Definition Enhancements

- [ ] **T-5.3.1** 🟠 `L` — Add inline task definitions in `architect.yml`
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

- [ ] **T-5.3.2** 🟠 `M` — Add task grouping and namespacing
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

- [ ] **T-5.3.3** 🟡 `M` — Add task templates / reusable task definitions
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

## PHASE 6 — IDE INTEGRATION & DEVELOPER TOOLING
> *Meet developers where they are — in their IDE.*

### 6.1 — VS Code Extension

- [ ] **T-6.1.1** 🔴 `XXL` — Build production-ready VS Code extension
  - Features:
    - Task explorer sidebar (tree view of all tasks by phase)
    - Run task from sidebar with click
    - `architect.yml` auto-completion (via JSON Schema)
    - `architect.yml` validation (inline errors/warnings)
    - Task output in VS Code terminal panel
    - CodeLens on `architect.yml` tasks (▶ Run | 📋 Plan)
    - Status bar: engine status indicator (running/stopped)
    - Command palette: `Architect: Run Task`, `Architect: Init`, etc.

- [ ] **T-6.1.2** 🟠 `L` — Add VS Code task provider integration
  - Register Architect tasks as VS Code tasks
  - Appears in `Tasks: Run Task` command
  - Supports problem matchers for build errors
  - `tasks.json` auto-generated from `architect.yml`

- [ ] **T-6.1.3** 🟡 `M` — Add VS Code debugging integration
  - Launch configurations for `architect watch`
  - Breakpoint-aware task execution (for test tasks)
  - Link build errors to source files

### 6.2 — IntelliJ Extension

- [ ] **T-6.2.1** 🔴 `XXL` — Build production-ready IntelliJ extension
  - Features:
    - Tool window: task explorer with run buttons
    - `architect.yml` language support (completion, validation, navigation)
    - Run configurations: "Architect Task" type
    - Gutter icons on `architect.yml` tasks (▶ Run)
    - Build tool integration (Build menu → Architect tasks)
    - Engine lifecycle management from IDE

- [ ] **T-6.2.2** 🟠 `L` — Add IntelliJ run configuration integration
  - "Architect Task" run configuration type
  - Configurable: task name, args, profile, watch mode
  - Output in IntelliJ Run tool window
  - Re-run, stop, debug support

### 6.3 — Language Server Protocol (LSP)

- [ ] **T-6.3.1** 🟠 `XL` — Create Architect Language Server for `architect.yml`
  - Features:
    - Auto-completion for all config keys
    - Validation with diagnostics
    - Hover documentation for each field
    - Go-to-definition for task references
    - Code actions: "Add missing required field", "Fix typo in plugin name"
  - Reusable across all editors (VS Code, IntelliJ, Neovim, Emacs)
  - Distribute as standalone binary

---

## PHASE 7 — CLOUD INTEGRATION & COLLABORATION
> *Extend Architect from local tool to team platform.*

### 7.1 — Cloud Dashboard

- [ ] **T-7.1.1** 🟠 `XXL` — Build execution dashboard web UI
  - Real-time execution monitoring across all team projects
  - Execution history with search, filter, drill-down
  - Task performance analytics (duration trends, failure rates)
  - Project health overview (build status, coverage, violations)
  - Team activity feed (who ran what, when)

- [ ] **T-7.1.2** 🟠 `L` — Add remote execution result storage
  - Sync execution history to cloud backend
  - Query cross-machine execution data
  - Compare: "this build is 30% slower than team average"

- [ ] **T-7.1.3** 🟡 `L` — Add team configuration sharing
  - Shared `architect.yml` templates via cloud
  - Organization-wide defaults and policies
  - `architect config pull` — sync team config
  - Version-controlled config with approval workflow

### 7.2 — Remote Caching

- [ ] **T-7.2.1** 🔴 `XL` — Implement distributed task output cache
  - Share build artifacts across team members and CI
  - Cache backend: S3, GCS, or Architect Cloud
  - Content-addressable storage (hash of inputs → outputs)
  - Automatic cache population on CI, consumption on dev machines
  - Configuration:
    ```yaml
    cache:
      remote:
        enabled: true
        backend: s3
        bucket: my-team-architect-cache
        region: us-east-1
    ```

- [ ] **T-7.2.2** 🟠 `M` — Add cache analytics
  - Cache hit ratio per task, per project, per developer
  - Time saved by cache hits
  - Cache size and eviction stats
  - `architect cache stats` command

### 7.3 — CI/CD Generation

- [ ] **T-7.3.1** 🔴 `L` — Generate CI/CD pipelines from `architect.yml`
  - `architect ci generate --provider github-actions`
  - Reads tasks, phases, dependencies from `architect.yml`
  - Generates optimized workflow YAML
  - Supports: GitHub Actions, GitLab CI, CircleCI, Jenkins
  - Smart caching, artifact passing, parallel jobs

- [ ] **T-7.3.2** 🟠 `M` — Add CI/CD drift detection
  - Compare generated pipeline vs actual workflow files
  - `architect ci diff` shows what would change
  - `architect ci sync` updates workflow files
  - Warning on `architect validate` if CI out of sync

- [ ] **T-7.3.3** 🟡 `M` — Add CI optimization recommendations
  - Analyze execution history for optimization opportunities
  - Suggest: "Task X takes 5min but rarely fails — move to post-merge"
  - Suggest: "Tasks A, B can run in parallel — saving 2min"
  - `architect ci optimize` generates optimized pipeline

---

## PHASE 8 — DISTRIBUTION & ECOSYSTEM
> *Make Architect easy to install, update, and extend.*

### 8.1 — Distribution Channels

- [ ] **T-8.1.1** 🔴 `L` — Set up cross-platform binary distribution
  - Platforms: macOS (arm64, x86_64), Linux (arm64, x86_64), Windows (x86_64)
  - Distribute via: GitHub Releases, Homebrew, apt, winget, Scoop
  - GraalVM native-image for instant startup
  - Signed binaries with SHA-256 checksums

- [ ] **T-8.1.2** 🟠 `M` — Add Docker distribution
  - Official Docker image: `ghcr.io/architect-platform/architect`
  - CI-optimized: includes common tools (git, node, python, go, java)
  - Slim variant: architect CLI only
  - `docker run architect build` works out of the box

- [ ] **T-8.1.3** 🟠 `M` — Add `npx` / `pip` / `go install` one-liner installation
  - `npx @architect-platform/cli init` — bootstrap without global install
  - `pip install architect-cli` — Python-native installation
  - Wrapper scripts that download and run the native binary

- [ ] **T-8.1.4** 🟡 `S` — Add version pinning in `architect.yml`
  - ```yaml
    architect:
      version: ">=2.3.0 <3.0.0"
    ```
  - CLI warns if version mismatch
  - `architect upgrade` respects version constraints

### 8.2 — Plugin Marketplace

- [ ] **T-8.2.1** 🔴 `XL` — Build plugin registry and marketplace
  - Web UI: browse, search, install plugins
  - Plugin metadata: description, author, downloads, rating, compatibility
  - `architect plugin search <query>` — search from CLI
  - `architect plugin install <plugin-id>` — add to architect.yml + download
  - Community plugins: submit via PR to registry repo

- [ ] **T-8.2.2** 🟠 `L` — Add plugin publishing workflow
  - `architect plugin publish` — package, sign, upload to registry
  - Versioning: semver with compatibility matrix
  - Automated compatibility testing against API versions
  - Security review process for community plugins

- [ ] **T-8.2.3** 🟡 `M` — Add plugin template gallery
  - `architect plugin create --template <name>`
  - Templates: kotlin-plugin, typescript-plugin, go-plugin, python-plugin
  - Each template: project structure, build config, test setup, CI workflow
  - Community templates via registry

### 8.3 — SDK & Multi-Language Plugin Support

- [ ] **T-8.3.1** 🟠 `XL` — Create TypeScript SDK for plugin development
  - npm package: `@architect-platform/sdk`
  - TypeScript types for all API contracts
  - Plugin runner that communicates with engine via gRPC/HTTP
  - Example plugins in TypeScript
  - Full test utilities

- [ ] **T-8.3.2** 🟠 `XL` — Create Go SDK for plugin development
  - Go module: `github.com/architect-platform/sdk-go`
  - Go interfaces matching Kotlin API contracts
  - Plugin runner with gRPC/HTTP bridge
  - Example plugins in Go

- [ ] **T-8.3.3** 🟡 `XL` — Create Python SDK for plugin development
  - PyPI package: `architect-sdk`
  - Python classes matching API contracts
  - Plugin runner with HTTP bridge
  - Example plugins in Python

---

## PHASE 9 — TESTING & QUALITY INFRASTRUCTURE
> *Ensure everything we build stays working.*

### 9.1 — Test Coverage Expansion

- [ ] **T-9.1.1** 🔴 `L` — Add end-to-end integration test suite
  - Full pipeline: CLI → Engine → Plugin → Task Execution → Result
  - Test scenarios:
    - Fresh project init → configure → build → test → release
    - Monorepo: multi-project affected detection → parallel execution
    - Plugin: install → configure → execute → uninstall
  - Run in CI on every PR

- [ ] **T-9.1.2** 🟠 `L` — Add engine stress/load tests
  - 100+ concurrent task executions
  - Event buffer overflow scenarios
  - Large monorepo (50+ subprojects)
  - Measure: throughput, latency, memory usage
  - Regression detection in CI

- [ ] **T-9.1.3** 🟠 `M` — Add CLI snapshot tests
  - Capture expected output for each command
  - Detect unintended output changes
  - Cover: all commands, all output modes (plain, json, interactive)

- [ ] **T-9.1.4** 🟡 `M` — Add mutation testing thresholds
  - PiTest already configured but no enforcement
  - Set minimum mutation score: 60%
  - Focus on: task execution logic, dependency resolver, event system

### 9.2 — CI/CD Pipeline Hardening

- [ ] **T-9.2.1** 🔴 `M` — Add cross-module dependency validation in CI
  - Detect: API breaking changes that affect engine/plugins
  - Run all downstream tests when API changes
  - Block merge if API changes break consumers

- [ ] **T-9.2.2** 🟠 `M` — Add automated release pipeline
  - Trigger: merge to main with conventional commit
  - Steps: version bump → changelog → build all → test all → publish → release notes
  - API publishes to GitHub Packages
  - CLI publishes binaries to GitHub Releases
  - Engine publishes Docker image

- [ ] **T-9.2.3** 🟡 `M` — Add security scanning in CI
  - CodeQL for Kotlin (SAST)
  - Dependency vulnerability scanning (Dependabot/Renovate)
  - SBOM generation on release
  - Upload SARIF to GitHub Security tab

---

## PHASE 10 — DOCUMENTATION & DEVELOPER EDUCATION
> *If it's not documented, it doesn't exist.*

### 10.1 — Documentation Overhaul

- [ ] **T-10.1.1** 🔴 `L` — Create comprehensive Getting Started guide
  - 5-minute quickstart: install → init → build → test
  - Language-specific guides: Kotlin, TypeScript, Python, Rust, Go
  - Each guide: real project from zero to CI/CD
  - Video walkthrough (optional)

- [ ] **T-10.1.2** 🔴 `L` — Create Architecture Decision Records (ADRs)
  - Document all major design decisions:
    - Why Micronaut (not Spring)?
    - Why daemon architecture?
    - Why SPI for plugin discovery?
    - Why YAML configuration?
  - Template: context, decision, consequences
  - Living documents: update as decisions evolve

- [ ] **T-10.1.3** 🟠 `M` — Create Plugin Development Tutorial
  - Step-by-step: "Build Your First Plugin in 30 Minutes"
  - Cover: scaffold, implement, test, publish
  - Include: common patterns, anti-patterns, debugging tips
  - Companion repo with example plugin

- [ ] **T-10.1.4** 🟠 `M` — Create CLI Command Reference (auto-generated)
  - `architect docs generate` — extract from PicoCLI annotations
  - One page per command with: usage, options, examples, related commands
  - Published to MkDocs site automatically

- [ ] **T-10.1.5** 🟡 `M` — Create Monorepo Best Practices Guide
  - How to structure a monorepo with Architect
  - Affected detection setup
  - Cross-project dependencies
  - CI optimization strategies
  - Real-world example: this repo as case study

### 10.2 — API Documentation

- [ ] **T-10.2.1** 🟠 `L` — Add KDoc to all public API interfaces
  - Every public class, interface, method, property
  - Include: description, parameters, return values, examples, since version
  - Generate Dokka HTML site
  - Publish alongside MkDocs site

- [ ] **T-10.2.2** 🟠 `M` — Create API migration guides
  - Document breaking changes between versions
  - Upgrade instructions with before/after code examples
  - Deprecation notices with replacement guidance

- [ ] **T-10.2.3** 🟡 `M` — Add REST API documentation (OpenAPI)
  - Generate OpenAPI spec from Micronaut annotations
  - Swagger UI at `http://localhost:9292/swagger-ui`
  - Export: openapi.json, openapi.yaml
  - Publish to docs site

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
| T-7.3.1 | T-5.3.1 |
| T-8.2.1 | T-3.1.1 |
| T-9.1.1 | T-0.2.1, T-2.1.1 |

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
