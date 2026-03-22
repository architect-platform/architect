package io.github.architectplatform.cli.plugin

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

enum class PluginTemplate(val id: String) {
  KOTLIN("kotlin"),
  TYPESCRIPT("typescript"),
  GO("go"),
  ;

  companion object {
    fun from(value: String): PluginTemplate? = entries.firstOrNull { it.id == value.lowercase() }
  }
}

class PluginScaffolder {
  fun scaffold(name: String, template: PluginTemplate, targetRoot: Path): Path {
    val normalizedName = normalizeName(name)
    require(normalizedName.isNotBlank()) { "Plugin name must contain letters or numbers" }

    val model = TemplateModel.from(normalizedName)
    val pluginDir = targetRoot.resolve(model.pluginName)
    require(!Files.exists(pluginDir)) { "Target directory already exists: $pluginDir" }

    val files = when (template) {
      PluginTemplate.KOTLIN -> kotlinFiles(model)
      PluginTemplate.TYPESCRIPT -> typeScriptFiles(model)
      PluginTemplate.GO -> goFiles(model)
    }

    files.forEach { (relativePath, content) ->
      val filePath = pluginDir.resolve(relativePath)
      filePath.parent?.createDirectories()
      filePath.writeText(content)
    }

    return pluginDir
  }

  private fun normalizeName(value: String): String {
    return value.trim()
      .lowercase()
      .replace(Regex("[^a-z0-9]+"), "-")
      .trim('-')
  }

  private fun kotlinFiles(model: TemplateModel): Map<String, String> = mapOf(
    "README.md" to kotlinReadme(model),
    "plugin.yml" to pluginManifest(model, PluginTemplate.KOTLIN, "io.github.architectplatform.plugins.${model.packageSegment}.${model.classPrefix}Plugin"),
    "app/build.gradle.kts" to kotlinBuildGradle(),
    "app/settings.gradle.kts" to "rootProject.name = \"${model.pluginName}\"\n",
    "app/src/main/kotlin/io/github/architectplatform/plugins/${model.packageSegment}/${model.classPrefix}Context.kt" to kotlinContext(model),
    "app/src/main/kotlin/io/github/architectplatform/plugins/${model.packageSegment}/${model.classPrefix}Task.kt" to kotlinTask(model),
    "app/src/main/kotlin/io/github/architectplatform/plugins/${model.packageSegment}/${model.classPrefix}Plugin.kt" to kotlinPlugin(model),
    "app/src/main/resources/META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin" to
      "io.github.architectplatform.plugins.${model.packageSegment}.${model.classPrefix}Plugin\n",
    "app/src/test/kotlin/io/github/architectplatform/plugins/${model.packageSegment}/${model.classPrefix}PluginTest.kt" to kotlinPluginTest(model),
  )

  private fun typeScriptFiles(model: TemplateModel): Map<String, String> = mapOf(
    "README.md" to typeScriptReadme(model),
    "plugin.yml" to pluginManifest(model, PluginTemplate.TYPESCRIPT, "node dist/src/index.js"),
    "package.json" to packageJson(model),
    "tsconfig.json" to typeScriptConfig(),
    "src/plugin.ts" to typeScriptPlugin(model),
    "src/index.ts" to typeScriptEntrypoint(),
    "test/plugin.test.ts" to typeScriptTest(model),
  )

  private fun goFiles(model: TemplateModel): Map<String, String> = mapOf(
    "README.md" to goReadme(model),
    "plugin.yml" to pluginManifest(model, PluginTemplate.GO, "./${model.binaryName}"),
    "go.mod" to goMod(model),
    "plugin.go" to goPlugin(model),
    "main.go" to goMain(),
    "plugin_test.go" to goTest(model),
  )

  private fun pluginManifest(model: TemplateModel, template: PluginTemplate, entrypoint: String): String = """
name: ${model.pluginName}
template: ${template.id}
version: 0.1.0
entrypoint: $entrypoint
description: Scaffolded ${template.id} plugin for Architect.
""".trimIndent() + "\n"

