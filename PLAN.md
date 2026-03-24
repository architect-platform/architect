# Architect Repository Refactor Plan

## Status
Overall Progress: 53/288 tasks completed (18.4%)
Current Phase: Phase 4 — Rationalize repository portfolio and directory hygiene
Last Updated: 2026-03-24T15:51:42Z

## Executive Summary

This repository has a strong Kotlin core, but it is not operating as a coherent monorepo yet. The primary execution stack (`architect-api`, `architect-core`, `architect-engine`, `architect-cli`) is comparatively mature, while surrounding surfaces show drift: duplicated core logic between modules, inconsistent dependency versions, generated-and-repeated CI workflows, incomplete secondary products (`architect-data`, `architect-server`, `architect-x`), and several plugin/product surfaces that are present but not standardized. The refactoring need is not only technical correctness; it is also about decomposing the repository into understandable units, clarifying boundaries, and making the codebase materially simpler to navigate, reason about, and change.

The highest-priority refactoring goal is to convert the repository from a collection of partially aligned subprojects into a governed platform with one build strategy, one dependency/version strategy, one plugin standard, one testing standard, explicit ownership/boundary rules, and a repository structure whose decomposition is obvious to new contributors.

### Top Priorities

- [ ] **Critical**: Repair build/test trustworthiness and repository entry points.
- [ ] **Critical**: Reorganize the repository into clearer product, platform, plugin, SDK, and incubating areas.
- [ ] **Critical**: Remove architectural duplication between `architect-core` and `architect-engine`.
- [ ] **High**: Refactor code and package structure to make responsibilities smaller, boundaries clearer, and flows easier to understand.
- [ ] **High**: Standardize plugin dependency versions, documentation, tests, and packaging.
- [ ] **High**: Rationalize skeletal modules and committed build-state noise.
- [ ] **High**: Replace repeated generated CI workflow patterns with reusable workflow composition.
- [ ] **Medium**: Raise quality of UI/IDE surfaces and make their validation real.
- [ ] **Medium**: Formalize repository-wide engineering standards for logging, errors, docs, architecture, and observability.

## Repository Overview

The repository currently contains several categories of assets:

- [ ] **Core runtime stack**: `architect-api`, `architect-core`, `architect-engine`, `architect-cli`
- [ ] **Platform extensions and integrations**: `plugins/*`
- [ ] **Secondary products**: `architect-cloud`, `architect-intellij`, `architect-vscode`
- [ ] **SDKs**: `sdk/typescript`, `sdk/python`, `sdk/go`
- [ ] **Incomplete/placeholder areas**: `architect-data`, `architect-server`, `architect-x`
- [ ] **Docs, policy, and delivery surfaces**: `docs/`, `README.md`, `CONTRIBUTING.md`, `.github/workflows/`, root `architect.yml`

## Audit Method / Scope

- [x] Reviewed top-level structure and major subprojects.
- [x] Inspected representative build files, README files, source layouts, workflows, and configuration files.
- [x] Reviewed core architecture documents and selected implementation hotspots.
- [x] Compared plugin maturity and dependency drift across all plugin directories.
- [x] Ran representative baseline validation commands on existing modules.

### Baseline Validation Snapshot

- [x] `architect-api/api`: `./gradlew test` passed.
- [x] `architect-core/core`: `./gradlew test` passed; baseline report indicated 179 tests completed successfully.
- [ ] `architect-engine/engine`: `./gradlew test` currently fails during composite-built CLI compilation in `ArchitectLauncher.kt` (`taskService`, `TaskConditionChecker`, ambiguous `forEach`), so engine tests do not run.
- [ ] `architect-cli/cli`: `./gradlew test` currently fails at Kotlin compilation in `ArchitectLauncher.kt` before test execution.
- [x] `architect-cloud/backend`: `./gradlew test` passed; baseline report indicated 57 tests completed successfully.
- [ ] `architect-cloud/ui`: cannot be meaningfully validated as-is; `package.json` declares empty `lint` and `test` scripts.

## Cross-Repository Findings

### 1. Build and repository ergonomics are fragmented

- [ ] Root documentation advertises `./gradlew build` and `./gradlew test`, but the repository root does not provide a root Gradle wrapper or unified orchestrator.
- [ ] Build execution is distributed across many nested wrappers, which increases onboarding friction and causes drift in conventions and plugin usage.
- [ ] The repo behaves like a monorepo from a documentation/product perspective, but like a set of disconnected repos operationally.

### 2. Version and dependency management drift is significant

