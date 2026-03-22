package io.github.architectplatform.engine.core.watch

/**
 * Parsed watch configuration from `architect.yml`.
 *
 * Can be global or per-task. Example:
 * ```yaml
 * watch:
 *   paths: ["src/main/kotlin"]
 *   debounce-ms: 500
 * ```
 *
 * @property paths Glob patterns relative to project root
 * @property debounceMs Debounce interval in milliseconds
 */
data class WatchConfig(
    val paths: List<String> = emptyList(),
    val debounceMs: Long = 500,
) {
  companion object {
    /** Default file patterns per plugin context key. */
    val DEFAULT_PATTERNS = mapOf(
        "gradle" to listOf("*.kt", "*.java", "*.kts"),
        "javascript" to listOf("*.ts", "*.js", "*.jsx", "*.tsx", "*.json"),
        "docs" to listOf("*.md", "*.yml", "*.yaml"),
        "git" to emptyList(),
        "github" to emptyList(),
        "scripts" to listOf("*.sh", "*.bash"),
    )

    /**
     * Parse a watch config from a raw YAML map.
     */
    fun fromMap(map: Map<*, *>?): WatchConfig {
      if (map == null) return WatchConfig()
      val paths = (map["paths"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
      val debounce = (map["debounce-ms"] as? Number)?.toLong() ?: 500
      return WatchConfig(paths = paths, debounceMs = debounce)
    }

    /**
     * Resolves watch config for a specific task:
     * 1. Task-level `watch` config (from inline task definition)
     * 2. Global `watch` config
     * 3. Defaults derived from loaded plugin context keys
     */
    fun resolve(
        taskId: String,
        config: Map<String, Any>,
        pluginContextKeys: Set<String> = emptySet(),
    ): WatchConfig {
      // Check task-level watch config
      val tasks = config["tasks"] as? Map<*, *>
      val taskConfig = tasks?.get(taskId) as? Map<*, *>
      val taskWatch = taskConfig?.get("watch") as? Map<*, *>
      if (taskWatch != null) return fromMap(taskWatch)

      // Check global watch config
      val globalWatch = config["watch"] as? Map<*, *>
      if (globalWatch != null) return fromMap(globalWatch)

      // Default: derive from loaded plugins
      val defaultPatterns = pluginContextKeys.flatMap { DEFAULT_PATTERNS[it] ?: emptyList() }.distinct()
      return WatchConfig(paths = defaultPatterns)
    }
  }
}
