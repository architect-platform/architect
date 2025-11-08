package io.github.architectplatform.engine.core.plugin.infra

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.engine.core.auth.AuthConfigManager
import io.github.architectplatform.engine.core.common.Result
import io.github.architectplatform.engine.core.config.EngineConfiguration
import io.github.architectplatform.engine.core.plugin.app.PluginDownloader
import io.github.architectplatform.engine.core.plugin.domain.PluginSource
import io.github.architectplatform.engine.core.plugin.domain.PluginSourceConfig
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.client.HttpClient
import jakarta.inject.Singleton
import java.io.File
import org.slf4j.LoggerFactory

/**
 * Plugin source that loads plugins from GitHub releases.
 */
@Singleton
class GitHubPluginSource(
    private val httpClient: HttpClient,
    private val downloader: PluginDownloader,
    private val authConfigManager: AuthConfigManager,
    
    @Property(name = EngineConfiguration.PluginLoader.USER_AGENT, defaultValue = EngineConfiguration.PluginLoader.DEFAULT_USER_AGENT)
    private val userAgent: String = EngineConfiguration.PluginLoader.DEFAULT_USER_AGENT
) : PluginSource {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = ObjectMapper().registerKotlinModule()
    
    override fun getType(): String = "github"
    
    override fun resolve(config: PluginSourceConfig): Result<File> {
        val repo = config.repo
        if (repo == null) {
            return Result.failure("GitHub repository is required but not provided")
        }
        
        val asset = config.asset
        if (asset == null) {
            return Result.failure("GitHub asset name is required but not provided")
        }
        
        logger.debug("Resolving GitHub plugin from repo: $repo")
        
        val tagResult = when {
            config.version == "latest" -> {
                val pattern = config.pattern ?: config.name
                resolveLatestTag(repo, pattern)
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
    
    private fun resolveLatestTag(repo: String, prefix: String): Result<String> {
        return Result.catching {
            val apiUrl = "https://api.github.com/repos/$repo/releases"
            
            // Try to get token from: 1) stored config, 2) environment variable, 3) system property
            val token = authConfigManager.getGitHubToken()
                ?: System.getenv("GITHUB_TOKEN")
                ?: System.getProperty("GITHUB_TOKEN")
            
            var req: MutableHttpRequest<*> = HttpRequest.GET<Any>(apiUrl)
                .header("User-Agent", userAgent)
            
            if (!token.isNullOrBlank()) {
                req = req.header("Authorization", "Bearer $token")
                logger.debug("Using authenticated GitHub API request")
            } else {
                logger.debug("Using unauthenticated GitHub API request (rate limits may apply)")
            }
            
            val response = httpClient.toBlocking().retrieve(req, String::class.java)
                ?: throw IllegalStateException("Failed to fetch releases from $apiUrl")
            
            @Suppress("UNCHECKED_CAST")
            val releases: List<Map<String, Any>> =
                objectMapper.readValue(response, List::class.java) as List<Map<String, Any>>
            
            logger.debug("Fetched ${releases.size} releases from $repo")
            
            val matchingTags = releases
                .mapNotNull { it["name"] as? String }
                .filter { it.startsWith(prefix) }
            
            if (matchingTags.isEmpty()) {
                throw IllegalArgumentException("No releases starting with '$prefix' found in $repo")
            }
            
            val latestTag = matchingTags.maxWithOrNull { a, b ->
                runCatching { compareVersions(a, b) }.getOrDefault(0)
            } ?: matchingTags.last()
            
            logger.info("Resolved latest tag: $latestTag")
            latestTag
        }
    }
    
    private fun compareVersions(a: String, b: String): Int {
        val partsA = a.removePrefix("v").split(".")
        val partsB = b.removePrefix("v").split(".")
        for (i in 0 until maxOf(partsA.size, partsB.size)) {
            val nA = partsA.getOrNull(i)?.toIntOrNull() ?: 0
            val nB = partsB.getOrNull(i)?.toIntOrNull() ?: 0
            if (nA != nB) return nA - nB
        }
        return 0
    }
}