- [ ] Core modules pin different artifact versions: for example `architect-api/api/build.gradle.kts` is `2.1.0`, `architect-core/core/build.gradle.kts` and `architect-engine/engine/build.gradle.kts` are `1.6.1`, `architect-cli/cli/build.gradle.kts` is `1.1.0`.
- [ ] Plugin API dependencies are inconsistent: plugins depend on `io.github.architectplatform:api` at `1.1.2`, `1.1.3`, and `2.1.0`.
- [ ] Kotlin and coroutines version forcing is copy-pasted in multiple module build files, with comments that do not match the declared versions.
- [ ] The repository lacks a central version catalog or convention plugin, so the same dependency rules are manually duplicated.

### 3. Architectural duplication exists in core runtime code

- [ ] Classes with the same names and responsibilities exist in both `architect-core` and `architect-engine`, including:
  - [ ] `ClassLoaderResourceExtractor.kt`
  - [ ] `ProjectPluginLoader.kt`
  - [ ] `LocalPluginSource.kt`
  - [ ] `SecretResolver.kt`
- [ ] The duplicate `ProjectPluginLoader` implementations have already diverged in behavior and integration style, which creates long-term correctness and maintenance risk.
- [ ] Shared logic is not yet clearly assigned to a single authoritative layer.

### 4. Repository portfolio management is unclear

- [ ] `architect-data`, `architect-server`, and `architect-x` currently read more like placeholders or remnants than actively governed products.
- [ ] Some of these areas contain build artifacts or metadata without corresponding source structure, which blurs product boundaries and creates noise.
- [ ] There is no clear repo-wide statement of module status such as active, incubating, deprecated, placeholder, or archived.
- [ ] The top-level layout does not yet clearly communicate which directories are platform internals, end-user products, extension ecosystems, SDKs, or experiments.

### 4a. Repository decomposition and navigability are under-designed

- [ ] The current top-level layout is historically accumulated rather than intentionally decomposed for discoverability.
- [ ] Similar concepts are split across multiple places (`architect-api`, `architect-core`, `architect-engine`, plugins, SDKs, IDE integrations) without a simple contributor mental model.
- [ ] There is no clear repository taxonomy such as:
  - [ ] `platform/` or equivalent for core runtime stack
  - [ ] `products/` for user-facing applications
  - [ ] `plugins/` for official extensions
  - [ ] `sdk/` for external integration SDKs
  - [ ] `incubating/` or equivalent for incomplete explorations
- [ ] The plan should therefore include structural reorganization, not just local code refactors.

### 5. Plugin ecosystem quality is inconsistent

- [ ] Mature plugins (`docs-architected`, `git-architected`, `gradle-architected`, `scripts-architected`, `github-architected`, `pipelines-architected`) coexist with thin plugins that have minimal tests and no docs/readme surface.
- [ ] Several plugins have only a few Kotlin files and a single test, suggesting template-level maturity rather than production-level maturity.
- [ ] Contract-test adoption is partial rather than standard across plugins.
- [ ] Plugin packaging, docs, examples, and resource layout are inconsistent.

### 6. Delivery automation is highly repetitive

- [ ] GitHub Actions workflows are largely generated clones with near-identical setup sequences.
- [ ] Many workflows repeat remote installer curls, JDK setup, Node setup, and `architect engine start`.
- [ ] This repetition increases maintenance cost and makes policy changes hard to roll out uniformly.

### 7. Docs and contributor guidance drift from reality

- [ ] Root docs describe the repo as a simpler three-component system, while the actual repository contains additional major products and placeholders.
- [ ] Root and contributor docs imply top-level build/test flows that do not exist in the repository layout.
- [ ] Multiple readmes describe idealized capabilities that exceed the actual maturity of some modules.

### 8. Quality gates are uneven across technology stacks

- [ ] Kotlin core surfaces have reasonable test discipline and coverage gates.
- [ ] `architect-cloud/ui` currently has no meaningful lint/test command configured.
- [ ] IDE extensions have minimal functionality and no visible automated tests.
- [ ] SDKs are in better shape than some product surfaces, but they are not visibly integrated into a repo-wide release and compatibility story.

## Subproject-by-Subproject Findings

### `architect-api`

- [ ] Strengths:
  - [ ] Clear core abstractions and workflow contracts.
  - [ ] Strong test posture and explicit quality gates (`ktlint`, `jacoco`, `pitest`).
  - [ ] Good candidate for becoming the single source of platform contracts and plugin quality tooling.
- [ ] Gaps:
  - [ ] Needs stronger repo-wide leverage; many downstream modules are not uniformly aligned to its standards or version.

