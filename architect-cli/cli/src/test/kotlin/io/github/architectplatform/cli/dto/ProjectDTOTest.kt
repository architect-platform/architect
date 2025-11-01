package io.github.architectplatform.cli.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for ProjectDTO data transfer object.
 */
class ProjectDTOTest {
  @Test
  fun `ProjectDTO should be created with correct values`() {
    val contextDTO =
        ProjectDTO.ProjectContextDTO(dir = "/home/user/project", config = mapOf("key" to "value"))
    val project = ProjectDTO(name = "test-project", path = "/home/user/project", context = contextDTO)

    assertEquals("test-project", project.name)
    assertEquals("/home/user/project", project.path)
    assertEquals("/home/user/project", project.context.dir)
    assertEquals(mapOf("key" to "value"), project.context.config)
  }

  @Test
  fun `ProjectContextDTO should hold config correctly`() {
    val config = mapOf("version" to "1.0.0", "author" to "test")
    val context = ProjectDTO.ProjectContextDTO(dir = "/test/dir", config = config)

    assertEquals("/test/dir", context.dir)
    assertEquals(2, context.config.size)
    assertEquals("1.0.0", context.config["version"])
    assertEquals("test", context.config["author"])
  }

  @Test
  fun `ProjectDTO equality should work correctly`() {
    val context1 =
        ProjectDTO.ProjectContextDTO(dir = "/dir", config = emptyMap())
    val context2 =
        ProjectDTO.ProjectContextDTO(dir = "/dir", config = emptyMap())

    val project1 = ProjectDTO(name = "proj", path = "/path", context = context1)
    val project2 = ProjectDTO(name = "proj", path = "/path", context = context2)

    assertEquals(project1, project2)
  }
}
