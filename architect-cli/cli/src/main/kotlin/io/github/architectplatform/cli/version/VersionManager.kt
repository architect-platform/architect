package io.github.architectplatform.cli.version

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.inject.Singleton
import java.io.File
import java.time.Instant

/**
 * Manages version checking and update notifications for the Architect CLI.
 *
 * This service handles:
 * - Checking for new CLI and Engine versions
 * - Storing last check timestamp to avoid excessive API calls
 * - Notifying users when updates are available (max once per day)
 */
@Singleton
class VersionManager(private val objectMapper: ObjectMapper) {

  companion object {
    private const val CLI_VERSION = "1.1.0"
    private const val CHECK_INTERVAL_HOURS = 24
  }

  private val configDir: File
    get() = File(System.getProperty("user.home"), ".architect-cli")

  private val versionCheckFile: File
    get() = File(configDir, "version-check.json")

  data class VersionCheckData(
    val lastCheckTime: Long = 0,
    val lastNotifiedCliVersion: String? = null,
    val lastNotifiedEngineVersion: String? = null
  )

  /**
   * Gets the current CLI version.
   */
  fun getCliVersion(): String = CLI_VERSION

  /**
   * Checks if a version check should be performed.
   * Returns true if more than CHECK_INTERVAL_HOURS have passed since last check.
   */
  fun shouldCheckForUpdates(): Boolean {
    if (!versionCheckFile.exists()) {
      return true
    }

    try {
      val data = objectMapper.readValue<VersionCheckData>(versionCheckFile)
      val hoursSinceLastCheck = (Instant.now().epochSecond - data.lastCheckTime) / 3600
      return hoursSinceLastCheck >= CHECK_INTERVAL_HOURS
    } catch (e: Exception) {
      // If we can't read the file, assume we should check
      return true
    }
  }

  /**
   * Saves the timestamp of the last version check.
   */
  fun recordVersionCheck(cliVersion: String? = null, engineVersion: String? = null) {
    configDir.mkdirs()
    
    val existingData = if (versionCheckFile.exists()) {
      try {
        objectMapper.readValue<VersionCheckData>(versionCheckFile)
      } catch (e: Exception) {
        VersionCheckData()
      }
    } else {
      VersionCheckData()
    }

    val data = VersionCheckData(
      lastCheckTime = Instant.now().epochSecond,
      lastNotifiedCliVersion = cliVersion ?: existingData.lastNotifiedCliVersion,
      lastNotifiedEngineVersion = engineVersion ?: existingData.lastNotifiedEngineVersion
    )

    objectMapper.writeValue(versionCheckFile, data)
  }

  /**
   * Gets the last check data.
   */
  fun getLastCheckData(): VersionCheckData {
    if (!versionCheckFile.exists()) {
      return VersionCheckData()
    }

    return try {
      objectMapper.readValue<VersionCheckData>(versionCheckFile)
    } catch (e: Exception) {
      VersionCheckData()
    }
  }

  /**
   * Compares two semantic version strings.
   * Returns:
   *  - negative if v1 < v2
   *  - 0 if v1 == v2
   *  - positive if v1 > v2
   */
  fun compareVersions(v1: String, v2: String): Int {
    val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
    val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }

    for (i in 0 until maxOf(parts1.size, parts2.size)) {
      val part1 = parts1.getOrNull(i) ?: 0
      val part2 = parts2.getOrNull(i) ?: 0
      if (part1 != part2) {
        return part1 - part2
      }
    }
    return 0
  }

  /**
   * Checks if we should notify about this version.
   * Returns true if this is a newer version than what we last notified about.
   */
  fun shouldNotifyAboutVersion(currentVersion: String, newVersion: String, componentType: String): Boolean {
    val lastData = getLastCheckData()
    val lastNotified = when (componentType) {
      "cli" -> lastData.lastNotifiedCliVersion
      "engine" -> lastData.lastNotifiedEngineVersion
      else -> null
    }

    // If we've never notified, or the new version is different from last notified
    if (lastNotified == null || newVersion != lastNotified) {
      // Only notify if new version is actually newer than current
      return compareVersions(newVersion, currentVersion) > 0
    }

    return false
  }

  /**
   * Displays update notification to the user.
   */
  fun displayUpdateNotification(componentType: String, currentVersion: String, newVersion: String) {
    println()
    println("═".repeat(80))
    println("⚠️  UPDATE AVAILABLE")
    println("═".repeat(80))
    println()
    println("A new version of Architect $componentType is available!")
    println("  Current version: $currentVersion")
    println("  Latest version:  $newVersion")
    println()
    if (componentType == "CLI") {
      println("To upgrade, run:")
      println("  curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-cli/.installers/bash | bash")
    } else if (componentType == "Engine") {
      println("To upgrade, run:")
      println("  architect engine stop")
      println("  architect engine clean")
      println("  architect engine install")
      println("  architect engine start")
    }
    println()
    println("═".repeat(80))
    println()
  }

  /**
   * Validates that the running CLI version matches the pinned version in architect.yml.
   */
  fun validatePinnedCliVersion(pinnedVersion: String?): Boolean {
    if (pinnedVersion == null) return true

    if (CLI_VERSION != pinnedVersion) {
      println()
      println("⚠️  WARNING: CLI version mismatch")
      println("  Required version: $pinnedVersion")
      println("  Current version:  $CLI_VERSION")
      println()
      println("This project requires CLI version $pinnedVersion.")
      println("You are running version $CLI_VERSION.")
      println()
      return false
    }
    return true
  }

  /**
   * Validates that the engine version matches the pinned version in architect.yml.
   */
  fun validatePinnedEngineVersion(pinnedVersion: String?, actualVersion: String?): Boolean {
    if (pinnedVersion == null) return true
    if (actualVersion == null) {
      println()
      println("⚠️  WARNING: Cannot verify engine version")
      println("  Required version: $pinnedVersion")
      println("  Engine version could not be determined")
      println()
      return false
    }

    if (actualVersion != pinnedVersion) {
      println()
      println("⚠️  WARNING: Engine version mismatch")
      println("  Required version: $pinnedVersion")
      println("  Current version:  $actualVersion")
      println()
      println("This project requires Engine version $pinnedVersion.")
      println("The running engine is version $actualVersion.")
      println()
      return false
    }
    return true
  }
}
