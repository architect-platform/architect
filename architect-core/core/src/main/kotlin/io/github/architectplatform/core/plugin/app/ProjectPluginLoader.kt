package io.github.architectplatform.core.plugin.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.project.getKey
import io.github.architectplatform.api.core.project.resolvePath
import io.github.architectplatform.core.events.EventBus
import io.github.architectplatform.core.plugin.domain.events.PluginEvents.pluginLoaded
import io.github.architectplatform.core.plugin.infra.GitHubReleaseResolver
import io.github.architectplatform.core.domain.events.ArchitectEvent
import jakarta.inject.Singleton
import java.io.File
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
    private val signatureVerifier: PluginSignatureVerifier,
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
        if (plugin.verifySignature && (plugin.type == "process" || plugin.type == "npm")) {
            throw IllegalArgumentException(
                "Plugin '${plugin.name}' type '${plugin.type}' does not support detached signature verification")
        }

        if (plugin.type == "process") {
            val cmd = plugin.command
                ?: throw IllegalArgumentException("Plugin '${plugin.name}' type 'process' requires 'command' field")
            val adapter = io.github.architectplatform.core.plugin.protocol.ProcessPluginAdapter(
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
            val adapter = io.github.architectplatform.core.plugin.protocol.ProcessPluginAdapter(
                pluginId = plugin.name,
                command = "npx --yes ${shellQuote(packageSpec)}",
                workingDir = context.dir.toString(),
            )
            eventBus(pluginLoaded(plugin.name))
            return listOf(adapter)
        }

        var signatureFile: File? = null
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
                    if (plugin.verifySignature) {
                        signatureFile = downloader.download("$url.asc")
                    }
                    downloader.download(url)
                }
                "local" -> {
                    val localPath = context.resolvePath(plugin.path, "Local plugin path")
                    if (!localPath.exists()) {
                        throw IllegalArgumentException(
                            "Local plugin asset not found: ${localPath.toAbsolutePath()}")
                    }
                    if (plugin.verifySignature) {
                        signatureFile = context.resolvePath("${plugin.path}.asc", "Local plugin signature path").toFile()
                    }
                    localPath.toFile()
                }
                else -> throw IllegalArgumentException("Unsupported plugin type: ${plugin.type}")
            }
        if (plugin.verifySignature) {
            signatureVerifier.verify(plugin, jar, signatureFile!!)
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
