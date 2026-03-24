# Architect Repository Refactor Plan

## Status
Overall Progress: 87/350 tasks completed (24.9%)
Current Phase: Phase 8 — Standardize testing, compatibility, and release confidence
Last Updated: 2026-03-24T22:55:00Z

## Standards and Principles Gaps

### Architecture

- [ ] Define authoritative module boundaries:
  - [ ] contracts in `architect-api`
  - [ ] shared runtime in `architect-core`
  - [ ] service/runtime host in `architect-engine`
  - [ ] product clients in `architect-cli`, IDE extensions, cloud UI/backend
- [ ] Prohibit duplicated runtime implementations across `core` and `engine`.
- [ ] Introduce architecture validation for the core stack, not only `architect-cloud`.
- [ ] Define repository decomposition rules: when to create a new top-level product/module, when to keep code inside an existing bounded context, and when to move experiments into an incubating area.

### Code Simplicity / Understandability

- [ ] Define a repo-wide expectation that refactors should reduce cognitive load, not only preserve behavior.
- [ ] Prefer smaller modules, smaller classes, clearer naming, and flatter dependency paths over framework-heavy indirection.
- [ ] Identify long or overloaded classes/functions and split by responsibility.
- [ ] Remove duplicate concepts, duplicate helpers, and ambiguous package placement.
- [ ] Standardize package/layout conventions so contributors can predict where code belongs.

### Build / Dependency Management

- [ ] Introduce a root build orchestration strategy or explicit monorepo task runner.
- [ ] Introduce shared Gradle convention plugins and/or version catalogs.
- [ ] Centralize Kotlin, coroutines, Micronaut, Jackson, test, and plugin dependency versions.

### Testing

- [ ] Define minimum testing expectations by surface:
  - [ ] core libraries
  - [ ] server/runtime products
  - [ ] plugins
  - [ ] SDKs
  - [ ] IDE extensions
  - [ ] frontend
- [ ] Make plugin contract tests mandatory for all supported plugins.
- [ ] Add compatibility tests between SDKs/process plugins and engine protocol handling.

### Logging / Error Handling

- [ ] Standardize structured logging expectations and levels across server/runtime modules.
- [ ] Standardize user-facing CLI/extension error messages.
- [ ] Define error taxonomy for configuration, plugin loading, task execution, network failure, and validation errors.

### Documentation

- [ ] Align root docs with actual repo topology and entry points.
- [ ] Add module status and ownership metadata.
- [ ] Require every supported plugin/product to carry minimum readme and example quality.

### Module Boundaries / Dependency Rules

- [ ] Define allowed dependency directions across the entire platform.
- [ ] Document when logic belongs in API vs core vs engine vs plugin.
- [ ] Add automated architecture checks where feasible.

### Observability / Security / Performance

- [ ] Define baseline observability for engine/cloud surfaces: logs, metrics, traces/events, error correlation.
- [ ] Define security standards for plugin loading, signature verification, remote downloads, and secret handling.
- [ ] Define when benchmarking/perf regression checks are required outside `architect-core`.

## Non-Functional Requirements Assessment

### Maintainability — **Medium**

- [ ] Strong in core Kotlin stack.
- [ ] Reduced by duplicated runtime logic, repeated build config, and repeated workflows.
- [ ] Also reduced by repository shape and package layout that do not make ownership and responsibility immediately obvious.

### Extensibility — **Medium**

- [ ] Conceptually strong due to plugin architecture and SDKs.
- [ ] Reduced by inconsistent plugin standards and version drift.

### Evolvability — **Medium-Low**

- [ ] Cross-repo change rollout is expensive because governance is weak and tooling is duplicated.

### Modularity — **Medium**

- [ ] Good intent, but actual boundaries are unevenly enforced.
- [ ] Module decomposition exists, but repository-level decomposition is still too implicit and hard to scan.

### Reliability — **Medium-Low**

- [ ] Baseline failures in engine and CLI reduce trust in current mainline health.

### Efficiency — **Medium**

- [ ] Some performance awareness exists (`jmh`, AOT settings), but CI and build orchestration are inefficient.

### Operability — **Medium-Low**

- [ ] Delivery automation exists but is duplicated; observability standards are not consistently visible.

### Security — **Medium**

- [ ] Positive signals exist around plugin signature verification and secret handling.
- [ ] Needs clearer repo-wide standards and tests around supply-chain and remote execution surfaces.

## Refactoring Strategy

- [ ] Stabilize first, then simplify, then standardize, then expand.
- [ ] Prefer extracting shared conventions and boundaries over isolated local cleanups.
- [ ] Prefer reorganization that reduces cognitive load: fewer places to look, fewer duplicate concepts, clearer naming, clearer ownership.
- [ ] Treat low-maturity modules as a portfolio governance problem before investing in feature work.
- [ ] Avoid sweeping rewrites; focus on staged convergence.

## Delivery Phases

## Phase 0 — Establish an accurate baseline

- [ ] **Priority**: Critical
- [ ] **Goal**: Make the repository measurable and trustworthy before broader refactoring.
- [ ] **Rationale**: Planning against stale docs and failing baselines will produce churn.
- [ ] **Impacted areas**: root docs, build/test entry points, CI, validation reports
- [ ] **Dependencies**: none
- [ ] **Risks**: exposes additional latent failures
- [ ] **Expected outcomes**: trusted baseline, clear scope, explicit module status

- [ ] Tasks
  - [x] Create a repository status matrix for every subproject: active, beta, incubating, placeholder, deprecated. | Finished: 2026-03-24T00:01:00Z | Notes: Created STATUS.md with full matrix across core platform, products, plugins, SDKs, and delivery infra.
  - [x] Create a repository decomposition map that explains what each top-level directory is for and why it exists. | Finished: 2026-03-24T00:02:00Z | Notes: Created docs/architecture/repository-map.md with full taxonomy, per-directory tables, contributor journeys, and root file inventory.
  - [x] Reconcile root `README.md` and `CONTRIBUTING.md` with actual build/test entry points. | Finished: 2026-03-24T00:03:00Z | Notes: Removed root-level ./gradlew build/test instructions (no root wrapper exists); replaced with per-module commands. Fixed wrong path (architect-cli → architect-cli/cli). Fixed Apache 2.0 vs MIT license error in CONTRIBUTING.md.
  - [x] Document the current baseline failures for `architect-engine`, `architect-cli`, and `architect-cloud/ui`. | Finished: 2026-03-24T09:18:02Z | Notes: Added docs/architecture/baseline-failures.md with exact current failure modes and validation commands. Updated STATUS.md and the PLAN baseline snapshot to replace stale `PluginConfig` and `CliEngineIntegrationTest` references with the current CLI compilation blocker and empty cloud UI validation scripts.
  - [x] Decide whether the repo will gain a root orchestrator or explicitly document per-module execution only. | Finished: 2026-03-24T09:19:50Z | Notes: Accepted ADR-009 to keep root execution explicitly per-module until a real orchestrator exists. Updated README.md, CONTRIBUTING.md, and docs/architecture/repository-map.md to clarify that the root `architect.yml` is metadata/configuration, not a supported monorepo build runner.

