# architect-api status

- Status: active
- Last reviewed: 2026-03-24
- Owner group: Platform Runtime

Current scope:
- Defines the authoritative contract library for the Architect platform.
- Provides core interfaces and abstractions: Task, Plugin, Phase, Environment, ProjectContext.
- Published to GitHub Packages for consumption by plugins and downstream modules.

Current limitations:
- API surface is stable; breaking changes require a coordinated version bump across all consumers.

Graduation criteria:
- Full test coverage of public interfaces and contract assertions.
- Semantic versioning enforced on every release to GitHub Packages.
