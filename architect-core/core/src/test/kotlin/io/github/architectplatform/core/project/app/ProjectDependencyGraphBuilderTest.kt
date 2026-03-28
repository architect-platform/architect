package io.github.architectplatform.core.project.app

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.core.project.domain.Project
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

  @Test
  fun `build gradle kts project references infer dependencies`() {
    val rootDir = createTempDirectory("graph-build-kts-refs")
    val apiDir = rootDir.resolve("architect-api")
    val engineDir = rootDir.resolve("architect-engine")
    java.nio.file.Files.createDirectories(apiDir)
    java.nio.file.Files.createDirectories(engineDir)

    // architect-engine's build.gradle.kts depends on architect-api
    engineDir.resolve("build.gradle.kts").writeText(
      """
      dependencies {
          implementation(project(":architect-api"))
      }
      """.trimIndent()
    )

    val api = project(name = "architect-api", path = apiDir.toString())
    val engine = project(name = "architect-engine", path = engineDir.toString())
    val root = project(name = "root", path = rootDir.toString(), subProjects = listOf(api, engine))

    val graph = builder.build(root)

    assertTrue("architect-api" in graph.dependenciesOf("architect-engine"),
      "architect-engine should depend on architect-api via build.gradle.kts")
  }

  @Test
  fun `build gradle project references infer dependencies`() {
    val rootDir = createTempDirectory("graph-build-refs")
    val coreDir = rootDir.resolve("core")
    val appDir = rootDir.resolve("app")
    java.nio.file.Files.createDirectories(coreDir)
    java.nio.file.Files.createDirectories(appDir)

    // app's build.gradle depends on core
    appDir.resolve("build.gradle").writeText(
      """
      dependencies {
          implementation project(':core')
      }
      """.trimIndent()
    )

    val core = project(name = "core", path = coreDir.toString())
    val app = project(name = "app", path = appDir.toString())
    val root = project(name = "root", path = rootDir.toString(), subProjects = listOf(core, app))

    val graph = builder.build(root)

    assertTrue("core" in graph.dependenciesOf("app"),
      "app should depend on core via build.gradle")
  }

  @Test
  fun `build gradle kts grouped notation resolves last segment`() {
    val rootDir = createTempDirectory("graph-build-group")
    val sharedDir = rootDir.resolve("shared")
    val serviceDir = rootDir.resolve("service")
    java.nio.file.Files.createDirectories(sharedDir)
    java.nio.file.Files.createDirectories(serviceDir)

    serviceDir.resolve("build.gradle.kts").writeText(
      """
      dependencies {
          api(project(":platform:shared"))
      }
      """.trimIndent()
    )

    val shared = project(name = "shared", path = sharedDir.toString())
    val service = project(name = "service", path = serviceDir.toString())
    val root = project(name = "root", path = rootDir.toString(), subProjects = listOf(shared, service))

    val graph = builder.build(root)

    assertTrue("shared" in graph.dependenciesOf("service"),
      "service should depend on shared via grouped notation :platform:shared")
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
