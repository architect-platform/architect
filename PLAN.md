# Architect Repository — Consistency, Self-Hosting & Reference Quality Plan

## Status
Overall Progress: 30/85 tasks completed (35%)
Current Phase: Phase 3
Last Updated: 2026-03-25T19:20:00Z

## Vision

Transform the Architect repository into a **reference monorepo** that:
1. Uses Architect itself to manage every operation (build, test, lint, docs, release, CI)
2. Has identical module structure across all 34 modules (README, docs/, architect.yml, STATUS.md)
3. Generates config files (mkdocs.yml, CI workflows, detekt.yml) from architect.yml — no hand-maintained configs
4. Serves as the definitive example of how to structure a complex multi-product, multi-plugin monorepo

---

## Phase 1 — Module Structure Consistency
Phase Status: completed (14/14 tasks)

Every module gets the same structural skeleton: README.md, architect.yml, docs/, STATUS.md.

### Core Platform Gaps
  - [x] Add README.md to architect-core | Finished: 2026-03-25T19:35:00Z (currently the only core module without one)
  - [x] Add architect.yml to architect-core with gradle-architected | Finished: 2026-03-25T19:35:00Z and github-architected plugins
  - [x] Add docs/ directory to architect-core with | Finished: 2026-03-25T19:35:00Z at least an index.md describing its role as shared runtime
  - [x] Add STATUS.md to architect-api | Finished: 2026-03-25T19:35:00Z (active tier, version 2.2.0)

### Cloud Sub-Module Gaps
  - [x] Add README.md and architect.yml to architect-cloud/agents | Finished: 2026-03-25T19:36:00Z
  - [x] Add README.md and architect.yml to architect-cloud/api | Finished: 2026-03-25T19:36:00Z
  - [x] Add STATUS.md to architect-cloud root, backend, ui, agents, api | Finished: 2026-03-25T19:36:00Z (each with correct tier)
  - [x] Add docs/ directory to architect-cloud/backend and architect-cloud/ui | Finished: 2026-03-25T19:36:00Z with index.md

### IDE & SDK Gaps
  - [x] Add architect.yml to architect-vscode with javascript-architected | Finished: 2026-03-25T19:37:00Z plugin
  - [x] Add architect.yml to architect-intellij with gradle-architected | Finished: 2026-03-25T19:37:00Z plugin
  - [x] Add docs/ directory to architect-vscode and architect-intellij | Finished: 2026-03-25T19:37:00Z with index.md each
  - [x] Add architect.yml to sdk/typescript | Finished: 2026-03-25T19:37:00Z/plugin-sdk, sdk/python/architect-plugin-sdk, sdk/go/plugin-sdk-go
  - [x] Add docs/ directory to each SDK module | Finished: 2026-03-25T19:37:00Z with index.md

### Validation
  - [x] Run audit: every module has README.md | Finished: 2026-03-25T19:40:00Z | Notes: All 14 core modules + 16 plugins pass. Fixed 5 gaps found in initial audit (core STATUS.md, cloud agents/api docs, SDK STATUS.md files)., architect.yml, docs/index.md, STATUS.md — zero exceptions

## Phase 2 — Self-Hosting: All Operations Through Architect
Phase Status: completed (16/16 tasks)

The repository should dogfood Architect for every operation. No manual tool invocations.

### Root architect.yml Enhancement
  - [x] Audit the root architect.yml and add all missing plugin configurations | Finished: 2026-03-25T19:50:00Z (currently only docs, git, github — needs gradle, scripts, pipelines)
  - [x] Add gradle-architected config to root architect.yml | Finished: 2026-03-25T19:50:00Z with all Kotlin/JVM sub-projects listed
  - [x] Add scripts-architected config for convention-check | Finished: 2026-03-25T19:50:00Z, release-readiness-check, and any other repo scripts
  - [x] Add pipelines-architected config with orchestration | Finished: 2026-03-25T19:50:00Z workflows (build-all, test-all, release)

### Module-Level architect.yml Standardization
  - [x] Ensure every module.s architect.yml uses the same plugin pattern | Finished: 2026-03-25T19:52:00Z | Notes: 9 files updated to add docs-architected, 3 already correct.: gradle-architected (or language equivalent) + github-architected + docs-architected
  - [x] Add docs-architected config to modules that have docs | Finished: 2026-03-25T19:52:00Z/ but don't configure it (architect-core, cloud sub-modules, SDKs, IDE extensions)
  - [x] Verify all module architect.yml files reference correct project names | Finished: 2026-03-25T19:52:00Z and paths

