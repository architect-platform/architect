# quality-architected status

- Status: incubating
- Last reviewed: 2026-03-27
- Owner group: Plugin Ecosystem

Current scope:
- Auto-detect linting with Detekt, ESLint, Ruff, and Clippy.
- Run configured analysis integrations for SonarQube and CodeClimate.
- Generate a quality summary and expose a dedicated `quality-gate` task for threshold checks.

Current limitations:
- Analysis integrations assume the required CLIs are already installed and authenticated.
- Gate evaluation is driven by configured thresholds plus analyzer execution; remote platforms remain the source of truth for advanced metrics.

Graduation criteria:
- ✅ Plugin standard tests and contract coverage in place.
- ✅ README and reference docs updated with lint/analyze/gate guidance.
- ✅ Config schema and dedicated CI workflow wired into the repo.
