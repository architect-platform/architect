package io.github.architectplatform.engine.core.plugin.app

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.engine.core.events.EventBus
import io.github.architectplatform.engine.core.plugin.infra.GitHubReleaseResolver
import io.github.architectplatform.engine.core.plugin.app.RemoteContentFetcher
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class ProjectPluginLoaderTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `should load independent plugins in parallel while preserving config order`() {
    val downloader = TrackingDownloader(tempDir)
    val loader = ProjectPluginLoader(
      spiLoader = TrackingSpiPluginLoader(),
      downloader = downloader,
      internalPlugins = emptyList(),
      releaseResolver = GitHubReleaseResolver(NoOpRemoteContentFetcher()),
      eventBus = { },
    )
    val context = ProjectContext(
      dir = tempDir,
      config = mapOf(
        "plugins" to listOf(
          mapOf(
            "name" to "plugin-a",
            "type" to "github",
            "repo" to "owner/plugin-a",
            "version" to "1.0.0",
            "asset" to "plugin-a.jar",
          ),
          mapOf(
            "name" to "plugin-b",
            "type" to "github",
            "repo" to "owner/plugin-b",
            "version" to "1.0.0",
            "asset" to "plugin-b.jar",
          ),
        ),
      ),
    )

    val plugins = loader.load(context)

    assertEquals(listOf("plugin-a", "plugin-b"), plugins.map { it.id })
    assertTrue(downloader.maxConcurrent.get() >= 2)
  }

  private class TrackingDownloader(
    private val tempDir: Path,
  ) : PluginDownloader {
    private val active = AtomicInteger(0)
    val maxConcurrent = AtomicInteger(0)
    private val started = CountDownLatch(2)

    override fun download(url: String): File {
      val current = active.incrementAndGet()
      maxConcurrent.getAndUpdate { maxOf(it, current) }
      started.countDown()
      started.await(250, TimeUnit.MILLISECONDS)
      Thread.sleep(50)

      val file = tempDir.resolve(url.substringAfterLast('/'))
      file.parent?.createDirectories()
      file.writeText("stub")

      active.decrementAndGet()
      return file.toFile()
    }
  }

  private class TrackingSpiPluginLoader : SpiPluginLoader() {
    override fun loadFrom(classLoader: ClassLoader): List<ArchitectPlugin<*>> {
      val urlLoader = classLoader as URLClassLoader
      val jarName = urlLoader.urLs.single().path.substringAfterLast('/').substringBeforeLast('.')
      return listOf(TestPlugin(jarName))
    }
  }

  private class TestPlugin(
    override val id: String,
  ) : ArchitectPlugin<HashMap<String, Any>> {
    override val contextKey: String = id
    override val ctxClass: Class<HashMap<String, Any>> = HashMap::class.java as Class<HashMap<String, Any>>
    override var context: HashMap<String, Any> = hashMapOf()

    override fun register(registry: TaskRegistry) = Unit
  }
  private class NoOpRemoteContentFetcher : RemoteContentFetcher {
    override fun fetchText(url: String, headers: Map<String, String>): String = "[]"

    override fun fetchBytes(url: String, headers: Map<String, String>): ByteArray = byteArrayOf()
  }
}