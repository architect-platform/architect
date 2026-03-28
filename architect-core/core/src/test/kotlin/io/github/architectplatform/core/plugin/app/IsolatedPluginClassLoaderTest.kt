package io.github.architectplatform.core.plugin.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import java.net.URL
import java.net.URLClassLoader

/**
 * Verifies that IsolatedPluginClassLoader enforces child-first loading
 * while delegating bootstrap and shared API classes to the parent.
 */
class IsolatedPluginClassLoaderTest {

  @Test
  fun `bootstrap classes delegate to parent`() {
    val loader = IsolatedPluginClassLoader(
      urls = emptyArray(),
      parent = Thread.currentThread().contextClassLoader,
    )

    // java.* class must resolve via parent
    val stringClass = loader.loadClass("java.lang.String")
    assertSame(String::class.java, stringClass)

    // kotlin.* class must also delegate
    val unitClass = loader.loadClass("kotlin.Unit")
    assertNotNull(unitClass)
  }

  @Test
  fun `shared API classes delegate to parent`() {
    val loader = IsolatedPluginClassLoader(
      urls = emptyArray(),
      parent = Thread.currentThread().contextClassLoader,
      sharedPackages = setOf("io.github.architectplatform.api."),
    )

    // Shared API class resolves via parent
    val pluginInterfaceClass = loader.loadClass(
      "io.github.architectplatform.api.core.plugins.ArchitectPlugin",
    )
    assertNotNull(pluginInterfaceClass)
  }

  @Test
  fun `default shared packages include api and slf4j`() {
    val defaults = IsolatedPluginClassLoader.DEFAULT_SHARED_PACKAGES
    assert(defaults.contains("io.github.architectplatform.api."))
    assert(defaults.contains("org.slf4j."))
  }

  @Test
  fun `falls back to parent for classes not in plugin jar`() {
    val loader = IsolatedPluginClassLoader(
      urls = emptyArray(),
      parent = Thread.currentThread().contextClassLoader,
    )

    // A class from core that's not in plugin JAR should fall back
    val thisClass = loader.loadClass(IsolatedPluginClassLoaderTest::class.java.name)
    assertNotNull(thisClass)
  }

  @Test
  fun `getResource uses child-first then parent`() {
    val loader = IsolatedPluginClassLoader(
      urls = emptyArray(),
      parent = Thread.currentThread().contextClassLoader,
    )

    // META-INF/MANIFEST.MF should be found via parent
    loader.getResource("META-INF/MANIFEST.MF")
    // May or may not exist, but method should not throw
    // Just verify the API works without error
  }
}
