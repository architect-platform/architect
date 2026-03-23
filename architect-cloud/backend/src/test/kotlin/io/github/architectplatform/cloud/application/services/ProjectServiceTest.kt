package io.github.architectplatform.cloud.application.services

import io.github.architectplatform.cloud.application.domain.Project
import io.github.architectplatform.cloud.application.ports.outbound.ProjectPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ProjectServiceTest {

  private lateinit var port: InMemoryProjectPort
  private lateinit var broadcast: EventBroadcastService
  private lateinit var service: ProjectService

  @BeforeEach
  fun setUp() {
    port = InMemoryProjectPort()
    broadcast = mock()
    service = ProjectService(port, broadcast)
  }

  @Test
  fun `registerProject creates and saves project`() {
    val project = service.registerProject("p1", "my-project", "/path", "e1", "A project")

    assertEquals("p1", project.id)
    assertEquals("my-project", project.name)
    assertEquals("/path", project.path)
    assertEquals("e1", project.engineId)
    assertEquals("A project", project.description)
    assertNotNull(port.findById("p1"))
  }

  @Test
  fun `registerProject broadcasts project registered event`() {
    service.registerProject("p1", "my-project", "/path", "e1")

    verify(broadcast).broadcastProjectRegistered(any())
  }

  @Test
  fun `registerProject with null description`() {
    val project = service.registerProject("p1", "my-project", "/path", "e1")

    assertNull(project.description)
  }

  @Test
  fun `getProject returns project when exists`() {
    port.save(Project(id = "p1", name = "test", path = "/p", engineId = "e1"))

    val result = service.getProject("p1")

    assertNotNull(result)
    assertEquals("p1", result!!.id)
  }

  @Test
  fun `getProject returns null when not exists`() {
    assertNull(service.getProject("nonexistent"))
  }

  @Test
  fun `getAllProjects returns all saved projects`() {
    port.save(Project(id = "p1", name = "a", path = "/a", engineId = "e1"))
    port.save(Project(id = "p2", name = "b", path = "/b", engineId = "e1"))

    val result = service.getAllProjects()

    assertEquals(2, result.size)
  }

  @Test
  fun `getProjectsByEngine filters by engine id`() {
    port.save(Project(id = "p1", name = "a", path = "/a", engineId = "e1"))
    port.save(Project(id = "p2", name = "b", path = "/b", engineId = "e2"))

    val result = service.getProjectsByEngine("e1")

    assertEquals(1, result.size)
    assertEquals("p1", result[0].id)
  }

  private class InMemoryProjectPort : ProjectPort {
    private val store = mutableMapOf<String, Project>()

    override fun save(project: Project): Project {
      store[project.id] = project
      return project
    }

    override fun findById(id: String) = store[id]
    override fun findAll() = store.values.toList()
    override fun findByEngineId(engineId: String) = store.values.filter { it.engineId == engineId }
    override fun findByName(name: String) = store.values.find { it.name == name }
  }
}
