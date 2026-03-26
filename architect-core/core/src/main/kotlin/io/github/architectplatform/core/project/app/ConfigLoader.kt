package io.github.architectplatform.core.project.app

import io.github.architectplatform.api.core.project.Config
import io.github.architectplatform.core.project.app.ports.ConfigParser
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.io.File

@Singleton
class ConfigLoader(private val configParser: ConfigParser) {

  private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)

  data class LoadResult(val config: Config, val rawYaml: String)

  fun load(path: String): Config? = loadWithRaw(path)?.config

  fun loadWithRaw(path: String, profileName: String? = null): LoadResult? {
    val yamlContext = getExternalConfiguration(path)
    if (yamlContext.isEmpty()) {
      return null
    }
    var config = ConfigInterpolator.interpolate(configParser.parse(yamlContext))

    // Apply parent config inheritance for monorepos
    config = applyInheritance(config, path)

    // Apply file-based profile overlay (e.g. architect.ci.yml)
    val resolvedProfile = profileName ?: ProfileMerger.detectProfile(null)
    if (resolvedProfile != "default") {
      val overlayYaml = loadProfileOverlay(path, resolvedProfile)
      if (overlayYaml != null) {
        val overlayConfig = ConfigInterpolator.interpolate(configParser.parse(overlayYaml))
        config = ProfileMerger.deepMerge(config, overlayConfig)
        logger.info("Applied profile overlay architect.$resolvedProfile.yml")
      }
    }

    return LoadResult(config, yamlContext)
  }

  /**
   * Loads an environment-specific overlay file: `architect.{profile}.yml` or `.yaml`.
   */
  private fun loadProfileOverlay(projectPath: String, profile: String): String? {
    val overlay = File(projectPath, "architect.$profile.yml").takeIf { it.exists() }
      ?: File(projectPath, "architect.$profile.yaml").takeIf { it.exists() }
      ?: return null

    return try {
      val content = overlay.readText()
      if (content.isBlank()) null else content
    } catch (e: Exception) {
      logger.error("Failed to read profile overlay ${overlay.name}, skipping.", e)
      null
    }
  }

  /**
   * Applies monorepo config inheritance.
   *
   * When the config contains `inherit: true`, walks up parent directories to find
   * a parent `architect.yml` and deep-merges it as the base. The child config
   * overrides the parent at leaf level.
   */
  @Suppress("UNCHECKED_CAST")
  private fun applyInheritance(config: Config, childPath: String): Config {
    val inherit = config["inherit"]
    if (inherit != true && inherit != "true") return config

    val parentYaml = findParentConfig(childPath) ?: run {
      logger.debug("inherit: true set but no parent architect.yml found")
      return config - "inherit"
    }

    val parentConfig = ConfigInterpolator.interpolate(configParser.parse(parentYaml))
    logger.info("Inheriting config from parent architect.yml")
    // Parent is the base; child overrides
    return ProfileMerger.deepMerge(parentConfig, config - "inherit")
  }

  /**
   * Walks up from [childPath] looking for a parent directory containing architect.yml.
   */
  private fun findParentConfig(childPath: String): String? {
    var dir = File(childPath).absoluteFile.parentFile
    // Walk up at most 10 levels to avoid infinite loops
    repeat(10) {
      dir = dir?.parentFile ?: return null
      val parentYml = File(dir, "architect.yml").takeIf { it.exists() }
        ?: File(dir, "architect.yaml").takeIf { it.exists() }
      if (parentYml != null) {
        return try {
          val content = parentYml.readText()
          if (content.isBlank()) null else content
        } catch (e: Exception) {
          logger.error("Failed to read parent ${parentYml.absolutePath}, skipping.", e)
          null
        }
      }
    }
    return null
  }

  private fun getExternalConfiguration(projectPath: String = "."): String {
    val configuration = StringBuilder()

    // 1) Load the main architect.yml/yaml if present
    val rootYaml =
        File(projectPath, "architect.yml").takeIf { it.exists() }
            ?: File(projectPath, "architect.yaml").takeIf { it.exists() }

    if (rootYaml == null || rootYaml.readText().isEmpty()) {
      return configuration.toString()
    }

    try {
      configuration.append(rootYaml.readText()).append("\n")
    } catch (e: Exception) {
      logger.error("Failed to read ${rootYaml.name}, skipping.", e)
    }

    // 2) Load all files in .architect folder
    val contextDir = File(projectPath, ".architect")
    if (!contextDir.exists() || !contextDir.isDirectory) {
      return configuration.toString()
    }

    val yamlFiles =
        contextDir
            .listFiles { file ->
              file.isFile &&
                  (file.extension.equals("yml", true) || file.extension.equals("yaml", true))
            }
            ?.sortedBy { it.name } // optional: deterministic order
        ?: emptyList()

    if (yamlFiles.isEmpty()) {
      return configuration.toString()
    }

    yamlFiles.forEach { file ->
      try {
        configuration.append(file.readText()).append("\n")
      } catch (e: Exception) {
        logger.error("Failed to read ${file.name}, skipping.", e)
      }
    }

    return configuration.toString()
  }
}
