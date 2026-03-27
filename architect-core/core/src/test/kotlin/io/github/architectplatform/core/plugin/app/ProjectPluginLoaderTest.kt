package io.github.architectplatform.core.plugin.app

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.core.events.EventBus
import io.github.architectplatform.core.plugin.infra.GitHubReleaseResolver
import io.github.architectplatform.core.plugin.app.RemoteContentFetcher
import io.github.architectplatform.core.domain.events.ArchitectEvent
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
import kotlin.test.assertFailsWith
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
      signatureVerifier = RecordingSignatureVerifier(),
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

  @Test
  fun `should download and verify detached signature for signed github plugin`() {
    val downloader = TrackingDownloader(tempDir)
    val verifier = RecordingSignatureVerifier()
    val loader = ProjectPluginLoader(
      spiLoader = TrackingSpiPluginLoader(),
      downloader = downloader,
      signatureVerifier = verifier,
      internalPlugins = emptyList(),
      releaseResolver = GitHubReleaseResolver(NoOpRemoteContentFetcher()),
      eventBus = { },
    )
    val context = ProjectContext(
      dir = tempDir,
      config = mapOf(
        "plugins" to listOf(
          mapOf(
            "name" to "signed-plugin",
            "type" to "github",
            "repo" to "owner/signed-plugin",
            "version" to "1.0.0",
            "asset" to "signed-plugin.jar",
            "verify-signature" to true,
            "trusted-keys" to listOf("0xABCD1234"),
          ),
        ),
      ),
    )

    val plugins = loader.load(context)

    assertEquals(listOf("signed-plugin"), plugins.map { it.id })
    assertTrue(downloader.downloadedUrls.any { it.endsWith("signed-plugin.jar.asc") })
    assertEquals("signed-plugin", verifier.verifiedPluginNames.single())
  }

  @Test
  fun `should reject local plugin paths outside project root`() {
    val loader = ProjectPluginLoader(
      spiLoader = TrackingSpiPluginLoader(),
      downloader = TrackingDownloader(tempDir),
      signatureVerifier = RecordingSignatureVerifier(),
      internalPlugins = emptyList(),
      releaseResolver = GitHubReleaseResolver(NoOpRemoteContentFetcher()),
      eventBus = { },
    )
    val context = ProjectContext(
      dir = tempDir,
      config = mapOf(
        "plugins" to listOf(
          mapOf(
            "name" to "escape-plugin",
            "type" to "local",
            "path" to "../escape-plugin.jar",
          ),
        ),
      ),
    )

    val error = assertFailsWith<IllegalArgumentException> {
      loader.load(context)
    }

    assertTrue(error.message!!.contains("project root"))
  }

  @Test
  fun `should keep newest plugin version when duplicate dependency is declared`() {
    val downloader = TrackingDownloader(tempDir)
    val loader = ProjectPluginLoader(
      spiLoader = TrackingSpiPluginLoader(),
      downloader = downloader,
      signatureVerifier = RecordingSignatureVerifier(),
      internalPlugins = emptyList(),
      releaseResolver = GitHubReleaseResolver(NoOpRemoteContentFetcher()),
      eventBus = { },
    )
    val context = ProjectContext(
      dir = tempDir,
      config = mapOf(
        "plugins" to listOf(
          mapOf(
            "name" to "shared-plugin",
            "type" to "github",
            "repo" to "owner/shared-plugin",
            "version" to "1.0.0",
            "asset" to "shared-plugin.jar",
          ),
          mapOf(
            "name" to "shared-plugin",
            "type" to "github",
            "repo" to "owner/shared-plugin",
            "version" to "2.0.0",
            "asset" to "shared-plugin.jar",
          ),
        ),
      ),
    )

    val plugins = loader.load(context)

    assertEquals(listOf("shared-plugin"), plugins.map { it.id })
    assertTrue(downloader.downloadedUrls.any { it.contains("/shared-plugin-2.0.0/shared-plugin.jar") })
    assertTrue(downloader.downloadedUrls.none { it.contains("/shared-plugin-1.0.0/shared-plugin.jar") })
  }

  private class TrackingDownloader(
    private val tempDir: Path,
  ) : PluginDownloader {
    private val active = AtomicInteger(0)
    val maxConcurrent = AtomicInteger(0)
    val downloadedUrls = mutableListOf<String>()
    private val started = CountDownLatch(2)

    override fun download(url: String): File {
      downloadedUrls += url
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

  private class RecordingSignatureVerifier : GpgPluginSignatureVerifier(GpgCommandRunner()) {
    val verifiedPluginNames = mutableListOf<String>()

    override fun verify(plugin: PluginConfig, pluginFile: File, signatureFile: File) {
      verifiedPluginNames += plugin.name
      assertTrue(signatureFile.exists())
      assertTrue(plugin.verifySignature)
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
