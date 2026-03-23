package io.github.architectplatform.engine.core.project.app

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.engine.core.plugin.app.PluginLoader
import io.github.architectplatform.engine.core.project.infra.InMemoryProjectRepository
import io.github.architectplatform.engine.core.project.infra.YamlConfigParser
import io.github.architectplatform.engine.plugins.inline.InlineTaskPlugin
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.Optional
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for ProjectService.
 */
@MicronautTest
class ProjectServiceTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should register a new project successfully`() {
        // Given
        val projectName = "test-project"
        val projectPath = tempDir.toString()
        createTestProjectStructure(projectPath)

        val configLoader = ConfigLoader(YamlConfigParser())
        val pluginLoader = TestPluginLoader()
        val projectService = ProjectService(InMemoryProjectRepository(), configLoader, pluginLoader, Optional.empty(), ConfigValidator())

        // When
        projectService.registerProject(projectName, projectPath)

        // Then
        val project = projectService.getProject(projectName)
        assertNotNull(project)
        assertEquals(projectName, project?.name)
        assertEquals(projectPath, project?.path)
    }

    @Test
    fun `should not re-register existing project`() {
        // Given
        val projectName = "test-project"
        val projectPath = tempDir.toString()
        createTestProjectStructure(projectPath)

        val configLoader = ConfigLoader(YamlConfigParser())
        val pluginLoader = TestPluginLoader()
        val projectService = ProjectService(InMemoryProjectRepository(), configLoader, pluginLoader, Optional.empty(), ConfigValidator())

        // When
        projectService.registerProject(projectName, projectPath)
        projectService.registerProject(projectName, projectPath) // Second registration

        // Then
        val projects = projectService.getAllProjects()
        assertEquals(1, projects.size)
    }

    @Test
    fun `should reload existing project when it uses local plugins`() {
        val projectName = "local-plugin-project"
        val projectPath = tempDir.toString()
        createTestProjectStructure(
            projectPath,
            includeLocalPlugin = true,
        )

        val configLoader = ConfigLoader(YamlConfigParser())
        val pluginLoader = CountingPluginLoader()
    val projectService = ProjectService(InMemoryProjectRepository(), configLoader, pluginLoader, Optional.empty(), ConfigValidator())

        projectService.registerProject(projectName, projectPath)
        projectService.registerProject(projectName, projectPath)

        assertEquals(0, pluginLoader.loadCalls)

        val project = projectService.getProject(projectName)
        assertNotNull(project)
        project!!.plugins

        assertEquals(1, pluginLoader.loadCalls)
    }

    @Test
    fun `should defer plugin loading until task access`() {
        val projectName = "lazy-project"
        val projectPath = tempDir.toString()
        createTaskProjectStructure(projectPath)

        val configLoader = ConfigLoader(YamlConfigParser())
        val pluginLoader = CountingInlineTaskPluginLoader()
    val projectService = ProjectService(InMemoryProjectRepository(), configLoader, pluginLoader, Optional.empty(), ConfigValidator())

        projectService.registerProject(projectName, projectPath)

        assertEquals(0, pluginLoader.loadCalls)

        val project = projectService.getProject(projectName)
        assertNotNull(project)
        assertNotNull(project!!.taskRegistry.get("build"))
        assertEquals(1, pluginLoader.loadCalls)
    }

    @Test
    fun `should retrieve all registered projects`() {
        // Given
        val projectPath1 = createTempProjectDir("project1")
        val projectPath2 = createTempProjectDir("project2")

        val configLoader = ConfigLoader(YamlConfigParser())
        val pluginLoader = TestPluginLoader()
        val projectService = ProjectService(InMemoryProjectRepository(), configLoader, pluginLoader, Optional.empty(), ConfigValidator())

        // When
        projectService.registerProject("project1", projectPath1)
        projectService.registerProject("project2", projectPath2)

        // Then
        val projects = projectService.getAllProjects()
        assertEquals(2, projects.size)
        assertTrue(projects.any { it.name == "project1" })
        assertTrue(projects.any { it.name == "project2" })
    }

    @Test
    fun `should return null for non-existent project`() {
        // Given
        val configLoader = ConfigLoader(YamlConfigParser())
        val pluginLoader = TestPluginLoader()
        val projectService = ProjectService(InMemoryProjectRepository(), configLoader, pluginLoader, Optional.empty(), ConfigValidator())

        // When
        val project = projectService.getProject("non-existent")

        // Then
        assertNull(project)
    }

    @Test
    fun `should throw exception when registering invalid project path`() {
        // Given
        val projectName = "invalid-project"
        val invalidPath = "/non/existent/path"

        val configLoader = ConfigLoader(YamlConfigParser())
        val pluginLoader = TestPluginLoader()
        val projectService = ProjectService(InMemoryProjectRepository(), configLoader, pluginLoader, Optional.empty(), ConfigValidator())

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            projectService.registerProject(projectName, invalidPath)
        }
    }

    @Test
    fun `should invalidate cached project when architect config changes`() {
        val projectName = "watched-project"
        val projectPath = tempDir.toString()
        createTaskProjectStructure(projectPath)

        val configLoader = ConfigLoader(YamlConfigParser())
        val pluginLoader = CountingInlineTaskPluginLoader()
        val projectService = ProjectService(InMemoryProjectRepository(), configLoader, pluginLoader, Optional.empty(), ConfigValidator())
        projectService.projectWatchDebounceMs = 50

        projectService.registerProject(projectName, projectPath)
        assertNotNull(projectService.getProject(projectName)!!.taskRegistry.get("build"))

        File(projectPath, "architect.yml").writeText(
            """
            project:
              name: test-project
            tasks:
              test:
                description: Reloaded task
                run: echo testing
            """.trimIndent() + "\n"
        )

        val reloaded = waitForProjectReload(projectService, projectName, "test")
        assertNotNull(reloaded.taskRegistry.get("test"))
    }

    private fun createTestProjectStructure(path: String, includeLocalPlugin: Boolean = false) {
        val architectFile = File(path, "architect.yml")
        val pluginsSection = if (includeLocalPlugin) {
            """
            plugins:
              - name: local-dev
                type: local
                path: build/local-dev.jar
            """.trimIndent()
        } else {
            ""
        }
        architectFile.writeText(
            listOf(
                """
                project:
                  name: test-project
                  description: \"Test project\"
                """.trimIndent(),
                pluginsSection,
            ).filter { it.isNotBlank() }.joinToString("\n") + "\n"
        )
    }

    private fun createTempProjectDir(name: String): String {
        val dir = File(tempDir.toFile(), name)
        dir.mkdirs()
        createTestProjectStructure(dir.absolutePath)
        return dir.absolutePath
    }

        private fun createTaskProjectStructure(path: String) {
                File(path, "architect.yml").writeText(
                        """
                        project:
                            name: test-project
                        tasks:
                            build:
                                description: Build lazily
                                run: echo building
                        """.trimIndent() + "\n"
                )
        }

    /**
     * Test implementation of PluginLoader that returns an empty list.
     */
    class TestPluginLoader : PluginLoader {
        override fun load(context: ProjectContext): List<ArchitectPlugin<*>> {
            return emptyList()
        }
    }

    class CountingPluginLoader : PluginLoader {
        var loadCalls: Int = 0

        override fun load(context: ProjectContext): List<ArchitectPlugin<*>> {
            loadCalls += 1
            return emptyList()
        }
    }

    class CountingInlineTaskPluginLoader : PluginLoader {
        var loadCalls: Int = 0

        override fun load(context: ProjectContext): List<ArchitectPlugin<*>> {
            loadCalls += 1
            return listOf(InlineTaskPlugin())
        }
    }

    private fun waitForProjectReload(
        projectService: ProjectService,
        projectName: String,
        expectedTaskId: String,
    ): io.github.architectplatform.engine.core.project.domain.Project {
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
