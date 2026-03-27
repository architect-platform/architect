package io.github.architectplatform.core.project.app

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.core.project.domain.ProjectProfile
import jakarta.inject.Singleton
import java.io.File
import java.nio.file.Path

@Singleton
class StackDetectionService(
  private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule(),
) {
  fun detect(projectDir: Path): ProjectProfile {
    val dir = projectDir.toFile()
    val languages = linkedSetOf<String>()
    val buildTools = linkedSetOf<String>()
    val testFrameworks = linkedSetOf<String>()
    val ciSystems = linkedSetOf<String>()
    val containerization = linkedSetOf<String>()
    val markers = linkedSetOf<String>()

    detectJavaScript(dir, languages, buildTools, testFrameworks, markers)
    detectJvm(dir, languages, buildTools, testFrameworks, markers)
    detectPython(dir, languages, buildTools, testFrameworks, markers)
    detectSimpleMarker(dir, "Cargo.toml", markers) {
      languages += "Rust"
      buildTools += "Cargo"
      testFrameworks += "Cargo Test"
    }
    detectSimpleMarker(dir, "go.mod", markers) {
      languages += "Go"
      buildTools += "Go Modules"
      testFrameworks += "Go Test"
    }
    detectSimpleMarker(dir, "Dockerfile", markers) {
      containerization += "Docker"
    }
    detectSimpleMarker(dir, ".git", markers)
    detectSimpleMarker(dir, "mkdocs.yml", markers)
    detectSimpleMarker(dir, "docusaurus.config.js", markers)
    detectSimpleMarker(dir, "docs", markers)

    if (File(dir, ".github/workflows").isDirectory) {
      markers += ".github/workflows"
      ciSystems += "GitHub Actions"
    }

    if (File(dir, "terraform").isDirectory || dir.listFiles()?.any { it.extension == "tf" } == true) {
      if (File(dir, "terraform").isDirectory) {
        markers += "terraform"
      }
      if (dir.listFiles()?.any { it.extension == "tf" } == true) {
        markers += "*.tf"
      }
      languages += "Terraform"
      buildTools += "Terraform"
    }

    return ProjectProfile(
      languages = languages,
      buildTools = buildTools,
      testFrameworks = testFrameworks,
      ciSystems = ciSystems,
      containerization = containerization,
      markers = markers,
    )
  }

  private fun detectJavaScript(
    dir: File,
    languages: MutableSet<String>,
    buildTools: MutableSet<String>,
    testFrameworks: MutableSet<String>,
    markers: MutableSet<String>,
  ) {
    val packageJson = File(dir, "package.json")
    if (!packageJson.exists()) {
      return
    }

    markers += "package.json"
    if (File(dir, "tsconfig.json").exists()) {
      markers += "tsconfig.json"
    }

    val packageTree = readJson(packageJson)
    val dependencies = dependencyNames(packageTree)
    val packageManagers = linkedSetOf<String>()
    detectPackageManagerMarker(dir, "package-lock.json", "npm", markers, packageManagers)
    detectPackageManagerMarker(dir, "npm-shrinkwrap.json", "npm", markers, packageManagers)
    detectPackageManagerMarker(dir, "yarn.lock", "yarn", markers, packageManagers)
    detectPackageManagerMarker(dir, "pnpm-lock.yaml", "pnpm", markers, packageManagers)
    detectPackageManagerMarker(dir, "bun.lockb", "bun", markers, packageManagers)
    detectPackageManagerMarker(dir, "bun.lock", "bun", markers, packageManagers)

    packageTree
      ?.path("packageManager")
      ?.takeIf { it.isTextual }
      ?.asText()
      ?.substringBefore("@")
      ?.takeIf { it in setOf("npm", "yarn", "pnpm", "bun") }
      ?.let { packageManagers += it }

    val hasTypeScript = "typescript" in dependencies || File(dir, "tsconfig.json").exists()
    languages += if (hasTypeScript) "TypeScript" else "JavaScript"
    buildTools += packageManagers.ifEmpty { setOf("npm") }

    if (dependencies.any { it.contains("jest") }) {
      testFrameworks += "Jest"
    }
    if ("vitest" in dependencies) {
      testFrameworks += "Vitest"
    }
  }

  private fun detectJvm(
    dir: File,
    languages: MutableSet<String>,
    buildTools: MutableSet<String>,
    testFrameworks: MutableSet<String>,
    markers: MutableSet<String>,
  ) {
    val gradleKts = File(dir, "build.gradle.kts")
    val gradleGroovy = File(dir, "build.gradle")
    val pom = File(dir, "pom.xml")

    if (gradleKts.exists()) {
      markers += "build.gradle.kts"
      buildTools += "Gradle"
      detectJvmLanguagesAndTests(dir, gradleKts.readTextSafely(), languages, testFrameworks, defaultLanguage = "Kotlin")
    }

    if (gradleGroovy.exists()) {
      markers += "build.gradle"
      buildTools += "Gradle"
      detectJvmLanguagesAndTests(dir, gradleGroovy.readTextSafely(), languages, testFrameworks, defaultLanguage = "Java")
    }

    if (pom.exists()) {
      markers += "pom.xml"
      buildTools += "Maven"
      detectJvmLanguagesAndTests(dir, pom.readTextSafely(), languages, testFrameworks, defaultLanguage = "Java")
    }
  }

  private fun detectJvmLanguagesAndTests(
    dir: File,
    buildContent: String,
    languages: MutableSet<String>,
    testFrameworks: MutableSet<String>,
    defaultLanguage: String,
  ) {
    val kotlinDetected =
      File(dir, "src/main/kotlin").exists() ||
        File(dir, "src/test/kotlin").exists() ||
        buildContent.contains("kotlin", ignoreCase = true)
    val javaDetected =
      File(dir, "src/main/java").exists() ||
        File(dir, "src/test/java").exists() ||
        buildContent.contains("java", ignoreCase = true)

    if (kotlinDetected) {
      languages += "Kotlin"
    }
    if (javaDetected || (!kotlinDetected && defaultLanguage == "Java")) {
      languages += "Java"
    }
    if (!kotlinDetected && !javaDetected && defaultLanguage == "Kotlin") {
      languages += "Kotlin"
    }

    if (
      buildContent.contains("junit", ignoreCase = true) ||
        File(dir, "src/test").exists()
    ) {
      testFrameworks += "JUnit"
    }
    if (buildContent.contains("kotest", ignoreCase = true)) {
      testFrameworks += "Kotest"
    }
  }

  private fun detectPython(
    dir: File,
    languages: MutableSet<String>,
    buildTools: MutableSet<String>,
    testFrameworks: MutableSet<String>,
    markers: MutableSet<String>,
  ) {
    val requirements = File(dir, "requirements.txt")
    if (requirements.exists()) {
      markers += "requirements.txt"
      languages += "Python"
      buildTools += "pip"
      if (requirements.readTextSafely().contains("pytest", ignoreCase = true)) {
        testFrameworks += "pytest"
      }
    }

    val pyproject = File(dir, "pyproject.toml")
    if (!pyproject.exists()) {
      return
    }

    markers += "pyproject.toml"
    languages += "Python"
    val content = pyproject.readTextSafely()
    when {
      File(dir, "uv.lock").exists() || content.contains("[tool.uv", ignoreCase = true) -> {
        buildTools += "uv"
        if (File(dir, "uv.lock").exists()) {
          markers += "uv.lock"
        }
      }
      content.contains("[tool.poetry]", ignoreCase = true) -> buildTools += "Poetry"
      else -> buildTools += "pip"
    }
    if (content.contains("pytest", ignoreCase = true)) {
      testFrameworks += "pytest"
    }
  }

  private fun detectPackageManagerMarker(
    dir: File,
    fileName: String,
    toolName: String,
    markers: MutableSet<String>,
    packageManagers: MutableSet<String>,
  ) {
    if (File(dir, fileName).exists()) {
      markers += fileName
      packageManagers += toolName
    }
  }

  private fun detectSimpleMarker(
    dir: File,
    fileName: String,
    markers: MutableSet<String>,
    onFound: (() -> Unit)? = null,
  ) {
    if (File(dir, fileName).exists()) {
      markers += fileName
      onFound?.invoke()
    }
  }

  private fun readJson(file: File): JsonNode? =
    runCatching { objectMapper.readTree(file) }.getOrNull()

  private fun dependencyNames(packageTree: JsonNode?): Set<String> {
    if (packageTree == null) {
      return emptySet()
    }

    return listOf("dependencies", "devDependencies", "peerDependencies", "optionalDependencies")
      .flatMap { fieldName ->
        packageTree.path(fieldName).fieldNames().asSequence().toList()
      }
      .toSet()
  }

  private fun File.readTextSafely(): String = runCatching { readText() }.getOrDefault("")
}
