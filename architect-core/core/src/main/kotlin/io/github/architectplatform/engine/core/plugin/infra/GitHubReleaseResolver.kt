package io.github.architectplatform.engine.core.plugin.infra

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.engine.core.common.Result
import io.github.architectplatform.engine.core.config.EngineConfiguration
import io.github.architectplatform.engine.core.plugin.app.RemoteContentFetcher
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Shared utility for resolving GitHub release tags.
 *
 * Extracted from [GitHubPluginSource] and [ProjectPluginLoader] to eliminate duplication.
 * Both callers now delegate `resolveLatestTag` and `compareVersions` here.
 */
@Singleton
class GitHubReleaseResolver(
    private val remoteContentFetcher: RemoteContentFetcher,
    private val userAgent: String = EngineConfiguration.PluginLoader.DEFAULT_USER_AGENT,
) {

    private val logger = LoggerFactory.getLogger(GitHubReleaseResolver::class.java)
    private val objectMapper = ObjectMapper().registerKotlinModule()

    /**
     * Resolves the latest GitHub release tag that starts with [prefix] in [repo].
     *
     * @param repo GitHub repository in `owner/repo` format.
     * @param prefix Tag prefix to filter on (e.g. `"my-plugin-"`).
     * @return [Result.success] with the tag name, or [Result.failure] with an error message.
     */
    fun resolveLatestTag(repo: String, prefix: String): Result<String> {
        return Result.catching {
            val apiUrl = "https://api.github.com/repos/$repo/releases"
            val token = System.getenv("GITHUB_TOKEN") ?: System.getProperty("GITHUB_TOKEN")

            val headers = mutableMapOf("User-Agent" to userAgent)
            if (!token.isNullOrBlank()) {
                headers["Authorization"] = "Bearer $token"
            }
            val body = remoteContentFetcher.fetchText(apiUrl, headers)

            @Suppress("UNCHECKED_CAST")
            val releases: List<Map<String, Any>> =
                objectMapper.readValue(body, List::class.java) as List<Map<String, Any>>

            logger.debug("Fetched {} releases from {}", releases.size, repo)

            val matchingTags = releases
                .mapNotNull { it["name"] as? String }
                .filter { it.startsWith(prefix) }

            if (matchingTags.isEmpty()) {
                throw IllegalArgumentException("No releases starting with '$prefix' found in $repo")
            }

            val latestTag = matchingTags.maxWithOrNull { a, b ->
                runCatching { compareVersions(a, b) }.getOrDefault(0)
            } ?: matchingTags.last()

            logger.info("Resolved latest tag: {}", latestTag)
            latestTag
        }
    }

    /**
     * Compares two semver-like version strings, ignoring a leading `v` prefix.
     *
     * @return positive if [a] > [b], negative if [a] < [b], zero if equal.
     */
    fun compareVersions(a: String, b: String): Int {
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
