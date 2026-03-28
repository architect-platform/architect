package io.github.architectplatform.core.project.app

import io.github.architectplatform.core.project.domain.Project
import io.github.architectplatform.core.project.domain.ProjectDependencyGraph
import java.io.File

/**
 * Builds a project dependency graph for affected-project resolution.
 *
 * Sources:
 * - Explicit declarations from `subprojects` config blocks
 * - Inferred parent-child hierarchy dependencies in discovered monorepos
 * - Inferred shared-build-script dependency (child uses parent's build file)
 * - Parsed `build.gradle.kts` / `build.gradle` for `project(":...")` dependency declarations
 */
class ProjectDependencyGraphBuilder {

  fun build(root: Project): ProjectDependencyGraph {
    val allProjects = flatten(root)
    val projectByName = allProjects.associateBy { it.name }
    val declaredDependencies = collectDeclaredDependencies(allProjects, projectByName)
    val dependencies = allProjects.associate { project ->
      project.name to resolveDependencies(project, projectByName, declaredDependencies)
    }
    return ProjectDependencyGraph(
      projects = projectByName.keys,
      dependencies = dependencies,
    )
  }

  private fun flatten(root: Project): List<Project> {
    val result = mutableListOf<Project>()

    fun visit(project: Project) {
      result += project
      project.subProjects.forEach(::visit)
    }

    visit(root)
    return result
  }

  private fun resolveDependencies(
    project: Project,
    projectByName: Map<String, Project>,
    declaredDependencies: Map<String, Set<String>>,
  ): Set<String> {
    val explicitNames = declaredDependencies[project.name].orEmpty().filter { it in projectByName }

    val inferred = mutableSetOf<String>()
    inferred += inferParentDependency(project, projectByName)
    inferred += inferSharedBuildDependency(project, projectByName)
    inferred += inferBuildFileDependencies(project, projectByName)

    return (explicitNames + inferred).filter { it != project.name }.toSet()
  }

  private fun collectDeclaredDependencies(
    projects: List<Project>,
    projectByName: Map<String, Project>,
  ): Map<String, Set<String>> {
    val result = mutableMapOf<String, MutableSet<String>>()

    projects.forEach { owner ->
      val rawSubprojects = owner.context.config["subprojects"]
      if (rawSubprojects !is List<*>) return@forEach

      rawSubprojects.forEach subprojects@{ entry ->
        when (entry) {
          is String -> {
            // Bare declaration means the subproject depends on its declaring parent.
            if (entry in projectByName) {
              result.getOrPut(entry) { mutableSetOf() } += owner.name
            }
          }
          is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val declared = entry as Map<String, Any?>
            val childName =
              (declared["name"] as? String)
                ?: (declared["path"] as? String)?.let { File(it).name }
                ?: return@subprojects

            if (childName !in projectByName) return@subprojects

            val dependsOn =
              (declared["dependsOn"] as? List<*>)?.filterIsInstance<String>().orEmpty() +
                (declared["dependencies"] as? List<*>)?.filterIsInstance<String>().orEmpty()

            val dependencySet = result.getOrPut(childName) { mutableSetOf() }
            if (dependsOn.isEmpty()) {
              dependencySet += owner.name
            } else {
              dependencySet += dependsOn
            }
          }
        }
      }
    }

    return result
  }

  private fun inferParentDependency(
    project: Project,
    projectByName: Map<String, Project>,
  ): Set<String> {
    val projectDir = File(project.path).canonicalFile
    val parent =
      projectByName.values.firstOrNull { candidate ->
        if (candidate.name == project.name) return@firstOrNull false
        val candidateDir = File(candidate.path).canonicalFile
        projectDir.parentFile == candidateDir
      }
    return if (parent != null) setOf(parent.name) else emptySet()
  }

  private fun inferSharedBuildDependency(
    project: Project,
    projectByName: Map<String, Project>,
  ): Set<String> {
    val projectDir = File(project.path)
    val hasOwnBuild = hasBuildFile(projectDir)
    if (hasOwnBuild) return emptySet()

    val parent =
      projectByName.values.firstOrNull { candidate ->
        if (candidate.name == project.name) return@firstOrNull false
        val candidateDir = File(candidate.path)
        projectDir.parentFile?.canonicalFile == candidateDir.canonicalFile && hasBuildFile(candidateDir)
      }
    return if (parent != null) setOf(parent.name) else emptySet()
  }

  /**
   * Parses `build.gradle.kts` and `build.gradle` files in the project directory for
   * Gradle `project(":name")` or `project(":group:name")` dependency declarations.
   * The last path segment of the project notation is matched against known project names.
   *
   * Example: `implementation(project(":architect-api"))` → dependency on "architect-api"
   */
  private fun inferBuildFileDependencies(
    project: Project,
    projectByName: Map<String, Project>,
  ): Set<String> {
    val projectDir = File(project.path)
    val buildFile = listOf(
      File(projectDir, "build.gradle.kts"),
      File(projectDir, "build.gradle"),
    ).firstOrNull { it.exists() } ?: return emptySet()

    val content = buildFile.readText()
    val projectRefPattern = Regex("""project\(\s*["']([^"']+)["']\s*\)""")
    val referenced = mutableSetOf<String>()
    for (match in projectRefPattern.findAll(content)) {
      val notation = match.groupValues[1] // e.g. ":architect-api" or ":group:module"
      val name = notation.trimStart(':').substringAfterLast(':')
      if (name.isNotBlank() && name in projectByName && name != project.name) {
        referenced += name
      }
    }
    return referenced
  }

  private fun hasBuildFile(dir: File): Boolean =
    File(dir, "build.gradle.kts").exists() || File(dir, "build.gradle").exists()
}
