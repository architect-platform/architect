package io.github.architectplatform.cloud.application.services

import io.github.architectplatform.cloud.application.domain.Execution
import io.github.architectplatform.cloud.application.domain.ExecutionStatus
import io.github.architectplatform.cloud.application.ports.outbound.ExecutionPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ExecutionServiceTest {

  private lateinit var port: InMemoryExecutionPort
  private lateinit var broadcast: EventBroadcastService
  private lateinit var service: ExecutionService

  @BeforeEach
  fun setUp() {
    port = InMemoryExecutionPort()
    broadcast = mock()
    service = ExecutionService(port, broadcast)
  }

  @Test
  fun `reportExecution creates new execution when not exists`() {
    val execution = service.reportExecution("x1", "p1", "e1", "build", ExecutionStatus.STARTED, "starting")

    assertEquals("x1", execution.id)
    assertEquals("p1", execution.projectId)
    assertEquals("e1", execution.engineId)
    assertEquals("build", execution.taskId)
    assertEquals(ExecutionStatus.STARTED, execution.status)
    assertEquals("starting", execution.message)
    assertNotNull(port.findById("x1"))
  }

  @Test
  fun `reportExecution updates existing execution`() {
    port.save(Execution(id = "x1", projectId = "p1", engineId = "e1", taskId = "build", status = ExecutionStatus.STARTED))

    val updated = service.reportExecution("x1", "p1", "e1", "build", ExecutionStatus.COMPLETED, "done")

    assertEquals(ExecutionStatus.COMPLETED, updated.status)
    assertEquals("done", updated.message)
  }

  @Test
  fun `reportExecution updates with error details`() {
    port.save(Execution(id = "x1", projectId = "p1", engineId = "e1", taskId = "build", status = ExecutionStatus.RUNNING))

    val updated = service.reportExecution("x1", "p1", "e1", "build", ExecutionStatus.FAILED, "failed", "stack trace")

    assertEquals(ExecutionStatus.FAILED, updated.status)
    assertEquals("stack trace", updated.errorDetails)
  }

  @Test
  fun `reportExecution broadcasts execution event`() {
    service.reportExecution("x1", "p1", "e1", "build", ExecutionStatus.STARTED)

    verify(broadcast).broadcastExecutionReported(any())
  }

  @Test
  fun `getExecution returns execution when exists`() {
    port.save(Execution(id = "x1", projectId = "p1", engineId = "e1", taskId = "build", status = ExecutionStatus.STARTED))

    val result = service.getExecution("x1")

    assertNotNull(result)
    assertEquals("x1", result!!.id)
  }

  @Test
  fun `getExecution returns null when not exists`() {
    assertNull(service.getExecution("nonexistent"))
  }

  @Test
  fun `getExecutionsByProject filters by project id`() {
    port.save(Execution(id = "x1", projectId = "p1", engineId = "e1", taskId = "build", status = ExecutionStatus.STARTED))
    port.save(Execution(id = "x2", projectId = "p2", engineId = "e1", taskId = "test", status = ExecutionStatus.STARTED))

    val result = service.getExecutionsByProject("p1")

    assertEquals(1, result.size)
    assertEquals("x1", result[0].id)
  }

  @Test
  fun `getExecutionsByEngine filters by engine id`() {
    port.save(Execution(id = "x1", projectId = "p1", engineId = "e1", taskId = "build", status = ExecutionStatus.STARTED))
    port.save(Execution(id = "x2", projectId = "p1", engineId = "e2", taskId = "build", status = ExecutionStatus.STARTED))

    val result = service.getExecutionsByEngine("e1")

    assertEquals(1, result.size)
    assertEquals("x1", result[0].id)
  }

  private class InMemoryExecutionPort : ExecutionPort {
    private val store = mutableMapOf<String, Execution>()

    override fun save(execution: Execution): Execution {
      store[execution.id] = execution
      return execution
    }

    override fun findById(id: String) = store[id]
    override fun findByProjectId(projectId: String) = store.values.filter { it.projectId == projectId }
    override fun findByEngineId(engineId: String) = store.values.filter { it.engineId == engineId }
    override fun findByStatus(status: ExecutionStatus) = store.values.filter { it.status == status }
  }
}
