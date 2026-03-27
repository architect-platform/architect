package io.github.architectplatform.cli.command

import io.github.architectplatform.core.project.app.PluginPresetService
import io.github.architectplatform.core.project.app.StackDetectionService
import io.github.architectplatform.core.project.domain.PluginPreset
import io.github.architectplatform.core.project.domain.ProjectProfile
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Handles `architect init` — interactive project scaffolding that detects the
 * existing stack, suggests plugins, and generates `architect.yml`.
 */
class InitCommandHandler(
  private val pluginPresetService: PluginPresetService = PluginPresetService(),
  private val stackDetectionService: StackDetectionService = StackDetectionService(),
) {

  data class PluginSuggestion(
    val id: String,
    val repo: String,
    val reason: String,
  )

  private val stdinReader: BufferedReader = BufferedReader(InputStreamReader(System.`in`))

  fun handle(args: List<String>) {
    val projectDir = File(System.getProperty("user.dir"))
    val yes = args.any { it == "--yes" || it == "-y" }
    val detectRequested = args.any { it == "--detect" }
    val explicitPresetId = parseOptionValue(args, "--preset")

    if (File(projectDir, "architect.yml").exists()) {
      println("⚠️  architect.yml already exists in ${projectDir.absolutePath}")
      if (!yes) {
        print("Overwrite? [y/N] ")
        val answer = stdinReader.readLine()?.trim()?.lowercase()
        if (answer != "y" && answer != "yes") {
          println("Aborted.")
          return
        }
      }
    }

    println()
    println("🏗️  Architect Project Initializer")
    println("━".repeat(50))
    println()

    val profile = detectStack(projectDir)
    if (!profile.isEmpty()) {
      println("🔍 Detected project profile:")
      printSection("Languages", profile.languages)
      printSection("Build tools", profile.buildTools)
      printSection("Test frameworks", profile.testFrameworks)
      printSection("CI systems", profile.ciSystems)
      printSection("Containerization", profile.containerization)
      println()
    }

    val explicitPreset = explicitPresetId?.let { presetId ->
      resolvePreset(presetId)?.also {
        println("🎯 Using preset: ${it.id}")
        println("   • ${it.description}")
        println()
      } ?: run {
        println("❌ Unknown preset: $presetId")
        println("Available presets: ${pluginPresetService.all().joinToString { it.id }}")
        return
      }
    }

    val projectName: String
    val projectDescription: String
    val selectedPlugins: List<PluginSuggestion>

    if (yes) {
      projectName = projectDir.name
      projectDescription = ""
      val preset = explicitPreset ?: suggestPresets(profile).firstOrNull()
      selectedPlugins =
        if (preset != null) {
          presetToSuggestions(preset, "Preset ${preset.id} applied")
        } else {
          suggestPlugins(profile)
        }
      println("📦 Project name: $projectName")
      println("🔌 Auto-selected plugins: ${selectedPlugins.joinToString { it.id }}")
    } else {
      projectName = promptWithDefault("Project name", projectDir.name)
      projectDescription = promptWithDefault("Description", "")
      selectedPlugins = explicitPreset?.let { presetToSuggestions(it, "Preset ${it.id} applied") }
        ?: selectPluginsInteractively(profile)
    }

    println()
    val yaml = generateYaml(
      name = projectName,
      description = projectDescription,
      plugins = selectedPlugins,
      profile = profile,
      includeDetectedConfig = detectRequested || !profile.isEmpty(),
    )
    val targetFile = File(projectDir, "architect.yml")
    targetFile.writeText(yaml)

    println("✅ Created architect.yml")
    println()
    println(yaml)
    println("━".repeat(50))
    println("Next steps:")
    println("  • Edit architect.yml to customize your configuration")
    println("  • Run 'architect tasks' to see available tasks")
    println("  • Run 'architect <task>' to execute a task")
  }

  internal fun detectStack(dir: File): ProjectProfile = stackDetectionService.detect(dir.toPath())

  internal fun suggestPresets(stack: ProjectProfile): List<PluginPreset> =
    pluginPresetService.matchingPresets(stack)

  internal fun resolvePreset(id: String): PluginPreset? = pluginPresetService.find(id)

  internal fun suggestPlugins(stack: ProjectProfile): List<PluginSuggestion> {
    val suggestions = mutableListOf<PluginSuggestion>()

    if (".git" in stack.markers) {
      suggestions.add(PluginSuggestion(
        "git-architected",
        "architectplatform/git-architected",
        "Git repository detected"
      ))
    }

    if ("GitHub Actions" in stack.ciSystems) {
      suggestions.add(PluginSuggestion(
        "github-architected",
        "architectplatform/github-architected",
        "GitHub workflows detected"
      ))
    }

    if ("Gradle" in stack.buildTools) {
      suggestions.add(PluginSuggestion(
        "gradle-architected",
        "architectplatform/gradle-architected",
        "Gradle build detected"
      ))
    }

    if (stack.buildTools.any { it in setOf("npm", "yarn", "pnpm", "bun") }) {
      suggestions.add(PluginSuggestion(
        "javascript-architected",
        "architectplatform/javascript-architected",
        "Node.js project detected"
      ))
    }

    if (stack.markers.any { it in setOf("mkdocs.yml", "docusaurus.config.js", "docs") }) {
      suggestions.add(PluginSuggestion(
        "docs-architected",
        "architectplatform/docs-architected",
        "Documentation framework detected"
      ))
    }

    return suggestions
  }

  internal fun presetToSuggestions(
    preset: PluginPreset,
    reason: String = "Preset ${preset.id} applied",
  ): List<PluginSuggestion> =
    preset.plugins.map { plugin ->
      PluginSuggestion(
        id = plugin.id,
        repo = plugin.repo,
        reason = reason,
      )
    }

  internal fun generateYaml(
    name: String,
    description: String,
    plugins: List<PluginSuggestion>,
    profile: ProjectProfile = ProjectProfile(),
    includeDetectedConfig: Boolean = false,
  ): String {
    val sb = StringBuilder()
    sb.appendLine("# Architect project configuration")
    sb.appendLine("# See: https://github.com/architectplatform/architect")
    if (includeDetectedConfig && !profile.isEmpty()) {
      sb.appendLine("# Generated from the detected project stack and selected plugins.")
    }
    sb.appendLine()
    sb.appendLine("project:")
    sb.appendLine("  name: $name")
    if (description.isNotBlank()) {
      sb.appendLine("  description: \"$description\"")
    }
    sb.appendLine()

    if (plugins.isNotEmpty()) {
      sb.appendLine("plugins:")
      for (plugin in plugins) {
        sb.appendLine("  - name: ${plugin.id}")
        sb.appendLine("    repo: ${plugin.repo}")
      }
      sb.appendLine()
    }

    if (includeDetectedConfig && !profile.isEmpty()) {
      appendDetectedConfiguration(sb, profile, plugins)
    }

    return sb.toString()
  }

  private fun promptWithDefault(prompt: String, default: String): String {
    val displayDefault = if (default.isNotEmpty()) " [$default]" else ""
    print("$prompt$displayDefault: ")
    val input = stdinReader.readLine()?.trim() ?: ""
    return input.ifEmpty { default }
  }

  private fun parsePluginSelection(
    selection: String,
    suggestions: List<PluginSuggestion>,
  ): List<PluginSuggestion> {
    val trimmed = selection.trim().uppercase()
    if (trimmed == "N" || trimmed == "NONE") return emptyList()
    if (trimmed == "A" || trimmed == "ALL" || trimmed.isEmpty()) return suggestions

    return selection.split(",")
      .mapNotNull { it.trim().toIntOrNull() }
      .filter { it in 1..suggestions.size }
      .map { suggestions[it - 1] }
      .ifEmpty { suggestions }
  }

  private fun selectPluginsInteractively(profile: ProjectProfile): List<PluginSuggestion> {
    val presets = suggestPresets(profile)
    if (presets.isNotEmpty()) {
      val recommendedPreset = presets.first()
      println()
      println("🎯 Recommended preset: ${recommendedPreset.id} — ${recommendedPreset.description}")
      println("   [Y] Apply recommended preset")
      println("   [M] Manually select plugins")
      println("   [N] None")
      println()
      print("Choose preset or manual selection [Y/m/N]: ")
      when ((stdinReader.readLine()?.trim() ?: "Y").uppercase()) {
        "", "Y", "YES" -> return presetToSuggestions(recommendedPreset)
        "N", "NONE" -> return emptyList()
      }
    }

    val suggestions = suggestPlugins(profile)
    if (suggestions.isEmpty()) {
      println("ℹ️  No plugins auto-detected. You can add them later in architect.yml.")
      return emptyList()
    }

    println()
    println("🔌 Suggested plugins based on detected stack:")
    suggestions.forEachIndexed { i, p ->
      println("   [${i + 1}] ${p.id} — ${p.reason}")
    }
    println("   [A] All suggested plugins")
    println("   [N] None")
    println()
    print("Select plugins (comma-separated numbers, A for all, N for none): ")
    val selection = stdinReader.readLine()?.trim() ?: "A"
    return parsePluginSelection(selection, suggestions)
  }

  private fun parseOptionValue(args: List<String>, option: String): String? {
    val optionIndex = args.indexOf(option)
    if (optionIndex != -1 && optionIndex + 1 < args.size) {
      return args[optionIndex + 1]
    }
    return args
      .firstOrNull { it.startsWith("$option=") }
      ?.substringAfter('=')
      ?.takeIf { it.isNotBlank() }
  }

  private fun appendDetectedConfiguration(
    sb: StringBuilder,
    profile: ProjectProfile,
    plugins: List<PluginSuggestion>,
  ) {
    val pluginIds = plugins.map { it.id }.toSet()
    val packageManager = firstMatch(profile.buildTools, listOf("pnpm", "yarn", "bun", "npm"))
    val pythonTool = firstMatch(profile.buildTools, listOf("uv", "Poetry", "pip"))
    val testFramework = firstMatch(profile.testFrameworks, listOf("Vitest", "Jest", "JUnit", "pytest", "Go Test", "Cargo Test"))

    if ("javascript-architected" in pluginIds) {
      sb.appendLine("# JavaScript/TypeScript settings inferred from package manager and source files.")
      sb.appendLine("javascript:")
      packageManager?.let { sb.appendLine("  packageManager: ${it.lowercase()}") }
      sb.appendLine("  packageFile: package.json")
      if ("TypeScript" in profile.languages) {
        sb.appendLine("  language: typescript")
      }
      sb.appendLine()
    }

    if ("gradle-architected" in pluginIds) {
      sb.appendLine("# Gradle defaults inferred from the detected JVM build.")
      sb.appendLine("gradle:")
      sb.appendLine("  wrapper: true")
      sb.appendLine("  command: ./gradlew")
      sb.appendLine()
    }

    if ("python-architected" in pluginIds) {
      sb.appendLine("# Python toolchain defaults inferred from project metadata.")
      sb.appendLine("python:")
      pythonTool?.let { sb.appendLine("  packageManager: ${it.lowercase()}") }
      sb.appendLine("  projectFile: ${if ("pyproject.toml" in profile.markers) "pyproject.toml" else "requirements.txt"}")
      sb.appendLine()
    }

    if ("rust-architected" in pluginIds) {
      sb.appendLine("# Rust settings inferred from Cargo.toml.")
      sb.appendLine("rust:")
      sb.appendLine("  manifest: Cargo.toml")
      sb.appendLine()
    }

    if ("github-architected" in pluginIds) {
      sb.appendLine("# GitHub integration defaults inferred from detected workflow files.")
      sb.appendLine("github:")
      sb.appendLine("  workflowsDir: .github/workflows")
      sb.appendLine()
    }

    if ("docker-architected" in pluginIds) {
      sb.appendLine("# Docker defaults inferred from the detected container setup.")
      sb.appendLine("docker:")
      sb.appendLine("  dockerfile: Dockerfile")
      sb.appendLine()
    }

    if ("kubernetes-architected" in pluginIds) {
      sb.appendLine("# Kubernetes defaults for deploying generated container workloads.")
      sb.appendLine("kubernetes:")
      sb.appendLine("  manifestsDir: k8s")
      sb.appendLine()
    }

    if ("testing-architected" in pluginIds) {
      sb.appendLine("# Testing defaults inferred from the detected test framework.")
      sb.appendLine("testing:")
      sb.appendLine("  framework: ${testFramework?.lowercase()?.replace(' ', '-') ?: "auto"}")
      sb.appendLine("  coverage:")
      sb.appendLine("    enabled: true")
      sb.appendLine("    threshold: 80")
      sb.appendLine()
    }

    if ("quality-architected" in pluginIds) {
      sb.appendLine("# Quality gate defaults for linting and static analysis.")
      sb.appendLine("quality:")
      sb.appendLine("  lint: true")
      sb.appendLine("  failOnIssues: true")
      sb.appendLine()
    }

    if ("security-architected" in pluginIds) {
      sb.appendLine("# Security scanning defaults for dependencies and container artifacts.")
      sb.appendLine("security:")
      sb.appendLine("  scan:")
      sb.appendLine("    enabled: true")
      sb.appendLine("    failOn: high")
      sb.appendLine()
    }

    if ("docs-architected" in pluginIds) {
      sb.appendLine("# Documentation defaults inferred from the detected docs framework.")
      sb.appendLine("docs:")
      sb.appendLine("  framework: ${detectedDocsFramework(profile)}")
      sb.appendLine()
    }
  }

  private fun detectedDocsFramework(profile: ProjectProfile): String =
    when {
      "mkdocs.yml" in profile.markers -> "mkdocs"
      "docusaurus.config.js" in profile.markers -> "docusaurus"
      "docs" in profile.markers -> "generic"
      else -> "generic"
    }

  private fun firstMatch(values: Set<String>, candidates: List<String>): String? =
    candidates.firstOrNull { it in values }

  private fun printSection(label: String, values: Set<String>) {
    if (values.isEmpty()) {
      return
    }
    println("   • $label: ${values.joinToString()}")
  }
}
