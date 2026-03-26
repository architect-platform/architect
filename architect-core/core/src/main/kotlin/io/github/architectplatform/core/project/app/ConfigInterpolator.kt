package io.github.architectplatform.core.project.app

import io.github.architectplatform.api.core.project.Config

/**
 * Resolves `${...}` placeholders inside configuration values.
 *
 * Supported prefixes:
 * - `${env.VAR}` — environment variable
 * - `${env.VAR:default}` — environment variable with fallback
 * - `${project.name}` / `${project.description}` — project metadata
 *
 * Placeholders in non-string values (numbers, booleans, maps, lists) are
 * left untouched. Only string leaf values are interpolated.
 */
object ConfigInterpolator {

  private val PLACEHOLDER = Regex("""\$\{([^}]+)}""")

  /**
   * Walks the entire config tree and resolves placeholders in string values.
   *
   * @param config The raw parsed configuration
   * @return A new config map with all resolvable placeholders replaced
   */
  fun interpolate(config: Config): Config {
    val projectVars = extractProjectVars(config)
    return interpolateMap(config, projectVars)
  }

  @Suppress("UNCHECKED_CAST")
  private fun interpolateMap(map: Map<String, Any>, projectVars: Map<String, String>): Map<String, Any> {
    return map.mapValues { (_, value) -> interpolateValue(value, projectVars) }
  }

  @Suppress("UNCHECKED_CAST")
  private fun interpolateValue(value: Any, projectVars: Map<String, String>): Any {
    return when (value) {
      is String -> interpolateString(value, projectVars)
      is Map<*, *> -> interpolateMap(value as Map<String, Any>, projectVars)
      is List<*> -> value.map { item -> if (item != null) interpolateValue(item, projectVars) else null }
      else -> value
    }
  }

  private fun interpolateString(value: String, projectVars: Map<String, String>): String {
    return PLACEHOLDER.replace(value) { match ->
      val expression = match.groupValues[1]
      resolve(expression, projectVars) ?: match.value
    }
  }

  private fun resolve(expression: String, projectVars: Map<String, String>): String? {
    // Split on first colon for default value support: env.PORT:8080
    val colonIdx = expression.indexOf(':')
    val key: String
    val default: String?
    if (colonIdx > 0) {
      key = expression.substring(0, colonIdx)
      default = expression.substring(colonIdx + 1)
    } else {
      key = expression
      default = null
    }

    return when {
      key.startsWith("env.") -> {
        val envName = key.removePrefix("env.")
        System.getenv(envName) ?: default
      }
      key.startsWith("project.") -> {
        projectVars[key] ?: default
      }
      else -> default
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun extractProjectVars(config: Config): Map<String, String> {
    val project = config["project"] as? Map<String, Any> ?: return emptyMap()
    return project.entries
      .filter { it.value is String }
      .associate { "project.${it.key}" to it.value as String }
  }
}
