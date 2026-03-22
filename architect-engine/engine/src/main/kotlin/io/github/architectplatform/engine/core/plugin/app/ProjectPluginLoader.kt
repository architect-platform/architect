package io.github.architectplatform.engine.core.plugin.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.project.getKey
import io.github.architectplatform.engine.core.plugin.domain.events.PluginEvents.pluginLoaded
import io.github.architectplatform.engine.core.plugin.infra.GitHubReleaseResolver
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.micronaut.context.annotation.Property
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import jakarta.inject.Singleton
import kotlin.io.path.exists
import org.slf4j.LoggerFactory

@Singleton
@ExecuteOn(TaskExecutors.BLOCKING)
class ProjectPluginLoader(
    private val spiLoader: SpiPluginLoader,
    private val downloader: PluginDownloader,
    private val internalPlugins: List<CommonPlugin>,
    private val releaseResolver: GitHubReleaseResolver,
    private val eventPublisher: ApplicationEventPublisher<ArchitectEvent<*>>,
    @Property(name = "architect.plugins.classloader.debug", defaultValue = "false")
    private val classloaderDebug: Boolean = false,
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
                                releaseResolver.resolveLatestTag(plugin.repo, plugin.pattern).getOrThrow()
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
            val loader = IsolatedPluginClassLoader(
              arrayOf(jar.toURI().toURL()),
              this::class.java.classLoader,
              debug = classloaderDebug,
            )
            val loaded = spiLoader.loadFrom(loader)
            eventPublisher.publishEvent(pluginLoaded(plugin.name))
            enabled += loaded
        }

        return enabled
    }

}
