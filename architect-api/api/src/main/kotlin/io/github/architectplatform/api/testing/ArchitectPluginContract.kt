package io.github.architectplatform.api.testing

import io.github.architectplatform.api.core.plugins.ArchitectPlugin

class ArchitectPluginContract<C : Any>(
  private val pluginFactory: () -> ArchitectPlugin<C>,
) {
  data class Verification<C : Any>(
    val config: Any? = null,
    val expectedTaskIds: Set<String> = emptySet(),
    val executableTaskIds: List<String> = emptyList(),
    val projectConfig: Map<String, Any> = emptyMap(),
    val services: Map<Class<*>, Any> = emptyMap(),
  )

  fun verify(verification: Verification<C> = Verification()) {
    val plugin = pluginFactory()
    require(plugin.id.isNotBlank()) { "Plugin id must not be blank" }
    require(plugin.contextKey.isNotBlank()) { "Plugin contextKey must not be blank" }
    requireNotNull(plugin.ctxClass) { "Plugin ctxClass must not be null" }

    val initialContext = plugin.context
    val contextMatches =
      plugin.ctxClass.isInstance(initialContext) ||
        plugin.ctxClass == Unit::class.java ||
        initialContext == Unit
    require(contextMatches) {
      "Plugin ${plugin.id} context must be assignable to ${plugin.ctxClass.name}, " +
        "but was ${initialContext.javaClass.name}"
    }

    val kit =
      ArchitectPluginTestKit(plugin)
        .withProjectConfig(verification.projectConfig)

    verification.services.forEach { (type, service) ->
      @Suppress("UNCHECKED_CAST")
      kit.withService(type as Class<Any>, service)
    }

    verification.config?.let { kit.configure(it) }

    val tasks = kit.tasks()
    require(tasks.isNotEmpty()) { "Plugin ${plugin.id} must register at least one task" }
    tasks.forEach { task ->
      require(task.id.isNotBlank()) { "Plugin ${plugin.id} registered a task with blank id" }
      require(task.description().isNotBlank()) {
        "Plugin ${plugin.id} task '${task.id}' must have a non-blank description"
      }
    }

    val taskIds = tasks.map { it.id }.toSet()
    val missingTasks = verification.expectedTaskIds - taskIds
    require(missingTasks.isEmpty()) {
      "Plugin ${plugin.id} is missing expected tasks: ${missingTasks.sorted().joinToString(", ")}"
    }

    verification.executableTaskIds.forEach { taskId ->
      val result = kit.executeTask(taskId)
      require(result.success) {
        "Plugin ${plugin.id} task '$taskId' failed contract execution: ${result.message}"
      }
    }
  }
}
