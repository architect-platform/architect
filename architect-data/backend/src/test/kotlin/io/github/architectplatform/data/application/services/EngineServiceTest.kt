package io.github.architectplatform.data.application.services

import io.github.architectplatform.data.application.domain.EngineInstance
import io.github.architectplatform.data.application.domain.EngineStatus
import io.github.architectplatform.data.application.ports.outbound.EngineInstancePort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * Unit tests for EngineService.
 * Uses MockK to mock dependencies and verify behavior.
 */
class EngineServiceTest {
    
    private lateinit var enginePort: EngineInstancePort
    private lateinit var engineService: EngineService
    
    @BeforeEach
    fun setup() {
        enginePort = mockk()
        engineService = EngineService(enginePort)
    }
    
    @Test
    fun `registerEngine should save new engine instance`() {
        val engine = EngineInstance(
            id = "engine-1",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0"
        )
        
        every { enginePort.save(any()) } returns Mono.just(engine)
        
        StepVerifier.create(engineService.registerEngine(engine))
            .expectNext(engine)
            .verifyComplete()
        
        verify(exactly = 1) { enginePort.save(engine) }
    }
    
    @Test
    fun `getEngine should return engine by id`() {
        val engine = EngineInstance(
            id = "engine-1",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0"
        )
        
        every { enginePort.findById("engine-1") } returns Mono.just(engine)
        
        StepVerifier.create(engineService.getEngine("engine-1"))
            .expectNext(engine)
            .verifyComplete()
    }
    
    @Test
    fun `getEngine should return empty when not found`() {
        every { enginePort.findById("unknown") } returns Mono.empty()
        
        StepVerifier.create(engineService.getEngine("unknown"))
            .verifyComplete()
    }
    
    @Test
    fun `getAllEngines should return all engines`() {
        val engines = listOf(
            EngineInstance("engine-1", "localhost", 9292, "1.0.0"),
            EngineInstance("engine-2", "remote", 9292, "1.0.1")
        )
        
        every { enginePort.findAll() } returns Flux.fromIterable(engines)
        
        StepVerifier.create(engineService.getAllEngines())
            .expectNext(engines[0])
            .expectNext(engines[1])
            .verifyComplete()
    }
    
    @Test
    fun `getActiveEngines should return only active engines`() {
        val activeEngine = EngineInstance("engine-1", "localhost", 9292, "1.0.0", status = EngineStatus.ACTIVE)
        
        every { enginePort.findByStatus(EngineStatus.ACTIVE) } returns Flux.just(activeEngine)
        
        StepVerifier.create(engineService.getActiveEngines())
            .expectNext(activeEngine)
            .verifyComplete()
        
        verify { enginePort.findByStatus(EngineStatus.ACTIVE) }
    }
    
    @Test
    fun `updateHeartbeat should update engine heartbeat`() {
        val engine = EngineInstance(
            id = "engine-1",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0"
        )
        
        val updatedEngine = engine.updateHeartbeat()
        
        every { enginePort.findById("engine-1") } returns Mono.just(engine)
        every { enginePort.save(any()) } returns Mono.just(updatedEngine)
        
        StepVerifier.create(engineService.updateHeartbeat("engine-1"))
            .expectNextMatches { it.lastHeartbeat.isAfter(engine.lastHeartbeat) }
            .verifyComplete()
        
        val savedSlot = slot<EngineInstance>()
        verify { enginePort.save(capture(savedSlot)) }
        assertTrue(savedSlot.captured.lastHeartbeat.isAfter(engine.lastHeartbeat))
    }
    
    @Test
    fun `markEngineOffline should change status to OFFLINE`() {
        val activeEngine = EngineInstance(
            id = "engine-1",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0",
            status = EngineStatus.ACTIVE
        )
        
        val offlineEngine = activeEngine.markOffline()
        
        every { enginePort.findById("engine-1") } returns Mono.just(activeEngine)
        every { enginePort.save(any()) } returns Mono.just(offlineEngine)
        
        StepVerifier.create(engineService.markEngineOffline("engine-1"))
            .expectNextMatches { it.status == EngineStatus.OFFLINE }
            .verifyComplete()
    }
    
    @Test
    fun `deleteEngine should call port delete`() {
        every { enginePort.deleteById("engine-1") } returns Mono.empty()
        
        StepVerifier.create(engineService.deleteEngine("engine-1"))
            .verifyComplete()
        
        verify(exactly = 1) { enginePort.deleteById("engine-1") }
    }
}
