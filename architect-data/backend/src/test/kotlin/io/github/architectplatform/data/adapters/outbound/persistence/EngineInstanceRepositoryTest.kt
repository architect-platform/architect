package io.github.architectplatform.data.adapters.outbound.persistence

import io.github.architectplatform.data.application.domain.EngineInstance
import io.github.architectplatform.data.application.domain.EngineStatus
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import reactor.test.StepVerifier

/**
 * Integration tests for EngineInstanceRepository.
 * Tests database operations with H2 in-memory database.
 */
@MicronautTest
class EngineInstanceRepositoryTest {
    
    @Inject
    lateinit var repository: EngineInstanceRepository
    
    @Inject
    lateinit var adapter: EngineInstancePersistenceAdapter
    
    @Test
    fun `should save and retrieve engine instance`() {
        val engine = EngineInstance(
            id = "test-engine-1",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0"
        )
        
        StepVerifier.create(adapter.save(engine))
            .expectNextMatches { it.id == engine.id }
            .verifyComplete()
        
        StepVerifier.create(adapter.findById("test-engine-1"))
            .expectNextMatches { 
                it.hostname == "localhost" && it.port == 9292 
            }
            .verifyComplete()
    }
    
    @Test
    fun `should find engines by status`() {
        val engine = EngineInstance(
            id = "test-engine-2",
            hostname = "remote",
            port = 9292,
            version = "1.0.0",
            status = EngineStatus.ACTIVE
        )
        
        adapter.save(engine).block()
        
        StepVerifier.create(adapter.findByStatus(EngineStatus.ACTIVE))
            .expectNextMatches { it.status == EngineStatus.ACTIVE }
            .thenCancel()
            .verify()
    }
    
    @Test
    fun `should delete engine instance`() {
        val engine = EngineInstance(
            id = "test-engine-3",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0"
        )
        
        adapter.save(engine).block()
        
        StepVerifier.create(adapter.deleteById("test-engine-3"))
            .verifyComplete()
        
        StepVerifier.create(adapter.findById("test-engine-3"))
            .verifyComplete()
    }
}