- [x] Validation
  - [x] Re-run representative tests and confirm documented outcomes match reality. | Finished: 2026-03-24T09:24:17Z | Notes: Re-ran `./gradlew test` for `architect-api/api`, `architect-core/core`, and `architect-cloud/backend` and confirmed all three still pass. Confirmed fresh failing evidence from this Phase 0 cycle for `architect-engine/engine`, `architect-cli/cli`, and `architect-cloud/ui` still matches STATUS.md and docs/architecture/baseline-failures.md.
  - [x] Verify contributor docs no longer instruct impossible commands. | Finished: 2026-03-24T09:24:17Z | Notes: Re-checked README.md, CONTRIBUTING.md, and docs/architecture/repository-map.md. All now state per-module execution and no longer imply a supported root Gradle build/test entry point.
  - [x] Verify a new contributor can understand the repository shape from the decomposition map alone. | Finished: 2026-03-24T09:24:17Z | Notes: Reviewed docs/architecture/repository-map.md and strengthened it with execution flow, dependency relationships, local plugin development steps, and a clearer description of the root `architect.yml`. Also corrected the cloud UI technology description from Vue to React.

## Phase 1a — Reorganize repository decomposition and ownership boundaries

- [ ] **Priority**: Critical
- [ ] **Goal**: Make the repository structure itself easier to understand.
- [ ] **Rationale**: Clean code inside confusing repo topology still produces high cognitive load.
- [ ] **Impacted areas**: top-level directories, docs, ownership metadata, possibly physical module placement
- [ ] **Dependencies**: Phase 0
- [ ] **Risks**: path churn, CI/documentation breakage, import/package movement cost
- [ ] **Expected outcomes**: obvious top-level taxonomy, explicit support tiers, clearer navigation

- [ ] Tasks
  - [x] Define the desired top-level taxonomy for the repository: | Finished: 2026-03-24T09:25:56Z | Notes: Accepted ADR-010 and documented the target grouping model in docs/architecture/repository-map.md so later restructuring work has an explicit destination taxonomy rather than an implied one.
    - [x] core platform/runtime
    - [x] end-user products
    - [x] official plugins
    - [x] SDKs
    - [x] incubating/experimental modules
    - [x] docs and policy
  - [x] Decide which existing modules stay top-level versus move under grouped parent directories. | Finished: 2026-03-24T09:27:14Z | Notes: Accepted ADR-011 and added a current-to-target placement table in docs/architecture/repository-map.md. Decided that the current `architect-*` platform and product directories are transitional and should eventually group under `platform/` and `products/`, while `plugins/`, `sdk/`, `docs/`, `.github/`, and `homebrew/` remain top-level category roots.
  - [x] Define ownership metadata per bounded area. | Finished: 2026-03-24T09:28:39Z | Notes: Added docs/architecture/ownership-map.md and accepted ADR-012. Defined bounded-area stewardship groups for platform runtime, product surfaces, plugin ecosystem, SDK ecosystem, and docs/governance so the repo now has explicit ownership metadata even before CODEOWNERS or named team mappings exist.
  - [x] Add a repository map to docs. | Finished: 2026-03-24T09:29:57Z | Notes: The repository map is now published under the MkDocs Architecture nav alongside the decision log, ownership map, and baseline failures documentation.
  - [x] Mark incomplete areas explicitly rather than letting them look production-adjacent. | Finished: 2026-03-24T09:31:08Z | Notes: Updated README.md and docs/architecture/repository-map.md to explicitly call out incubating areas such as `architect-cloud/ui`, `architect-vscode`, `architect-intellij`, and the thin plugin tier so they are not presented as production-stable surfaces.

- [x] Validation
  - [x] Review navigation paths for key contributor journeys. | Finished: 2026-03-24T09:34:39Z | Notes: Reviewed docs/architecture/repository-map.md, fixed journey guidance for current-vs-target paths, verified `architect plugin create` exists in the CLI scaffolder path, and added explicit incubating-status warnings for engine and CLI workflows.
  - [x] Verify build, docs, and CI references still resolve after any structural changes. | Finished: 2026-03-24T09:34:39Z | Notes: Confirmed MkDocs nav still resolves repository-map, ownership-map, and baseline-failures pages. Verified current build/docs paths in README.md, CONTRIBUTING.md, and docs/architecture/repository-map.md still match the on-disk module layout. Re-checked workflow references under `.github/workflows/` and confirmed they still point to existing module paths because no physical directory moves have been applied yet.

## Phase 2 — Unify build, dependency, and version governance

- [ ] **Priority**: Critical
- [ ] **Goal**: Remove version drift and repeated build logic.
- [ ] **Rationale**: This is the largest systemic source of maintainability cost.
- [ ] **Impacted areas**: all Gradle modules, plugins, package metadata, release/versioning
- [ ] **Dependencies**: Phase 0, Phase 1a
- [ ] **Risks**: dependency resolution regressions, publishing changes
- [ ] **Expected outcomes**: centralized versions, predictable builds, easier upgrades

- [ ] Tasks
  - [x] Introduce a shared Gradle convention plugin and/or version catalog for Kotlin/Micronaut/Jackson/testing/jacoco. | Finished: 2026-03-24T09:47:38Z | Notes: Added a repository-level `gradle/libs.versions.toml` and wired `architect-api/api`, `architect-core/core`, `architect-cli/cli`, `architect-engine/engine`, and `architect-cloud/backend` settings to it. Updated those build scripts to consume shared plugin, dependency, coroutines, Kotlin, Jackson, testing, and JaCoCo versions from the catalog while keeping existing source-level failures isolated from build-configuration validation.
  - [x] Standardize artifact versioning strategy across API/core/engine/CLI/plugins. | Finished: 2026-03-24T09:53:45Z | Notes: Extended `gradle/libs.versions.toml` to become the shared source of truth for platform and plugin artifact versions, updated API/core/engine/CLI build scripts to read artifact versions and intra-platform dependency versions from the catalog, and wired all first-party plugin builds to use the shared API contract version. This removed mixed plugin API dependency versions (`1.1.2`, `1.1.3`, `2.1.0`) in favor of the current shared `2.1.0` contract.
  - [x] Eliminate repeated `resolutionStrategy.eachDependency` blocks where a single shared mechanism can be used. | Finished: 2026-03-24T09:58:21Z | Notes: Added `gradle/architect-kotlin-alignment.gradle.kts` and replaced the repeated Kotlin/coroutines alignment blocks in `architect-core/core`, `architect-engine/engine`, `architect-cli/cli`, and `architect-cloud/backend` with one shared applied script. Re-validated core and cloud backend test runs plus engine/CLI configuration resolution after the deduplication.
  - [x] Standardize plugin build scripts onto one template with deliberate deviations only. | Finished: 2026-03-24T10:05:56Z | Notes: Added `gradle/architect-plugin-conventions.gradle.kts` and refactored all first-party plugin builds to share one common template for group, repositories, Java compatibility, test/JaCoCo wiring, and coverage enforcement. The remaining deliberate deviations are now limited to plugin-specific dependencies, artifact version selection, and explicit `mavenLocal()` opt-in where local development needs it.
  - [x] Audit and modernize Shadow plugin usage consistently across all relevant modules. | Finished: 2026-03-24T10:07:17Z | Notes: Audited Shadow usage across the repo and confirmed only `architect-cli/cli` still used the legacy `com.github.johnrengelman.shadow` plugin line. Switched the CLI to the shared modern `com.gradleup.shadow` alias and removed the unused legacy alias from `gradle/libs.versions.toml`.

