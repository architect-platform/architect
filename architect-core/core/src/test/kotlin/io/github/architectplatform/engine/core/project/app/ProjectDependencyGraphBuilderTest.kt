package io.github.architectplatform.engine.core.project.app

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.engine.core.project.domain.Project
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectDependencyGraphBuilderTest {

  private val builder = ProjectDependencyGraphBuilder()

  @Test
  fun `declared subprojects as strings create child to parent dependency`() {
    val rootDir = createTempDirectory("graph-root")
    val appDir = rootDir.resolve("app")
    java.nio.file.Files.createDirectories(appDir)

    val app = project(name = "app", path = appDir.toString())
    val root =
      project(
        name = "root",
        path = rootDir.toString(),
        config = mapOf("subprojects" to listOf("app")),
        subProjects = listOf(app),
      )

    val graph = builder.build(root)

    assertEquals(setOf("root", "app"), graph.projects)
    assertEquals(setOf("root"), graph.dependenciesOf("app"))
  }

  @Test
  fun `declared subproject dependencies are respected`() {
    val rootDir = createTempDirectory("graph-explicit")
    val sharedDir = rootDir.resolve("shared-lib")
    val apiDir = rootDir.resolve("api")
    java.nio.file.Files.createDirectories(sharedDir)
    java.nio.file.Files.createDirectories(apiDir)

    val shared = project(name = "shared-lib", path = sharedDir.toString())
    val api = project(name = "api", path = apiDir.toString())
    val root =
      project(
        name = "root",
        path = rootDir.toString(),
        config =
          mapOf(
            "subprojects" to
              listOf(
                mapOf("name" to "api", "dependsOn" to listOf("shared-lib")),
                "shared-lib",
              ),
          ),
        subProjects = listOf(shared, api),
      )

    val graph = builder.build(root)

    assertTrue("shared-lib" in graph.dependenciesOf("api"))
  }

  @Test
  fun `hierarchy infers child dependency on direct parent`() {
    val rootDir = createTempDirectory("graph-hierarchy")
    val childDir = rootDir.resolve("child")
    java.nio.file.Files.createDirectories(childDir)

    val child = project(name = "child", path = childDir.toString())
    val root = project(name = "root", path = rootDir.toString(), subProjects = listOf(child))

    val graph = builder.build(root)

    assertTrue("root" in graph.dependenciesOf("child"))
  }

  @Test
  fun `shared build file infers dependency when child has no build file`() {
    val rootDir = createTempDirectory("graph-build")
    rootDir.resolve("build.gradle.kts").writeText("plugins {}")
    val childDir = rootDir.resolve("module-a")
    java.nio.file.Files.createDirectories(childDir)

    val child = project(name = "module-a", path = childDir.toString())
    val root = project(name = "root", path = rootDir.toString(), subProjects = listOf(child))

    val graph = builder.build(root)

    assertTrue("root" in graph.dependenciesOf("module-a"))
  }

  private fun project(
    name: String,
    path: String,
    config: Map<String, Any> = emptyMap(),
    subProjects: List<Project> = emptyList(),
  ): Project =
    Project(
      name = name,
      path = path,
      context = ProjectContext(java.nio.file.Path.of(path), config),
      plugins = emptyList(),
      subProjects = subProjects,
    )
}
