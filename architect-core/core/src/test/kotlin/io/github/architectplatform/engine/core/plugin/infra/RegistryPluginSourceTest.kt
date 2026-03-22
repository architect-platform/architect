package io.github.architectplatform.engine.core.plugin.infra

import io.github.architectplatform.engine.core.plugin.domain.PluginSourceConfig
import io.github.architectplatform.engine.core.plugin.domain.SemverConstraint
import io.github.architectplatform.engine.core.plugin.domain.SemverVersion
import io.github.architectplatform.engine.core.plugin.app.PluginDownloader
import io.github.architectplatform.engine.core.plugin.app.RemoteContentFetcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class RegistryPluginSourceTest {

  // ── SemverVersion parsing ─────────────────────────────────────────

  @Test
  fun `SemverVersion parses standard version`() {
    val v = SemverVersion.parse("1.2.3")
    assertNotNull(v)
    assertEquals(1, v!!.major)
    assertEquals(2, v.minor)
    assertEquals(3, v.patch)
  }

  @Test
  fun `SemverVersion parses version with v prefix`() {
    val v = SemverVersion.parse("v2.0.1")
    assertEquals(SemverVersion(2, 0, 1), v)
  }

  @Test
  fun `SemverVersion parses two-part version`() {
    val v = SemverVersion.parse("1.5")
    assertEquals(SemverVersion(1, 5, 0), v)
  }

  @Test
  fun `SemverVersion strips pre-release suffix`() {
    val v = SemverVersion.parse("1.2.3-beta.1")
    assertEquals(SemverVersion(1, 2, 3), v)
  }

  @Test
  fun `SemverVersion returns null for invalid`() {
    assertEquals(null, SemverVersion.parse("abc"))
    assertEquals(null, SemverVersion.parse(""))
  }

  @Test
  fun `SemverVersion comparison`() {
    assertTrue(SemverVersion(1, 0, 0) < SemverVersion(2, 0, 0))
    assertTrue(SemverVersion(1, 2, 0) < SemverVersion(1, 3, 0))
    assertTrue(SemverVersion(1, 2, 3) < SemverVersion(1, 2, 4))
    assertEquals(0, SemverVersion(1, 2, 3).compareTo(SemverVersion(1, 2, 3)))
  }

  // ── SemverConstraint matching ─────────────────────────────────────

  @Test
  fun `caret constraint matches compatible versions`() {
    assertTrue(SemverConstraint.satisfies("1.2.3", "^1.2.0"))
    assertTrue(SemverConstraint.satisfies("1.9.0", "^1.2.0"))
    assertFalse(SemverConstraint.satisfies("2.0.0", "^1.2.0"))
    assertFalse(SemverConstraint.satisfies("1.1.0", "^1.2.0"))
  }

  @Test
  fun `tilde constraint matches patch versions`() {
    assertTrue(SemverConstraint.satisfies("1.2.3", "~1.2.0"))
    assertTrue(SemverConstraint.satisfies("1.2.9", "~1.2.0"))
    assertFalse(SemverConstraint.satisfies("1.3.0", "~1.2.0"))
    assertFalse(SemverConstraint.satisfies("1.1.0", "~1.2.0"))
  }

  @Test
  fun `comparison operators work`() {
    assertTrue(SemverConstraint.satisfies("2.0.0", ">=1.0.0"))
    assertTrue(SemverConstraint.satisfies("1.0.0", ">=1.0.0"))
    assertFalse(SemverConstraint.satisfies("0.9.0", ">=1.0.0"))

    assertTrue(SemverConstraint.satisfies("1.0.0", "<2.0.0"))
    assertFalse(SemverConstraint.satisfies("2.0.0", "<2.0.0"))

    assertTrue(SemverConstraint.satisfies("1.5.0", ">1.0.0"))
    assertFalse(SemverConstraint.satisfies("1.0.0", ">1.0.0"))
  }

  @Test
  fun `range constraint with space-separated parts`() {
    assertTrue(SemverConstraint.satisfies("1.5.0", ">=1.0.0 <2.0.0"))
    assertFalse(SemverConstraint.satisfies("2.0.0", ">=1.0.0 <2.0.0"))
    assertFalse(SemverConstraint.satisfies("0.9.0", ">=1.0.0 <2.0.0"))
  }

  @Test
  fun `exact match constraint`() {
    assertTrue(SemverConstraint.satisfies("1.2.3", "1.2.3"))
    assertTrue(SemverConstraint.satisfies("1.2.3", "=1.2.3"))
    assertFalse(SemverConstraint.satisfies("1.2.4", "1.2.3"))
  }

  @Test
  fun `bestMatch returns highest matching version`() {
    val versions = listOf("1.0.0", "1.1.0", "1.2.0", "2.0.0", "2.1.0")
    assertEquals("1.2.0", SemverConstraint.bestMatch(versions, "^1.0.0"))
    assertEquals("2.1.0", SemverConstraint.bestMatch(versions, "^2.0.0"))
    assertEquals(null, SemverConstraint.bestMatch(versions, "^3.0.0"))
  }

  // ── RegistryPluginSource ─────────────────────────────────────────

  @Test
  fun `resolve fails when registry URL is missing`() {
    val source = RegistryPluginSource(StubFetcher(), StubDownloader())
    val config = PluginSourceConfig(type = "registry", name = "test", version = "1.0.0")
    val result = source.resolve(config)
    assertTrue(result.isFailure())
  }

  @Test
  fun `resolve finds plugin from registry json`(@TempDir dir: Path) {
    val jarFile = dir.resolve("test-1.0.0.jar").toFile()
    Files.writeString(jarFile.toPath(), "fake-jar-content")

    val registryJson = """
      {
        "plugins": [
          { "id": "test-plugin", "version": "1.0.0", "asset": "https://example.com/test-1.0.0.jar" },
          { "id": "test-plugin", "version": "2.0.0", "asset": "https://example.com/test-2.0.0.jar" }
        ]
      }
    """.trimIndent()

    val source = RegistryPluginSource(
      StubFetcher(registryJson),
      StubDownloader(jarFile),
    )
    val config = PluginSourceConfig(
      type = "registry",
      name = "test-plugin",
      version = "^1.0.0",
      registry = "https://example.com/registry.json",
    )
    val result = source.resolve(config)
    assertTrue(result.isSuccess())
  }

  @Test
  fun `resolve returns latest version`(@TempDir dir: Path) {
    val jarFile = dir.resolve("latest.jar").toFile()
    Files.writeString(jarFile.toPath(), "jar")

    val registryJson = """
      {
        "plugins": [
          { "id": "my-plugin", "version": "1.0.0", "asset": "https://x.com/1.jar" },
          { "id": "my-plugin", "version": "2.0.0", "asset": "https://x.com/2.jar" }
        ]
      }
    """.trimIndent()

    val source = RegistryPluginSource(StubFetcher(registryJson), StubDownloader(jarFile))
    val config = PluginSourceConfig(
      type = "registry", name = "my-plugin", version = "latest",
      registry = "https://example.com/registry.json",
    )
    val result = source.resolve(config)
    assertTrue(result.isSuccess())
  }

  @Test
  fun `resolve fails when plugin not found in registry`() {
    val registryJson = """{ "plugins": [{ "id": "other", "version": "1.0.0", "asset": "x" }] }"""
    val source = RegistryPluginSource(StubFetcher(registryJson), StubDownloader())
    val config = PluginSourceConfig(
      type = "registry", name = "missing", version = "1.0.0",
      registry = "https://example.com/registry.json",
    )
    val result = source.resolve(config)
    assertTrue(result.isFailure())
  }

  @Test
  fun `resolve verifies SHA256 and rejects mismatch`(@TempDir dir: Path) {
    val jarFile = dir.resolve("tampered.jar").toFile()
    Files.writeString(jarFile.toPath(), "tampered-content")

    val registryJson = """
      { "plugins": [{ "id": "p", "version": "1.0.0", "asset": "https://x.com/p.jar" }] }
    """.trimIndent()

    val source = RegistryPluginSource(StubFetcher(registryJson), StubDownloader(jarFile))
    val config = PluginSourceConfig(
      type = "registry", name = "p", version = "1.0.0",
      registry = "https://example.com/registry.json",
      sha256 = "0000000000000000000000000000000000000000000000000000000000000000",
    )
    val result = source.resolve(config)
    assertTrue(result.isFailure())
  }

  @Test
  fun `search filters by query`() {
    val registryJson = """
      {
        "plugins": [
          { "id": "git-plugin", "version": "1.0.0", "asset": "x", "description": "Git integration" },
          { "id": "gradle-plugin", "version": "1.0.0", "asset": "x", "description": "Gradle build" },
          { "id": "docs-plugin", "version": "1.0.0", "asset": "x", "description": "Documentation" }
        ]
      }
    """.trimIndent()

    val source = RegistryPluginSource(StubFetcher(registryJson), StubDownloader())
    val results = source.search("https://example.com/registry.json", "gradle")
    assertEquals(1, results.size)
    assertEquals("gradle-plugin", results[0].id)
  }

  // ── HttpPluginSource ──────────────────────────────────────────────

  @Test
  fun `http source fails when url is missing`() {
    val source = HttpPluginSource(StubDownloader())
    val config = PluginSourceConfig(type = "http", name = "test", version = "1.0.0")
    val result = source.resolve(config)
    assertTrue(result.isFailure())
  }

  @Test
  fun `http source downloads from url`(@TempDir dir: Path) {
    val jarFile = dir.resolve("plugin.jar").toFile()
    Files.writeString(jarFile.toPath(), "jar-content")

    val source = HttpPluginSource(StubDownloader(jarFile))
    val config = PluginSourceConfig(
      type = "http", name = "test", version = "1.0.0",
      url = "https://example.com/plugin.jar",
    )
    val result = source.resolve(config)
    assertTrue(result.isSuccess())
  }

  @Test
  fun `http source verifies SHA256`(@TempDir dir: Path) {
    val jarFile = dir.resolve("verified.jar").toFile()
    Files.writeString(jarFile.toPath(), "content")
    val realHash = RegistryPluginSource.sha256(jarFile)

    val source = HttpPluginSource(StubDownloader(jarFile))
    val config = PluginSourceConfig(
      type = "http", name = "test", version = "1.0.0",
      url = "https://example.com/plugin.jar",
      sha256 = realHash,
    )
    val result = source.resolve(config)
    assertTrue(result.isSuccess())
  }

  // ── Test doubles ──────────────────────────────────────────────────

  private class StubFetcher(private val textResponse: String = "{}") : RemoteContentFetcher {
    override fun fetchText(url: String, headers: Map<String, String>): String = textResponse
    override fun fetchBytes(url: String, headers: Map<String, String>): ByteArray = textResponse.toByteArray()
  }

  private class StubDownloader(private val result: File? = null) : PluginDownloader {
    override fun download(url: String): File = result ?: throw IllegalStateException("No file configured")
  }
}
