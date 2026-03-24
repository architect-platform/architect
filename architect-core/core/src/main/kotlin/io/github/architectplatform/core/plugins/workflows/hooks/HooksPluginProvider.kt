package io.github.architectplatform.core.plugins.workflows.hooks

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.core.plugin.app.CommonPlugin
import jakarta.inject.Singleton

@Singleton
class HooksPluginProvider : CommonPlugin {
  override fun getPlugin(): ArchitectPlugin<*> = HooksPlugin()
}
