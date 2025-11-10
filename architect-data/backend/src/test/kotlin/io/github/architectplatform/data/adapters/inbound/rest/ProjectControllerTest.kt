package io.github.architectplatform.data.adapters.inbound.rest

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Integration tests for ProjectController.
 */
@MicronautTest
class ProjectControllerTest {
    
    @Inject
    @field:Client("/")
    lateinit var client: HttpClient
    
    @Test
    fun `GET api projects should return all projects`() {
        val request = HttpRequest.GET<List<DataDTO.ProjectDTO>>("/api/projects")
        
        val response = client.toBlocking().exchange(request, List::class.java)
        
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
    }
    
    @Test
    fun `GET api projects id should return specific project`() {
        val request = HttpRequest.GET<DataDTO.ProjectDTO>("/api/projects/proj-1")
        
        val response = client.toBlocking().exchange(request, DataDTO.ProjectDTO::class.java)
        
        assertTrue(response.status == HttpStatus.OK || response.status == HttpStatus.NOT_FOUND)
    }
    
    @Test
    fun `POST api projects should create new project`() {
        val projectDTO = DataDTO.ProjectDTO(
            id = "test-proj",
            name = "test-project",
            path = "/path/to/project",
            engineId = "engine-1",
            description = "Test project"
        )
        
        val request = HttpRequest.POST("/api/projects", projectDTO)
        
        val response = client.toBlocking().exchange(request, DataDTO.ProjectDTO::class.java)
        
        assertEquals(HttpStatus.CREATED, response.status)
    }
    
    @Test
    fun `GET api projects engine engineId should return projects for engine`() {
        val request = HttpRequest.GET<List<DataDTO.ProjectDTO>>("/api/projects/engine/engine-1")
        
        val response = client.toBlocking().exchange(request, List::class.java)
        
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
    }
    
    @Test
    fun `DELETE api projects id should delete project`() {
        val request = HttpRequest.DELETE<Any>("/api/projects/proj-1")
        
        val response = client.toBlocking().exchange(request, Void::class.java)
        
        assertTrue(response.status == HttpStatus.NO_CONTENT || response.status == HttpStatus.NOT_FOUND)
    }
}
