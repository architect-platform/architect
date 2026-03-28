package io.github.architectplatform.core.project.domain

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

  /**
   * Detects cycles in the dependency graph using iterative DFS.
   * Returns a list of cycles, where each cycle is a list of project names forming the cycle
   * (the first element is repeated at the end to show the loop).
   */
  fun detectCycles(): List<List<String>> {
    val cycles = mutableListOf<List<String>>()
    val visited = mutableSetOf<String>()
    val recursionStack = mutableSetOf<String>()

    fun dfs(node: String, path: List<String>) {
      visited += node
      recursionStack += node
      for (dep in dependenciesOf(node)) {
        if (dep !in visited) {
          dfs(dep, path + dep)
        } else if (dep in recursionStack) {
          // Found a cycle: extract the cycle portion of the path
          val cycleStart = path.indexOf(dep)
          val cycle = if (cycleStart >= 0) path.subList(cycleStart, path.size) + dep
          else listOf(dep, node, dep)
          cycles += cycle
        }
      }
      recursionStack -= node
    }

    for (project in projects.sorted()) {
      if (project !in visited) {
        dfs(project, listOf(project))
      }
    }
    return cycles
  }

  /**
   * Returns the set of projects that are shared dependencies (depended on by more than one project).
   */
  fun sharedDependencies(): Set<String> {
    val dependentCount = mutableMapOf<String, Int>()
    for ((_, deps) in dependencies) {
      for (dep in deps) {
        dependentCount[dep] = (dependentCount[dep] ?: 0) + 1
      }
    }
    return dependentCount.filter { it.value > 1 }.keys.toSet()
  }

  /**
   * Returns an ordered list of execution tiers for parallel task execution.
   * Each tier contains projects whose dependencies have all been satisfied by prior tiers.
   * Projects within the same tier can be executed in parallel.
   *
   * Uses Kahn's algorithm (dependency-first topological sort).
   * If a cycle is detected, the remaining cyclic nodes are added as the final tier.
   */
  fun topologicalTiers(): List<Set<String>> {
    // in-degree for each project = number of its unresolved dependencies
    val inDegree = projects.associateWithTo(mutableMapOf()) { p ->
      dependencies[p]?.size ?: 0
    }

    val tiers = mutableListOf<Set<String>>()
    val remaining = projects.toMutableSet()

    while (remaining.isNotEmpty()) {
      // Projects with in-degree 0 have all dependencies satisfied → can run now
      val tier = remaining.filter { (inDegree[it] ?: 0) == 0 }.toSet()
      if (tier.isEmpty()) {
        // Cycle detected: add all remaining projects as a final unordered tier
        tiers += remaining.toSet()
        break
      }
      tiers += tier
      remaining -= tier
      // Reduce in-degree: for each remaining project, count how many of its deps are still pending
      for (project in remaining) {
        inDegree[project] = dependencies[project].orEmpty().count { it in remaining }
      }
    }

    return tiers
  }
}
