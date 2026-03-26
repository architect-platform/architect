package io.github.architectplatform.cli.command

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Handles `architect init` — interactive project scaffolding that detects the
 * existing stack, suggests plugins, and generates `architect.yml`.
 */
class InitCommandHandler {

  data class DetectedStack(
    val languages: List<String>,
    val buildTools: List<String>,
    val markers: Map<String, File>,
  )

  data class PluginSuggestion(
    val id: String,
    val repo: String,
    val reason: String,
  )

  private val stdinReader: BufferedReader = BufferedReader(InputStreamReader(System.`in`))

  fun handle(args: List<String>) {
    val projectDir = File(System.getProperty("user.dir"))
    val yes = args.any { it == "--yes" || it == "-y" }

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

    val stack = detectStack(projectDir)
    if (stack.languages.isNotEmpty() || stack.buildTools.isNotEmpty()) {
      println("🔍 Detected stack:")
      stack.languages.forEach { println("   • Language: $it") }
      stack.buildTools.forEach { println("   • Build tool: $it") }
      println()
    }

    val projectName: String
    val projectDescription: String
    val selectedPlugins: List<PluginSuggestion>

    if (yes) {
      projectName = projectDir.name
      projectDescription = ""
      selectedPlugins = suggestPlugins(stack)
      println("📦 Project name: $projectName")
      println("🔌 Auto-selected plugins: ${selectedPlugins.joinToString { it.id }}")
    } else {
      projectName = promptWithDefault("Project name", projectDir.name)
      projectDescription = promptWithDefault("Description", "")

      val suggestions = suggestPlugins(stack)
      selectedPlugins = if (suggestions.isNotEmpty()) {
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
        parsePluginSelection(selection, suggestions)
      } else {
        println("ℹ️  No plugins auto-detected. You can add them later in architect.yml.")
        emptyList()
      }
    }

    println()
    val yaml = generateYaml(projectName, projectDescription, selectedPlugins)
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

  internal fun detectStack(dir: File): DetectedStack {
    val languages = mutableListOf<String>()
    val buildTools = mutableListOf<String>()
    val markers = mutableMapOf<String, File>()

    val checks = listOf(
      Triple("package.json", "JavaScript/TypeScript", "npm/yarn/pnpm"),
      Triple("build.gradle.kts", "Kotlin", "Gradle"),
      Triple("build.gradle", "Java/Groovy", "Gradle"),
      Triple("pom.xml", "Java", "Maven"),
      Triple("Cargo.toml", "Rust", "Cargo"),
      Triple("go.mod", "Go", "Go Modules"),
      Triple("requirements.txt", "Python", "pip"),
      Triple("pyproject.toml", "Python", "Poetry/Hatch"),
      Triple("Gemfile", "Ruby", "Bundler"),
      Triple("composer.json", "PHP", "Composer"),
      Triple("Package.swift", "Swift", "SPM"),
      Triple("CMakeLists.txt", "C/C++", "CMake"),
      Triple("Makefile", "C/C++", "Make"),
    )

    for ((file, lang, tool) in checks) {
      val f = File(dir, file)
      if (f.exists()) {
        if (lang !in languages) languages.add(lang)
        if (tool !in buildTools) buildTools.add(tool)
        markers[file] = f
      }
    }

    // Check for docs frameworks
    for (docFile in listOf("mkdocs.yml", "docusaurus.config.js", "docs/")) {
      val f = File(dir, docFile)
      if (f.exists()) markers[docFile] = f
    }

    // Check for git
    if (File(dir, ".git").exists()) markers[".git"] = File(dir, ".git")

    // Check for GitHub workflows
    if (File(dir, ".github").exists()) markers[".github"] = File(dir, ".github")

    return DetectedStack(languages, buildTools, markers)
  }

  internal fun suggestPlugins(stack: DetectedStack): List<PluginSuggestion> {
    val suggestions = mutableListOf<PluginSuggestion>()

    if (".git" in stack.markers) {
      suggestions.add(PluginSuggestion(
        "git-architected",
        "architectplatform/git-architected",
        "Git repository detected"
      ))
    }

    if (".github" in stack.markers) {
      suggestions.add(PluginSuggestion(
        "github-architected",
        "architectplatform/github-architected",
        "GitHub workflows detected"
      ))
    }

    if (stack.buildTools.contains("Gradle")) {
      suggestions.add(PluginSuggestion(
        "gradle-architected",
        "architectplatform/gradle-architected",
        "Gradle build detected"
      ))
    }

    if (stack.buildTools.any { it in listOf("npm/yarn/pnpm") }) {
      suggestions.add(PluginSuggestion(
        "javascript-architected",
        "architectplatform/javascript-architected",
        "Node.js project detected"
      ))
    }

    if (stack.markers.keys.any { it in listOf("mkdocs.yml", "docusaurus.config.js", "docs/") }) {
      suggestions.add(PluginSuggestion(
        "docs-architected",
        "architectplatform/docs-architected",
        "Documentation framework detected"
      ))
    }

    return suggestions
  }

  internal fun generateYaml(
    name: String,
    description: String,
    plugins: List<PluginSuggestion>,
  ): String {
    val sb = StringBuilder()
    sb.appendLine("# Architect project configuration")
    sb.appendLine("# See: https://github.com/architectplatform/architect")
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
}
