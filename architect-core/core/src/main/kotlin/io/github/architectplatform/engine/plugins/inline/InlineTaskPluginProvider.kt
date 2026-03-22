package io.github.architectplatform.engine.plugins.inline

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.engine.core.plugin.app.CommonPlugin
import jakarta.inject.Singleton

@Singleton
class InlineTaskPluginProvider : CommonPlugin {
    override fun getPlugin(): ArchitectPlugin<*> = InlineTaskPlugin()
}