- [x] Validation
  - [x] Run module builds against the new shared conventions. | Finished: 2026-03-24T10:11:09Z | Notes: Re-ran `./gradlew test` for `architect-api/api`, `architect-core/core`, and `architect-cloud/backend`; re-ran `./gradlew help` for `architect-engine/engine`; and re-ran representative plugin `help` checks after the shared catalog, versioning, alignment, and plugin convention changes.
  - [x] Confirm dependency trees no longer show mixed API versions across plugins. | Finished: 2026-03-24T10:11:09Z | Notes: Re-checked plugin build scripts and confirmed the old `io.github.architectplatform:api:1.1.2` and `1.1.3` references are gone. All first-party plugin builds now consume the shared `architect-api` contract version from `gradle/libs.versions.toml`.
  - [x] Verify publishing metadata and artifact coordinates still resolve correctly. | Finished: 2026-03-24T10:11:09Z | Notes: Ran `architect-api/api` with `./gradlew test generatePomFileForGprPublication` successfully, confirming the published API artifact metadata still resolves after the shared-version changes.

## Phase 3 — Re-establish architectural boundaries in the runtime stack

- [ ] **Priority**: Critical
- [ ] **Goal**: Make `architect-api`, `architect-core`, and `architect-engine` non-overlapping in responsibility.
- [ ] **Rationale**: Duplicated runtime logic is a correctness and evolvability hazard.
- [ ] **Impacted areas**: `architect-core`, `architect-engine`, tests, plugin loading
- [ ] **Dependencies**: Phase 2
- [ ] **Risks**: behavior regressions in plugin loading and execution
- [ ] **Expected outcomes**: one authoritative implementation per concern, cleaner layering

- [ ] Tasks
  - [x] Inventory duplicated classes and decide canonical ownership.
  - [x] Converge identical plugin-loading support types into `architect-core`. | Finished: 2026-03-24T10:21:10Z | Notes: Removed six byte-identical plugin-loading support classes from `architect-engine/engine` (`CommonPlugin`, `PluginDownloader`, `PluginLoader`, `PluginSourceRegistry`, `GitHubPluginSource`, `LocalPluginSource`) so the engine now resolves the canonical implementations from `architect-core` instead of shadowing them locally.
  - [x] Split drifted plugin-loading runtime classes into `architect-core` logic plus `architect-engine` host adapters. | Finished: 2026-03-24T10:24:32Z | Notes: Replaced engine-local plugin-loading runtime classes with two thin Micronaut host adapters (`MicronautArchitectEventBus`, `MicronautRemoteContentFetcher`) and removed the engine shadow copies of `ProjectPluginLoader`, `CachedPluginDownloader`, `GitHubReleaseResolver`, `SpiPluginLoader`, `IsolatedPluginClassLoader`, `PluginConfig`, and `PluginSource`. The engine now uses `architect-core` for plugin loader/downloader/config/source behavior while keeping engine-only event-publication and HTTP transport wiring local.
  - [x] Converge secret resolution into `architect-core`. | Finished: 2026-03-24T10:26:43Z | Notes: Removed the engine-local `SecretResolver` implementation set and replaced it with a Micronaut factory that exposes `architect-core`'s `CompositeSecretResolver.default()` as the engine bean. Secret resolution logic now lives in `architect-core`, while `architect-engine` keeps only the host bean registration needed by `ApplicationEnvironment`.
  - [x] Converge config loading and validation into `architect-core`. | Finished: 2026-03-24T10:28:54Z | Notes: Removed the engine-local `ConfigLoader` and `ConfigValidator` copies so `architect-engine` now consumes the richer `architect-core` implementations, including raw-YAML loading, profile-aware validation inputs, schema/plugin-section validation, and line-aware diagnostics.
  - [x] Converge project lifecycle service into `architect-core`. | Finished: 2026-03-24T11:05:00Z | Notes: Deleted the engine-local ProjectService and 6 identical project domain types (Project, ProjectRepository, LazyProjectLoadState, ConfigParser, InMemoryProjectRepository, YamlConfigParser). Added reloadProject() and hasLocalPlugins() to core's ProjectService. Created ProjectServiceFactory in engine to construct ProjectService with Micronaut property-driven config and to adapt CloudReporterService to the core ProjectRegistrationReporter interface.
  - [x] Converge task execution, cache, and runtime event services into `architect-core`. | Finished: 2026-03-24T12:00:00Z | Notes: Deleted 41 byte-identical files and 13 engine-local shadow classes (including TaskExecutor, TaskCache, BashCommandExecutor, InlineTaskPlugin, InlineTaskConfig, and 8 Serdeable-only drifted DTOs/events). Merged InlineTaskRequirements feature from engine into core's InlineTaskConfig/InlineTaskPlugin. Removed @Singleton from core's TaskExecutor, TaskCache, and BashCommandExecutor. Created RuntimeServiceFactory in engine for Micronaut property-driven construction of these services. Created SerdeImports.kt for centralized @SerdeImport declarations. Made TaskPermissionScope/TaskPermissionContext public for cross-module access.
  - [x] Remove shadow implementations after parity tests exist. | Finished: 2026-03-24T12:15:00Z | Notes: Deleted 8 remaining Serdeable-only main source shadows from engine (ExecutionRecord, PluginEvents, ArchitectEventDTO, TaskDTO, TaskPlanDTO, TaskResultDTO, ExecutionEvents, TaskEvents) and 1 trivially-different test shadow (VerifyCommitMessageTaskTest). Core's SerdeImports.kt covers all serialization needs. ApplicationEnvironment legitimately differs between core (standalone/test) and engine (Micronaut production). 3 engine @MicronautTest test shadows retained as integration tests. Core tests pass (179 tests).
  - [x] Introduce architecture rules to prevent future duplication and dependency leaks. | Finished: 2026-03-24T12:30:00Z | Notes: Added ArchUnit to both core and engine build files. Created CoreArchitectureTest (enforces no Micronaut imports in core) and EngineArchitectureTest (enforces no shadow implementations of TaskExecutor, TaskCache, BashCommandExecutor, ProjectService, ConfigLoader, SecretResolver). Core architecture tests pass. Engine architecture tests will be validated after the CLI baseline blocker is fixed (task 47).
  - [x] Repair the current `architect-engine` baseline blocker as part of this convergence work. | Finished: 2026-03-24T13:42:48Z | Notes: Restored the engine host-side Micronaut wiring for converged core services by explicitly providing project/config/plugin/history beans, bound core task/plugin services to the Micronaut event bus adapter, and reinstated the built-in `CommonPlugin` set so inline `tasks:` registration and CLI HTTP execution flows work again. Verified with `architect-core/core` and `architect-engine/engine` test suites plus targeted `ProjectsApiIntegrationTest` and `CliEngineHttpIntegrationTest` runs.
  - [x] Rework package structure where needed so runtime concerns are discoverable and responsibility-aligned. | Finished: 2026-03-24T15:36:14Z | Notes: Completed a runtime package reorganization by moving shared runtime sources from legacy `io.github.architectplatform.engine.*` paths into responsibility-aligned `io.github.architectplatform.core.*` namespaces, with git recording the move as coherent renames. Re-validated targeted boundaries with `architect-core/core` tests (`ProjectPluginLoaderTest`, `LocalPluginSourceTest`, `SecretResolverTest`, `CoreArchitectureTest`) and `architect-engine/engine` `EngineArchitectureTest`.

