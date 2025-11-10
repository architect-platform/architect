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
 * Integration tests for ExecutionController.
 */
@MicronautTest
class ExecutionControllerTest {
    
    @Inject
    @field:Client("/")
    lateinit var client: HttpClient
    
    @Test
    fun `GET api executions id should return specific execution`() {
        val request = HttpRequest.GET<DataDTO.ExecutionDTO>("/api/executions/exec-1")
        
        val response = client.toBlocking().exchange(request, DataDTO.ExecutionDTO::class.java)
        
        assertTrue(response.status == HttpStatus.OK || response.status == HttpStatus.NOT_FOUND)
    }
    
    @Test
    fun `POST api executions should create new execution`() {
        val executionDTO = DataDTO.ExecutionDTO(
            id = "test-exec",
            projectId = "proj-1",
            engineId = "engine-1",
            taskId = "build",
            status = "STARTED"
        )
        
        val request = HttpRequest.POST("/api/executions", executionDTO)
        
        val response = client.toBlocking().exchange(request, DataDTO.ExecutionDTO::class.java)
        
        assertEquals(HttpStatus.CREATED, response.status)
    }
    
    @Test
    fun `PUT api executions id status should update execution status`() {
        val statusUpdate = mapOf(
            "status" to "RUNNING",
            "message" to "Executing..."
        )
        
        val request = HttpRequest.PUT("/api/executions/exec-1/status", statusUpdate)
        
        val response = client.toBlocking().exchange(request, DataDTO.ExecutionDTO::class.java)
        
        assertTrue(response.status == HttpStatus.OK || response.status == HttpStatus.NOT_FOUND)
    }
    
    @Test
    fun `GET api executions project projectId should return executions for project`() {
        val request = HttpRequest.GET<List<DataDTO.ExecutionDTO>>("/api/executions/project/proj-1")
        
        val response = client.toBlocking().exchange(request, List::class.java)
        
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
    }
    
    @Test
    fun `GET api executions engine engineId should return executions for engine`() {
        val request = HttpRequest.GET<List<DataDTO.ExecutionDTO>>("/api/executions/engine/engine-1")
        
        val response = client.toBlocking().exchange(request, List::class.java)
        
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
    }
    
    @Test
    fun `GET api executions id events should return events for execution`() {
        val request = HttpRequest.GET<List<DataDTO.ExecutionEventDTO>>("/api/executions/exec-1/events")
        
        val response = client.toBlocking().exchange(request, List::class.java)
        
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
    }
    
    @Test
    fun `POST api executions id events should record new event`() {
        val eventDTO = DataDTO.ExecutionEventDTO(
            id = "test-event",
            executionId = "exec-1",
            eventType = "task.started",
            taskId = "build",
            message = "Task started"
        )
        
        val request = HttpRequest.POST("/api/executions/exec-1/events", eventDTO)
        
        val response = client.toBlocking().exchange(request, DataDTO.ExecutionEventDTO::class.java)
        
        assertEquals(HttpStatus.CREATED, response.status)
    }
    
    @Test
    fun `DELETE api executions id should delete execution`() {
        val request = HttpRequest.DELETE<Any>("/api/executions/exec-1")
        
        val response = client.toBlocking().exchange(request, Void::class.java)
        
        assertTrue(response.status == HttpStatus.NO_CONTENT || response.status == HttpStatus.NOT_FOUND)
    }
}