  private fun kotlinBuildGradle(): String = """
plugins { kotlin("jvm") version "1.9.25" }

group = "io.github.architectplatform.plugins"
version = "0.1.0"

java { sourceCompatibility = JavaVersion.toVersion("17") }

kotlin { jvmToolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

repositories {
  mavenCentral()
  maven {
    name = "GitHubPackages"
    url = uri("https://maven.pkg.github.com/architect-platform/architect")
    credentials {
      username = System.getenv("GITHUB_USER") ?: project.findProperty("githubUser") as String? ?: "github-actions"
      password = System.getenv("REGISTRY_TOKEN") ?: System.getenv("GITHUB_TOKEN") ?: project.findProperty("githubToken") as String?
    }
  }
}

dependencies {
  implementation("io.github.architectplatform:api:2.1.0")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

tasks.test { useJUnitPlatform() }
""".trimIndent() + "\n"

  private fun kotlinContext(model: TemplateModel): String = """
package io.github.architectplatform.plugins.${model.packageSegment}

data class ${model.classPrefix}Context(
  val enabled: Boolean = true,
)
""".trimIndent() + "\n"

  private fun kotlinTask(model: TemplateModel): String = """
package io.github.architectplatform.plugins.${model.packageSegment}

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult

class ${model.classPrefix}Task(
  private val context: ${model.classPrefix}Context,
) : Task {
  override val id: String = "${model.pluginName}-hello"

  override fun description(): String = "Example task generated for ${model.pluginName}."

  override fun phase() = CoreWorkflow.BUILD

  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult {
    if (!context.enabled) {
      return TaskResult.success("${model.pluginName} disabled")
    }
    return TaskResult.success("Hello from ${model.pluginName} at ${'$'}{projectContext.dir}")
  }
}
""".trimIndent() + "\n"

  private fun kotlinPlugin(model: TemplateModel): String = """
package io.github.architectplatform.plugins.${model.packageSegment}

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry

class ${model.classPrefix}Plugin : ArchitectPlugin<${model.classPrefix}Context> {
  override val id: String = "${model.pluginName}-plugin"
  override val contextKey: String = "${model.packageSegment}"
  override val ctxClass: Class<${model.classPrefix}Context> = ${model.classPrefix}Context::class.java
  override var context: ${model.classPrefix}Context = ${model.classPrefix}Context()

  override fun register(registry: TaskRegistry) {
    registry.add(${model.classPrefix}Task(context))
  }
}
""".trimIndent() + "\n"

  private fun kotlinPluginTest(model: TemplateModel): String = """
package io.github.architectplatform.plugins.${model.packageSegment}

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ${model.classPrefix}PluginTest {
  @Test
  fun `plugin metadata is scaffolded correctly`() {
    val plugin = ${model.classPrefix}Plugin()

    assertEquals("${model.pluginName}-plugin", plugin.id)
    assertEquals("${model.packageSegment}", plugin.contextKey)
    assertTrue(plugin.context.enabled)
  }
}
""".trimIndent() + "\n"

  private fun kotlinReadme(model: TemplateModel): String = """
# ${model.classPrefix} Plugin

Scaffolded Kotlin plugin for Architect.

## Build

```bash
cd app
./gradlew build
```

## Files

- `plugin.yml` manifest for your plugin metadata
- `app/src/main/kotlin` for implementation
- `app/src/test/kotlin` for the generated test harness
""".trimIndent() + "\n"

  private fun packageJson(model: TemplateModel): String = """
{
  "name": "${model.pluginName}",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "scripts": {
    "build": "tsc -p tsconfig.json",
    "test": "npm run build && node --test dist/test/plugin.test.js",
    "start": "node dist/src/index.js"
  },
  "dependencies": {
    "@architect-platform/plugin-sdk": "^0.1.0"
  },
  "devDependencies": {
    "@types/node": "^22.10.0",
    "typescript": "^5.7.2"
  }
}
""".trimIndent() + "\n"

  private fun typeScriptConfig(): String = """
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "NodeNext",
    "moduleResolution": "NodeNext",
    "outDir": "dist",
    "rootDir": ".",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true
  },
  "include": ["src/**/*.ts", "test/**/*.ts"]
}
""".trimIndent() + "\n"