### `architect-core`

- [ ] Strengths:
  - [ ] Rich shared runtime logic.
  - [ ] Strong test base and some performance orientation (`jmh`, `pitest`, `jacoco`).
- [ ] Gaps:
  - [ ] Boundary with `architect-engine` is not fully enforced.
  - [ ] Shared runtime responsibilities are duplicated rather than centralized.

### `architect-engine`

- [ ] Strengths:
  - [ ] Mature execution surface and plugin runtime responsibilities.
  - [ ] Broad test coverage and integration scenarios.
- [ ] Gaps:
  - [ ] Baseline compilation/test trust is currently broken.
  - [ ] Owns behavior that appears duplicated with `architect-core`.
  - [ ] Needs an explicit convergence plan with `architect-core`.

### `architect-cli`

- [ ] Strengths:
  - [ ] Feature-rich CLI with embedded/daemon execution modes.
  - [ ] Good breadth of tests and product features.
- [ ] Gaps:
  - [ ] Current baseline shows failing integration tests.
  - [ ] Depends on a runtime stack whose contracts and packaging need stronger stability guarantees.

### `architect-cloud`

- [ ] Backend:
  - [ ] Strongest architectural documentation in the repo.
  - [ ] Clear hexagonal design and ArchUnit enforcement.
  - [ ] Baseline tests pass.
- [ ] UI:
  - [ ] Very small surface relative to stated product scope.
  - [ ] `lint` and `test` scripts are empty.
  - [ ] Requires a real frontend architecture, state model, testing, and observability story.

### `architect-intellij`

- [ ] Strengths:
  - [ ] Useful schema association and light IDE integration.
- [ ] Gaps:
  - [ ] Minimal feature depth.
  - [ ] No visible automated tests.
  - [ ] Needs product decision: thin schema helper vs first-class IDE integration.

### `architect-vscode`

- [ ] Strengths:
  - [ ] Useful starter extension capabilities around task execution and schema validation.
- [ ] Gaps:
  - [ ] Parses YAML manually in `src/taskTreeProvider.ts`, which is brittle.
  - [ ] Uses untyped `child_process` spawning directly in `src/extension.ts`.
  - [ ] No visible automated tests.
  - [ ] Needs stronger extension architecture, resilience, and UX behavior.

### `sdk`

- [ ] Strengths:
  - [ ] TypeScript, Python, and Go SDKs all document protocol coverage and include tests/examples.
  - [ ] Better cross-language consistency than several product/plugin surfaces.
- [ ] Gaps:
  - [ ] No obvious repo-level release/version compatibility policy tied back to engine/plugin protocol evolution.
  - [ ] Need conformance and compatibility testing against the authoritative protocol implementation.

### `architect-data`, `architect-server`, `architect-x`

- [ ] Current state is unclear and should be treated as a portfolio problem, not just a cleanup task.
- [ ] These directories need an explicit decision:
  - [ ] promote to active products,
  - [ ] mark as incubating with owners and standards,
  - [ ] archive/remove from mainline repo,
  - [ ] or convert into documented placeholders with no misleading build/docs claims.

### `plugins/*`

- [ ] Stronger/more complete plugin surfaces:
  - [ ] `docs-architected`
  - [ ] `git-architected`
  - [ ] `gradle-architected`
  - [ ] `github-architected`
  - [ ] `scripts-architected`
  - [ ] `pipelines-architected`
- [ ] Thin or template-level plugin surfaces:
  - [ ] `docker-architected`
  - [ ] `go-architected`
  - [ ] `kubernetes-architected`
  - [ ] `maven-architected`
  - [ ] `nx-architected`
  - [ ] `python-architected`
  - [ ] `rust-architected`
  - [ ] `terraform-architected`
- [ ] Cross-plugin problems:
  - [ ] API version drift.
  - [ ] Inconsistent docs/readme coverage.
  - [ ] Partial contract-test usage.
  - [ ] Repeated build logic.
  - [ ] No visible shared plugin convention/build plugin.

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
  - [ ] Add lightweight status docs for incubating modules if they remain in-tree.
  - [ ] Update root docs and navigation to reflect the actual supported module set.

- [ ] Validation
  - [ ] Confirm each remaining top-level directory has a declared status and reason to exist.
  - [ ] Confirm ignored/generated files are not tracked unintentionally.

## Phase 5 — Standardize the plugin platform

