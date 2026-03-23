package io.github.architectplatform.cloud.adapters.outbound.persistence

import io.github.architectplatform.cloud.application.domain.*
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@MicronautTest
class PersistenceAdapterIntegrationTest {

  @Inject lateinit var engineAdapter: EngineInstancePersistenceAdapter
  @Inject lateinit var projectAdapter: ProjectPersistenceAdapter
  @Inject lateinit var executionAdapter: ExecutionPersistenceAdapter
  @Inject lateinit var eventAdapter: ExecutionEventPersistenceAdapter

  // --- EngineInstance ---

  @Test
  fun `save and find engine instance by id`() {
    val engine = EngineInstance(id = "e-int-1", hostname = "host1", port = 8080, version = "1.0")
    engineAdapter.save(engine)

    val found = engineAdapter.findById("e-int-1")

    assertNotNull(found)
    assertEquals("host1", found!!.hostname)
    assertEquals(8080, found.port)
  }

  @Test
  fun `find engine instance returns null when not exists`() {
    assertNull(engineAdapter.findById("nonexistent-engine"))
  }

  @Test
  fun `find engines by status`() {
    engineAdapter.save(EngineInstance(id = "e-int-2", hostname = "h2", port = 8081, version = null, status = EngineStatus.ACTIVE))
    engineAdapter.save(EngineInstance(id = "e-int-3", hostname = "h3", port = 8082, version = null, status = EngineStatus.OFFLINE))

    val active = engineAdapter.findByStatus(EngineStatus.ACTIVE)

    assertTrue(active.any { it.id == "e-int-2" })
    assertFalse(active.any { it.id == "e-int-3" })
  }

  @Test

  fun `save and find project by id`() {
    val project = Project(id = "p-int-1", name = "test-project", path = "/path", engineId = "e1", description = "desc")
    projectAdapter.save(project)

    val found = projectAdapter.findById("p-int-1")

    assertNotNull(found)
    assertEquals("test-project", found!!.name)
    assertEquals("desc", found.description)
  }

  @Test
  fun `find project by name`() {
    projectAdapter.save(Project(id = "p-int-2", name = "unique-project", path = "/p", engineId = "e1"))

    val found = projectAdapter.findByName("unique-project")

    assertNotNull(found)
    assertEquals("p-int-2", found!!.id)
  }

  @Test
  fun `find projects by engine id`() {
    projectAdapter.save(Project(id = "p-int-3", name = "a", path = "/a", engineId = "eng-filter-1"))
    projectAdapter.save(Project(id = "p-int-4", name = "b", path = "/b", engineId = "eng-filter-2"))

    val result = projectAdapter.findByEngineId("eng-filter-1")

    assertTrue(result.any { it.id == "p-int-3" })
    assertFalse(result.any { it.id == "p-int-4" })
  }

  // --- Execution ---

  @Test
  fun `save and find execution by id`() {
    val execution = Execution(id = "x-int-1", projectId = "p1", engineId = "e1", taskId = "build", status = ExecutionStatus.STARTED, message = "running")
    executionAdapter.save(execution)

    val found = executionAdapter.findById("x-int-1")

    assertNotNull(found)
    assertEquals(ExecutionStatus.STARTED, found!!.status)
    assertEquals("running", found.message)
  }

  @Test
  fun `find executions by project id`() {
    executionAdapter.save(Execution(id = "x-int-2", projectId = "proj-filter-1", engineId = "e1", taskId = "t1", status = ExecutionStatus.STARTED))
    executionAdapter.save(Execution(id = "x-int-3", projectId = "proj-filter-2", engineId = "e1", taskId = "t2", status = ExecutionStatus.STARTED))

    val result = executionAdapter.findByProjectId("proj-filter-1")

    assertTrue(result.any { it.id == "x-int-2" })
    assertFalse(result.any { it.id == "x-int-3" })
  }

  @Test
  fun `find executions by status`() {
    executionAdapter.save(Execution(id = "x-int-4", projectId = "p1", engineId = "e1", taskId = "t1", status = ExecutionStatus.COMPLETED))
    executionAdapter.save(Execution(id = "x-int-5", projectId = "p1", engineId = "e1", taskId = "t2", status = ExecutionStatus.FAILED))

    val completed = executionAdapter.findByStatus(ExecutionStatus.COMPLETED)

    assertTrue(completed.any { it.id == "x-int-4" })
    assertFalse(completed.any { it.id == "x-int-5" })
  }

  // --- ExecutionEvent ---

  @Test
  fun `save and find execution events by execution id`() {
    eventAdapter.save(ExecutionEvent(id = "ev-int-1", executionId = "exec-filter-1", eventType = "STARTED"))
    eventAdapter.save(ExecutionEvent(id = "ev-int-2", executionId = "exec-filter-1", eventType = "COMPLETED"))
    eventAdapter.save(ExecutionEvent(id = "ev-int-3", executionId = "exec-filter-2", eventType = "STARTED"))

    val result = eventAdapter.findByExecutionId("exec-filter-1")

    assertEquals(2, result.size)
    assertTrue(result.all { it.executionId == "exec-filter-1" })
  }

  @Test
  fun `find execution events returns empty for unknown execution`() {
    val result = eventAdapter.findByExecutionId("nonexistent-exec")

    assertTrue(result.isEmpty())
  }
}
