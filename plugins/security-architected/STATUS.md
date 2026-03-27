# security-architected status

- Status: incubating
- Last reviewed: 2026-03-27
- Owner group: Plugin Ecosystem

Current scope:
- Security scanning via Trivy and Snyk, with CodeQL support through explicit database/query configuration.
- Dependency audits for npm, Python, and Cargo ecosystems.
- CycloneDX / SPDX SBOM generation with severity-threshold enforcement.

Current limitations:
- Built-in CodeQL execution requires a prepared database and query suite.
- Severity normalization depends on the scanner or audit tool emitting parseable JSON or SARIF.

Graduation criteria:
- ✅ Plugin standard tests and contract coverage in place.
- ✅ README and reference docs updated with task/config guidance.
- ✅ Config schema and structured security result output implemented.
