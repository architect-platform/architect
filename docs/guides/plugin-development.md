# Plugin Development Guide

This guide covers the full plugin lifecycle: scaffold, implement, test, document, validate, and publish.

## 1) Scaffold a plugin project

Use the CLI scaffolder to start from a supported template:

```bash
architect plugin create my-plugin
architect plugin create my-plugin --template typescript
architect plugin create my-plugin --template go
```

Templates include baseline metadata (`plugin.yml`), starter implementation, and test scaffolding.

## 2) Use the recommended project layout

For JVM plugins:

```text
plugins/my-plugin/
├── README.md
├── STATUS.md
├── architect.yml
└── app/
    ├── build.gradle.kts
    ├── src/main/kotlin/io/github/architectplatform/plugins/myplugin/
    │   ├── MyPluginContext.kt
    │   ├── MyPlugin.kt
    │   └── MyTask.kt
    ├── src/main/resources/META-INF/services/
    │   └── io.github.architectplatform.api.core.plugins.ArchitectPlugin
    └── src/test/kotlin/
```

Best practices:

- Keep tasks as top-level classes (avoid anonymous/inner task classes).
- Use stable, lowercase-hyphen plugin and task IDs.
- Keep context schema explicit and versionable.

## 3) Implement the API contracts

JVM plugins implement `ArchitectPlugin<C>`:

```kotlin
data class ExampleContext(
  val enabled: Boolean = true,
)

class ExamplePlugin : ArchitectPlugin<ExampleContext> {
  override val id = "example-plugin"
  override val contextKey = "example"
  override val ctxClass = ExampleContext::class.java
  override var context: ExampleContext = ExampleContext()

  override fun configSchema(): Map<String, Any> = mapOf(
    "type" to "object",
    "properties" to mapOf(
      "enabled" to mapOf("type" to "boolean"),
    ),
  )

  override fun register(registry: TaskRegistry) {
    registry.add(ExampleTask())
  }
}
```

Task implementations should return `TaskResult` and provide meaningful descriptions:

```kotlin
class ExampleTask : Task {
  override val id: String = "example-check"
  override fun description(): String = "Runs an example verification"

  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult = TaskResult.success("Example check passed")
}
```

## 4) Test plugin behavior

Run plugin unit tests:

```bash
cd plugins/my-plugin/app
./gradlew test
```

For JVM plugins, use `ArchitectPluginTestKit` and contract verification utilities:

- `ArchitectPluginTestKit` for in-memory execution checks
- `ArchitectPluginContract` / `ArchitectPluginContractTestSuite` for contract conformance

## 5) Generate and validate plugin artifacts

Generate docs from plugin metadata:

```bash
architect plugin docs path/to/plugin
```

Validate packaging and wiring:

```bash
architect plugin validate path/to/plugin.jar
architect plugin test path/to/plugin.jar
```

`architect plugin test` verifies:

- SPI provider discovery from plugin JAR
- `ArchitectPlugin` contract checks
- `configSchema()` structure validation
- task registration integrity

## 6) Publish safely

Before publishing:

1. Build and test pass.
2. `architect plugin docs` output is up to date.
3. `architect plugin validate` and `architect plugin test` pass.
4. README includes configuration and task examples.
5. STATUS declares maturity and support level.

Typical publication flow:

```bash
cd plugins/my-plugin/app
./gradlew clean build
# publish artifact using your release workflow (GitHub Releases / registry path)
```

## 7) Recommended pre-release checklist

- Task IDs are stable and documented.
- Error paths return `TaskResult.failure(...)`, not uncaught exceptions.
- Configuration keys are represented in `configSchema()`.
- Examples in docs match actual runtime behavior.

## Related references

- `docs/guides/authoring-plugins.md`
- `docs/guides/plugin-standard.md`
- `docs/plugin-protocol.md`
- `architect-api/api/src/main/kotlin/io/github/architectplatform/api/core/plugins/ArchitectPlugin.kt`
- `architect-api/api/src/main/kotlin/io/github/architectplatform/api/testing/ArchitectPluginContractTestSuite.kt`