- [ ] **Priority**: High
- [ ] **Goal**: Make every official plugin conform to the same engineering contract.
- [ ] **Rationale**: The plugin ecosystem is the main extensibility story, but maturity is inconsistent.
- [ ] **Impacted areas**: all `plugins/*`
- [ ] **Dependencies**: Phase 2
- [ ] **Risks**: exposing weak or incomplete plugins that need downgrading in support level
- [ ] **Expected outcomes**: uniform plugin quality, lower onboarding cost, easier release management

- [ ] Tasks
  - [ ] Define the minimum official plugin standard:
    - [ ] README
    - [ ] docs surface
    - [ ] architect config example
    - [ ] unit tests
    - [ ] plugin contract test
    - [ ] version alignment
    - [ ] release/publish metadata
  - [ ] Apply the standard to all mature plugins first.
  - [ ] For thin/template plugins, choose one:
    - [ ] promote and complete
    - [ ] mark experimental
    - [ ] remove from official set
  - [ ] Extract common plugin build/test conventions.
  - [ ] Standardize resource layout and task registration patterns.
  - [ ] Standardize plugin internal package/layout patterns so plugin code is easy to scan and compare across the ecosystem.

- [ ] Validation
  - [ ] Run plugin tests in batches by maturity tier.
  - [ ] Confirm all official plugins pass contract tests.
  - [ ] Confirm every official plugin has docs/examples aligned to actual capabilities.

## Phase 6 — Simplify CI/CD and delivery automation

- [ ] **Priority**: High
- [ ] **Goal**: Replace cloned workflow logic with reusable delivery patterns.
- [ ] **Rationale**: CI duplication makes policy, caching, and reliability improvements expensive.
- [ ] **Impacted areas**: `.github/workflows/*`, generated workflow templates, plugins that emit workflows
- [ ] **Dependencies**: Phase 2, Phase 5
- [ ] **Risks**: workflow behavior changes during migration
- [ ] **Expected outcomes**: leaner CI, lower maintenance cost, consistent policy rollout

- [ ] Tasks
  - [ ] Inventory repeated workflow steps and convert them into reusable workflows or composite actions.
  - [ ] Separate generated workflow templates from hand-maintained CI policy logic.
  - [ ] Standardize caching, runtime setup, permissions, and release gates.
  - [ ] Decide when CI should invoke Architect-generated behavior versus direct build tool commands.
  - [ ] Add a CI validation rule for generated workflow drift if generation remains part of the model.

- [ ] Validation
  - [ ] Dry-run equivalent CI paths for API, engine, CLI, cloud, and plugin modules.
  - [ ] Confirm workflow count or repeated step volume drops materially.

## Phase 7 — Raise product-surface quality outside the Kotlin core

- [ ] **Priority**: High
- [ ] **Goal**: Bring frontend and IDE surfaces up to explicit, supportable standards.
- [ ] **Rationale**: These products are visible but currently under-specified and under-tested.
- [ ] **Impacted areas**: `architect-cloud/ui`, `architect-vscode`, `architect-intellij`
- [ ] **Dependencies**: Phase 0, Phase 2
- [ ] **Risks**: feature expectations may exceed current intended scope
- [ ] **Expected outcomes**: honest scope, real tests, better user-facing quality

- [ ] Tasks
  - [ ] For `architect-cloud/ui`, define the actual product scope and frontend architecture.
  - [ ] Replace empty `lint` and `test` scripts with real tooling consistent with repo standards.
  - [ ] Introduce typed state/API handling and component test coverage for the cloud UI.
  - [ ] For `architect-vscode`, replace ad hoc YAML parsing with a robust parser/model strategy.
  - [ ] For `architect-vscode` and `architect-intellij`, add automated tests for extension/plugin behavior.
  - [ ] Decide whether IDE integrations are supported products or thin reference integrations.

- [ ] Validation
  - [ ] Run UI lint/build/test.
  - [ ] Run VS Code extension tests.
  - [ ] Run IntelliJ plugin verification/tests as supported by the build.

## Phase 8 — Standardize testing, compatibility, and release confidence

- [ ] **Priority**: High
- [ ] **Goal**: Make quality gates consistent across module types.
- [ ] **Rationale**: Current test quality is strongest where conventions exist and weakest where they do not.
- [ ] **Impacted areas**: all modules
- [ ] **Dependencies**: Phase 2 through Phase 7
- [ ] **Risks**: longer CI before optimizations
- [ ] **Expected outcomes**: predictable release confidence and fewer hidden regressions

- [ ] Tasks
  - [ ] Define minimum test matrices for libraries, services, plugins, SDKs, frontend, and IDE tools.
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
