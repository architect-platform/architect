# Architect Platform — Implementation Plan

Derived from `analysis.md`. Each phase is a coherent, independently committable improvement.
Tasks are marked with status: `[ ]` pending, `[x]` done, `[~]` in progress.

---

## Phase 1: Plan Mode / Dry Run

**Goal:** `architect plan <task>` shows the full ordered execution graph without running anything.

### Tasks
- [x] 1.1 Add `TaskPlanDTO` to engine (ordered list of tasks with id, description, phase, deps)
- [x] 1.2 Add `GET /api/projects/{name}/tasks/{task}/plan` endpoint to `TasksApiController`
- [x] 1.3 Add `plan` method to `TaskService` using existing `TaskDependencyResolver`
- [x] 1.4 Add `plan` HTTP call to CLI `EngineCommandClient`
- [x] 1.5 Handle `architect plan <task>` in `ArchitectLauncher` and print the plan tree

---

## Phase 2: Parallel Task Execution

**Goal:** Independent tasks (no dependency relationship between them) run concurrently instead of sequentially.

### Tasks
- [x] 2.1 Add `toBatches()` to `TaskDependencyResolver` — groups topologically sorted tasks into parallel batches
- [x] 2.2 Modify `TaskExecutor.syncExecuteTask()` to execute each batch via `async/awaitAll`
- [x] 2.3 Add `parallelExecution` flag to `EngineConfiguration` (default `true`)

---

## Phase 3: Inline Task Definitions in `architect.yml`

**Goal:** Projects define lightweight tasks directly in config without writing a plugin.

```yaml
tasks:
  deploy:
    description: "Deploy to staging"
    phase: PUBLISH
    depends: [build, test]
    run: "kubectl apply -f k8s/"
  smoke-test:
    description: "Smoke test"
    run: "curl -f https://staging.example.com/health"
```

### Tasks
- [x] 3.1 Define `InlineTaskConfig` data class (id, description, run, phase?, depends?)
- [x] 3.2 Create `InlineTaskPlugin` — built-in engine plugin that reads `tasks:` from config and registers `SimpleTask` instances with shell execution
- [x] 3.3 Register `InlineTaskPlugin` in the engine's built-in plugin list
- [x] 3.4 Add tests for `InlineTaskPlugin`

---

## Phase 4: Local Execution History

**Goal:** Execution results are persisted to `~/.architect/history/` as JSON. `architect history` shows recent executions.

### Tasks
- [x] 4.1 Create `ExecutionRecord` data class (id, project, task, timestamp, success, duration, message)
- [x] 4.2 Create `HistoryService` — writes/reads JSON files in `~/.architect/history/`
- [x] 4.3 Wire `HistoryService` into `TaskService.executeTask()` to record completions
- [x] 4.4 Add `GET /api/history` and `GET /api/history/{project}` endpoints
- [x] 4.5 Add `history` HTTP call to CLI `EngineCommandClient`
- [x] 4.6 Handle `architect history` in `ArchitectLauncher` with formatted table output

---

## Phase 5: Config Validation

**Goal:** `architect.yml` errors are caught at project load time with meaningful messages, not silently at runtime.

### Tasks
- [x] 5.1 Create `ConfigValidator` — validates required fields (`project.name`), warns on unknown top-level keys
- [x] 5.2 Wire `ConfigValidator` into `ProjectService.loadProject()` — throw `ConfigValidationException` on hard errors, log warnings
- [x] 5.3 Add `architect validate` CLI command that registers the project and returns validation results
- [x] 5.4 Add `GET /api/projects/{name}/validate` engine endpoint

---

## Phase 6: Auto-Start Engine from CLI

**Goal:** `architect build` works even if the daemon is not running — CLI auto-starts it, waits for readiness, then executes.

### Tasks
- [x] 6.1 Add `EngineHealthClient` — simple `GET /health` call with short timeout
- [x] 6.2 In `ArchitectLauncher`, before any engine call: check health, if down → start engine process, poll until ready (max 30s)
- [x] 6.3 Add `--no-daemon` flag to skip auto-start (for CI environments that manage the daemon themselves)
- [x] 6.4 Print status messages during engine startup: `⚙️ Starting Architect Engine...`, `✅ Engine ready`

---

## Status

| Phase | Status | Commit |
|-------|--------|--------|
| 1: Plan Mode | ✅ done | — |
| 2: Parallel Execution | ✅ done | — |
| 3: Inline Tasks | ✅ done | — |
| 4: History | ✅ done | — |
| 5: Validation | ✅ done | — |
| 6: Auto-Start | ✅ done | — |
