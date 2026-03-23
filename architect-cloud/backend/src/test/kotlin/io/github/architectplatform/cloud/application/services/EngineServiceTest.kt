package io.github.architectplatform.cloud.application.services

import io.github.architectplatform.cloud.application.domain.EngineInstance
import io.github.architectplatform.cloud.application.domain.EngineStatus
import io.github.architectplatform.cloud.application.ports.outbound.EngineInstancePort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class EngineServiceTest {

  private lateinit var port: InMemoryEngineInstancePort
  private lateinit var broadcast: EventBroadcastService
  private lateinit var service: EngineService

  @BeforeEach
  fun setUp() {
    port = InMemoryEngineInstancePort()
    broadcast = mock()
    service = EngineService(port, broadcast)
  }

  @Test
  fun `registerEngine creates and saves engine instance`() {
    val engine = service.registerEngine("e1", "localhost", 8080, "1.0.0")

    assertEquals("e1", engine.id)
    assertEquals("localhost", engine.hostname)
    assertEquals(8080, engine.port)
    assertEquals("1.0.0", engine.version)
    assertEquals(EngineStatus.ACTIVE, engine.status)
    assertNotNull(port.findById("e1"))
  }

  @Test
  fun `registerEngine broadcasts engine registered event`() {
    service.registerEngine("e1", "localhost", 8080, "1.0.0")

    verify(broadcast).broadcastEngineRegistered(any())
  }

  @Test
  fun `recordHeartbeat updates heartbeat via port`() {
    port.save(EngineInstance(id = "e1", hostname = "localhost", port = 8080, version = "1.0.0"))

    service.recordHeartbeat("e1")

    assertTrue(port.heartbeatUpdated.contains("e1"))
  }

  @Test
  fun `recordHeartbeat broadcasts heartbeat event`() {
    service.recordHeartbeat("e1")

    verify(broadcast).broadcastEngineHeartbeat("e1")
  }

  @Test
  fun `getEngine returns engine when exists`() {
    port.save(EngineInstance(id = "e1", hostname = "localhost", port = 8080, version = "1.0.0"))

    val result = service.getEngine("e1")

    assertNotNull(result)
    assertEquals("e1", result!!.id)
  }

  @Test
  fun `getEngine returns null when not exists`() {
    assertNull(service.getEngine("nonexistent"))
  }

  @Test
  fun `getAllEngines returns all saved engines`() {
    port.save(EngineInstance(id = "e1", hostname = "h1", port = 8080, version = null))
    port.save(EngineInstance(id = "e2", hostname = "h2", port = 8081, version = null))

    val result = service.getAllEngines()

    assertEquals(2, result.size)
  }

  @Test
  fun `getActiveEngines returns only active engines`() {
    port.save(EngineInstance(id = "e1", hostname = "h1", port = 8080, version = null, status = EngineStatus.ACTIVE))
    port.save(EngineInstance(id = "e2", hostname = "h2", port = 8081, version = null, status = EngineStatus.OFFLINE))

    val result = service.getActiveEngines()

    assertEquals(1, result.size)
    assertEquals("e1", result[0].id)
  }

  private class InMemoryEngineInstancePort : EngineInstancePort {
    private val store = mutableMapOf<String, EngineInstance>()
    val heartbeatUpdated = mutableListOf<String>()

    override fun save(engine: EngineInstance): EngineInstance {
      store[engine.id] = engine
      return engine
    }

    override fun findById(id: String) = store[id]
    override fun findAll() = store.values.toList()
    override fun findByStatus(status: EngineStatus) = store.values.filter { it.status == status }
    override fun updateHeartbeat(id: String): Boolean {
      heartbeatUpdated.add(id)
      return true
    }
  }
}
