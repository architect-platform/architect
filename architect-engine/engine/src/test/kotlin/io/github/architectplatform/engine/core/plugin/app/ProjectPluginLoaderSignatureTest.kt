package io.github.architectplatform.engine.core.plugin.app

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.core.plugin.app.PluginConfig
import io.github.architectplatform.core.plugin.app.PluginDownloader
import io.github.architectplatform.core.plugin.app.PluginSignatureVerifier
import io.github.architectplatform.core.plugin.app.ProjectPluginLoader
import io.github.architectplatform.core.plugin.app.RemoteContentFetcher
import io.github.architectplatform.core.plugin.app.SpiPluginLoader
import io.github.architectplatform.core.plugin.infra.GitHubReleaseResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class ProjectPluginLoaderSignatureTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `should download and verify detached signature for signed github plugin`() {
    val downloader = TrackingDownloader(tempDir)
    val verifier = RecordingSignatureVerifier()
    val spiLoader = mock<SpiPluginLoader>()
    whenever(spiLoader.loadFrom(any())).thenReturn(listOf(TestPlugin("signed-plugin")))
    val loader =
      ProjectPluginLoader(
        spiLoader = spiLoader,
        downloader = downloader,
        signatureVerifier = verifier,
        internalPlugins = emptyList(),
        releaseResolver = GitHubReleaseResolver(mock<RemoteContentFetcher>()),
        eventBus = {},
        classloaderDebug = false,
      )
    val context =
      ProjectContext(
        dir = tempDir,
        config =
          mapOf(
            "plugins" to
              listOf(
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
    assertEquals(listOf("signed-plugin"), verifier.verifiedPluginNames)
  }

  @Test
  fun `should reject local plugin paths outside project root`() {
    val spiLoader = mock<SpiPluginLoader>()
    val loader =
      ProjectPluginLoader(
        spiLoader = spiLoader,
        downloader = TrackingDownloader(tempDir),
        signatureVerifier = RecordingSignatureVerifier(),
        internalPlugins = emptyList(),
        releaseResolver = GitHubReleaseResolver(mock<RemoteContentFetcher>()),
        eventBus = {},
        classloaderDebug = false,
      )
    val context =
      ProjectContext(
        dir = tempDir,
        config =
          mapOf(
            "plugins" to
              listOf(
                mapOf(
                  "name" to "escape-plugin",
                  "type" to "local",
                  "path" to "../escape-plugin.jar",
                ),
              ),
          ),
      )

    val error = assertThrows(IllegalArgumentException::class.java) {
      loader.load(context)
    }

    assertTrue(error.message!!.contains("project root"))
  }

  private class TrackingDownloader(
    private val tempDir: Path,
  ) : PluginDownloader {
    val downloadedUrls = mutableListOf<String>()

    override fun download(url: String): File {
      downloadedUrls += url
      val file = tempDir.resolve(url.substringAfterLast('/'))
      file.parent?.createDirectories()
      file.writeText("stub")
      return file.toFile()
    }
  }

  private class RecordingSignatureVerifier : PluginSignatureVerifier {
    val verifiedPluginNames = mutableListOf<String>()

    override fun verify(plugin: PluginConfig, pluginFile: File, signatureFile: File) {
      verifiedPluginNames += plugin.name
      assertTrue(plugin.verifySignature)
      assertTrue(signatureFile.exists())
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
}