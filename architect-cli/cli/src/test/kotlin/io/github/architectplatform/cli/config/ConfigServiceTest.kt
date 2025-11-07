package io.github.architectplatform.cli.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ConfigServiceTest {

  private lateinit var configService: ConfigService

  @TempDir
  lateinit var tempDir: Path

  @BeforeEach
  fun setUp() {
    configService = ConfigService()
  }

  @Test
  fun `readConfig returns null when no architect yml exists`() {
    val config = configService.readConfig(tempDir.toString())
    assertNull(config)
  }

  @Test
  fun `readConfig reads architect yml file`() {
    val yamlContent = """
      project:
        name: test-project
      architect:
        cliVersion: "1.1.0"
        engineVersion: "1.6.1"
    """.trimIndent()
    
    File(tempDir.toFile(), "architect.yml").writeText(yamlContent)
    
    val config = configService.readConfig(tempDir.toString())
    assertNotNull(config)
    
    @Suppress("UNCHECKED_CAST")
    val project = config!!["project"] as? Map<String, Any>
    assertNotNull(project)
    assertEquals("test-project", project!!["name"])
  }

  @Test
  fun `readConfig reads architect yaml file`() {
    val yamlContent = """
      project:
        name: test-project
    """.trimIndent()
    
    File(tempDir.toFile(), "architect.yaml").writeText(yamlContent)
    
    val config = configService.readConfig(tempDir.toString())
    assertNotNull(config)
  }

  @Test
  fun `getConfigValue retrieves nested values`() {
    val config = mapOf(
      "project" to mapOf(
        "name" to "test-project"
      ),
      "architect" to mapOf(
        "cliVersion" to "1.1.0"
      )
    )
    
    val projectName = configService.getConfigValue<String>(config, "project.name")
    assertEquals("test-project", projectName)
    
    val cliVersion = configService.getConfigValue<String>(config, "architect.cliVersion")
    assertEquals("1.1.0", cliVersion)
  }

  @Test
  fun `getConfigValue returns null for non-existent key`() {
    val config = mapOf("project" to mapOf("name" to "test-project"))
    
    val result = configService.getConfigValue<String>(config, "nonexistent.key")
    assertNull(result)
  }

  @Test
  fun `getPinnedCliVersion retrieves CLI version`() {
    val config = mapOf(
      "architect" to mapOf(
        "cliVersion" to "1.1.0"
      )
    )
    
    val version = configService.getPinnedCliVersion(config)
    assertEquals("1.1.0", version)
  }

  @Test
  fun `getPinnedCliVersion returns null when not configured`() {
    val config = mapOf("project" to mapOf("name" to "test-project"))
    
    val version = configService.getPinnedCliVersion(config)
    assertNull(version)
  }

  @Test
  fun `getPinnedEngineVersion retrieves Engine version`() {
    val config = mapOf(
      "architect" to mapOf(
        "engineVersion" to "1.6.1"
      )
    )
    
    val version = configService.getPinnedEngineVersion(config)
    assertEquals("1.6.1", version)
  }

  @Test
  fun `getPinnedEngineVersion returns null when not configured`() {
    val config = mapOf("project" to mapOf("name" to "test-project"))
    
    val version = configService.getPinnedEngineVersion(config)
    assertNull(version)
  }

  @Test
  fun `readConfig handles both pinned versions`() {
    val yamlContent = """
      project:
        name: test-project
      architect:
        cliVersion: "1.1.0"
        engineVersion: "1.6.1"
    """.trimIndent()
    
    File(tempDir.toFile(), "architect.yml").writeText(yamlContent)
    
    val config = configService.readConfig(tempDir.toString())
    assertNotNull(config)
    
    val cliVersion = configService.getPinnedCliVersion(config)
    val engineVersion = configService.getPinnedEngineVersion(config)
    
    assertEquals("1.1.0", cliVersion)
    assertEquals("1.6.1", engineVersion)
  }
}
