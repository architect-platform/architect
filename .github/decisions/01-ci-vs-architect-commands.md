# ADR 01: CI Workflows — Architect CLI vs. Direct Build Tool Commands

**Status**: Accepted  
**Date**: 2026-03-24

## Context

The repository has two categories of CI workflow:

1. **Generated project pipelines** (`architect-*-pipeline.yml`, `*-architected.yml`): thin callers
   produced by the `github-pipeline` Architect task from the `classic-java-17.yml` template.
2. **Hand-maintained one-off workflows** (SAST, repo health, semantic-release helpers, actions
   etc.): created and edited directly by maintainers.

The question is: when should a workflow step call `architect <phase>` versus a direct build tool
command like `./gradlew test` or `npx semantic-release`?

## Decision

### Generated project pipelines → always delegate to Architect

The `build` and `release` jobs in `reusable-kotlin-pipeline.yml` call Architect phases
(`architect init`, `architect verify`, `architect build`, `architect test`, `architect release`,
`architect publish`). Architect resolves the correct plugins and tooling per project, so:

- Adding a new plugin to `architect.yml` automatically enriches the pipeline without a YAML edit.
- Policy changes (new phase, caching, secrets) are made once in the reusable workflow, not in
  every caller.

### Hand-maintained one-off workflows → direct tool commands

Workflows that perform a specific, cross-cutting check (linting, security scanning, freshness
checks) call tools directly (`./gradlew`, `npm`, shell scripts). Reasons:

- These checks operate outside any single project's lifecycle; there is no natural Architect phase
  to attach them to.
- They often need fine-grained control over flags, output format, and exit codes that Architect's
  phase system does not expose.
- They are not intended to ship with every module; coupling them to Architect would require all
  modules to opt in.

## Consequences

- New modules get CI by copying/generating their `architect.yml` and running the `github-pipeline`
  task; no CI YAML authoring required.
- Any change to build policy (e.g., new cache key strategy, Node.js version bump) is made once in
  `reusable-kotlin-pipeline.yml`.
- One-off governance workflows remain independent and cannot be accidentally modified by plugin
  authors publishing new Architect tasks.
- If a project genuinely needs a non-standard build step that Architect does not cover, the
  preferred path is to add a plugin task — not to edit the generated YAML directly.
