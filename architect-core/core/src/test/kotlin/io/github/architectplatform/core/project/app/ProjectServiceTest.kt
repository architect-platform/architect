package io.github.architectplatform.core.project.app

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.core.plugin.app.PluginLoader
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskPermission
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.core.project.infra.InMemoryProjectRepository
import io.github.architectplatform.core.project.infra.YamlConfigParser
import io.github.architectplatform.core.plugins.inline.InlineTaskPlugin
import java.nio.file.Path
import java.util.Optional
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.seconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class ProjectServiceTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `should load project round-trip with inline tasks`() {
    val projectDir = tempDir.resolve("inline-project")
    projectDir.createDirectories()
    projectDir.resolve("architect.yml").writeText(
      """
      project:
        name: inline-project
      tasks:
        build:
          description: Build the project
          run: echo building
      """.trimIndent()
    )

    val projectService = createProjectService(InlineTaskPluginLoader())

    projectService.registerProject("inline-project", projectDir.toString())
    val project = projectService.getProject("inline-project")

    assertNotNull(project)
    assertEquals("inline-project", project.name)
    assertTrue(project.plugins.any { it.id == "inline-tasks" })
    assertNotNull(project.taskRegistry.get("build"))
  }

  @Test
  fun `should expose grouped aliases and templates from architect config`() {
    val projectDir = tempDir.resolve("grouped-project")
    projectDir.createDirectories()
    projectDir.resolve("architect.yml").writeText(
      """
      project:
        name: grouped-project
      templates:
        npm-script:
          timeout: 120s
          requires:
            tools: [node, npm]
      tasks:
        frontend-build:
          extends: npm-script
          run: npm run build
        backend-build:
          run: ./gradlew build
      groups:
        build: [frontend-build, backend-build]
      """.trimIndent()
    )

    val projectService = createProjectService(InlineTaskPluginLoader())

    projectService.registerProject("grouped-project", projectDir.toString())
    val project = projectService.getProject("grouped-project")

    assertNotNull(project)
    assertNotNull(project.taskRegistry.get("build"))
    assertNotNull(project.taskRegistry.get("build:frontend"))
    assertNotNull(project.taskRegistry.get("build:backend"))
    assertNotNull(project.taskRegistry.get("build:*"))
    assertEquals(listOf("node", "npm"), project.taskRegistry.get("frontend-build")!!.requires()!!.tools)
  }

  @Test
  fun `should create plugin namespace aliases when task ids match plugin prefix`() {
    val projectDir = tempDir.resolve("plugin-alias-project")
    projectDir.createDirectories()
    projectDir.resolve("architect.yml").writeText(
      """
      project:
        name: plugin-alias-project
      """.trimIndent()
    )

    val projectService = createProjectService(GitStylePluginLoader())

    projectService.registerProject("plugin-alias-project", projectDir.toString())
    val project = projectService.getProject("plugin-alias-project")

    assertNotNull(project)
    assertNotNull(project.taskRegistry.get("git:status"))
    assertNull(project.taskRegistry.get("git:missing"))
  }

  @Test
  fun `should defer plugin loading until task access`() {
    val projectDir = tempDir.resolve("lazy-project")
    projectDir.createDirectories()
    projectDir.resolve("architect.yml").writeText(
      """
      project:
        name: lazy-project
      tasks:
        build:
          description: Build lazily
          run: echo building
      """.trimIndent()
    )

    val pluginLoader = CountingInlineTaskPluginLoader()
    val projectService = createProjectService(pluginLoader)

    projectService.registerProject("lazy-project", projectDir.toString())

    assertEquals(0, pluginLoader.loadCalls)

    val project = projectService.getProject("lazy-project")

    assertNotNull(project)
    assertNotNull(project.taskRegistry.get("build"))
    assertEquals(1, pluginLoader.loadCalls)
  }

  @Test
  fun `should discover nested subprojects when loading root project`() {
    val rootDir = tempDir.resolve("workspace")
    rootDir.createDirectories()
    rootDir.resolve("architect.yml").writeText(
      """
      project:
        name: workspace
      tasks:
        root-task:
          run: echo root
      """.trimIndent()
    )

    val childDir = rootDir.resolve("service-a")
    childDir.createDirectories()
    childDir.resolve("architect.yml").writeText(
      """
      project:
        name: service-a
      tasks:
        child-task:
          run: echo child
      """.trimIndent()
    )

    val projectService = createProjectService(InlineTaskPluginLoader())

    projectService.registerProject("workspace", rootDir.toString())
    val project = projectService.getProject("workspace")

    assertNotNull(project)
    assertEquals(1, project.subProjects.size)
    val childProject = project.subProjects.single()
    assertEquals("service-a", childProject.name)
    assertNotNull(childProject.taskRegistry.get("child-task"))
  }

  @Test
  fun `should throw validation exception on invalid config`() {
    val projectDir = tempDir.resolve("invalid-project")
    projectDir.createDirectories()
    projectDir.resolve("architect.yml").writeText(
      """
      project:
        description: missing required name
      """.trimIndent()
    )

    val projectService = createProjectService(EmptyPluginLoader())

    val exception = assertFailsWith<ConfigValidationException> {
      projectService.registerProject("invalid-project", projectDir.toString())
    }

    assertTrue(exception.message.orEmpty().contains("project.name"))
  }

  @Test
  fun `should invalidate cached project when architect config changes`() {
    val projectDir = tempDir.resolve("watched-project")
    projectDir.createDirectories()
    projectDir.resolve("architect.yml").writeText(
      """
      project:
        name: watched-project
      tasks:
        build:
          run: echo build
      """.trimIndent()
    )

    val projectService = ProjectService(
      projectRepository = InMemoryProjectRepository(),
      configLoader = ConfigLoader(YamlConfigParser()),
      pluginLoader = InlineTaskPluginLoader(),
      projectReporter = Optional.empty(),
      configValidator = ConfigValidator(),
      projectWatchDebounceMs = 50,
    )

    projectService.registerProject("watched-project", projectDir.toString())
    assertNotNull(projectService.getProject("watched-project")!!.taskRegistry.get("build"))

    projectDir.resolve("architect.yml").writeText(
      """
      project:
        name: watched-project
      tasks:
        test:
          run: echo test
      """.trimIndent()
    )

    val reloaded = waitForProjectReload(projectService, "watched-project", "test")
    assertNotNull(reloaded.taskRegistry.get("test"))
  }

  @Test
  fun `should reject undeclared plugin task permissions in strict mode`() {
    val projectDir = tempDir.resolve("strict-plugin-security")
    projectDir.createDirectories()
    projectDir.resolve("architect.yml").writeText(
      """
      project:
        name: strict-plugin-security
      """.trimIndent()
    )

    val projectService = ProjectService(
      projectRepository = InMemoryProjectRepository(),
      configLoader = ConfigLoader(YamlConfigParser()),
      pluginLoader = UndeclaredPermissionPluginLoader(),
      projectReporter = Optional.empty(),
      configValidator = ConfigValidator(),
      pluginSecurityStrictMode = true,
    )

    val exception = assertFailsWith<ConfigValidationException> {
      projectService.registerProject("strict-plugin-security", projectDir.toString())
      projectService.getProject("strict-plugin-security")?.taskRegistry?.all()
    }

    assertTrue(exception.message.orEmpty().contains("undeclared permissions"))
  }

  private fun createProjectService(pluginLoader: PluginLoader): ProjectService =
    ProjectService(
      projectRepository = InMemoryProjectRepository(),
      configLoader = ConfigLoader(YamlConfigParser()),
      pluginLoader = pluginLoader,
      projectReporter = Optional.empty(),
      configValidator = ConfigValidator(),
    )

  private class InlineTaskPluginLoader : PluginLoader {
    override fun load(context: ProjectContext): List<ArchitectPlugin<*>> = listOf(InlineTaskPlugin())
  }

  private class CountingInlineTaskPluginLoader : PluginLoader {
    var loadCalls: Int = 0

    override fun load(context: ProjectContext): List<ArchitectPlugin<*>> {
      loadCalls += 1
      return listOf(InlineTaskPlugin())
    }
  }

  private class EmptyPluginLoader : PluginLoader {
    override fun load(context: ProjectContext): List<ArchitectPlugin<*>> = emptyList()
  }

  private class GitStylePluginLoader : PluginLoader {
    override fun load(context: ProjectContext): List<ArchitectPlugin<*>> = listOf(GitStylePlugin())
  }

  private class UndeclaredPermissionPluginLoader : PluginLoader {
    override fun load(context: ProjectContext): List<ArchitectPlugin<*>> = listOf(UndeclaredPermissionPlugin())
  }

  private class UndeclaredPermissionPlugin : ArchitectPlugin<Any> {
    override val id: String = "security-plugin"
    override val contextKey: String = "securityPlugin"
    override val ctxClass: Class<Any> = Any::class.java
    override var context: Any = Unit

    override fun register(registry: TaskRegistry) {
      registry.add(
        object : Task {
          override val id: String = "security-plugin-task"
          override fun requiredPermissions(): Set<TaskPermission> = setOf(TaskPermission.PROCESS_EXEC)
          override fun execute(
            environment: Environment,
            projectContext: ProjectContext,
            args: List<String>,
          ): TaskResult = TaskResult.success()
        },
      )
    }

    override fun configSchema(): Map<String, Any> = mapOf(
      "type" to "object",
      "x-permissions" to listOf(TaskPermission.FILE_SYSTEM_READ.wireName),
    )
  }

  private class GitStylePlugin : ArchitectPlugin<Any> {
    override val id: String = "git-plugin"
    override val contextKey: String = "git"
    override val ctxClass: Class<Any> = Any::class.java
    override var context: Any = Unit

    override fun register(registry: TaskRegistry) {
      registry.add(
        object : Task {
          override val id: String = "git-status"
          override fun execute(
            environment: Environment,
            projectContext: ProjectContext,
            args: List<String>,
          ): TaskResult = TaskResult.success("git-status")
        },
      )
    }
  }

  private fun waitForProjectReload(
    projectService: ProjectService,
    projectName: String,
    expectedTaskId: String,
  ): io.github.architectplatform.core.project.domain.Project {
    val deadline = System.currentTimeMillis() + 3.seconds.inWholeMilliseconds
    while (System.currentTimeMillis() < deadline) {
      val project = projectService.getProject(projectName)
      if (project != null && project.taskRegistry.get(expectedTaskId) != null) {
        return project
      }
      Thread.sleep(50)
    }
    error("Timed out waiting for cached project reload of $projectName")
  }
}
