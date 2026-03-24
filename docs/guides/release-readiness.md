# Release Readiness Checks

Every module must pass release-readiness checks appropriate to its **support tier** before a version bump or release.

## Quick Start

```bash
# Check a specific module
./scripts/release-readiness-check.sh architect-api/api

# Override the tier (if auto-detection fails)
./scripts/release-readiness-check.sh plugins/docker-architected --tier incubating

# Check all active modules
for mod in architect-api/api architect-core/core; do
  ./scripts/release-readiness-check.sh "$mod"
done
```

## Checks by Tier

### Active / Beta (production-quality)

| Check | Description | Failure = block release? |
|---|---|---|
| README.md | Module must have a README | ✗ Yes |
| STATUS.md | Status document required | ✗ Yes |
| Version declared | build.gradle.kts / package.json / pyproject.toml | ✗ Yes |
| Test files exist | At least one test file in `test/` or `tests/` | ✗ Yes |
| Build configuration | A recognized build file must exist | ✗ Yes |
| SPI registration (plugins) | META-INF/services entry for `ArchitectPlugin` | ✗ Yes (active plugins) |
| Blocking TODOs | No `TODO.*block`, `FIXME.*release`, `HACK.*remove` in source | ⚠ Warning |

### Incubating

| Check | Description | Failure = block release? |
|---|---|---|
| README.md | Module should have a README | ✗ Yes |
| STATUS.md | Nice to have | ⚠ Warning |
| Version declared | Not required | ⚠ Warning |
| Test files exist | Not required | ⚠ Warning |
| Graduation criteria | STATUS.md should mention graduation path | ⚠ Warning |

## CI Integration

Add this step to your release workflow:

```yaml
- name: Release readiness check
  run: ./scripts/release-readiness-check.sh ${{ matrix.module-path }}
```

The script exits with:
- `0` — All required checks pass
- `1` — One or more checks failed (blocks release)
- `2` — Usage or configuration error

## Tier Detection

The script auto-detects the module's tier by:
1. Reading `<module>/STATUS.md` for a `Status:` line
2. Falling back to the root `STATUS.md` matrix
3. Defaulting to `incubating` if undetectable

Override with `--tier <active|beta|incubating>` when needed.
