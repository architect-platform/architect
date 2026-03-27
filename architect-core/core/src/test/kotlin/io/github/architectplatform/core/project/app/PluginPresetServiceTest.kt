package io.github.architectplatform.core.project.app

import io.github.architectplatform.core.project.domain.ProjectProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PluginPresetServiceTest {

  private val service = PluginPresetService()

  @Test
  fun `matchingPresets recommends kotlin gradle preset`() {
    val profile = ProjectProfile(
      languages = setOf("Kotlin"),
      buildTools = setOf("Gradle"),
    )

    val presets = service.matchingPresets(profile)

    assertEquals("kotlin-gradle", presets.first().id)
  }

  @Test
  fun `matchingPresets recommends typescript preset`() {
    val profile = ProjectProfile(
      languages = setOf("TypeScript"),
      buildTools = setOf("pnpm"),
    )

    val presets = service.matchingPresets(profile)

    assertEquals("typescript-npm", presets.first().id)
  }

  @Test
  fun `matchingPresets recommends rust preset`() {
    val profile = ProjectProfile(
      languages = setOf("Rust"),
      buildTools = setOf("Cargo"),
    )

    val presets = service.matchingPresets(profile)

    assertEquals("rust-cargo", presets.first().id)
  }

  @Test
  fun `matchingPresets recommends python uv preset`() {
    val profile = ProjectProfile(
      languages = setOf("Python"),
      buildTools = setOf("uv"),
    )

    val presets = service.matchingPresets(profile)

    assertEquals("python-uv", presets.first().id)
  }

  @Test
  fun `matchingPresets recommends fullstack preset for javascript docker projects`() {
    val profile = ProjectProfile(
      languages = setOf("TypeScript"),
      buildTools = setOf("npm"),
      containerization = setOf("Docker"),
    )

    val presets = service.matchingPresets(profile)

    assertTrue(presets.any { it.id == "fullstack" })
  }

  @Test
  fun `find returns preset with expected plugins`() {
    val preset = service.find("kotlin-gradle")

    assertNotNull(preset)
    assertEquals(
      listOf(
        "gradle-architected",
        "git-architected",
        "github-architected",
        "testing-architected",
        "quality-architected",
      ),
      preset!!.plugins.map { it.id },
    )
  }
}
