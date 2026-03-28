package io.github.architectplatform.core.project.app

import io.github.architectplatform.core.project.domain.Project
import io.github.architectplatform.core.project.domain.ProjectDependencyGraph
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Paths

/**
 * Resolves which projects are "affected" by changes since a given git base ref.
 *
 * Steps:
 * 1. Run `git diff --name-only` against the base ref to get changed file paths
 * 2. Map each changed file to its owning project based on source roots
 * 3. Walk the dependency graph to include all transitively dependent projects
 * 4. Apply always-include / never-include filters from configuration
 */
class AffectedProjectResolver(
  private val commandRunner: (command: String, workingDir: String?) -> Pair<Int, String> = ::defaultCommandRunner,
) {

  data class AffectedConfig(
    val alwaysInclude: Set<String> = emptySet(),
    val neverInclude: Set<String> = emptySet(),
    // Glob patterns for files that should not trigger any project to be marked affected.
    // Examples: ["**-slash-*.md", "docs/**", "*.txt"]
    val ignorePatterns: List<String> = emptyList(),
  )

  /**
   * Cache validator hook for Phase 17 integration. When a task output cache is available,
   * this function can filter out projects whose cached outputs are still valid (cache hit),
   * even if source files changed. Returns the subset of projects that are truly affected
   * (cache miss or no cache).
   *
   * Default: identity (no cache filtering — all projects are considered affected).
   */
  var cacheValidator: (Set<String>) -> Set<String> = { it }

  /**
   * Resolves the set of affected project names.
   *
   * @param root The root project (contains subprojects)
   * @param graph The pre-built project dependency graph
   * @param baseRef The git ref to compare against (e.g. "HEAD~1", "origin/main")
   * @param config Optional always-include / never-include configuration
   * @return Set of affected project names
   */
  fun resolve(
    root: Project,
    graph: ProjectDependencyGraph,
    baseRef: String = "HEAD~1",
    config: AffectedConfig = AffectedConfig(),
  ): Set<String> {
    val changedFiles = gitChangedFiles(root.path, baseRef)
    val filteredFiles = filterIgnoredFiles(changedFiles, config.ignorePatterns)
    if (filteredFiles.isEmpty() && config.alwaysInclude.isEmpty()) {
      return emptySet()
    }

    val allProjects = flattenProjects(root)
    val directlyAffected = mapFilesToProjects(filteredFiles, allProjects, root.path)
    val transitivelyAffected = expandTransitiveDependents(directlyAffected, graph)

    val result = transitivelyAffected.toMutableSet()
    result += config.alwaysInclude.filter { it in graph.projects }
    result -= config.neverInclude

    // Phase 17 integration: filter out projects whose cached outputs are still valid
    return cacheValidator(result)
  }

  /**
   * Returns the list of changed file paths (relative to repo root) from git diff.
   */
  internal fun gitChangedFiles(projectPath: String, baseRef: String): List<String> {
    val (exitCode, output) = commandRunner(
      "git diff --name-only $baseRef",
      projectPath,
    )
    if (exitCode != 0) {
      return emptyList()
    }
    return output.lines().filter { it.isNotBlank() }
  }

  /**
   * Filters out files that match any of the given glob ignore patterns.
   * Patterns use standard glob syntax, e.g. double-star-slash-*.md, docs-slash-double-star, *.txt.
   */
  internal fun filterIgnoredFiles(files: List<String>, ignorePatterns: List<String>): List<String> {
    if (ignorePatterns.isEmpty()) return files
    val fs = FileSystems.getDefault()
    val matchers = ignorePatterns.map { pattern ->
      fs.getPathMatcher("glob:$pattern")
    }
    return files.filter { file ->
      val path = Paths.get(file)
      matchers.none { it.matches(path) }
    }
  }

  /**
   * Maps changed file paths to the project that owns them, based on longest-prefix matching
   * of project paths.
   */
  internal fun mapFilesToProjects(
    changedFiles: List<String>,
    projects: List<Project>,
    rootPath: String,
  ): Set<String> {
    val rootDir = File(rootPath).canonicalFile

    // Build a mapping of relative project paths (from repo root) to project names.
    // Sort by path length descending so that the most specific (deepest) project matches first.
    val projectPaths = projects
      .map { project ->
        val projectDir = File(project.path).canonicalFile
        val relativePath = projectDir.relativeTo(rootDir).path.replace('\\', '/')
        relativePath to project.name
      }
      .sortedByDescending { it.first.length }

    val affected = mutableSetOf<String>()

    for (changedFile in changedFiles) {
      val normalizedFile = changedFile.replace('\\', '/')
      // Find the deepest project directory that contains this file
      val owner = projectPaths.firstOrNull { (relPath, _) ->
        if (relPath.isEmpty()) true // root project matches everything
        else normalizedFile.startsWith("$relPath/")
      }
      if (owner != null) {
        affected += owner.second
      }
    }

    return affected
  }

  /**
   * Expands a set of directly affected projects to include all projects that
   * transitively depend on them.
   */
  internal fun expandTransitiveDependents(
    directlyAffected: Set<String>,
    graph: ProjectDependencyGraph,
  ): Set<String> {
    val result = mutableSetOf<String>()
    val queue = ArrayDeque(directlyAffected)

    while (queue.isNotEmpty()) {
      val current = queue.removeFirst()
      if (current in result) continue
      result += current
      // Find all projects that depend on `current` and add them to the queue
      val dependents = graph.dependentsOf(current)
      queue.addAll(dependents.filter { it !in result })
    }

    return result
  }

  private fun flattenProjects(root: Project): List<Project> {
    val result = mutableListOf<Project>()
    fun visit(project: Project) {
      result += project
      project.subProjects.forEach(::visit)
    }
    visit(root)
    return result
  }

  companion object {
    fun parseConfig(config: Map<String, Any>?): AffectedConfig {
      if (config == null) return AffectedConfig()
      val affected = config["affected"] as? Map<*, *> ?: return AffectedConfig()
      val alwaysInclude = (affected["always-include"] as? List<*>)
        ?.filterIsInstance<String>()?.toSet() ?: emptySet()
      val neverInclude = (affected["never-include"] as? List<*>)
        ?.filterIsInstance<String>()?.toSet() ?: emptySet()
      val ignorePatterns = (affected["ignore"] as? List<*>)
        ?.filterIsInstance<String>() ?: emptyList()
      return AffectedConfig(
        alwaysInclude = alwaysInclude,
        neverInclude = neverInclude,
        ignorePatterns = ignorePatterns,
      )
    }
  }
}

private fun defaultCommandRunner(command: String, workingDir: String?): Pair<Int, String> {
  val processBuilder = ProcessBuilder("sh", "-c", command)
  workingDir?.let { processBuilder.directory(File(it)) }
  processBuilder.redirectErrorStream(true)
  val process = processBuilder.start()
  val output = process.inputStream.bufferedReader().readText().trim()
  val exitCode = process.waitFor()
  return exitCode to output
}
