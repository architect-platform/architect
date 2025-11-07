package io.github.architectplatform.cli.integration

import io.github.architectplatform.cli.config.ConfigService
import io.github.architectplatform.cli.version.VersionManager
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Integration tests for version management functionality.
 * These tests verify the complete flow from reading configuration to validation.
 */
class VersionManagementIntegrationTest {

  private lateinit var configService: ConfigService
  private lateinit var versionManager: VersionManager

  @TempDir
  lateinit var tempDir: Path

  @BeforeEach
  fun setUp() {
    configService = ConfigService()
    versionManager = VersionManager(ObjectMapper())
    
    // Override config directory for testing
    System.setProperty("user.home", tempDir.toString())
  }

  @Test
  fun `complete flow - project with pinned versions matching current`() {
    // Arrange: Create architect.yml with current versions
    val yamlContent = """
      project:
        name: test-project
      architect:
        cliVersion: "1.1.0"
        engineVersion: "1.6.1"
    """.trimIndent()
    
    File(tempDir.toFile(), "architect.yml").writeText(yamlContent)
    
    // Act: Read config and validate
    val config = configService.readConfig(tempDir.toString())
    val pinnedCliVersion = configService.getPinnedCliVersion(config)
    val pinnedEngineVersion = configService.getPinnedEngineVersion(config)
    
    val cliValid = versionManager.validatePinnedCliVersion(pinnedCliVersion)
    val engineValid = versionManager.validatePinnedEngineVersion(pinnedEngineVersion, "1.6.1")
    
    // Assert: Both validations should pass
    assertNotNull(config)
    assertEquals("1.1.0", pinnedCliVersion)
    assertEquals("1.6.1", pinnedEngineVersion)
    assertTrue(cliValid, "CLI version should be valid")
    assertTrue(engineValid, "Engine version should be valid")
  }

  @Test
  fun `complete flow - project with pinned versions not matching current`() {
    // Arrange: Create architect.yml with different versions
    val yamlContent = """
      project:
        name: test-project
      architect:
        cliVersion: "2.0.0"
        engineVersion: "2.0.0"
    """.trimIndent()
    
    File(tempDir.toFile(), "architect.yml").writeText(yamlContent)
    
    // Act: Read config and validate
    val config = configService.readConfig(tempDir.toString())
    val pinnedCliVersion = configService.getPinnedCliVersion(config)
    val pinnedEngineVersion = configService.getPinnedEngineVersion(config)
    
    val cliValid = versionManager.validatePinnedCliVersion(pinnedCliVersion)
    val engineValid = versionManager.validatePinnedEngineVersion(pinnedEngineVersion, "1.6.1")
    
    // Assert: Both validations should fail
    assertNotNull(config)
    assertEquals("2.0.0", pinnedCliVersion)
    assertEquals("2.0.0", pinnedEngineVersion)
    assertFalse(cliValid, "CLI version should be invalid")
    assertFalse(engineValid, "Engine version should be invalid")
  }

  @Test
  fun `complete flow - project without pinned versions`() {
    // Arrange: Create architect.yml without version pinning
    val yamlContent = """
      project:
        name: test-project
      plugins:
        - name: docs-architected
          repo: architect-platform/architect
    """.trimIndent()
    
    File(tempDir.toFile(), "architect.yml").writeText(yamlContent)
    
    // Act: Read config
    val config = configService.readConfig(tempDir.toString())
    val pinnedCliVersion = configService.getPinnedCliVersion(config)
    val pinnedEngineVersion = configService.getPinnedEngineVersion(config)
    
    val cliValid = versionManager.validatePinnedCliVersion(pinnedCliVersion)
    val engineValid = versionManager.validatePinnedEngineVersion(pinnedEngineVersion, "1.6.1")
    
    // Assert: No versions pinned, validations should pass
    assertNotNull(config)
    assertNull(pinnedCliVersion, "CLI version should not be pinned")
    assertNull(pinnedEngineVersion, "Engine version should not be pinned")
    assertTrue(cliValid, "CLI validation should pass when not pinned")
    assertTrue(engineValid, "Engine validation should pass when not pinned")
  }

  @Test
  fun `complete flow - project with only CLI version pinned`() {
    // Arrange: Create architect.yml with only CLI version
    val yamlContent = """
      project:
        name: test-project
      architect:
        cliVersion: "1.1.0"
    """.trimIndent()
    
    File(tempDir.toFile(), "architect.yml").writeText(yamlContent)
    
    // Act: Read config and validate
    val config = configService.readConfig(tempDir.toString())
    val pinnedCliVersion = configService.getPinnedCliVersion(config)
    val pinnedEngineVersion = configService.getPinnedEngineVersion(config)
    
    val cliValid = versionManager.validatePinnedCliVersion(pinnedCliVersion)
    val engineValid = versionManager.validatePinnedEngineVersion(pinnedEngineVersion, "1.6.1")
    
    // Assert: CLI pinned and valid, engine not pinned
    assertEquals("1.1.0", pinnedCliVersion)
    assertNull(pinnedEngineVersion)
    assertTrue(cliValid, "CLI version should be valid")
    assertTrue(engineValid, "Engine validation should pass when not pinned")
  }

  @Test
  fun `complete flow - project with only Engine version pinned`() {
    // Arrange: Create architect.yml with only Engine version
    val yamlContent = """
      project:
        name: test-project
      architect:
        engineVersion: "1.6.1"
    """.trimIndent()
    
    File(tempDir.toFile(), "architect.yml").writeText(yamlContent)
    
    // Act: Read config and validate
    val config = configService.readConfig(tempDir.toString())
    val pinnedCliVersion = configService.getPinnedCliVersion(config)
    val pinnedEngineVersion = configService.getPinnedEngineVersion(config)
    
    val cliValid = versionManager.validatePinnedCliVersion(pinnedCliVersion)
    val engineValid = versionManager.validatePinnedEngineVersion(pinnedEngineVersion, "1.6.1")
    
    // Assert: Engine pinned and valid, CLI not pinned
    assertNull(pinnedCliVersion)
    assertEquals("1.6.1", pinnedEngineVersion)
    assertTrue(cliValid, "CLI validation should pass when not pinned")
    assertTrue(engineValid, "Engine version should be valid")
  }

  @Test
  fun `update checking flow - first check should return true`() {
    // First check should always return true
    assertTrue(versionManager.shouldCheckForUpdates())
  }

  @Test
  fun `update checking flow - recent check should return false`() {
    // Record a check
    versionManager.recordVersionCheck()
    
    // Immediate subsequent check should return false
    assertFalse(versionManager.shouldCheckForUpdates())
  }

  @Test
  fun `update checking flow - track notified versions`() {
    // Record that we notified about version 1.2.0
    versionManager.recordVersionCheck(cliVersion = "1.2.0")
    
    // Should not notify again about 1.2.0
    assertFalse(versionManager.shouldNotifyAboutVersion("1.1.0", "1.2.0", "cli"))
    
    // But should notify about 1.3.0
    assertTrue(versionManager.shouldNotifyAboutVersion("1.1.0", "1.3.0", "cli"))
  }
}
