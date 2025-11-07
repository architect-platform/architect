package io.github.architectplatform.cli.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import jakarta.inject.Singleton
import java.io.File

/**
 * Service for reading architect.yml configuration files.
 */
@Singleton
class ConfigService {

  private val yamlMapper = ObjectMapper(YAMLFactory())

  /**
   * Reads the architect.yml file from the given project path.
   */
  fun readConfig(projectPath: String): Map<String, Any>? {
    val yamlFile = File(projectPath, "architect.yml").takeIf { it.exists() }
      ?: File(projectPath, "architect.yaml").takeIf { it.exists() }

    if (yamlFile == null || !yamlFile.exists()) {
      return null
    }

    return try {
      @Suppress("UNCHECKED_CAST")
      yamlMapper.readValue(yamlFile, Map::class.java) as? Map<String, Any>
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Retrieves a value from the configuration using a dot-notation key path.
   */
  fun <T> getConfigValue(config: Map<String, Any>?, key: String): T? {
    if (config == null) return null

    val keys = key.split('.')
    var current: Any? = config

    for (k in keys) {
      when (current) {
        is Map<*, *> -> {
          @Suppress("UNCHECKED_CAST")
          current = (current as Map<String, Any>)[k]
        }
        else -> return null
      }
    }

    @Suppress("UNCHECKED_CAST")
    return current as? T
  }

  /**
   * Gets the pinned CLI version from the config.
   */
  fun getPinnedCliVersion(config: Map<String, Any>?): String? {
    return getConfigValue(config, "architect.cliVersion")
  }

  /**
   * Gets the pinned Engine version from the config.
   */
  fun getPinnedEngineVersion(config: Map<String, Any>?): String? {
    return getConfigValue(config, "architect.engineVersion")
  }
}
