package io.github.architectplatform.api.core.project

/**
 * Type alias for configuration data.
 *
 * Configuration is represented as a nested map structure that can contain strings, numbers,
 * booleans, lists, and nested maps.
 */
typealias Config = Map<String, Any>

/**
 * Retrieves a value from the configuration using a dot-notation key path.
 *
 * This extension function allows accessing nested configuration values using a dot-separated
 * path. It supports both map navigation and list indexing.
 *
 * Example usage:
 * ```kotlin
 * val config: Config = mapOf(
 *   "database" to mapOf(
 *     "host" to "localhost",
 *     "port" to 5432
 *   ),
 *   "servers" to listOf("server1", "server2")
 * )
 *
 * val host = config.getKey<String>("database.host") // Returns "localhost"
 * val port = config.getKey<Int>("database.port")    // Returns 5432
 * val server = config.getKey<String>("servers.0")   // Returns "server1"
 * ```
 *
 * @param key Dot-separated path to the configuration value (e.g., "database.host")
 * @return The value at the specified path, or null if not found
 * @throws IllegalArgumentException if a path segment cannot be parsed as an index for a list
 * @throws IllegalStateException if the path traverses through an unexpected type
 * @throws IndexOutOfBoundsException if a list index is out of bounds
 */
fun <T> Config.getKey(key: String): T? {
  val keys = key.split('.')
  var current: Any? = this
  for (k in keys) {
    when (current) {
      is Map<*, *> -> {
        current = current[k]
      }
      is List<*> -> {
        val index = k.toIntOrNull() ?: throw IllegalArgumentException("Invalid index: $k")
        if (index < 0 || index >= current.size) {
          throw IndexOutOfBoundsException("Index $index is out of bounds for list of size ${current.size}")
        }
        current = current[index]
      }
      null -> {
        // Key path doesn't exist, return null
        return null
      }
      else -> {
        // Trying to traverse through a primitive value
        throw IllegalStateException("Cannot traverse through primitive value of type ${current.javaClass}")
      }
    }
  }
  @Suppress("UNCHECKED_CAST")
  return current as? T
}
