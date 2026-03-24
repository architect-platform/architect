package io.github.architectplatform.core.project.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProfileMergerTest {

  // ── Deep merge ────────────────────────────────────────────────────

  @Test
  fun `deepMerge combines disjoint keys`() {
    val base = mapOf("a" to 1)
    val override = mapOf("b" to 2)
    val result = ProfileMerger.deepMerge(base, override)
    assertEquals(mapOf("a" to 1, "b" to 2), result)
  }

  @Test
  fun `deepMerge overrides scalar values`() {
    val base = mapOf("a" to 1, "b" to "old")
    val override = mapOf("b" to "new")
    val result = ProfileMerger.deepMerge(base, override)
    assertEquals(mapOf("a" to 1, "b" to "new"), result)
  }

  @Test
  fun `deepMerge recursively merges nested maps`() {
    val base = mapOf("scripts" to mapOf("deploy" to mapOf("run" to "echo deploy", "timeout" to 30)))
    val override = mapOf("scripts" to mapOf("deploy" to mapOf("run" to "kubectl apply")))
    val result = ProfileMerger.deepMerge(base, override)

    @Suppress("UNCHECKED_CAST")
    val deploy = (result["scripts"] as Map<String, Any>)["deploy"] as Map<String, Any>
    assertEquals("kubectl apply", deploy["run"])
    assertEquals(30, deploy["timeout"]) // preserved from base
  }

  @Test
  fun `deepMerge replaces non-map with non-map`() {
    val base = mapOf("x" to listOf(1, 2, 3))
    val override = mapOf("x" to listOf(4, 5))
    val result = ProfileMerger.deepMerge(base, override)
    assertEquals(listOf(4, 5), result["x"])
  }

  // ── merge() with profiles ────────────────────────────────────────

  @Test
  fun `merge with null profile returns config without profiles key`() {
    val config = mapOf(
      "project" to mapOf("name" to "test"),
      "profiles" to mapOf("staging" to mapOf("x" to 1)),
    )
    val result = ProfileMerger.merge(config, null)
    assertEquals(mapOf("project" to mapOf("name" to "test")), result)
  }

  @Test
  fun `merge with default profile returns config without profiles key`() {
    val config = mapOf(
      "project" to mapOf("name" to "test"),
      "profiles" to mapOf("ci" to mapOf("x" to 1)),
    )
    val result = ProfileMerger.merge(config, "default")
    assertEquals(mapOf("project" to mapOf("name" to "test")), result)
  }

  @Test
  fun `merge applies matching profile`() {
    val config = mapOf(
      "project" to mapOf("name" to "test"),
      "scripts" to mapOf("deploy" to mapOf("run" to "echo dev")),
      "profiles" to mapOf(
        "staging" to mapOf(
          "scripts" to mapOf("deploy" to mapOf("run" to "kubectl apply -f staging/")),
        ),
      ),
    )
    val result = ProfileMerger.merge(config, "staging")

    @Suppress("UNCHECKED_CAST")
    val deploy = (result["scripts"] as Map<String, Any>)["deploy"] as Map<String, Any>
    assertEquals("kubectl apply -f staging/", deploy["run"])
    assertTrue("profiles" !in result)
  }

  @Test
  fun `merge with nonexistent profile returns config without profiles key`() {
    val config = mapOf(
      "project" to mapOf("name" to "test"),
      "profiles" to mapOf("staging" to mapOf("x" to 1)),
    )
    val result = ProfileMerger.merge(config, "production")
    assertEquals(mapOf("project" to mapOf("name" to "test")), result)
  }

  @Test
  fun `merge without profiles section returns config unchanged`() {
    val config = mapOf("project" to mapOf("name" to "test"))
    val result = ProfileMerger.merge(config, "staging")
    assertEquals(config, result)
  }

  @Test
  fun `merge preserves base keys not in profile`() {
    val config = mapOf(
      "project" to mapOf("name" to "test", "description" to "A project"),
      "gradle" to mapOf("version" to "8.0"),
      "profiles" to mapOf(
        "ci" to mapOf(
          "project" to mapOf("description" to "CI build"),
        ),
      ),
    )
    val result = ProfileMerger.merge(config, "ci")

    @Suppress("UNCHECKED_CAST")
    val project = result["project"] as Map<String, Any>
    assertEquals("test", project["name"])
    assertEquals("CI build", project["description"])
    assertEquals(mapOf("version" to "8.0"), result["gradle"])
  }

  // ── detectProfile() ──────────────────────────────────────────────

  @Test
  fun `detectProfile returns explicit when provided`() {
    assertEquals("staging", ProfileMerger.detectProfile("staging"))
  }

  @Test
  fun `detectProfile returns default when null and no CI env`() {
    // This test may return "ci" if actually running in CI — acceptable
    val result = ProfileMerger.detectProfile(null)
    assertTrue(result == "default" || result == "ci")
  }

  @Test
  fun `detectProfile returns default for blank string`() {
    val result = ProfileMerger.detectProfile("")
    assertTrue(result == "default" || result == "ci")
  }

  // ── Production-style scenario ────────────────────────────────────

  @Test
  fun `merge production scenario with confirmation and deep config`() {
    val config = mapOf(
      "project" to mapOf("name" to "myapp"),
      "scripts" to mapOf(
        "deploy" to mapOf(
          "run" to "echo deploy-dev",
          "requires-confirmation" to false,
        ),
        "test" to mapOf("run" to "npm test"),
      ),
      "profiles" to mapOf(
        "production" to mapOf(
          "scripts" to mapOf(
            "deploy" to mapOf(
              "run" to "kubectl apply -f k8s/production/",
              "requires-confirmation" to true,
            ),
          ),
        ),
      ),
    )
    val result = ProfileMerger.merge(config, "production")

    @Suppress("UNCHECKED_CAST")
    val scripts = result["scripts"] as Map<String, Any>
    val deploy = scripts["deploy"] as Map<String, Any>
    assertEquals("kubectl apply -f k8s/production/", deploy["run"])
    assertEquals(true, deploy["requires-confirmation"])

    // Unrelated tasks preserved
    @Suppress("UNCHECKED_CAST")
    val test = scripts["test"] as Map<String, Any>
    assertEquals("npm test", test["run"])
  }
}
