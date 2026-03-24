# Baseline Failures

> Maintained as part of Phase 0 baseline | Last updated: 2026-03-24T09:18:02Z

This page captures the current failing validation points that block a trustworthy
repository baseline. It records the exact commands run, the observed failure
mode, and the concrete source locations implicated by the current workspace.

---

## Validation Commands

```bash
cd architect-engine/engine && ./gradlew test
cd architect-cli/cli && ./gradlew test
```

For the cloud UI, the repository does not currently expose meaningful validation
commands because the frontend package manifest defines empty `lint` and `test`
scripts.

---

## architect-engine/engine

**Current status**: failing

**Observed command**:

```bash
cd architect-engine/engine && ./gradlew test
```

**Observed failure**:

- The engine build includes the CLI as a composite build via
  `includeBuild("../../architect-cli/cli")`, so the engine test run currently
  fails during `:cli:compileKotlin` before engine tests execute.
- The failure is no longer the earlier `PluginConfig` issue recorded during the
  initial audit snapshot.

**Relevant files**:

- `architect-engine/engine/settings.gradle.kts` wires the CLI composite build.
- `architect-cli/cli/src/main/kotlin/io/github/architectplatform/cli/ArchitectLauncher.kt`
  currently fails to compile.

**Compiler errors observed from the engine invocation**:

- `Unresolved reference: taskService`
- `Unresolved reference: TaskConditionChecker`
- `Unresolved reference: it` at multiple call sites in the new task-condition
  reporting block
- `Overload resolution ambiguity` on `forEach`
- `Cannot infer a type for this parameter`

**Failing source locations**:

- `ArchitectLauncher.kt:1529` unresolved `taskService`
- `ArchitectLauncher.kt:1530` unresolved `TaskConditionChecker`
- `ArchitectLauncher.kt:1533-1578` unresolved `it` references and ambiguous
  `forEach` inference in the condition-report rendering block

---

## architect-cli/cli

**Current status**: failing

**Observed command**:

```bash
cd architect-cli/cli && ./gradlew test
```

**Observed failure**:

- The CLI test run fails at Kotlin compilation before tests start.
- The current blocker is not the earlier report of 8 failing
  `CliEngineIntegrationTest` cases. The present failure is a compile break in
  `ArchitectLauncher.kt`.

**Compiler errors observed**:

- `Unresolved reference: taskService`
- `Unresolved reference: TaskConditionChecker`
- `Unresolved reference: it` at multiple call sites
- `Overload resolution ambiguity` on `forEach`
- `Cannot infer a type for this parameter`

**Failing source locations**:

- `ArchitectLauncher.kt:1529` unresolved `taskService`
- `ArchitectLauncher.kt:1530` unresolved `TaskConditionChecker`
- `ArchitectLauncher.kt:1533-1578` unresolved lambda references and ambiguous
  iteration over results and issues

**Local code context**:

- The broken block is the CLI `check` command implementation that builds an
  embedded execution context, fetches tasks, runs a task-condition checker, and
  prints blocked/ready task summaries.

---

## architect-cloud/ui

**Current status**: not meaningfully validated

**Observed file**:

```json
{
  "scripts": {
    "dev": "vite",
    "lint": "",
    "build": "vite build",
    "test": "",
    "preview": "vite preview"
  }
}
```

**Observed failure mode**:

- The UI package defines empty `lint` and `test` scripts, so the repository has
  no repo-standard frontend validation entry points for this module.
- `build` exists, but Phase 0 requires documenting the absence of meaningful
  lint/test trust, not treating a build-only path as sufficient validation.

**Relevant file**:

- `architect-cloud/ui/package.json`

---

## Implications

- The repository baseline is currently blocked first by CLI compilation, which
  also prevents a clean engine baseline because the engine build composes the
  CLI module.
- The cloud UI remains outside the repository's normal quality gates because it
  lacks runnable lint/test commands.
- Phase 0 baseline documentation must therefore point to these current failure
  modes instead of the earlier audit snapshot.