### Phase 3 Notes

- 2026-03-24: inventoried `architect-core` and `architect-engine` duplicate runtime sources and recorded canonical ownership in `docs/architecture/runtime-boundaries.md` plus ADR-013. The inventory found 78 same-path Kotlin files duplicated across the two modules, with 25 already drifted. Canonical rule: shared runtime behavior lives in `architect-core`; `architect-engine` keeps Micronaut host/transport adapters only.
- 2026-03-24: decomposed the broad extraction task into smaller convergence workstreams because the inventory exposed 78 duplicate runtime files across multiple concern clusters. Completed the first low-risk extraction by deleting six byte-identical plugin-loading support classes from `architect-engine` so the server now consumes the canonical `architect-core` implementations for those types.
- 2026-03-24: completed the plugin-loading adapter split by removing the remaining engine-local plugin loader/downloader/config/source shadow classes and replacing them with two engine-only Micronaut adapters for event publication and remote HTTP fetching. Plugin-loading runtime behavior now resolves from `architect-core`, while `architect-engine` retains only host wiring for that concern.
- 2026-03-24: further decomposed the remaining shared-runtime convergence work into secret-resolution, project-service, and task-service slices. Completed the secret-resolution slice by deleting the engine-local resolver implementation set and registering the core `CompositeSecretResolver` through a Micronaut factory bean.
- 2026-03-24: decomposed the project-service slice into configuration and lifecycle work. Completed the configuration half by removing the engine-local `ConfigLoader` and `ConfigValidator` copies so the engine now uses core's richer config loading and validation behavior.
- 2026-03-24: completed the project lifecycle convergence by deleting the engine-local ProjectService and 6 identical project domain types, adding engine-only methods (reloadProject, hasLocalPlugins) to core's richer ProjectService, and creating a Micronaut factory bean to wire configuration and adapt CloudReporterService to the core ProjectRegistrationReporter interface.
- 2026-03-24: completed the task execution/cache/events convergence. Deleted 41 identical files and 13 drifted engine shadow classes across multiple concern areas: task execution (TaskExecutor, TaskCache, BashCommandExecutor), inline plugins (InlineTaskPlugin, InlineTaskConfig), domain events and DTOs (8 Serdeable-annotated types). Merged InlineTaskRequirements feature into core, created RuntimeServiceFactory for Micronaut property-driven construction, and introduced centralized SerdeImports for core types needing Micronaut serialization. Made TaskPermissionScope/TaskPermissionContext public to support ApplicationEnvironment cross-module access.
- 2026-03-24: repaired the remaining engine baseline regressions exposed by the convergence. Rebuilt the engine's Micronaut host wiring around the core-owned project/plugin/task/history services, resolved function-type event bus injection through `MicronautArchitectEventBus`, and restored the built-in internal plugin providers so inline `tasks:` blocks register again. Fresh validation now shows `architect-core/core` and `architect-engine/engine` `./gradlew test` both passing.
- 2026-03-24: completed the runtime package-structure rework for discoverability. The move set now stages as renames from legacy `io.github.architectplatform.engine.*` paths to responsibility-aligned `io.github.architectplatform.core.*` paths, and targeted core/engine architecture and runtime regression tests passed after the reorganization.
- 2026-03-24: completed targeted regression validation for plugin loading, local plugin sources, and secret resolution by running `ProjectPluginLoaderTest`, `LocalPluginSourceTest`, and `SecretResolverTest` in `architect-core/core` after the package rework.
- 2026-03-24: validated that duplicated runtime class paths are removed across core and engine. A normalized path overlap check reported one residual overlap (`project/app/ApplicationEnvironment.kt`), and file comparison confirmed it is an intentional host-specific adapter split (core standalone map/event-bus environment vs engine Micronaut `BeanContext`/`ApplicationEventPublisher` bridge), not duplicated runtime responsibility.

- [ ] Validation
  - [x] Run `architect-core/core` and `architect-engine/engine` tests. | Finished: 2026-03-24T13:42:48Z | Notes: Re-ran both module test suites after restoring host wiring; `architect-core/core` and `architect-engine/engine` now both pass, including the previously failing `ProjectsApiIntegrationTest` and `CliEngineHttpIntegrationTest` coverage inside the engine suite.
  - [x] Add targeted regression tests around plugin loading, secret resolution, and local plugin sources. | Finished: 2026-03-24T15:38:13Z | Notes: Verified and executed the focused regression suite in `architect-core/core` (`ProjectPluginLoaderTest`, `LocalPluginSourceTest`, `SecretResolverTest`) to ensure plugin loading, local plugin path hardening, and secret-resolution flows remain stable after runtime package movement.
  - [x] Verify no duplicated runtime class remains across core/engine for the same responsibility. | Finished: 2026-03-24T15:39:12Z | Notes: Compared normalized runtime-relative Kotlin path sets between `architect-core/core` and `architect-engine/engine`; overlap dropped to one intentional host adapter (`ApplicationEnvironment`). Direct file diff confirms distinct host concerns (core standalone environment wiring vs engine Micronaut bean/event integration), so duplicated authoritative runtime implementations are eliminated.

## Phase 4 — Rationalize repository portfolio and directory hygiene

- [ ] **Priority**: High
- [ ] **Goal**: Make inactive or incomplete modules explicit and reduce repo noise.
- [ ] **Rationale**: Placeholder modules create false complexity and mislead contributors.
- [ ] **Impacted areas**: `architect-data`, `architect-server`, `architect-x`, root docs, ignore/build hygiene
- [ ] **Dependencies**: Phase 0, Phase 1a
- [ ] **Risks**: accidental removal of intended future work
- [ ] **Expected outcomes**: clear product boundaries and cleaner repository navigation

- [ ] Tasks
  - [x] Decide status and ownership for `architect-data`, `architect-server`, and `architect-x`. | Finished: 2026-03-24T15:49:34Z | Notes: Confirmed all three paths are not present in-tree, formalized their governance ownership under the Docs and Governance stewardship group, and documented that any reintroduction requires explicit portfolio/status/ownership approval before code is added.
  - [x] Remove committed/generated build-state noise where not intended for source control. | Finished: 2026-03-24T15:51:42Z | Notes: Audited tracked files for generated/build-state patterns (`build/`, `dist/`, `target/`, `out/`, IDE/OS noise) and confirmed no such artifacts are currently tracked. Verified on-disk generated directories resolve to zero tracked entries and `.gitignore` already enforces the relevant exclusions.
  - [x] Add lightweight status docs for incubating modules if they remain in-tree. | Finished: 2026-03-24T16:04:32Z | Notes: Added lightweight `STATUS.md` docs for all currently incubating modules that remain in-tree across platform (`architect-engine`, `architect-cli`), product surfaces (`architect-cloud/ui`, `architect-vscode`, `architect-intellij`), thin official plugins (`plugins/*-architected` incubating set), and SDKs (`sdk/typescript`, `sdk/python`, `sdk/go`).
  - [x] Update root docs and navigation to reflect the actual supported module set. | Finished: 2026-03-24T16:07:22Z | Notes: Updated root README component descriptions to match the real platform/product/plugin/SDK support tiers, added `docs/architecture/status-matrix.md`, updated docs landing page component/status framing, and exposed the status matrix in MkDocs architecture navigation.

