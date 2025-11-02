package io.github.architectplatform.plugins.github.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for Asset data class.
 */
class AssetTest {
  @Test
  fun `Asset should be created with correct values`() {
    val asset = Asset(name = "app.jar", path = "build/libs/app.jar")

    assertEquals("app.jar", asset.name)
    assertEquals("build/libs/app.jar", asset.path)
  }

  @Test
  fun `Asset equality should work correctly`() {
    val asset1 = Asset(name = "app.jar", path = "build/libs/app.jar")
    val asset2 = Asset(name = "app.jar", path = "build/libs/app.jar")
    val asset3 = Asset(name = "other.jar", path = "build/libs/other.jar")

    assertEquals(asset1, asset2)
    assertNotEquals(asset1, asset3)
  }
}