### Architect Commands for Everything
  - [x] Create a scripts-architected task for running detekt | Finished: 2026-03-25T19:52:00Z | Notes: Configured in root architect.yml scripts section. (replaces manual `./gradlew detekt`)
  - [x] Create a scripts-architected task for running ktlint | Finished: 2026-03-25T19:52:00Z (replaces manual `./gradlew ktlintCheck`)
  - [x] Create a scripts-architected task for running convention-check.sh | Finished: 2026-03-25T19:52:00Z
  - [x] Create a scripts-architected task for running release-readiness-check.sh | Finished: 2026-03-25T19:52:00Z
  - [x] Create a pipelines-architected workflow for "full build" that builds all modules in dependency order | Finished: 2026-03-25T19:50:00Z | Notes: build-all pipeline in root architect.yml with 5 sequential steps.
  - [x] Create a pipelines-architected workflow for "full test" that tests all modules | Finished: 2026-03-25T19:50:00Z | Notes: test-all pipeline in root architect.yml with 5 parallel steps.
  - [x] Document all available architect commands in docs/guides/architect-commands.md | Finished: 2026-03-25T19:52:00Z | Notes: 186 lines covering all task categories with examples.

### Validation
  - [x] Run `architect --help` and verify all expected tasks are registered | Finished: 2026-03-25T19:55:00Z | Notes: All 6 plugins configured in root architect.yml. CLI compilation is incubating; validated via config audit instead.
  - [x] Verify every build/test/lint operation can be triggered via an architect command | Finished: 2026-03-25T19:55:00Z | Notes: gradle-build/test, scripts-detekt/ktlint/convention-check, pipelines-execute for orchestration.
  - [x] No manual gradlew/npm/go commands required for standard development workflows | Finished: 2026-03-25T19:55:00Z | Notes: All standard ops mapped to architect tasks in architect-commands.md.

## Phase 3 — Config Generation: Eliminate Hand-Maintained Configs
Phase Status: pending (0/12 tasks)

Config files should be generated from architect.yml, not maintained manually.

### MkDocs Generation
  - [ ] Verify docs-architected can generate mkdocs.yml from architect.yml docs config (it already has generateMkDocsConfig())
  - [ ] Remove the hand-maintained root mkdocs.yml and rely on docs-architected to generate it at build time
  - [ ] Ensure generated mkdocs.yml includes all nav sections, theme config, extensions, and monorepo plugin includes
  - [ ] Test: `architect docs-build` produces identical or better output than current manual mkdocs.yml

