package io.github.architectplatform.plugins.release

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.execution.CommandResult
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ReleasePluginTest {
  @Test
  fun `plugin id and context key`() {
    val plugin = ReleasePlugin()
    assertEquals("release-plugin", plugin.id)
    assertEquals("release", plugin.contextKey)
    assertEquals(ReleaseContext::class.java, plugin.ctxClass)
  }

  @Test
  fun `registers all expected tasks`() {
    val plugin = ReleasePlugin()
    val registry = TestTaskRegistry()
    plugin.register(registry)
    assertEquals(setOf("release-prepare", "release-publish", "release-rollback"), registry.taskIds())
  }

  @Test
  fun `config schema is provided`() {
    val schema = ReleasePlugin().configSchema()
    assertNotNull(schema)
    assertEquals("object", schema["type"])
  }

  @Test
  fun `release prepare bumps semantic version and writes changelog`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("VERSION"), "1.2.3\n")
    val executor = RecordingCommandExecutor { command ->
      when {
        command.startsWith("git describe --tags --abbrev=0 --match") -> CommandResult(exitCode = 0, stdout = "v1.2.3")
        command.startsWith("git log ") -> CommandResult(
          exitCode = 0,
          stdout = buildGitLog(
            "abc1234" to Pair("feat: add release automation", ""),
            "def5678" to Pair("fix: stabilize publish command", ""),
          ),
        )
        else -> CommandResult(exitCode = 0, stdout = "")
      }
    }
    val task = registerAndGet(ReleaseContext(), "release-prepare")

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals("1.3.0", result.data["nextVersion"])
    assertEquals("minor", result.data["recommendedBump"])
    assertEquals("v1.3.0", result.data["tag"])
    assertEquals("1.3.0\n", Files.readString(tempDir.resolve("VERSION")))
    assertTrue(Files.readString(tempDir.resolve("CHANGELOG.md")).contains("## v1.3.0 - "))
    assertTrue(Files.readString(tempDir.resolve("CHANGELOG.md")).contains("### Features"))
    assertTrue(Files.readString(tempDir.resolve("build/release-notes.md")).contains("feat: add release automation"))
  }

  @Test
  fun `release prepare supports calendar versioning`(@TempDir tempDir: Path) {
    val today = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    Files.writeString(tempDir.resolve("VERSION"), "$today\n")
    val executor = RecordingCommandExecutor { command ->
      when {
        command.startsWith("git describe --tags --abbrev=0 --match") -> CommandResult(exitCode = 1, stdout = "", stderr = "no tag")
        command.startsWith("git describe --tags --abbrev=0") -> CommandResult(exitCode = 1, stdout = "", stderr = "no tag")
        command.startsWith("git log ") -> CommandResult(
          exitCode = 0,
          stdout = buildGitLog("abc1234" to Pair("chore: cut same-day release", "")),
        )
        else -> CommandResult(exitCode = 0, stdout = "")
      }
    }
    val task = registerAndGet(ReleaseContext(strategy = "calendar"), "release-prepare")

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals("$today.1", result.data["nextVersion"])
    assertEquals("$today.1\n", Files.readString(tempDir.resolve("VERSION")))
  }

  @Test
  fun `release publish builds commands for npm docker and github release`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("VERSION"), "2.0.0\n")
    Files.createDirectories(tempDir.resolve("dist"))
    Files.writeString(tempDir.resolve("dist/app.tar.gz"), "artifact")
    Files.createDirectories(tempDir.resolve("build"))
    Files.writeString(tempDir.resolve("build/release-notes.md"), "notes")

    val executor = RecordingCommandExecutor { CommandResult(exitCode = 0, stdout = "ok") }
    val task = registerAndGet(
      ReleaseContext(
        artifacts = listOf(
          ReleaseArtifact(type = "npm", registry = "https://registry.npmjs.org"),
          ReleaseArtifact(type = "docker", registry = "ghcr.io", image = "architect-platform/architect", tags = listOf("latest")),
          ReleaseArtifact(type = "github-release", assets = listOf("dist/*.tar.gz")),
        ),
      ),
      "release-publish",
    )

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals(3, executor.commands.size)
    assertEquals("npm publish --registry 'https://registry.npmjs.org'", executor.commands[0])
    assertTrue(executor.commands[1].contains("docker build"))
    assertTrue(executor.commands[1].contains("'ghcr.io/architect-platform/architect:2.0.0'"))
    assertTrue(executor.commands[1].contains("'ghcr.io/architect-platform/architect:latest'"))
    assertTrue(executor.commands[2].contains("gh release create 'v2.0.0'"))
    assertTrue(executor.commands[2].contains(tempDir.resolve("dist/app.tar.gz").toString()))
  }

  @Test
  fun `release rollback deletes tag and restores prepared files`(@TempDir tempDir: Path) {
    Files.writeString(tempDir.resolve("VERSION"), "1.3.0\n")
    Files.writeString(tempDir.resolve("CHANGELOG.md"), "# Changelog\n")
    val executor = RecordingCommandExecutor { command ->
      when {
        command.startsWith("git tag --list ") -> CommandResult(exitCode = 0, stdout = "v1.3.0\n")
        else -> CommandResult(exitCode = 0, stdout = "ok")
      }
    }
    val task = registerAndGet(ReleaseContext(), "release-rollback")

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertTrue(result.success)
    assertEquals("git tag --list 'v1.3.0'", executor.commands[0])
    assertEquals("git tag -d 'v1.3.0'", executor.commands[1])
    assertEquals("git checkout -- 'CHANGELOG.md' 'VERSION'", executor.commands[2])
  }

  @Test
  fun `disabled plugin skips tasks`(@TempDir tempDir: Path) {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(ReleaseContext(enabled = false), "release-prepare")

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertEquals(TaskResult.Status.SKIPPED, result.status)
    assertTrue(executor.commands.isEmpty())
  }

  @Test
  fun `task rejects working directory traversal`(@TempDir tempDir: Path) {
    val executor = RecordingCommandExecutor()
    val task = registerAndGet(ReleaseContext(workingDirectory = "../outside"), "release-prepare")

    val result = task.execute(TestEnvironment(executor), ProjectContext(tempDir, emptyMap()), emptyList())

    assertFalse(result.success)
    assertNull(executor.command)
    assertTrue(result.message!!.contains("invalid working directory"))
  }

  private fun registerAndGet(ctx: ReleaseContext, taskId: String): Task {
    val plugin = ReleasePlugin()
    plugin.init(ctx)
    val registry = TestTaskRegistry()
    plugin.register(registry)
    return registry.get(taskId)!!
  }

  private fun buildGitLog(vararg entries: Pair<String, Pair<String, String>>): String =
    entries.joinToString(separator = "") { (hash, payload) ->
      val (subject, body) = payload
      "$hash\u001f$subject\u001f$body\u001e"
    }

  private class TestTaskRegistry : TaskRegistry {
    private val tasks = mutableListOf<Task>()

    fun taskIds() = tasks.map { it.id }.toSet()

    override fun add(task: Task) {
      tasks.add(task)
    }

    override fun get(id: String): Task? = tasks.find { it.id == id }

    override fun all(): List<Task> = tasks.toList()
  }

  private class RecordingCommandExecutor(
    private val handler: (String) -> CommandResult = { CommandResult(exitCode = 0, stdout = "") },
  ) : CommandExecutor {
    val commands = mutableListOf<String>()
    val workingDirs = mutableListOf<String?>()
    val command: String?
      get() = commands.lastOrNull()

    override fun execute(command: String, workingDir: String?) {
      commands += command
      workingDirs += workingDir
    }

    override fun executeWithResult(
      command: String,
      workingDir: String?,
      timeoutSeconds: Long,
      env: Map<String, String>,
    ): CommandResult {
      commands += command
      workingDirs += workingDir
      return handler(command)
    }
  }

  private class TestEnvironment(private val commandExecutor: CommandExecutor) : Environment {
    override fun <T> service(type: Class<T>): T {
      if (type == CommandExecutor::class.java) {
        @Suppress("UNCHECKED_CAST")
        return commandExecutor as T
      }
      throw IllegalArgumentException("Unsupported service: ${type.name}")
    }

    override fun publish(event: Any) {}
  }
}
