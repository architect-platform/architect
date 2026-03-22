package io.github.architectplatform.engine.core.plugin.infra

import io.github.architectplatform.engine.core.common.Result
import io.github.architectplatform.engine.core.plugin.app.PluginDownloader
import io.github.architectplatform.engine.core.plugin.domain.PluginSource
import io.github.architectplatform.engine.core.plugin.domain.PluginSourceConfig
import jakarta.inject.Singleton
import java.io.File

/**
 * Resolves plugins from a direct HTTP URL.
 *
 * Usage in architect.yml:
 * ```yaml
 * plugins:
 *   - name: my-plugin
 *     type: http
 *     url: https://example.com/my-plugin-1.0.0.jar
 *     sha256: abc123...  # optional integrity check
 * ```
 */
@Singleton
class HttpPluginSource(
  private val downloader: PluginDownloader,
) : PluginSource {

  override fun getType(): String = "http"

  override fun resolve(config: PluginSourceConfig): Result<File> {
    val url = config.url
      ?: return Result.failure("Plugin '${config.name}': 'url' is required for type 'http'", errorCode = "MISSING_URL")

    val file = try {
      downloader.download(url)
    } catch (e: Exception) {
      return Result.failure("Failed to download plugin '${config.name}' from $url: ${e.message}", errorCode = "DOWNLOAD_FAILED")
    }

    // SHA256 verification
    if (config.sha256 != null) {
      val actualHash = RegistryPluginSource.sha256(file)
      if (!actualHash.equals(config.sha256, ignoreCase = true)) {
        file.delete()
        return Result.failure(
          "SHA256 mismatch for '${config.name}': expected ${config.sha256} but got $actualHash",
          errorCode = "SHA256_MISMATCH"
        )
      }
    }

    return Result.success(file)
  }
}
