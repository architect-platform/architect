package io.github.architectplatform.core.plugin.app

/**
 * Resolves plugin dependency version conflicts.
 *
 * Conflict scope is a dependency coordinate derived from plugin type + source:
 * - github: repo + asset
 * - npm: package name
 * - local: path
 * - process: command
 * - fallback: plugin name
 */
object PluginVersionConflictResolver {

  data class PluginVersionConflict(
    val dependencyKey: String,
    val kept: PluginConfig,
    val dropped: PluginConfig,
  )

  data class ResolutionResult(
    val selected: List<PluginConfig>,
    val conflicts: List<PluginVersionConflict>,
  )

  fun resolve(
    plugins: List<PluginConfig>,
    versionComparator: (String, String) -> Int,
  ): ResolutionResult {
    data class IndexedPlugin(val index: Int, val plugin: PluginConfig)

    val winners = mutableMapOf<String, IndexedPlugin>()
    val conflicts = mutableListOf<PluginVersionConflict>()

    plugins.forEachIndexed { index, candidate ->
      val key = dependencyKey(candidate)
      val existing = winners[key]
      if (existing == null) {
        winners[key] = IndexedPlugin(index, candidate)
        return@forEachIndexed
      }

      val comparison = compareVersion(candidate.version, existing.plugin.version, versionComparator)
      if (comparison > 0) {
        conflicts += PluginVersionConflict(key, kept = candidate, dropped = existing.plugin)
        winners[key] = IndexedPlugin(index, candidate)
      } else if (comparison < 0) {
        conflicts += PluginVersionConflict(key, kept = existing.plugin, dropped = candidate)
      }
      // Equal versions are not conflicts; keep first declaration.
    }

    val selected = winners.values.sortedBy { it.index }.map { it.plugin }
    return ResolutionResult(selected = selected, conflicts = conflicts)
  }

  fun dependencyKey(plugin: PluginConfig): String =
    when (plugin.type) {
      "github" -> "github:${plugin.repo}:${plugin.asset}"
      "npm" -> "npm:${plugin.packageName ?: plugin.name}"
      "local" -> "local:${plugin.path}"
      "process" -> "process:${plugin.command ?: plugin.name}"
      else -> "${plugin.type}:${plugin.name}"
    }

  private fun compareVersion(
    candidate: String,
    existing: String,
    versionComparator: (String, String) -> Int,
  ): Int {
    if (candidate == existing) return 0
    if (candidate.equals("latest", ignoreCase = true) && !existing.equals("latest", ignoreCase = true)) return 1
    if (existing.equals("latest", ignoreCase = true) && !candidate.equals("latest", ignoreCase = true)) return -1

    val semantic = versionComparator(candidate, existing)
    if (semantic != 0) return semantic
    return candidate.compareTo(existing)
  }
}
