package io.github.architectplatform.engine.core.plugin.app

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import jakarta.inject.Singleton
import java.util.*

@Singleton
open class SpiPluginLoader {
  /** Discover plugins in the given ClassLoader via `META-INF/services`. */
  open fun loadFrom(classLoader: ClassLoader): List<ArchitectPlugin<*>> =
      ServiceLoader.load(ArchitectPlugin::class.java, classLoader).toList()
}
