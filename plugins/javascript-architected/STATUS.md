# javascript-architected status

- Status: active
- Last reviewed: 2026-03-27
- Owner group: Plugin Ecosystem

Current scope:
- JavaScript package-manager workflows (npm, yarn classic + berry, pnpm, bun).
- Monorepo workspace detection and lockfile validation.
- Security auditing, version bumping, and package publishing tasks.

Current limitations:
- Publish flows still depend on registry credentials and CI policy outside plugin control.

Graduation criteria:
- ✅ Plugin standard tests and contract coverage in place.
- ✅ Stable behavior across supported package managers and lockfile validation paths.
- ✅ Documentation updated with configuration and task guidance.
