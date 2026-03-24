package io.github.architectplatform.core.plugin.app

import io.github.architectplatform.core.common.Result
import io.github.architectplatform.core.plugin.domain.PluginSource
import io.github.architectplatform.core.plugin.domain.PluginSourceConfig
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PluginSourceRegistryTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `delegates to the matching source by type`() {
    val registry = PluginSourceRegistry(
      listOf(
        StubPluginSource("local"),
        StubPluginSource("github"),
      ),
    )

    val result = registry.resolve(
      PluginSourceConfig(type = "local", name = "p", version = "1.0.0", path = "/tmp/p.jar"),
    )
    assertTrue(result.isSuccess())
  }

  @Test
  fun `returns failure for unsupported type`() {
    val registry = PluginSourceRegistry(
      listOf(StubPluginSource("local")),
    )

    val result = registry.resolve(
      PluginSourceConfig(type = "ftp", name = "p", version = "1.0.0"),
    )
    assertTrue(result.isFailure())
    assertTrue((result as Result.Failure).message.contains("Unsupported plugin type"))
  }

  @Test
  fun `getSupportedTypes returns all registered types`() {
    val registry = PluginSourceRegistry(
      listOf(
        StubPluginSource("local"),
        StubPluginSource("github"),
        StubPluginSource("registry"),
      ),
    )

    assertEquals(listOf("local", "github", "registry"), registry.getSupportedTypes())
  }

  @Test
  fun `getSourceCount returns number of sources`() {
    val registry = PluginSourceRegistry(
      listOf(StubPluginSource("a"), StubPluginSource("b")),
    )
    assertEquals(2, registry.getSourceCount())
  }

  private class StubPluginSource(private val type: String) : PluginSource {
    override fun getType(): String = type

    override fun resolve(config: PluginSourceConfig): Result<File> {
      return Result.success(File("/tmp/stub.jar"))
    }
  }
}
