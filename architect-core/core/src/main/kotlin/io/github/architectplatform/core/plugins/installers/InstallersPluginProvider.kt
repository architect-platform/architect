package io.github.architectplatform.core.plugins.installers

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.core.plugin.app.CommonPlugin
import jakarta.inject.Singleton

@Singleton
class InstallersPluginProvider : CommonPlugin {
  override fun getPlugin(): ArchitectPlugin<*> = InstallersPlugin()
}
