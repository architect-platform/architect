package io.github.architectplatform.plugins.gradlearchitected

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for GradleContext data class.
 */
class GradleContextTest {
  @Test
  fun `GradleContext should have empty projects by default`() {
    val context = GradleContext()

    assertTrue(context.projects.isEmpty())
  }

  @Test
  fun `GradleContext should store multiple projects`() {
    val project1 = GradleProjectContext(name = "api", path = "api")
    val project2 = GradleProjectContext(name = "web", path = "web")
    val context = GradleContext(projects = listOf(project1, project2))

    assertEquals(2, context.projects.size)
    assertEquals("api", context.projects[0].name)
    assertEquals("web", context.projects[1].name)
  }

  @Test
  fun `GradleContext equality should work correctly`() {
    val project = GradleProjectContext(name = "test", path = ".")
    val context1 = GradleContext(projects = listOf(project))
    val context2 = GradleContext(projects = listOf(project))

    assertEquals(context1, context2)
  }
}
