# release-architected status

- Status: incubating
- Last reviewed: 2026-03-27
- Owner group: Plugin Ecosystem

Current scope:
- Conventional-commit-aware release preparation with semantic, calendar, and manual versioning strategies.
- Multi-artifact publish orchestration for npm, Docker, and GitHub Releases.
- Safe local rollback for prepared files and tags.

Current limitations:
- GitHub Releases rely on the local `gh` CLI rather than direct API integration.
- Version-file updates intentionally cover common file formats and may need explicit configuration for highly custom layouts.

Graduation criteria:
- ✅ Plugin standard tests and contract coverage in place.
- ✅ README and reference docs updated with release task guidance.
- ✅ Config schema and dedicated CI workflow wired into the repo.
