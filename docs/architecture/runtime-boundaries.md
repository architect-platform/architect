# Runtime Boundaries

> Maintained as part of Phase 3 runtime convergence | Last updated: 2026-03-24

This document defines canonical ownership between `architect-core` and
`architect-engine` before any extraction/removal work begins.

---

## Inventory Summary

Phase 3 inventory found the following duplication under `src/main/kotlin`:

- 78 same-path Kotlin source files exist in both `architect-core` and
	`architect-engine`
- 53 are still byte-identical copies
- 25 have already drifted

That is large enough to treat as an architectural boundary failure rather than a
temporary migration artifact.

Representative drifted files include:

- `core/plugin/app/ProjectPluginLoader.kt`
- `core/secrets/SecretResolver.kt`
- `core/project/app/ApplicationEnvironment.kt`
- `core/project/app/ProjectService.kt`
- `core/tasks/application/TaskExecutor.kt`
- `core/tasks/application/TaskCache.kt`

Representative still-identical files include:

- `core/execution/ClassLoaderResourceExtractor.kt`
- `core/plugin/infra/LocalPluginSource.kt`
- `core/tasks/domain/TaskDependencyResolver.kt`
- `plugins/workflows/core/CorePlugin.kt`
- `plugins/installers/InstallersPlugin.kt`

## Canonical Ownership

### `architect-core` owns

- shared runtime logic used by both embedded CLI execution and engine-hosted
	execution
- plugin loading, signature verification, plugin source resolution, and related
	classloader infrastructure
- secret resolution, `.env` loading, vault/cloud secret adapters, and runtime
	environment expansion helpers
- project loading, validation, config parsing, repository abstractions, and
	project cache behavior
- task execution internals, dependency resolution, caches, and runtime domain
	events
- built-in workflow/installers/inline plugin implementations that must behave
	identically in embedded and server execution modes
- shared runtime configuration constants and execution utilities

### `architect-engine` owns

- Micronaut application startup and bean graph assembly
- HTTP controllers, transport DTOs, and request/response validation at the
	transport boundary
- SSE and execution-streaming adapters that expose runtime events over HTTP
- engine-only orchestration services for the long-lived server host
- cloud reporting, startup profiling, and other operational/server concerns

## Adapter Rule

When a class currently mixes shared runtime logic with Micronaut wiring,
split it instead of choosing one duplicate copy wholesale.

The target shape is:

1. Core contains the logic and domain-facing abstraction.
2. Engine contains only the Micronaut adapter or transport wrapper.
3. Embedded CLI and engine both call the same core implementation.

Examples from the current inventory:

- `ProjectPluginLoader`: core should own plugin-loading behavior; engine should
	only provide host wiring/event publication adapters where needed.
- `SecretResolver`: core should own resolution chain and providers; engine may
	provide DI-friendly construction only.
- `TaskExecutor`: core should own execution semantics; engine should not carry a
	parallel execution implementation.

## Removal Order

Use this order when converging the duplicates:

1. Reconcile drifted files where engine-specific annotations or adapters are the
	only differences.
2. Move or expose the shared implementation from `architect-core`.
3. Add regression tests around plugin loading, secret resolution, local plugin
	sources, and task execution.
4. Delete the engine-side duplicate only after the engine is delegating to core.
5. Add architecture rules so same-path runtime classes cannot reappear in both
	modules.

## Non-Negotiable Boundary

After convergence, there should be no same-responsibility runtime class living
in both modules. `architect-engine` may wrap `architect-core`, but it must not
shadow it.
