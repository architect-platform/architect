package io.github.architectplatform.plugins.gradlearchitected

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for GradleProjectContext data class.
 */
class GradleProjectContextTest {
  @Test
  fun `GradleProjectContext should be created with correct values`() {
    val project =
        GradleProjectContext(
            name = "api", path = "api", githubPackageRelease = true, gradlePath = "./gradlew")

    assertEquals("api", project.name)
    assertEquals("api", project.path)
    assertTrue(project.githubPackageRelease)
    assertEquals("./gradlew", project.gradlePath)
  }

  @Test
  fun `GradleProjectContext should have default values`() {
    val project = GradleProjectContext(name = "core")

    assertEquals("core", project.name)
    assertEquals(".", project.path)
    assertFalse(project.githubPackageRelease)
    assertEquals("./gradlew", project.gradlePath)
  }

  @Test
  fun `GradleProjectContext equality should work correctly`() {
    val project1 = GradleProjectContext(name = "web", path = "web")
    val project2 = GradleProjectContext(name = "web", path = "web")
    val project3 = GradleProjectContext(name = "api", path = "api")

    assertEquals(project1, project2)
    assertNotEquals(project1, project3)
  }

  @Test
  fun `GradleProjectContext should allow custom gradle path`() {
    val project = GradleProjectContext(name = "legacy", path = "legacy", gradlePath = "../gradlew")

    assertEquals("../gradlew", project.gradlePath)
  }

  @Test
  fun `GradleProjectContext should support github package release flag`() {
    val project1 = GradleProjectContext(name = "lib", githubPackageRelease = true)
    val project2 = GradleProjectContext(name = "app", githubPackageRelease = false)

    assertTrue(project1.githubPackageRelease)
    assertFalse(project2.githubPackageRelease)
  }
}