- [x] Validation
  - [x] Confirm each remaining top-level directory has a declared status and reason to exist. | Finished: 2026-03-24T16:07:22Z | Notes: Repository-level and module-level status documentation now covers platform, products, plugin tiers, SDKs, and historical not-present paths (`architect-data`, `architect-server`, `architect-x`) with explicit ownership and support intent.
  - [x] Confirm ignored/generated files are not tracked unintentionally. | Finished: 2026-03-24T15:51:42Z | Notes: Audit confirmed generated/build-state patterns are ignored and not tracked.

## Phase 5 — Standardize the plugin platform

- [ ] **Priority**: High
- [ ] **Goal**: Make every official plugin conform to the same engineering contract.
- [ ] **Rationale**: The plugin ecosystem is the main extensibility story, but maturity is inconsistent.
- [ ] **Impacted areas**: all `plugins/*`
- [ ] **Dependencies**: Phase 2
- [ ] **Risks**: exposing weak or incomplete plugins that need downgrading in support level
- [ ] **Expected outcomes**: uniform plugin quality, lower onboarding cost, easier release management

- [ ] Tasks
  - [x] Define the minimum official plugin standard: | Finished: 2026-03-24T16:08:55Z | Notes: Added `docs/guides/plugin-standard.md` defining official plugin support tiers, minimum quality checklist, required layout, maturity rules, and validation commands; surfaced the guide in MkDocs navigation.
    - [x] README
    - [x] docs surface
    - [x] architect config example
    - [x] unit tests
    - [x] plugin contract test
    - [x] version alignment
    - [x] release/publish metadata
  - [x] Apply the standard to all mature plugins first. | Finished: 2026-03-24T16:15:01Z | Notes: Applied baseline standardization to mature plugins by adding missing `STATUS.md` files for `docs-architected`, `git-architected`, `gradle-architected`, `scripts-architected`, `github-architected`, and `pipelines-architected`; added missing contract tests for docs/github/pipelines plugins; and fixed `pipelines-architected` compile drift by restoring its SLF4J API dependency needed by existing logger usage. Validated via targeted contract test runs in each updated mature plugin module.
  - [x] For thin/template plugins, choose one: | Finished: 2026-03-24T16:31:01Z | Notes: Kept thin/template plugins in the official set as incubating/experimental, added the missing README and `architect.yml` example artifacts for `rust-architected` and `terraform-architected`, normalized `architecture-architected` and `javascript-architected` to the incubating standard, and surfaced plugin tier status in the reference index.
    - [ ] promote and complete
    - [x] mark experimental
    - [ ] remove from official set
  - [x] Extract common plugin build/test conventions. | Finished: 2026-03-24T17:42:44Z | Notes: Extended `gradle/architect-plugin-conventions.gradle.kts` to own the shared Java 17 toolchain plus common plugin API/JUnit dependencies, then removed those duplicate lines from every `plugins/*/app/build.gradle.kts` file while preserving plugin-specific repositories and extra dependencies. Validation passed for `docs-architected` and `pipelines-architected`; `go-architected` still fails to compile because of unresolved `ShellUtils`, and `gradle-architected` still has a failing functional test (`GradlePluginTest` path-traversal case), both appearing unrelated to the convention extraction itself.
  - [x] Standardize resource layout and task registration patterns. | Finished: 2026-03-24T17:55:08Z | Notes: Normalized the `gradle-architected` and `javascript-architected` package/layout to `io.github.architectplatform.plugins.gradle|javascript`, moved `DocsContext` and `GithubContext` into their root plugin packages, extracted `DocsTask`, `GithubTask`, and `PipelinesTask` into standalone files, and moved GitHub workflow templates under `resources/workflows/github` while making workflow type lookup accept slash-delimited template paths. Validation passed with `gradle-architected` compile/resources checks, `javascript-architected` compile checks, and full test runs for `docs-architected`, `github-architected`, and `pipelines-architected`.
  - [x] Standardize plugin internal package/layout patterns so plugin code is easy to scan and compare across the ecosystem. | Finished: 2026-03-24T18:10:00Z | Notes: Extracted `ArchitectureValidateTask` inner class from `ArchitecturePlugin.kt` into a top-level `ArchitectureTask.kt` following the standard three-file layout (Context/Plugin/Task). Added `ArchitecturePluginContractTest.kt` using `ArchitectPluginContractTestSuite`. Updated `docs/guides/plugin-standard.md` with an explicit "Internal Code Layout Convention" section documenting the canonical three-file minimum, rules about top-level task files, sub-package thresholds, and naming conventions. Validated with `architecture-architected` full test run (9 tasks, BUILD SUCCESSFUL).

- [ ] Validation
  - [x] Run plugin tests in batches by maturity tier. | Finished: 2026-03-24T19:00:00Z | Notes: Ran all 16 official plugins by tier. Mature tier (docs-architected, git-architected, github-architected, gradle-architected, scripts-architected, pipelines-architected) — all BUILD SUCCESSFUL. Incubating tier (architecture-architected, docker-architected, go-architected, javascript-architected, kubernetes-architected, maven-architected, nx-architected, python-architected, rust-architected, terraform-architected) — all BUILD SUCCESSFUL after updating test assertions to match ShellUtils.escapeShellArg single-quoting behavior (security-correct behavior introduced previously; tests pre-dated it). Also fixed gradle-architected path-traversal test failure and resolved API version cache issue by bumping architect-api to 2.2.0 and adding mavenLocal() first in plugin conventions.
  - [x] Confirm all official plugins pass contract tests. | Finished: 2026-03-24T19:30:00Z | Notes: Added missing contract tests for 8 incubating plugins (go, javascript, kubernetes, maven, nx, python, rust, terraform) using ArchitectPluginContractTestSuite. All 16 official plugins now have contract tests and all pass (BUILD SUCCESSFUL for each).
  - [x] Confirm every official plugin has docs/examples aligned to actual capabilities. | Finished: 2026-03-24T20:00:00Z | Notes: Verified all 16 plugins have README.md and architect.yml. Fixed docs/capability drift: added missing docker-compose-down and docker-compose-logs to docker-architected README, added missing py-publish to python-architected README, corrected ./gradlew to gradle in Local Build and Test sections of all 8 incubating plugins (which have no gradlew wrapper). Mature plugin docs (git, gradle, github, docs, scripts, pipelines) already accurate.

## Phase 6 — Simplify CI/CD and delivery automation

