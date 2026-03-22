package io.github.architectplatform.engine.core.plugin.app

import org.slf4j.LoggerFactory
import java.net.URL
import java.net.URLClassLoader

/**
 * Child-first (parent-last) classloader for plugin isolation.
 *
 * Each plugin gets its own instance. Classes are resolved in this order:
 * 1. Bootstrap/platform classes (java.*, javax.*, etc.)
 * 2. Shared API classes (architect-api packages passed via [sharedPackages])
 * 3. Plugin's own JAR (child-first)
 * 4. Parent classloader (fallback)
 */
class IsolatedPluginClassLoader(
  urls: Array<URL>,
  parent: ClassLoader,
  private val sharedPackages: Set<String> = DEFAULT_SHARED_PACKAGES,
  private val debug: Boolean = false,
) : URLClassLoader(urls, parent) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun loadClass(name: String, resolve: Boolean): Class<*> {
    synchronized(getClassLoadingLock(name)) {
      var c = findLoadedClass(name)
      if (c != null) {
        if (debug) logger.debug("[classloader] already loaded: {}", name)
        return c
      }

      if (isBootstrapClass(name)) {
        if (debug) logger.debug("[classloader] bootstrap: {}", name)
        return super.loadClass(name, resolve)
      }

      if (isSharedApiClass(name)) {
        if (debug) logger.debug("[classloader] shared API: {}", name)
        return super.loadClass(name, resolve)
      }

      try {
        c = findClass(name)
        if (debug) logger.debug("[classloader] found in plugin JAR: {}", name)
        if (resolve) resolveClass(c)
        return c
      } catch (_: ClassNotFoundException) {
        // fall through
      }

      if (debug) logger.debug("[classloader] fallback to parent: {}", name)
      return super.loadClass(name, resolve)
    }
  }

  override fun getResource(name: String?): URL? {
    if (name == null) return null
    return findResource(name) ?: parent?.getResource(name)
  }

  private fun isBootstrapClass(name: String): Boolean =
    name.startsWith("java.") ||
      name.startsWith("javax.") ||
      name.startsWith("sun.") ||
      name.startsWith("jdk.") ||
      name.startsWith("kotlin.") ||
      name.startsWith("kotlinx.")

  private fun isSharedApiClass(name: String): Boolean =
    sharedPackages.any { name.startsWith(it) }

  companion object {
    val DEFAULT_SHARED_PACKAGES = setOf(
      "io.github.architectplatform.api.",
      "org.slf4j.",
    )
  }
}
