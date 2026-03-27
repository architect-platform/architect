package io.github.architectplatform.core.project.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class StackDetectionServiceTest {

  private val service = StackDetectionService()

  @Test
  fun `detect identifies pnpm typescript vitest github actions and docker`(@TempDir dir: Path) {
    File(dir.toFile(), "package.json").writeText(
      """
        {
          "name": "web-app",
          "packageManager": "pnpm@9.0.0",
          "devDependencies": {
            "typescript": "^5.0.0",
            "vitest": "^2.0.0"
          }
        }
      """.trimIndent()
    )
    File(dir.toFile(), "pnpm-lock.yaml").writeText("lockfileVersion: '9.0'")
    File(dir.toFile(), "tsconfig.json").writeText("{}")
    File(dir.toFile(), "Dockerfile").writeText("FROM node:20")
    File(dir.resolve(".github/workflows").toFile().apply { mkdirs() }, "ci.yml").writeText("name: ci")

    val profile = service.detect(dir)

    assertEquals(setOf("TypeScript"), profile.languages)
    assertTrue("pnpm" in profile.buildTools)
    assertTrue("Vitest" in profile.testFrameworks)
    assertTrue("GitHub Actions" in profile.ciSystems)
    assertTrue("Docker" in profile.containerization)
  }

  @Test
  fun `detect identifies kotlin gradle junit project`(@TempDir dir: Path) {
    File(dir.toFile(), "build.gradle.kts").writeText(
      """
        plugins {
          kotlin("jvm") version "1.9.25"
        }

        dependencies {
          testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
        }
      """.trimIndent()
    )
    File(dir.resolve("src/main/kotlin").toFile().apply { mkdirs() }, "App.kt").writeText("class App")

    val profile = service.detect(dir)

    assertTrue("Kotlin" in profile.languages)
    assertTrue("Gradle" in profile.buildTools)
    assertTrue("JUnit" in profile.testFrameworks)
  }

  @Test
  fun `detect identifies java maven project without kotlin markers`(@TempDir dir: Path) {
    File(dir.toFile(), "pom.xml").writeText(
      """
        <project>
          <groupId>demo</groupId>
          <artifactId>demo</artifactId>
        </project>
      """.trimIndent()
    )

    val profile = service.detect(dir)

    assertTrue("Java" in profile.languages)
    assertTrue("Maven" in profile.buildTools)
  }

  @Test
  fun `detect identifies poetry pytest project`(@TempDir dir: Path) {
    File(dir.toFile(), "pyproject.toml").writeText(
      """
        [tool.poetry]
        name = "demo"

        [tool.pytest.ini_options]
        addopts = "-q"
      """.trimIndent()
    )

    val profile = service.detect(dir)

    assertTrue("Python" in profile.languages)
    assertTrue("Poetry" in profile.buildTools)
    assertTrue("pytest" in profile.testFrameworks)
  }

  @Test
  fun `detect identifies uv project from lockfile`(@TempDir dir: Path) {
    File(dir.toFile(), "pyproject.toml").writeText("[project]\nname = \"demo\"")
    File(dir.toFile(), "uv.lock").writeText("version = 1")

    val profile = service.detect(dir)

    assertTrue("uv" in profile.buildTools)
    assertTrue("uv.lock" in profile.markers)
  }

  @Test
  fun `detect identifies go rust terraform and git markers`(@TempDir dir: Path) {
    File(dir.toFile(), "go.mod").writeText("module demo")
    File(dir.toFile(), "Cargo.toml").writeText("[package]\nname = \"demo\"")
    File(dir.toFile(), ".git").mkdir()
    dir.resolve("terraform").toFile().mkdirs()

    val profile = service.detect(dir)

    assertTrue("Go" in profile.languages)
    assertTrue("Rust" in profile.languages)
    assertTrue("Terraform" in profile.languages)
    assertTrue(".git" in profile.markers)
    assertTrue("Go Test" in profile.testFrameworks)
    assertTrue("Cargo Test" in profile.testFrameworks)
  }

  @Test
  fun `detect returns empty profile for bare directory`(@TempDir dir: Path) {
    val profile = service.detect(dir)

    assertTrue(profile.isEmpty())
    assertTrue(profile.languages.isEmpty())
    assertTrue(profile.buildTools.isEmpty())
  }
}