- [ ] **Priority**: High
- [ ] **Goal**: Replace cloned workflow logic with reusable delivery patterns.
- [ ] **Rationale**: CI duplication makes policy, caching, and reliability improvements expensive.
- [ ] **Impacted areas**: `.github/workflows/*`, generated workflow templates, plugins that emit workflows
- [ ] **Dependencies**: Phase 2, Phase 5
- [ ] **Risks**: workflow behavior changes during migration
- [ ] **Expected outcomes**: leaner CI, lower maintenance cost, consistent policy rollout

- [ ] Tasks
  - [x] Inventory repeated workflow steps and convert them into reusable workflows or composite actions. | Finished: 2026-03-24T20:05:00Z | Notes: Replaced repeated module pipeline logic with two reusable workflow entry points (`reusable-kotlin-pipeline.yml` and `reusable-kotlin-no-release-pipeline.yml`) and regenerated the thin caller workflows so per-module YAML now delegates instead of cloning full job definitions.
  - [x] Separate generated workflow templates from hand-maintained CI policy logic. | Finished: 2026-03-24T20:05:00Z | Notes: Moved the hand-maintained CI policy into reusable workflow files and slash-delimited workflow templates under `plugins/github-architected/.../workflows/github/`, while keeping generated caller workflows as thin wrappers produced by `.github/scripts/gen_workflows.py`.
  - [x] Standardize caching, runtime setup, permissions, and release gates. | Finished: 2026-03-24T20:05:00Z | Notes: Centralized JDK/Node setup, Gradle cache keys, Architect CLI/Engine bootstrap, permissions, concurrency, and main-branch release/publish gating inside the reusable workflow definitions so all generated Kotlin pipelines share one policy surface.
  - [x] Decide when CI should invoke Architect-generated behavior versus direct build tool commands. | Finished: 2026-03-24T20:05:00Z | Notes: Accepted `.github/decisions/01-ci-vs-architect-commands.md`, which makes generated project pipelines delegate to Architect phases while keeping hand-maintained governance/security workflows on direct tool commands.
  - [x] Add a CI validation rule for generated workflow drift if generation remains part of the model. | Finished: 2026-03-24T20:05:00Z | Notes: Added `.github/workflows/workflow-drift-check.yml` to regenerate caller workflows on PRs and fail when committed generated workflow files diverge from the current template and generator script output.

- [ ] Validation
  - [x] Dry-run equivalent CI paths for API, engine, CLI, cloud, and plugin modules. | Finished: 2026-03-24T20:40:00Z | Notes: Replayed the reusable workflow build/test paths locally across representative surfaces. `architect-api/api`, `architect-engine/engine`, `architect-cloud/backend`, `plugins/docs-architected/app`, and `plugins/javascript-architected/app` all pass when run in isolation. `architect-cli/cli` still fails reproducibly with 8 `CliEngineIntegrationTest` assertion failures, confirming the CLI baseline remains red. `architect-cloud/ui` still is not a trustworthy CI surface in this environment: `npm run build` fails with `vite: command not found`, and its `lint`/`test` scripts remain empty. An initial parallel false negative on `docs-architected` was eliminated by rerunning serially because these Gradle builds share included projects.
  - [x] Confirm workflow count or repeated step volume drops materially. | Finished: 2026-03-24T20:50:00Z | Notes: Compared the current workflow tree against pre-refactor commit `67b361e4297700e310303219798c884e81a7de35` (the parent of the reusable-workflow introduction). Workflow files increased from 19 to 22 because the shared policy now lives in `reusable-kotlin-pipeline.yml`, `reusable-kotlin-no-release-pipeline.yml`, and the drift-check workflow, but total workflow YAML lines dropped from 1925 to 1226 (~36%). Repeated setup/pipeline steps also fell sharply: checkout 31→11, setup-java 27→6, setup-node 14→3, setup-architect 25→4, and each explicit `architect init|verify|build|test --plain` invocation 13→2.

## Phase 7 — Raise product-surface quality outside the Kotlin core

- [ ] **Priority**: High
- [ ] **Goal**: Bring frontend and IDE surfaces up to explicit, supportable standards.
- [ ] **Rationale**: These products are visible but currently under-specified and under-tested.
- [ ] **Impacted areas**: `architect-cloud/ui`, `architect-vscode`, `architect-intellij`
- [ ] **Dependencies**: Phase 0, Phase 2
- [ ] **Risks**: feature expectations may exceed current intended scope
- [ ] **Expected outcomes**: honest scope, real tests, better user-facing quality

- [ ] Tasks
  - [x] For `architect-cloud/ui`, define the actual product scope and frontend architecture. | Finished: 2026-03-24T21:00:00Z | Notes: Added `architect-cloud/ui/README.md` and `architect-cloud/ui/ARCHITECTURE.md` to define the UI truthfully as an incubating React + Vite monitoring stub built around one polling `App.jsx` component. Expanded `architect-cloud/ui/STATUS.md` with the concrete current scope, limitations, and graduation criteria, and corrected `architect-cloud/README.md` so the parent cloud docs no longer claim a complete real-time dashboard or stale `npm run serve` workflow.
  - [x] Replace empty `lint` and `test` scripts with real tooling consistent with repo standards. | Finished: 2026-03-24T21:15:00Z | Notes: Added a real frontend toolchain to `architect-cloud/ui`: ESLint flat config (`eslint.config.js`), Vitest + jsdom wiring in `vite.config.js`, Testing Library setup in `src/test/setup.js`, and `src/App.test.jsx` coverage for the current fetch/error behavior. Replaced the empty `lint` and `test` scripts with `eslint .` and `vitest run`, updated the UI stub to render lightweight summary cards so the tests assert visible behavior, and validated the full loop with `npm run lint && npm test && npm run build`.
  - [x] Introduce typed state/API handling and component test coverage for the cloud UI. | Finished: 2026-03-24T21:50:00Z | Notes: Converted `architect-cloud/ui` from ad hoc JSX state to a small typed TypeScript structure: `src/types/cloud.ts` defines backend response/state contracts, `src/api/cloudApi.ts` centralizes typed REST fetches, `src/hooks/useCloudDashboard.ts` owns typed polling state, and `src/components/` contains focused `ErrorBanner` and `SummaryStats` components. Migrated entrypoints/tests to TS/TSX, added `tsconfig.json`, extended linting to run `tsc --noEmit`, and added component coverage for `App`, `ErrorBanner`, and `SummaryStats`. Verified with `npm run lint && npm test && npm run build` in `architect-cloud/ui`.
  - [x] For `architect-vscode`, replace ad hoc YAML parsing with a robust parser/model strategy. | Finished: 2026-03-24T21:58:00Z | Notes: Replaced the line-by-line indentation parser in `architect-vscode/src/taskTreeProvider.ts` with a shared YAML-backed config model in `src/architectConfigModel.ts` using the `yaml` package. The new model parses both top-level `tasks:` and `scripts.scripts:` task definitions, preserves description/phase metadata, and supports both `architect.yml` and `architect.yaml`. Added parser regression tests in `src/test/architectConfigModel.test.ts` and validated with `npm test` in `architect-vscode`.
  - [x] For `architect-vscode` and `architect-intellij`, add automated tests for extension/plugin behavior. | Finished: 2026-03-24T22:10:00Z | Notes: Added automated coverage for both IDE integrations. `architect-vscode` now validates its YAML-backed config model with parser regression tests executed by `npm test`. `architect-intellij` now includes plugin tests for schema-provider availability and task line-marker behavior, and the task also fixed two host-side issues uncovered while enabling that coverage: `ArchitectRunConfigurationType` now returns a proper `CommandLineState`, and `ArchitectTaskLineMarkerProvider` resolves YAML task keys by enclosing `YAMLKeyValue` position rather than relying on a brittle leaf-node shape. Verified with `cd architect-vscode && npm test` and `cd architect-intellij && gradle test`.
  - [x] Decide whether IDE integrations are supported products or thin reference integrations. | Finished: 2026-03-24T22:25:00Z | Notes: Classified `architect-vscode` and `architect-intellij` as thin reference integrations rather than supported products. The current code supports useful schema/task helpers and now has a basic automated test baseline, but both modules remain version `0.1.0`, depend on the external CLI, and lack marketplace distribution, release automation, and the broader UX/error-handling depth expected from supported IDE products. Updated the root status/docs plus each module's README/STATUS to make that support tier explicit.

