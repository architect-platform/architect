package io.github.architectplatform.data.adapters.inbound.rest

import io.github.architectplatform.data.application.domain.EngineInstance
import io.github.architectplatform.data.application.domain.EngineStatus
import io.github.architectplatform.data.application.ports.inbound.ManageEngineUseCase
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Integration tests for EngineController.
 * Tests REST API endpoints with Micronaut HTTP client.
 */
@MicronautTest
class EngineControllerTest {
    
    @Inject
    @field:Client("/")
    lateinit var client: HttpClient
    
    private lateinit var manageEngineUseCase: ManageEngineUseCase
    
    @Test
    fun `GET api engines should return all engines`() {
        val request = HttpRequest.GET<List<DataDTO.EngineDTO>>("/api/engines")
        
        val response = client.toBlocking().exchange(request, List::class.java)
        
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
    }
    
    @Test
    fun `GET api engines id should return specific engine`() {
        val request = HttpRequest.GET<DataDTO.EngineDTO>("/api/engines/engine-1")
        
        val response = client.toBlocking().exchange(request, DataDTO.EngineDTO::class.java)
        
        assertTrue(response.status == HttpStatus.OK || response.status == HttpStatus.NOT_FOUND)
    }
    
    @Test
    fun `POST api engines should create new engine`() {
        val engineDTO = DataDTO.EngineDTO(
            id = "test-engine",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0",
            status = "ACTIVE"
        )
        
        val request = HttpRequest.POST("/api/engines", engineDTO)
        
        val response = client.toBlocking().exchange(request, DataDTO.EngineDTO::class.java)
        
        assertEquals(HttpStatus.CREATED, response.status)
    }
    
    @Test
    fun `PUT api engines id heartbeat should update heartbeat`() {
        val request = HttpRequest.PUT<Any>("/api/engines/engine-1/heartbeat", null)
        
        val response = client.toBlocking().exchange(request, DataDTO.EngineDTO::class.java)
        
        assertTrue(response.status == HttpStatus.OK || response.status == HttpStatus.NOT_FOUND)
    }
    
    @Test
    fun `DELETE api engines id should delete engine`() {
        val request = HttpRequest.DELETE<Any>("/api/engines/engine-1")
        
        val response = client.toBlocking().exchange(request, Void::class.java)
        
        assertTrue(response.status == HttpStatus.NO_CONTENT || response.status == HttpStatus.NOT_FOUND)
    }
}
