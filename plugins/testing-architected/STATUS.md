# testing-architected status

- Status: incubating
- Last reviewed: 2026-03-27
- Owner group: Plugin Ecosystem

Current scope:
- Unified testing entrypoints for unit, integration, e2e, and coverage tasks.
- Auto-detection for Gradle/JUnit, pytest, Jest, Vitest, Go, and Cargo projects.
- Coverage aggregation and threshold enforcement across common report formats.

Current limitations:
- Flaky-test retry behavior depends on framework-specific command overrides.
- Cargo coverage assumes `cargo llvm-cov` unless you provide an explicit coverage command.

Graduation criteria:
- ✅ Plugin standard tests and contract coverage in place.
- ✅ README and reference docs updated with configuration and task guidance.
- ✅ Config schema and structured coverage output implemented.
