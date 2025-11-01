package io.github.architectplatform.engine.core.plugin.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.project.getKey
import io.github.architectplatform.engine.core.config.EngineConfiguration
import io.github.architectplatform.engine.core.plugin.domain.events.PluginEvents.pluginLoaded
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.micronaut.context.annotation.Property
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import jakarta.inject.Singleton
import java.net.URLClassLoader
import kotlin.io.path.exists
import org.slf4j.LoggerFactory

@Singleton
@ExecuteOn(TaskExecutors.BLOCKING)
class ProjectPluginLoader(
    private val spiLoader: SpiPluginLoader,
    private val downloader: PluginDownloader,
    private val internalPlugins: List<CommonPlugin>,
    private val httpClient: HttpClient,
    private val eventPublisher: ApplicationEventPublisher<ArchitectEvent<*>>,
    
    @Property(name = EngineConfiguration.PluginLoader.USER_AGENT, defaultValue = EngineConfiguration.PluginLoader.DEFAULT_USER_AGENT)
    private val userAgent: String = EngineConfiguration.PluginLoader.DEFAULT_USER_AGENT
) : PluginLoader {

  private val logger = LoggerFactory.getLogger(this::class.java)

  private val objectMapper = ObjectMapper().registerKotlinModule()

    override fun load(context: ProjectContext): List<ArchitectPlugin<*>> {
        val enabled = mutableListOf<ArchitectPlugin<*>>()
        // 1) Always include internal plugins
        enabled += internalPlugins.map { it.getPlugin() }

        val rawContext = context.config.getKey<Any>("plugins") ?: emptyList<PluginConfig>()
        val plugins: List<PluginConfig> =
            when (rawContext) {
                is List<*> -> {
                    // Config contains a list, so we deserialize as List<ctxClass>
                    rawContext.map { item -> objectMapper.convertValue(item, PluginConfig::class.java) }
                }
                else -> {
                    throw IllegalArgumentException(
                        "Invalid plugins context format: expected list, got ${rawContext::class.qualifiedName}")
                }
            }
        // 2) Download & load each project-declared plugin JAR
        plugins.forEach { plugin ->
            val jar =
                when (plugin.type) {
                    "github" -> {
                        val tag =
                            if (plugin.version == "latest") {
                                resolveLatestTag(plugin.repo, plugin.pattern)
                            } else {
                                "${plugin.name}-${plugin.version}"
                            }
                        val url =
                            "https://github.com/${plugin.repo}/releases/download/$tag/${plugin.asset}"
                        downloader.download(url)
                    }
                    "local" -> {
                        // Local plugin, assume the asset is a local path
                        val localPath = context.dir.resolve(plugin.path)
                        if (!localPath.exists()) {
                            throw IllegalArgumentException(
                                "Local plugin asset not found: ${localPath.toAbsolutePath()}")
                        }
                        localPath.toFile()
                    }
                    else -> throw IllegalArgumentException("Unsupported plugin type: ${plugin.type}")
                }
            val loader = URLClassLoader(arrayOf(jar.toURI().toURL()), this::class.java.classLoader)
            val loaded = spiLoader.loadFrom(loader)
            eventPublisher.publishEvent(pluginLoaded(plugin.name))
            enabled += loaded
        }

        return enabled
    }

    private fun resolveLatestTag(repo: String, prefix: String): String {
        val apiUrl = "https://api.github.com/repos/$repo/releases"

        val token = System.getenv("GITHUB_TOKEN") ?: System.getProperty("GITHUB_TOKEN")

        var req: MutableHttpRequest<*> = HttpRequest.GET<Any>(apiUrl)
            .header("User-Agent", userAgent)

        if (!token.isNullOrBlank()) {
            req = req.header("Authorization", "Bearer $token")
        }

        val response = httpClient.toBlocking().retrieve(req, String::class.java)
            ?: error("Failed to fetch tags from $apiUrl")

        @Suppress("UNCHECKED_CAST")
        val tags: List<Map<String, Any>> =
            objectMapper.readValue(response, List::class.java) as List<Map<String, Any>>

        println("Fetched ${tags.size} tags from $repo: $tags")

        val matchingTags = tags.mapNotNull { it["name"] as? String }.filter { it.startsWith(prefix) }

        if (matchingTags.isEmpty()) {
            error("No tags starting with '$prefix' found in $repo")
        }

        val latestTag = matchingTags.maxWithOrNull { a, b ->
            runCatching { compareVersions(a, b) }.getOrDefault(0)
        } ?: matchingTags.last()

        return latestTag
    }


    // Simple semver comparator
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
