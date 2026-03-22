package io.github.architectplatform.engine.core.plugin.infra

import io.github.architectplatform.engine.core.common.Result
import io.github.architectplatform.engine.core.plugin.app.PluginDownloader
import io.github.architectplatform.engine.core.plugin.domain.PluginSource
import io.github.architectplatform.engine.core.plugin.domain.PluginSourceConfig
import jakarta.inject.Singleton
import java.io.File
import org.slf4j.LoggerFactory

/**
 * Plugin source that loads plugins from GitHub releases.
 */
@Singleton
class GitHubPluginSource(
    private val downloader: PluginDownloader,
    private val releaseResolver: GitHubReleaseResolver,
) : PluginSource {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getType(): String = "github"

    override fun resolve(config: PluginSourceConfig): Result<File> {
        val repo = config.repo
            ?: return Result.failure("GitHub repository is required but not provided")
        val asset = config.asset
            ?: return Result.failure("GitHub asset name is required but not provided")

        logger.debug("Resolving GitHub plugin from repo: $repo")

        val tagResult = when {
            config.version == "latest" -> {
                val pattern = config.pattern ?: config.name
                releaseResolver.resolveLatestTag(repo, pattern)
            }
            else -> Result.success("${config.name}-${config.version}")
        }

        return tagResult.flatMap { tag ->
            Result.catching {
                val url = "https://github.com/$repo/releases/download/$tag/$asset"
                logger.info("Downloading plugin from: $url")
                downloader.download(url)
            }
        }
    }
}
