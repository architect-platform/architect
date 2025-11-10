package io.github.architectplatform.data.application.domain

import io.github.architectplatform.data.application.domain.EngineStatus.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

/**
 * Unit tests for EngineInstance domain model.
 * Tests business logic without any infrastructure dependencies.
 */
class EngineInstanceTest {
    
    @Test
    fun `should create engine instance with default values`() {
        val engine = EngineInstance(
            id = "engine-1",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0"
        )
        
        assertEquals("engine-1", engine.id)
        assertEquals("localhost", engine.hostname)
        assertEquals(9292, engine.port)
        assertEquals("1.0.0", engine.version)
        assertEquals(ACTIVE, engine.status)
        assertNotNull(engine.createdAt)
        assertNotNull(engine.lastHeartbeat)
    }
    
    @Test
    fun `updateHeartbeat should update lastHeartbeat timestamp`() {
        val engine = EngineInstance(
            id = "engine-1",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0"
        )
        
        val originalHeartbeat = engine.lastHeartbeat
        Thread.sleep(10) // Ensure time difference
        
        val updated = engine.updateHeartbeat()
        
        assertTrue(updated.lastHeartbeat.isAfter(originalHeartbeat))
        assertEquals(engine.id, updated.id)
        assertEquals(engine.hostname, updated.hostname)
    }
    
    @Test
    fun `markInactive should change status to INACTIVE`() {
        val engine = EngineInstance(
            id = "engine-1",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0",
            status = ACTIVE
        )
        
        val inactive = engine.markInactive()
        
        assertEquals(INACTIVE, inactive.status)
        assertEquals(engine.id, inactive.id)
    }
    
    @Test
    fun `markOffline should change status to OFFLINE`() {
        val engine = EngineInstance(
            id = "engine-1",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0",
            status = ACTIVE
        )
        
        val offline = engine.markOffline()
        
        assertEquals(OFFLINE, offline.status)
        assertEquals(engine.id, offline.id)
    }
    
    @Test
    fun `reactivate should change status to ACTIVE and update heartbeat`() {
        val engine = EngineInstance(
            id = "engine-1",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0",
            status = OFFLINE
        )
        
        val originalHeartbeat = engine.lastHeartbeat
        Thread.sleep(10)
        
        val reactivated = engine.reactivate()
        
        assertEquals(ACTIVE, reactivated.status)
        assertTrue(reactivated.lastHeartbeat.isAfter(originalHeartbeat))
    }
    
    @Test
    fun `engine instance should be immutable`() {
        val engine = EngineInstance(
            id = "engine-1",
            hostname = "localhost",
            port = 9292,
            version = "1.0.0"
        )
        
        val updated = engine.copy(hostname = "remote-host")
        
        assertEquals("localhost", engine.hostname)
        assertEquals("remote-host", updated.hostname)
        assertEquals(engine.id, updated.id)
    }
}