- [ ] Validation
  - [x] Run UI lint/build/test. | Finished: 2026-03-24T22:35:00Z | Notes: Re-ran the current `architect-cloud/ui` validation loop with `npm run lint && npm test && npm run build`. All checks passed. The run still emits non-blocking warnings from `baseline-browser-mapping` freshness and Vite's React/esbuild deprecation notices, but those do not fail the lint, test, or build steps.
  - [x] Run VS Code extension tests. | Finished: 2026-03-24T22:40:00Z | Notes: Re-ran `npm test` in `architect-vscode`. The command recompiles the extension TypeScript sources and executes the parser-model regression suite in `out/test/**/*.test.js`; all 3 tests passed.
  - [x] Run IntelliJ plugin verification/tests as supported by the build. | Finished: 2026-03-24T22:45:00Z | Notes: Re-ran `gradle test` in `architect-intellij`. The plugin test suite passed, and the build also completed the built-in `verifyPluginConfiguration` step. The run still emits the Gradle IntelliJ advisory about Kotlin stdlib alignment with the IDE platform, but it does not fail the verification/test flow.

## Phase 8 — Standardize testing, compatibility, and release confidence

- [ ] **Priority**: High
- [ ] **Goal**: Make quality gates consistent across module types.
- [ ] **Rationale**: Current test quality is strongest where conventions exist and weakest where they do not.
- [ ] **Impacted areas**: all modules
- [ ] **Dependencies**: Phase 2 through Phase 7
- [ ] **Risks**: longer CI before optimizations
- [ ] **Expected outcomes**: predictable release confidence and fewer hidden regressions

- [ ] Tasks
  - [x] Define minimum test matrices for libraries, services, plugins, SDKs, frontend, and IDE tools. | Finished: 2026-03-24T22:55:00Z | Notes: Added `docs/guides/testing-standard.md` as the new repository-wide minimum test matrix, covering libraries, services, plugins, SDKs, frontend products, and IDE/editor integrations with support-tier expectations and suggested local validation commands. Linked the guide from `mkdocs.yml` and updated `CONTRIBUTING.md` so contributors now have one authoritative cross-module testing baseline.
  - [ ] Add compatibility tests between engine protocol handling and the SDK implementations.
  - [ ] Add smoke/integration suites for plugin loading across local, GitHub, and process plugin paths.
  - [ ] Stabilize and fix current CLI integration test failures.
  - [ ] Introduce release-readiness checks per support tier.

- [ ] Validation
  - [ ] Confirm the failing engine and CLI baselines are green.
  - [ ] Confirm official plugins and SDKs pass compatibility suites.

## Phase 9 — Refactor for simplicity, readability, and smaller units

- [ ] **Priority**: High
- [ ] **Goal**: Make the codebase cleaner and simpler to understand after boundary and governance issues are addressed.
- [ ] **Rationale**: Standardization alone does not guarantee low cognitive load.
- [ ] **Impacted areas**: core runtime stack, mature plugins, IDE integrations, cloud surfaces
- [ ] **Dependencies**: Phase 3, Phase 5, Phase 7
- [ ] **Risks**: accidental behavior changes during readability refactors
- [ ] **Expected outcomes**: smaller units, clearer naming, less indirection, easier maintenance

- [ ] Tasks
  - [ ] Identify the highest-cognitive-load classes/modules by size, branching, and overlapping responsibility.
  - [ ] Split overloaded classes into clearer collaborators with narrower responsibilities.
  - [ ] Rename ambiguous types, packages, and modules to better express intent.
  - [ ] Flatten deeply nested or redundant package structures where they hinder comprehension.
  - [ ] Remove stale abstractions, duplicate wrappers, and “utility dumping ground” patterns.
  - [ ] Add concise architecture comments only where code would otherwise remain hard to parse.

- [ ] Validation
  - [ ] Use targeted regression tests around refactored areas.
  - [ ] Review representative modules for lower file/class complexity and clearer ownership.
  - [ ] Verify new contributors can trace core flows with fewer jumps across modules.

## Phase 10 — Formalize non-functional engineering standards

- [ ] **Priority**: Medium
- [ ] **Goal**: Make maintainability, observability, security, and performance deliberate rather than incidental.
- [ ] **Rationale**: The repo has good local practices but weak cross-repo codification.
- [ ] **Impacted areas**: architecture docs, contributor docs, server/runtime products, plugins
- [ ] **Dependencies**: Phase 3, Phase 5, Phase 8, Phase 9
- [ ] **Risks**: standards without enforcement can become shelfware
- [ ] **Expected outcomes**: consistent engineering behavior across teams and modules

- [ ] Tasks
  - [ ] Define logging and error-handling conventions by module type.
  - [ ] Define observability expectations for engine/cloud surfaces.
  - [ ] Define security requirements for remote downloads, signatures, secrets, and generated workflows.
  - [ ] Define performance-testing triggers and ownership.
  - [ ] Add automated checks where possible, not just prose guidance.

- [ ] Validation
  - [ ] Verify standards are referenced by build/CI/tests/templates.
  - [ ] Verify at least one enforcement mechanism exists per standard category.

## Phase 11 — Rewrite the repository narrative

- [ ] **Priority**: Medium
- [ ] **Goal**: Make docs reflect the actual platform and its support levels.
- [ ] **Rationale**: Documentation drift currently hides the true shape of the repo.
- [ ] **Impacted areas**: root docs, component docs, plugin docs, architecture docs
- [ ] **Dependencies**: all earlier phases
- [ ] **Risks**: stale docs if done too early
- [ ] **Expected outcomes**: contributors can navigate, build, test, and extend the repo correctly

- [ ] Tasks
  - [ ] Rewrite the root `README.md` around the real repository topology.
  - [ ] Rewrite `CONTRIBUTING.md` around real workflows, support tiers, and quality gates.
  - [ ] Document the final repository decomposition and boundary model explicitly.
  - [ ] Add a supported-products/modules matrix.
  - [ ] Add a plugin maturity/support matrix.
  - [ ] Add architecture decision records or equivalent for key boundary decisions.

