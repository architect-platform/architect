package io.github.architectplatform.core.project.domain

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry

data class LoadedProjectPlugins(
  val plugins: List<ArchitectPlugin<*>>,
  val taskRegistry: TaskRegistry,
  val pluginContextKeys: Set<String>,
)

internal class LazyProjectLoadState(
  private val initializer: () -> LoadedProjectPlugins,
) {
  @Volatile
  private var loaded: LoadedProjectPlugins? = null

  fun get(): LoadedProjectPlugins {
    loaded?.let { return it }
    return synchronized(this) {
      loaded ?: initializer().also { loaded = it }
    }
  }

  companion object {
    fun eager(
      plugins: List<ArchitectPlugin<*>>,
      taskRegistry: TaskRegistry,
    ): LazyProjectLoadState =
      LazyProjectLoadState {
        LoadedProjectPlugins(
          plugins = plugins,
          taskRegistry = taskRegistry,
          pluginContextKeys = plugins.map { it.contextKey }.toSet(),
        )
      }
  }
}

internal class LazyTaskRegistry(
  private val state: LazyProjectLoadState,
) : TaskRegistry {
  override fun add(task: Task) {
    state.get().taskRegistry.add(task)
  }

  override fun addAlias(aliasId: String, targetId: String, description: String) {
    state.get().taskRegistry.addAlias(aliasId, targetId, description)
  }

  override fun addGroup(groupId: String, memberIds: List<String>, description: String) {
    state.get().taskRegistry.addGroup(groupId, memberIds, description)
  }

  override fun get(id: String): Task? = state.get().taskRegistry.get(id)

  override fun resolve(reference: String): List<Task> = state.get().taskRegistry.resolve(reference)

  override fun all(): List<Task> = state.get().taskRegistry.all()

  override fun groups(): Map<String, List<String>> = state.get().taskRegistry.groups()
}
