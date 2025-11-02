package io.github.architectplatform.engine.core.project.app

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.engine.core.plugin.app.PluginLoader
import io.github.architectplatform.engine.core.project.app.repositories.ProjectRepository
import io.github.architectplatform.engine.core.project.infra.InMemoryProjectRepository
import io.github.architectplatform.engine.core.project.infra.YamlConfigParser
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.Optional

/**
 * Unit tests for ProjectService.
 */
@MicronautTest
class ProjectServiceTest {

    @Inject
    lateinit var projectRepository: ProjectRepository

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
        val projectService = ProjectService(projectRepository, configLoader, pluginLoader, Optional.empty())

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
        val projectService = ProjectService(projectRepository, configLoader, pluginLoader, Optional.empty())

        // When
        projectService.registerProject(projectName, projectPath)
        projectService.registerProject(projectName, projectPath) // Second registration

        // Then
        val projects = projectService.getAllProjects()
        assertEquals(1, projects.size)
    }

    @Test
    fun `should retrieve all registered projects`() {
        // Given
        val projectPath1 = createTempProjectDir("project1")
        val projectPath2 = createTempProjectDir("project2")

        val configLoader = ConfigLoader(YamlConfigParser())
        val pluginLoader = TestPluginLoader()
        val projectService = ProjectService(InMemoryProjectRepository(), configLoader, pluginLoader, Optional.empty())

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
        val projectService = ProjectService(InMemoryProjectRepository(), configLoader, pluginLoader, Optional.empty())

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
        val projectService = ProjectService(InMemoryProjectRepository(), configLoader, pluginLoader, Optional.empty())

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            projectService.registerProject(projectName, invalidPath)
        }
    }

    private fun createTestProjectStructure(path: String) {
        val architectFile = File(path, "architect.yml")
        architectFile.writeText("""
            project:
              name: test-project
              description: "Test project"
        """.trimIndent())
    }

    private fun createTempProjectDir(name: String): String {
        val dir = File(tempDir.toFile(), name)
        dir.mkdirs()
        createTestProjectStructure(dir.absolutePath)
        return dir.absolutePath
    }

    /**
     * Test implementation of PluginLoader that returns an empty list.
     */
    class TestPluginLoader : PluginLoader {
        override fun load(context: ProjectContext): List<ArchitectPlugin<*>> {
            return emptyList()
        }
    }
}
