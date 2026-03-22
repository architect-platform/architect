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

  /**
   * Returns all projects that transitively depend on the given project.
   */
  fun transitiveDependentsOf(project: String): Set<String> {
    val result = mutableSetOf<String>()
    val queue = ArrayDeque(listOf(project))
    while (queue.isNotEmpty()) {
      val current = queue.removeFirst()
      val directDependents = dependentsOf(current)
      for (dep in directDependents) {
        if (result.add(dep)) {
          queue.addLast(dep)
        }
      }
    }
    return result
  }
}