- [ ] Validation
  - [ ] Perform a fresh onboarding walkthrough from docs only.
  - [ ] Confirm no core instruction points to a missing or misleading workflow.

## Risks / Dependencies / Sequencing Notes

- [ ] Do not start broad plugin cleanup before dependency/version governance exists.
- [ ] Do not start large-scale code simplification refactors before canonical ownership and boundaries are decided.
- [ ] Do not merge architecture-boundary work without targeted regression coverage for plugin loading and execution.
- [ ] Treat placeholder-module decisions as product/portfolio governance, not just engineering cleanup.
- [ ] Delay major docs rewrites until support tiers and boundaries are decided.
- [ ] Frontend and IDE work should follow explicit product-scope decisions, not assumptions.

## Definition of Done

- [ ] The repository has a documented and truthful entry-point strategy for build/test/dev.
- [ ] Shared dependency and version governance is centralized.
- [ ] The repository has an explicit, understandable decomposition model and support-tier map.
- [ ] `architect-core` and `architect-engine` no longer carry duplicated authoritative implementations.
- [ ] Major code paths have been simplified into smaller, clearer responsibility units.
- [ ] All official plugins meet the defined plugin standard.
- [ ] CI uses reusable delivery patterns instead of cloned workflow logic.
- [ ] Placeholder/incubating modules have explicit status and ownership.
- [ ] Cloud UI and IDE extensions have real validation and support scope.
- [ ] Root and contributor docs match the actual repo.
- [ ] Baseline validation is green for all officially supported modules.

## Appendix: Evidence and File References

### Repository shape and docs drift

- [ ] Root structure shows many more major surfaces than the root architecture narrative: `README.md`, repository root listing.
- [ ] Root docs still instruct top-level `./gradlew build` / `./gradlew test`: `README.md:408-419`, `CONTRIBUTING.md:50-77`.

### Core runtime and duplication

- [ ] `architect-api/api/build.gradle.kts:1-116`
- [ ] `architect-core/core/build.gradle.kts:1-121`
- [ ] `architect-engine/engine/build.gradle.kts:1-126`
- [ ] Duplicated runtime classes:
  - [ ] `architect-core/core/src/main/kotlin/io/github/architectplatform/engine/core/plugin/app/ProjectPluginLoader.kt`
  - [ ] `architect-engine/engine/src/main/kotlin/io/github/architectplatform/engine/core/plugin/app/ProjectPluginLoader.kt`
  - [ ] `architect-core/core/src/main/kotlin/io/github/architectplatform/engine/core/execution/ClassLoaderResourceExtractor.kt`
  - [ ] `architect-engine/engine/src/main/kotlin/io/github/architectplatform/engine/core/execution/ClassLoaderResourceExtractor.kt`
  - [ ] `architect-core/core/src/main/kotlin/io/github/architectplatform/engine/core/plugin/infra/LocalPluginSource.kt`
  - [ ] `architect-engine/engine/src/main/kotlin/io/github/architectplatform/engine/core/plugin/infra/LocalPluginSource.kt`
  - [ ] `architect-core/core/src/main/kotlin/io/github/architectplatform/engine/core/secrets/SecretResolver.kt`
  - [ ] `architect-engine/engine/src/main/kotlin/io/github/architectplatform/engine/core/secrets/SecretResolver.kt`

### Cloud, UI, and IDE surfaces

- [ ] `architect-cloud/backend/build.gradle.kts:1-127`
- [ ] `architect-cloud/ARCHITECTURE.md:1-349`
- [ ] `architect-cloud/ui/package.json:1-23`
- [ ] `architect-vscode/package.json:1-142`
- [ ] `architect-vscode/src/extension.ts:1-89`
- [ ] `architect-vscode/src/taskTreeProvider.ts:1-96`
- [ ] `architect-intellij/build.gradle.kts:1-29`
- [ ] `architect-intellij/src/main/kotlin/io/github/architectplatform/intellij/ArchitectSchemaProviderFactory.kt:1-30`
- [ ] `architect-intellij/src/main/kotlin/io/github/architectplatform/intellij/ArchitectTaskLineMarkerProvider.kt:1-37`

### Plugin drift and inconsistency

- [ ] Representative mature plugin build/docs:
  - [ ] `plugins/docs-architected/app/build.gradle.kts:1-69`
  - [ ] `plugins/docs-architected/README.md:1-687`
  - [ ] `plugins/pipelines-architected/README.md:1-443`
- [ ] Representative thin plugin build files:
  - [ ] `plugins/go-architected/app/build.gradle.kts:1-67`
  - [ ] `plugins/nx-architected/app/build.gradle.kts:1-50`
  - [ ] `plugins/docker-architected/app/build.gradle.kts:1-69`
- [ ] Mixed plugin API dependency versions appear across plugin build files:
  - [ ] examples include `plugins/python-architected/app/build.gradle.kts`, `plugins/javascript-architected/app/build.gradle.kts`, `plugins/gradle-architected/app/build.gradle.kts`, `plugins/docs-architected/app/build.gradle.kts`

### Delivery and CI duplication

- [ ] Generated workflow pattern: `.github/workflows/architect-api-pipeline.yml:1-109`, `.github/workflows/architect-engine-pipeline.yml:1-109`, `.github/workflows/architect-cloud-ui.yml:1-66`
- [ ] Workflow headers explicitly state generation: multiple `.github/workflows/*.yml`
- [ ] Repeated setup patterns across workflows include JDK/Node setup, remote installer curls, and `architect engine start`

### SDK health

- [ ] `sdk/typescript/plugin-sdk/package.json:1-40`
- [ ] `sdk/typescript/plugin-sdk/README.md:1-86`
- [ ] `sdk/python/architect-plugin-sdk/pyproject.toml:1-32`
- [ ] `sdk/python/architect-plugin-sdk/README.md:1-75`
- [ ] `sdk/go/plugin-sdk-go/README.md:1-90`

### Baseline validation evidence

- [ ] Baseline validation session results captured during this audit:
  - [ ] `architect-api/api` tests passed
  - [ ] `architect-core/core` tests passed
  - [ ] `architect-engine/engine` test/compile baseline failed
  - [ ] `architect-cli/cli` integration test baseline failed
  - [ ] `architect-cloud/backend` tests passed
  - [ ] `architect-cloud/ui` lacks functional `lint`/`test` scripts

## Assumptions and Open Questions

- [ ] Assumption: the repository intends to behave as one platform repository, not just a loose collection of colocated projects.
- [ ] Assumption: `architect-core` is intended to be the shared runtime implementation layer rather than a parallel engine copy.
- [ ] Assumption: repository organization itself is in scope for refactoring, including moving or regrouping directories if that reduces confusion.
- [ ] Open question: which top-level modules are officially supported products versus incubating experiments?
- [ ] Open question: should official plugins include thin/template plugins today, or should the supported plugin set shrink until standards are met?
- [ ] Open question: should CI primarily validate Architect-generated workflows, direct tool invocations, or both?
- [ ] Open question: are IDE integrations and Cloud UI strategic products, or supporting demos/reference implementations?