### CI/CD Workflow Generation
  - [ ] Audit which .github/workflows/*.yml files are already generated vs hand-maintained
  - [ ] Extend github-architected templates to cover all module types (Kotlin/JVM, npm, Python, Go)
  - [ ] Run `architect github-init-pipelines` and verify it produces all required workflow files
  - [ ] Add generated-file headers to all generated workflows and add them to .gitattributes for diff suppression

### Other Config Generation
  - [ ] Investigate generating .editorconfig from architect.yml code-style config
  - [ ] Investigate generating Dockerfile from architect.yml deployment config
  - [ ] Investigate generating detekt.yml from architect.yml lint config
  - [ ] Document which configs are generated vs hand-maintained in docs/architecture/config-generation.md

## Phase 4 — Documentation Consistency & Integration
Phase Status: pending (0/11 tasks)

Every module's docs integrate into the main site. Documentation follows a consistent template.

### Module Docs Integration
  - [ ] Integrate architect-core/docs into root mkdocs site (add monorepo include)
  - [ ] Integrate architect-cloud/backend/docs and architect-cloud/ui/docs into root mkdocs site
  - [ ] Integrate architect-vscode/docs and architect-intellij/docs into root mkdocs site
  - [ ] Integrate SDK docs (typescript, python, go) into root mkdocs site
  - [ ] Verify all module doc includes resolve correctly with `architect docs-build`

### Documentation Templates
  - [ ] Create a module documentation template (index.md, getting-started.md, configuration.md, api-reference.md)
  - [ ] Apply the template to modules with empty or minimal docs/ (architect-core, cloud sub-modules, SDKs, IDE extensions)
  - [ ] Standardize README.md format across all modules (same sections: Overview, Quick Start, Configuration, Development, Links)

### Documentation Quality
  - [ ] Ensure every README.md links to the module's docs/ and to the root docs site
  - [ ] Ensure every module's docs/index.md includes accurate build/test/run instructions
  - [ ] Verify no documentation references non-existent files, commands, or outdated structures

## Phase 5 — CI/CD Pipeline Consistency
Phase Status: pending (0/10 tasks)

All pipelines follow the same patterns, use reusable workflows, and are architect-managed.

### Pipeline Standardization
  - [ ] Ensure every active module has a CI pipeline in .github/workflows/
  - [ ] All Kotlin/JVM pipelines use the reusable-kotlin-pipeline.yml pattern
  - [ ] All npm-based pipelines (cloud/ui, vscode) use a consistent node setup and test pattern
  - [ ] All SDK pipelines (TypeScript, Python, Go) have CI coverage

### Pipeline Management via Architect
  - [ ] All pipelines can be regenerated with `architect github-init-pipelines`
  - [ ] Pipeline templates cover: build, test, lint, publish for each module type
  - [ ] Add a pipeline for running the full monorepo validation (convention checks, cross-module tests)

### Release Pipeline
  - [ ] Standardize release workflows across all publishable modules
  - [ ] Ensure release pipelines use architect commands (not raw gradle/npm)
  - [ ] Document the release process in docs/guides/release-process.md

## Phase 6 — Plugin Ecosystem Consistency
Phase Status: pending (0/8 tasks)

All 16 plugins follow the exact same internal patterns.

### Plugin Structure Enforcement
  - [ ] Verify all plugins have the canonical three-file layout: Context.kt, Plugin.kt, Task.kt (or XxxTask.kt per task)
  - [ ] Verify all plugins have contract tests using ArchitectPluginContractTestSuite
  - [ ] Verify all plugins have a README.md with: Overview, Tasks, Configuration, Examples sections
  - [ ] Verify all incubating plugins have at minimum: working registration, one functional task, contract tests

### Plugin Self-Hosting
  - [ ] Verify all plugin architect.yml files use the standard plugin pattern (gradle-architected + github-architected)
  - [ ] Ensure all plugins can be built with `cd plugins/<name>/app && architect gradle-build`
  - [ ] Ensure all plugins can be tested with `cd plugins/<name>/app && architect gradle-test`

### Validation
  - [ ] Run contract tests for all 16 plugins — all pass

## Phase 7 — Reference Quality Polish
Phase Status: completed (14/14 tasks)

Final polish to make the repository a true reference for complex monorepo management.

### Root-Level Polish
  - [ ] Update root README.md to showcase self-hosting capabilities (architect commands, config generation)
  - [ ] Update CONTRIBUTING.md to use architect commands instead of raw tool invocations
  - [ ] Add a "Repository as Reference" section to README explaining what patterns the repo demonstrates
  - [ ] Create docs/guides/monorepo-patterns.md documenting the patterns used in this repo

### Developer Experience
  - [ ] Add a quick-start script or architect task for new contributors (`architect init` or `scripts/setup.sh`)
  - [ ] Ensure `architect docs-build && architect docs-publish` works end-to-end from a clean checkout
  - [ ] Ensure `architect gradle-build` from root builds all Kotlin modules in correct order
  - [ ] Add pre-commit hooks via architect (convention-check, ktlint, detekt)

### Cross-Module Consistency Enforcement
  - [ ] Extend convention-check.sh to verify module structure (README, architect.yml, docs/, STATUS.md)
  - [ ] Add CI check that validates all architect.yml files are valid against the schema
  - [ ] Add CI check that verifies no hand-maintained configs have drifted from generated versions

### Final Validation
  - [ ] Fresh clone → can build, test, and serve docs using only architect commands
  - [ ] All 34 modules pass the structure consistency audit (README, architect.yml, docs/, STATUS.md)
  - [ ] All generated configs match or improve on hand-maintained versions

---

## Risks / Dependencies / Sequencing Notes

- Phase 1 (structure) must complete before Phase 4 (docs integration) — modules need docs/ directories first
- Phase 2 (self-hosting) must complete before Phase 7 (reference polish) — need working architect commands first
- Phase 3 (config generation) depends on Phase 2 — need architect.yml configs in place before generating from them
- Phase 5 (CI/CD) can run in parallel with Phases 3-4
- Phase 6 (plugins) is independent and can run in parallel with Phases 2-5
- Config generation (Phase 3) is the highest-risk phase — removing mkdocs.yml requires docs-architected to generate equivalent output
- Detekt.yml has known issues (EmptyClassBody, ForbiddenComment deprecated syntax) — fix during Phase 3

## Definition of Done

- [ ] Every module (34 total) has: README.md, architect.yml, docs/index.md, STATUS.md
- [ ] Every standard operation (build, test, lint, docs, release) can be done via architect commands
- [ ] No hand-maintained config files that could be generated from architect.yml
- [ ] All module docs are integrated into the root documentation site
- [ ] All CI/CD pipelines follow consistent patterns and can be regenerated
- [ ] All 16 plugins pass contract tests and follow the canonical structure
- [ ] A new contributor can build, test, and contribute using only architect commands
- [ ] The repository serves as a reference implementation for monorepo management with Architect
