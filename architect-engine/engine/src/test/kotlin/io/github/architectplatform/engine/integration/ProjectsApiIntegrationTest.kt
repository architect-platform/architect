package io.github.architectplatform.engine.integration

import io.github.architectplatform.engine.core.project.interfaces.dto.RegisterProjectRequest
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Integration tests for the Projects API endpoints.
 */
@MicronautTest
class ProjectsApiIntegrationTest {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should get empty list of projects initially`() {
        // When
        val request = HttpRequest.GET<Any>("/api/projects")
        val response = client.toBlocking().exchange(request, String::class.java)

        // Then
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
    }

    @Test
    fun `should register a project successfully`() {
        // Given
        val projectPath = createTestProject("test-project")
        val registerRequest = RegisterProjectRequest("test-project", projectPath)

        // When
        val request = HttpRequest.POST("/api/projects", registerRequest)
        val response = client.toBlocking().exchange(request, Map::class.java)

        // Then
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
        val project = response.body()!!
        assertEquals("test-project", project["name"])
        assertEquals(projectPath, project["path"])
    }

    @Test
    fun `should get registered project by name`() {
        // Given
        val projectPath = createTestProject("my-project")
        val registerRequest = RegisterProjectRequest("my-project", projectPath)
        client.toBlocking().exchange(HttpRequest.POST("/api/projects", registerRequest), Map::class.java)

        // When
        val request = HttpRequest.GET<Any>("/api/projects/my-project")
        val response = client.toBlocking().exchange(request, Map::class.java)

        // Then
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
        assertEquals("my-project", response.body()!!["name"])
    }

    @Test
    fun `should get project configuration`() {
        // Given
        val projectPath = createTestProject("config-project")
        val registerRequest = RegisterProjectRequest("config-project", projectPath)
        client.toBlocking().exchange(HttpRequest.POST("/api/projects", registerRequest), Map::class.java)

        // When
        val request = HttpRequest.GET<Any>("/api/projects/config-project/config")
        val response = client.toBlocking().exchange(request, Map::class.java)

        // Then
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
        val config = response.body()!!
        assertTrue(config.containsKey("project"))
    }

    @Test
    fun `should get all registered projects`() {
        // Given
        val path1 = createTestProject("project-one")
        val path2 = createTestProject("project-two")
        client.toBlocking().exchange(
            HttpRequest.POST("/api/projects", RegisterProjectRequest("project-one", path1)),
            Map::class.java
        )
        client.toBlocking().exchange(
            HttpRequest.POST("/api/projects", RegisterProjectRequest("project-two", path2)),
            Map::class.java
        )

        // When
        val request = HttpRequest.GET<Any>("/api/projects")
        val response = client.toBlocking().exchange(request, String::class.java)

        // Then
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
        assertTrue(response.body()!!.contains("project-one") || response.body()!!.contains("project-two"))
    }

    @Test
    fun `should handle invalid project path gracefully`() {
        // Given
        val registerRequest = RegisterProjectRequest("invalid-project", "/non/existent/path")

        // When & Then
        val request = HttpRequest.POST("/api/projects", registerRequest)
        assertThrows(HttpClientResponseException::class.java) {
            client.toBlocking().exchange(request, Map::class.java)
        }
    }

    private fun createTestProject(name: String): String {
        val projectDir = File(tempDir.toFile(), name)
        projectDir.mkdirs()
        
        val architectFile = File(projectDir, "architect.yml")
        architectFile.writeText("""
            project:
              name: $name
              description: "Test project $name"
        """.trimIndent())
        
        return projectDir.absolutePath
    }
}
