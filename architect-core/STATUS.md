# architect-core status

- Status: active
- Last reviewed: 2026-03-25
- Owner group: Platform Runtime

Current scope:
- Shared runtime library providing plugin loading, task execution, dependency resolution,
  secret management, project configuration parsing, and classloader infrastructure.
- Consumed by architect-engine and architect-cli.

Current limitations:
- Some runtime classes are still duplicated in architect-engine (convergence ongoing).

Graduation criteria:
- Already active. Maintains stable test baseline and clean architecture boundaries.
