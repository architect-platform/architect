package io.github.architectplatform.core.plugin.infra

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.core.common.Result
import io.github.architectplatform.core.plugin.app.PluginDownloader
import io.github.architectplatform.core.plugin.app.RemoteContentFetcher
import io.github.architectplatform.core.plugin.domain.PluginSource
import io.github.architectplatform.core.plugin.domain.PluginSourceConfig
import io.github.architectplatform.core.plugin.domain.SemverConstraint
import jakarta.inject.Singleton
import java.io.File
import java.security.MessageDigest

/**
 * Resolves plugins from an HTTP-hosted registry.json.
 *
 * Registry format:
 * ```json
 * {
 *   "plugins": [
 *     { "id": "my-plugin", "version": "1.0.0", "asset": "https://example.com/my-plugin-1.0.0.jar" }
 *   ]
 * }
 * ```
 */
@Singleton
class RegistryPluginSource(
  private val remoteContentFetcher: RemoteContentFetcher,
  private val downloader: PluginDownloader,
) : PluginSource {

  private val objectMapper = ObjectMapper().registerKotlinModule()

  override fun getType(): String = "registry"

  override fun resolve(config: PluginSourceConfig): Result<File> {
    val registryUrl = config.registry
      ?: return Result.failure("Plugin '${config.name}': 'registry' URL is required for type 'registry'", errorCode = "MISSING_REGISTRY_URL")

    val registryJson = try {
      remoteContentFetcher.fetchText(registryUrl)
    } catch (e: Exception) {
      return Result.failure("Failed to fetch registry from $registryUrl: ${e.message}", errorCode = "REGISTRY_FETCH_FAILED")
    }

    val registry = try {
      objectMapper.readValue(registryJson, PluginRegistry::class.java)
    } catch (e: Exception) {
      return Result.failure("Failed to parse registry.json from $registryUrl: ${e.message}", errorCode = "REGISTRY_PARSE_FAILED")
    }

    val candidates = registry.plugins.filter { it.id == config.name }
    if (candidates.isEmpty()) {
      return Result.failure("Plugin '${config.name}' not found in registry at $registryUrl", errorCode = "PLUGIN_NOT_FOUND")
    }

    val entry = if (config.version == "latest") {
      candidates.maxByOrNull { it.version }
    } else {
      // Try semver constraint matching
      val versions = candidates.map { it.version }
      val bestVersion = SemverConstraint.bestMatch(versions, config.version)
      if (bestVersion != null) {
        candidates.find { it.version == bestVersion }
      } else {
        // Fall back to exact match
        candidates.find { it.version == config.version }
      }
    }

    if (entry == null) {
      return Result.failure(
        "No version of '${config.name}' matching '${config.version}' found in registry. Available: ${candidates.map { it.version }}",
        errorCode = "VERSION_NOT_FOUND"
      )
    }

    val file = downloader.download(entry.asset)

    // SHA256 verification
    val expectedHash = config.sha256 ?: entry.sha256
    if (expectedHash != null) {
      val actualHash = sha256(file)
      if (!actualHash.equals(expectedHash, ignoreCase = true)) {
        file.delete()
        return Result.failure(
          "SHA256 mismatch for '${config.name}': expected $expectedHash but got $actualHash. The plugin JAR may have been tampered with.",
          errorCode = "SHA256_MISMATCH"
        )
      }
    }

    return Result.success(file)
  }

  /**
   * Searches a registry for plugins matching a query.
   */
  fun search(registryUrl: String, query: String): List<PluginRegistryEntry> {
    val registryJson = remoteContentFetcher.fetchText(registryUrl)
    val registry = objectMapper.readValue(registryJson, PluginRegistry::class.java)
    val lowerQuery = query.lowercase()
    return registry.plugins.filter {
      it.id.lowercase().contains(lowerQuery) ||
        (it.description?.lowercase()?.contains(lowerQuery) == true)
    }
  }

  companion object {
    fun sha256(file: File): String {
      val digest = MessageDigest.getInstance("SHA-256")
      val bytes = file.readBytes()
      val hash = digest.digest(bytes)
      return hash.joinToString("") { "%02x".format(it) }
    }
  }
}

data class PluginRegistry(
  val plugins: List<PluginRegistryEntry> = emptyList(),
)

data class PluginRegistryEntry(
  val id: String,
  val version: String,
  val asset: String,
  val sha256: String? = null,
  val description: String? = null,
)
