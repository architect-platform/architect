package io.github.architectplatform.engine.core.plugin.infra

import io.github.architectplatform.engine.core.plugin.app.PluginDownloader
import io.github.architectplatform.engine.core.plugin.app.RemoteContentFetcher
import io.github.architectplatform.engine.core.events.EventBus
import io.github.architectplatform.engine.core.plugin.domain.events.PluginEvents.pluginDownloadCompleted
import io.github.architectplatform.engine.core.plugin.domain.events.PluginEvents.pluginDownloadSkipped
import io.github.architectplatform.engine.core.plugin.domain.events.PluginEvents.pluginDownloadStarted
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import jakarta.inject.Singleton
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@Singleton
class CachedPluginDownloader(
  private val remoteContentFetcher: RemoteContentFetcher,
  private val eventBus: EventBus<ArchitectEvent<*>>,
  private val cacheEnabled: Boolean = true,
) : PluginDownloader {
  private val cache =
      Paths.get(System.getProperty("user.home"), ".architect-engine", "plugins").also {
        Files.createDirectories(it)
      }

  override fun download(url: String): File {
    val jarName = "${url.hashCode()}.jar"
    val target = cache.resolve(jarName).toFile()
    if (cacheEnabled && target.exists()) {
      eventBus(pluginDownloadSkipped(pluginId = jarName))
      return target
    }
    eventBus(pluginDownloadStarted(pluginId = jarName))
    val body = remoteContentFetcher.fetchBytes(url)
    Files.write(target.toPath(), body)

    eventBus(pluginDownloadCompleted(pluginId = jarName))
    return target
  }
}
