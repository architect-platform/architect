package io.github.architectplatform.engine.core.project.domain

/**
 * Immutable dependency graph where each key is a project and values are direct dependencies.
 */
data class ProjectDependencyGraph(
  val projects: Set<String>,
  val dependencies: Map<String, Set<String>>,
) {
  fun dependenciesOf(project: String): Set<String> = dependencies[project].orEmpty()

  fun dependentsOf(project: String): Set<String> =
    dependencies
      .filterValues { project in it }
      .keys
}
