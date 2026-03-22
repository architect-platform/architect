package io.github.architectplatform.engine.core.project.app

import io.github.architectplatform.api.core.project.Config

/**
 * Merges environment profile configuration on top of the root config.
 *
 * Resolution order:
 * 1. Start with the root config (everything except the `profiles` key)
 * 2. If a matching profile exists under `profiles.<profileName>`, deep-merge it on top
 *
 * Deep-merge rules:
 * - Maps are recursively merged (profile values override root values at the leaf level)
 * - Non-map values from the profile replace root values entirely
 * - Keys present only in the root are preserved
 * - Keys present only in the profile are added
 */
object ProfileMerger {

  /**
   * Applies the named profile to the config.
   *
   * @param config The full parsed config (may contain a `profiles` section)
   * @param profileName The profile to apply (e.g., "staging", "ci"). Null or "default" means no profile.
   * @return A new config map with the profile merged on top, and the `profiles` key removed
   */
  fun merge(config: Config, profileName: String?): Config {
    if (profileName.isNullOrBlank() || profileName == "default") {
      return config - "profiles"
    }

    @Suppress("UNCHECKED_CAST")
    val profiles = config["profiles"] as? Map<String, Any> ?: return config - "profiles"

    @Suppress("UNCHECKED_CAST")
    val profileConfig = profiles[profileName] as? Map<String, Any>
      ?: return config - "profiles"

    val base = config - "profiles"
    return deepMerge(base, profileConfig)
  }

  /**
   * Detects the active profile from environment and CLI flag.
   *
   * Priority: explicit CLI flag > CI auto-detection > "default"
   */
  fun detectProfile(explicitProfile: String?): String {
    if (!explicitProfile.isNullOrBlank()) return explicitProfile

    // Auto-detect CI environments
    val ciEnvVars = listOf("CI", "GITHUB_ACTIONS", "GITLAB_CI", "JENKINS_URL", "CIRCLECI", "BUILDKITE")
    if (ciEnvVars.any { System.getenv(it) != null }) {
      return "ci"
    }

    return "default"
  }

  /**
   * Deep-merges two maps. Values from [override] take precedence.
   * If both values for a key are maps, they are recursively merged.
   */
  @Suppress("UNCHECKED_CAST")
  fun deepMerge(base: Map<String, Any>, override: Map<String, Any>): Map<String, Any> {
    val result = base.toMutableMap()
    for ((key, overrideValue) in override) {
      val baseValue = result[key]
      result[key] = if (baseValue is Map<*, *> && overrideValue is Map<*, *>) {
        deepMerge(baseValue as Map<String, Any>, overrideValue as Map<String, Any>)
      } else {
        overrideValue
      }
    }
    return result
  }
}
