package io.github.architectplatform.engine.core.plugin.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.project.getKey
import io.github.architectplatform.engine.core.events.EventBus
import io.github.architectplatform.engine.core.plugin.domain.events.PluginEvents.pluginLoaded
import io.github.architectplatform.engine.core.plugin.infra.GitHubReleaseResolver
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.io.path.exists
import org.slf4j.LoggerFactory

@Singleton
class ProjectPluginLoader(
    private val spiLoader: SpiPluginLoader,
    private val downloader: PluginDownloader,
    private val internalPlugins: List<CommonPlugin>,
    private val releaseResolver: GitHubReleaseResolver,
    private val eventBus: EventBus<ArchitectEvent<*>>,
    private val classloaderDebug: Boolean = false,
) : PluginLoader {

  private val logger = LoggerFactory.getLogger(this::class.java)

  private val objectMapper = ObjectMapper().registerKotlinModule()

    override fun load(context: ProjectContext): List<ArchitectPlugin<*>> {
        val rawContext = context.config.getKey<Any>("plugins") ?: emptyList<PluginConfig>()
        val plugins: List<PluginConfig> =
            when (rawContext) {
                is List<*> -> rawContext.map { item -> objectMapper.convertValue(item, PluginConfig::class.java) }
                else -> {
                    throw IllegalArgumentException(
                        "Invalid plugins context format: expected list, got ${rawContext::class.qualifiedName}")
                }
            }

        val loadedPlugins =
            runBlocking {
                plugins.mapIndexed { index, plugin ->
                    async(Dispatchers.IO) {
                        index to loadConfiguredPlugin(plugin, context)
                    }
                }.awaitAll()
            }.sortedBy { it.first }
                .flatMap { it.second }

        return buildList {
            addAll(internalPlugins.map { it.getPlugin() })
            addAll(loadedPlugins)
        }
    }

    private fun loadConfiguredPlugin(
        plugin: PluginConfig,
        context: ProjectContext,
    ): List<ArchitectPlugin<*>> {
        if (plugin.type == "process") {
            val cmd = plugin.command
                ?: throw IllegalArgumentException("Plugin '${plugin.name}' type 'process' requires 'command' field")
            val adapter = io.github.architectplatform.engine.core.plugin.protocol.ProcessPluginAdapter(
                pluginId = plugin.name,
                command = cmd,
                workingDir = context.dir.toString(),
            )
            eventBus(pluginLoaded(plugin.name))
            return listOf(adapter)
        }

        if (plugin.type == "npm") {
            val packageName = plugin.packageName
                ?: throw IllegalArgumentException("Plugin '${plugin.name}' type 'npm' requires 'package' field")
            val packageSpec =
                if (plugin.version.isBlank() || plugin.version == "latest") {
                    packageName
                } else {
                    "$packageName@${plugin.version}"
                }
            val adapter = io.github.architectplatform.engine.core.plugin.protocol.ProcessPluginAdapter(
                pluginId = plugin.name,
                command = "npx --yes ${shellQuote(packageSpec)}",
                workingDir = context.dir.toString(),
            )
            eventBus(pluginLoaded(plugin.name))
            return listOf(adapter)
        }

        val jar =
            when (plugin.type) {
                "github" -> {
                    val tag =
                        if (plugin.version == "latest") {
                            releaseResolver.resolveLatestTag(plugin.repo, plugin.pattern).getOrThrow()
                        } else {
                            "${plugin.name}-${plugin.version}"
                        }
                    val url =
                        "https://github.com/${plugin.repo}/releases/download/$tag/${plugin.asset}"
                    downloader.download(url)
                }
                "local" -> {
                    val localPath = context.dir.resolve(plugin.path)
                    if (!localPath.exists()) {
                        throw IllegalArgumentException(
                            "Local plugin asset not found: ${localPath.toAbsolutePath()}")
                    }
                    localPath.toFile()
                }
                else -> throw IllegalArgumentException("Unsupported plugin type: ${plugin.type}")
            }
        val loader = IsolatedPluginClassLoader(
            arrayOf(jar.toURI().toURL()),
            this::class.java.classLoader,
            debug = classloaderDebug,
        )
        val loaded = spiLoader.loadFrom(loader)
        eventBus(pluginLoaded(plugin.name))
        return loaded
    }

    private fun shellQuote(value: String): String =
        "'${value.replace("'", "'\"'\"'")}'"

}
