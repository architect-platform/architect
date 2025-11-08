package io.github.architectplatform.engine.core.plugin.infra

import io.github.architectplatform.engine.core.auth.AuthConfigManager
import io.github.architectplatform.engine.core.plugin.app.PluginDownloader
import io.github.architectplatform.engine.core.plugin.domain.events.PluginEvents.pluginDownloadCompleted
import io.github.architectplatform.engine.core.plugin.domain.events.PluginEvents.pluginDownloadSkipped
import io.github.architectplatform.engine.core.plugin.domain.events.PluginEvents.pluginDownloadStarted
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.micronaut.context.annotation.Property
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import jakarta.inject.Singleton
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import org.slf4j.LoggerFactory

@Singleton
@ExecuteOn(TaskExecutors.BLOCKING)
class CachedPluginDownloader(
    private val httpClient: HttpClient,
    private val eventPublisher: ApplicationEventPublisher<ArchitectEvent<*>>,
    private val authConfigManager: AuthConfigManager,
) : PluginDownloader {
  private val logger = LoggerFactory.getLogger(this::class.java)
  
  private val cache =
      Paths.get(System.getProperty("user.home"), ".architect-engine", "plugins").also {
        Files.createDirectories(it)
      }

  @Property(name = "architect.engine.plugin.cache.enabled", defaultValue = "true")
  var cacheEnabled: Boolean = true

  override fun download(url: String): File {
    val jarName = "${url.hashCode()}.jar"
    val target = cache.resolve(jarName).toFile()
    if (cacheEnabled && target.exists()) {
      eventPublisher.publishEvent(pluginDownloadSkipped(pluginId = jarName))
      return target
    }
    eventPublisher.publishEvent(pluginDownloadStarted(pluginId = jarName))
    
    var request: MutableHttpRequest<Any> = HttpRequest.GET(url)
    
    // Add authentication for GitHub URLs
    if (url.contains("github.com")) {
      val token = authConfigManager.getGitHubToken()
          ?: System.getenv("GITHUB_TOKEN")
          ?: System.getProperty("GITHUB_TOKEN")
      
      if (!token.isNullOrBlank()) {
        request = request.header("Authorization", "Bearer $token")
        logger.debug("Using authenticated download for GitHub URL")
      }
    }
    
    val response = httpClient.toBlocking().exchange(request, ByteArray::class.java)
    val body = response.body() ?: error("Failed to download plugin: empty response body")
    Files.write(target.toPath(), body)

    eventPublisher.publishEvent(pluginDownloadCompleted(pluginId = jarName))
    return target
  }
}
