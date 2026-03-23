package io.github.architectplatform.engine.core.project.domain

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.engine.core.tasks.infrastructure.InMemoryTaskRegistry

class Project(
    val name: String,
    val path: String,
    val context: ProjectContext,
    plugins: List<ArchitectPlugin<*>> = emptyList(),
    val subProjects: List<Project> = emptyList(),
    taskRegistry: TaskRegistry = InMemoryTaskRegistry(),
    lazyPluginLoader: (() -> LoadedProjectPlugins)? = null,
) {
    private val pluginState =
        lazyPluginLoader?.let { LazyProjectLoadState(it) }
            ?: LazyProjectLoadState.eager(plugins, taskRegistry)

    val plugins: List<ArchitectPlugin<*>>
        get() = pluginState.get().plugins

    val taskRegistry: TaskRegistry = LazyTaskRegistry(pluginState)

    fun pluginContextKeys(): Set<String> = pluginState.get().pluginContextKeys
}
