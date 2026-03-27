package io.github.architectplatform.core.project.app

import io.github.architectplatform.core.project.domain.PluginPreset
import io.github.architectplatform.core.project.domain.PluginPresetPlugin
import io.github.architectplatform.core.project.domain.ProjectProfile
import jakarta.inject.Singleton

@Singleton
class PluginPresetService {
  private val javascriptBuildTools = setOf("npm", "yarn", "pnpm", "bun")

  private val presets = listOf(
    PluginPreset(
      id = "kotlin-gradle",
      description = "Gradle-based Kotlin projects with testing and quality defaults",
      plugins = listOf(
        PluginPresetPlugin("gradle-architected"),
        PluginPresetPlugin("git-architected"),
        PluginPresetPlugin("github-architected"),
        PluginPresetPlugin("testing-architected"),
        PluginPresetPlugin("quality-architected"),
      ),
    ),
    PluginPreset(
      id = "typescript-npm",
      description = "TypeScript projects with JavaScript, testing, and quality plugins",
      plugins = listOf(
        PluginPresetPlugin("javascript-architected"),
        PluginPresetPlugin("git-architected"),
        PluginPresetPlugin("github-architected"),
        PluginPresetPlugin("testing-architected"),
        PluginPresetPlugin("quality-architected"),
      ),
    ),
    PluginPreset(
      id = "rust-cargo",
      description = "Rust projects with test and security automation",
      plugins = listOf(
        PluginPresetPlugin("rust-architected"),
        PluginPresetPlugin("git-architected"),
        PluginPresetPlugin("github-architected"),
        PluginPresetPlugin("testing-architected"),
        PluginPresetPlugin("security-architected"),
      ),
    ),
    PluginPreset(
      id = "python-uv",
      description = "Python projects managed with uv plus testing and quality defaults",
      plugins = listOf(
        PluginPresetPlugin("python-architected"),
        PluginPresetPlugin("git-architected"),
        PluginPresetPlugin("github-architected"),
        PluginPresetPlugin("testing-architected"),
        PluginPresetPlugin("quality-architected"),
      ),
    ),
    PluginPreset(
      id = "fullstack",
      description = "JavaScript applications with Docker, Kubernetes, and security automation",
      plugins = listOf(
        PluginPresetPlugin("javascript-architected"),
        PluginPresetPlugin("docker-architected"),
        PluginPresetPlugin("kubernetes-architected"),
        PluginPresetPlugin("security-architected"),
      ),
    ),
  )

  fun all(): List<PluginPreset> = presets

  fun find(id: String): PluginPreset? = presets.firstOrNull { it.id == id }

  fun recommend(profile: ProjectProfile): PluginPreset? = matchingPresets(profile).firstOrNull()

  fun matchingPresets(profile: ProjectProfile): List<PluginPreset> =
    presets.filter { preset ->
      when (preset.id) {
        "kotlin-gradle" -> "Gradle" in profile.buildTools && "Kotlin" in profile.languages
        "typescript-npm" ->
          profile.languages.contains("TypeScript") &&
            profile.buildTools.any { it in javascriptBuildTools }
        "rust-cargo" -> "Cargo" in profile.buildTools
        "python-uv" -> "Python" in profile.languages && "uv" in profile.buildTools
        "fullstack" ->
          profile.buildTools.any { it in javascriptBuildTools } &&
            "Docker" in profile.containerization
        else -> false
      }
    }
}