  private fun typeScriptPlugin(model: TemplateModel): String = """
import type { ArchitectProcessPlugin } from "@architect-platform/plugin-sdk";

export const plugin: ArchitectProcessPlugin = {
  listTasks() {
    return [
      {
        id: "${model.pluginName}-hello",
        description: "Example task generated for ${model.pluginName}",
        phase: "BUILD",
      },
    ];
  },

  async executeTask(request, writer) {
    if (request.id !== "${model.pluginName}-hello") {
      throw new Error(`Unknown task ${'$'}{request.id}`);
    }
    await writer.output("Hello from ${model.pluginName}\n");
    return 0;
  },
};
""".trimIndent() + "\n"

  private fun typeScriptEntrypoint(): String = """
import { runPlugin } from "@architect-platform/plugin-sdk";
import { plugin } from "./plugin.js";

void runPlugin(plugin);
""".trimIndent() + "\n"

  private fun typeScriptTest(model: TemplateModel): String = """
import test from "node:test";
import assert from "node:assert/strict";
import { plugin } from "../src/plugin.js";

test("plugin exposes the scaffolded task", async () => {
  const tasks = await plugin.listTasks();
  assert.equal(tasks[0]?.id, "${model.pluginName}-hello");
});
""".trimIndent() + "\n"

  private fun typeScriptReadme(model: TemplateModel): String = """
# ${model.classPrefix} Plugin

Scaffolded TypeScript process plugin for Architect.

## Commands

```bash
npm install
npm run build
npm test
```

The generated `plugin.yml` points at `node dist/src/index.js`.
""".trimIndent() + "\n"

  private fun goMod(model: TemplateModel): String = """
module github.com/architect-platform/${model.pluginName}

go 1.22

require github.com/architect-platform/plugin-sdk-go v0.1.0
""".trimIndent() + "\n"

  private fun goPlugin(model: TemplateModel): String = """
package main

import sdk "github.com/architect-platform/plugin-sdk-go"

type Plugin struct{}

func (Plugin) ListTasks() []sdk.TaskDescriptor {
  return []sdk.TaskDescriptor{
    {
      ID:          "${model.pluginName}-hello",
      Description: "Example task generated for ${model.pluginName}",
      Phase:       "BUILD",
    },
  }
}

func (Plugin) ExecuteTask(request sdk.ExecuteTaskParams, writer sdk.TaskEventWriter) error {
  if request.ID != "${model.pluginName}-hello" {
    return writer.Error("unknown task")
  }
  if err := writer.Output("Hello from ${model.pluginName}\n"); err != nil {
    return err
  }
  return nil
}
""".trimIndent() + "\n"

  private fun goMain(): String = """
package main

import sdk "github.com/architect-platform/plugin-sdk-go"

func main() {
  sdk.RunPlugin(Plugin{})
}
""".trimIndent() + "\n"

  private fun goTest(model: TemplateModel): String = """
package main

import "testing"

func TestPluginListsScaffoldedTask(t *testing.T) {
  tasks := Plugin{}.ListTasks()
  if len(tasks) != 1 {
    t.Fatalf("expected 1 task, got %d", len(tasks))
  }
  if tasks[0].ID != "${model.pluginName}-hello" {
    t.Fatalf("unexpected task id: %s", tasks[0].ID)
  }
}
""".trimIndent() + "\n"

  private fun goReadme(model: TemplateModel): String = """
# ${model.classPrefix} Plugin

Scaffolded Go process plugin for Architect.

## Commands

```bash
go test ./...
go build ./...
```

The generated `plugin.yml` points at the compiled binary entrypoint.
""".trimIndent() + "\n"

  private data class TemplateModel(
    val pluginName: String,
    val packageSegment: String,
    val classPrefix: String,
    val binaryName: String,
  ) {
    companion object {
      fun from(pluginName: String): TemplateModel {
        val packageSegment = pluginName.replace("-", "")
        val classPrefix = pluginName
          .split('-')
          .filter { it.isNotBlank() }
          .joinToString("") { part -> part.replaceFirstChar { it.uppercase() } }
          .ifBlank { "Plugin" }
        return TemplateModel(
          pluginName = pluginName,
          packageSegment = packageSegment,
          classPrefix = classPrefix,
          binaryName = pluginName,
        )
      }
    }
  }
}