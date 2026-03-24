# Official Plugin Standard

This guide defines the minimum quality bar for first-party plugins under `plugins/`.

## Scope

Applies to all official plugins that are intended to remain in the supported set.

Support tiers:
- `active`: Meets the full standard and is supportable.
- `incubating`: Incomplete against this standard; must publish gaps and a plan.

## Minimum Standard Checklist

Every official plugin must provide the following:

1. README
- Clear purpose and support tier.
- Task list with short behavior notes.
- Local build and test commands.

2. Docs surface
- A docs page under `docs/reference/plugins/` describing configuration and tasks.
- At least one runnable example.

3. Architect config example
- Example `architect.yml` snippet covering the plugin's main usage path.
- Example must match the plugin's current context schema and task IDs.

4. Unit tests
- Positive-path task execution coverage.
- At least one error-path or validation-path test.

5. Plugin contract test
- Use the `ArchitectPluginContractTestSuite` from `architect-api`.
- Verify task registration shape, plugin metadata, and context mapping behavior.

6. Version alignment
- Kotlin/Gradle/dependency versions must follow repository alignment conventions.
- No plugin-local version drift without explicit justification.

7. Release/publish metadata
- Declared plugin version and release intent.
- Changelog or release notes source path.
- Publish path documented (or explicit internal-only declaration).

## Required Repository Layout

Each plugin should follow this baseline layout:

```text
plugins/<plugin>/
├── README.md
├── STATUS.md
├── architect.yml
└── app/
    ├── build.gradle.kts
    ├── src/main/kotlin/...Plugin.kt
    ├── src/main/resources/META-INF/services/
    │   └── io.github.architectplatform.api.core.plugins.ArchitectPlugin
    └── src/test/kotlin/
```

## Internal Code Layout Convention

All plugin source lives under `io.github.architectplatform.plugins.<name>`. The
canonical three-file minimum is:

```text
src/main/kotlin/io/github/architectplatform/plugins/<name>/
├── <Name>Context.kt     — Plugin configuration / context data class
├── <Name>Plugin.kt      — ArchitectPlugin implementation; calls register()
└── <Name>Task.kt        — Default task implementation (top-level class)
```

**Rules:**

- Tasks must live in a dedicated top-level `<Name>Task.kt` file, not as inner
  classes of `<Name>Plugin.kt`.
- When a plugin registers multiple distinct task classes, each gets its own file
  (e.g. `<Name>BuildTask.kt`, `<Name>PublishTask.kt`).
- Utility/helper logic is extracted to `<Name>Utils.kt` when it is reusable
  across tasks. Do not create a generic `Utils.kt`.
- Sub-packages (`builders/`, `dto/`, `publishers/`) are only introduced when a
  plugin is complex enough that a flat layout hurts readability. Incubating
  plugins stay flat.
- File and class names follow the plugin prefix precisely, matching the plugin
  directory name (e.g. `docs-architected` → `Docs` prefix throughout).

## Maturity Rules

For `active` tier:
- All checklist items above are required.
- Tests and contract checks must pass in CI.
- Docs must reflect actual behavior.

For `incubating` tier:
- Must still include `README.md`, `STATUS.md`, and `architect.yml` example.
- Must declare missing checklist items and target graduation criteria.

## Verification Commands

Use plugin-local validation while iterating:

```bash
cd plugins/<plugin>/app
./gradlew test
```

When contract tests are present, run them explicitly if needed:

```bash
cd plugins/<plugin>/app
./gradlew test --tests "*Contract*"
```

## Adoption Notes

Phase 5 applies this standard in two steps:
1. Bring active plugins to full compliance first.
2. For incubating plugins, decide whether to promote, keep incubating, or remove from the official set.
