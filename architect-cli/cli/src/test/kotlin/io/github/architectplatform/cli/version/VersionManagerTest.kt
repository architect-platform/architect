package io.github.architectplatform.cli.version

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class VersionManagerTest {

  private lateinit var versionManager: VersionManager
  private lateinit var objectMapper: ObjectMapper

  @TempDir
  lateinit var tempDir: Path

  @BeforeEach
  fun setUp() {
    objectMapper = ObjectMapper()
    versionManager = VersionManager(objectMapper)
    
    // Override config directory for testing
    System.setProperty("user.home", tempDir.toString())
  }

  @Test
  fun `getCliVersion returns correct version`() {
    assertEquals("1.1.0", versionManager.getCliVersion())
  }

  @Test
  fun `compareVersions correctly compares semantic versions`() {
    assertTrue(versionManager.compareVersions("1.0.0", "2.0.0") < 0)
    assertTrue(versionManager.compareVersions("2.0.0", "1.0.0") > 0)
    assertEquals(0, versionManager.compareVersions("1.0.0", "1.0.0"))
    
    assertTrue(versionManager.compareVersions("1.1.0", "1.2.0") < 0)
    assertTrue(versionManager.compareVersions("1.2.0", "1.1.0") > 0)
    
    assertTrue(versionManager.compareVersions("1.1.0", "1.1.1") < 0)
    assertTrue(versionManager.compareVersions("1.1.1", "1.1.0") > 0)
  }

  @Test
  fun `shouldCheckForUpdates returns true when no check file exists`() {
    assertTrue(versionManager.shouldCheckForUpdates())
  }

  @Test
  fun `shouldCheckForUpdates returns false when check was recent`() {
    versionManager.recordVersionCheck()
    assertFalse(versionManager.shouldCheckForUpdates())
  }

  @Test
  fun `recordVersionCheck creates version check file`() {
    versionManager.recordVersionCheck(cliVersion = "1.1.0", engineVersion = "1.6.1")
    
    val data = versionManager.getLastCheckData()
    assertNotNull(data)
    assertEquals("1.1.0", data.lastNotifiedCliVersion)
    assertEquals("1.6.1", data.lastNotifiedEngineVersion)
    assertTrue(data.lastCheckTime > 0)
  }

  @Test
  fun `shouldNotifyAboutVersion returns true for new version`() {
    versionManager.recordVersionCheck(cliVersion = "1.0.0")
    assertTrue(versionManager.shouldNotifyAboutVersion("1.0.0", "1.1.0", "cli"))
  }

  @Test
  fun `shouldNotifyAboutVersion returns false for already notified version`() {
    versionManager.recordVersionCheck(cliVersion = "1.1.0")
    assertFalse(versionManager.shouldNotifyAboutVersion("1.0.0", "1.1.0", "cli"))
  }

  @Test
  fun `shouldNotifyAboutVersion returns false for older version`() {
    versionManager.recordVersionCheck(cliVersion = "1.0.0")
    assertFalse(versionManager.shouldNotifyAboutVersion("1.1.0", "1.0.0", "cli"))
  }

  @Test
  fun `validatePinnedCliVersion returns true when no version pinned`() {
    assertTrue(versionManager.validatePinnedCliVersion(null))
  }

  @Test
  fun `validatePinnedCliVersion returns true when version matches`() {
    assertTrue(versionManager.validatePinnedCliVersion("1.1.0"))
  }

  @Test
  fun `validatePinnedCliVersion returns false when version does not match`() {
    assertFalse(versionManager.validatePinnedCliVersion("2.0.0"))
  }

  @Test
  fun `validatePinnedEngineVersion returns true when no version pinned`() {
    assertTrue(versionManager.validatePinnedEngineVersion(null, "1.6.1"))
  }

  @Test
  fun `validatePinnedEngineVersion returns true when version matches`() {
    assertTrue(versionManager.validatePinnedEngineVersion("1.6.1", "1.6.1"))
  }

  @Test
  fun `validatePinnedEngineVersion returns false when version does not match`() {
    assertFalse(versionManager.validatePinnedEngineVersion("2.0.0", "1.6.1"))
  }

  @Test
  fun `validatePinnedEngineVersion returns false when actual version is null`() {
    assertFalse(versionManager.validatePinnedEngineVersion("1.6.1", null))
  }
}
