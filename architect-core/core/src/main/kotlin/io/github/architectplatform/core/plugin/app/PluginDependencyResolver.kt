package io.github.architectplatform.core.plugin.app

import io.github.architectplatform.api.core.plugins.ArchitectPlugin

object PluginDependencyResolver {

  fun sortByDependencies(plugins: List<ArchitectPlugin<*>>): List<ArchitectPlugin<*>> {
    val byId = plugins.associateBy { it.id }
    val visited = mutableSetOf<String>()
    val visiting = mutableSetOf<String>()
    val ordered = mutableListOf<ArchitectPlugin<*>>()

    fun dfs(plugin: ArchitectPlugin<*>) {
      val id = plugin.id
      if (id in visiting) {
        throw IllegalStateException("Circular plugin dependency detected involving plugin: $id")
      }
      if (!visited.add(id)) return

      visiting.add(id)
      plugin.dependencies().forEach { depId ->
        val dependency = byId[depId]
          ?: throw IllegalStateException("Missing plugin dependency '$depId' required by '$id'")
        dfs(dependency)
      }
      visiting.remove(id)
      ordered += plugin
    }

    plugins.forEach(::dfs)
    return ordered
  }
}
