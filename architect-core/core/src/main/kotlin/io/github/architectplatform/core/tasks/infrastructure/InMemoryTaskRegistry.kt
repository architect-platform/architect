package io.github.architectplatform.core.tasks.infrastructure

import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.builtin.SimpleTask

/** In-memory registry implementation. */
class InMemoryTaskRegistry : TaskRegistry {
  private val tasks = linkedMapOf<String, Task>()
  private val aliasTargets = linkedMapOf<String, String>()
  private val groups = linkedMapOf<String, List<String>>()
  private val syntheticGroupIds = linkedSetOf<String>()

  override fun add(task: Task) {
    aliasTargets.remove(task.id)?.let { tasks.remove(task.id) }
    require(task.id !in tasks) { "Task '${task.id}' already registered" }
    tasks[task.id] = task
    registerNamespaceAlias(task.id)
  }

  override fun addAlias(
    aliasId: String,
    targetId: String,
    description: String,
  ) {
    if (aliasId in tasks) return
    aliasTargets[aliasId] = targetId
    tasks[aliasId] = dependencyTask(aliasId, description, listOf(targetId))
  }

  override fun addGroup(
    groupId: String,
    memberIds: List<String>,
    description: String,
  ) {
    val members = memberIds.distinct()
    require(members.isNotEmpty()) { "Task group '$groupId' must include at least one member" }
    groups[groupId] = members
    if (groupId !in tasks) {
      syntheticGroupIds.add(groupId)
      tasks[groupId] = dependencyTask(groupId, description, members)
    }
    registerGroupAliases(groupId, members)
  }

  override fun get(id: String): Task? {
    tasks[id]?.let { return it }

    if (id.endsWith(":*")) {
      val prefix = id.removeSuffix("*")
      val matchedTargets =
        aliasTargets
          .filterKeys { it.startsWith(prefix) }
          .values
          .distinct()
      if (matchedTargets.isEmpty()) return null
      return dependencyTask(
        id = id,
        description = "Wildcard task '$id' runs ${matchedTargets.joinToString(", ")}",
        dependencies = matchedTargets,
      )
    }

    return null
  }

  override fun all(): List<Task> = tasks.values.toList()

  fun aliasIds(): Set<String> = aliasTargets.keys.toSet()

  override fun resolve(reference: String): List<Task> = resolve(reference, groups)

  override fun resolve(reference: String, groups: Map<String, List<String>>): List<Task> {
    val directTaskIds =
      tasks.keys.filterNot { it in aliasTargets || it in syntheticGroupIds }
    val generatedAliases = TaskReferenceResolver.generatedAliasMap(directTaskIds)
    val aliases = linkedMapOf<String, String>().apply {
      putAll(generatedAliases)
      putAll(aliasTargets)
    }
    val resolvedGroups = if (groups.isEmpty()) this.groups else groups
    val resolvedIds = TaskReferenceResolver.resolve(reference, directTaskIds, aliases, resolvedGroups)
    return resolvedIds.mapNotNull { tasks[it] }
  }

  override fun groups(): Map<String, List<String>> = groups.toMap()

  private fun registerNamespaceAlias(taskId: String) {
    if (!taskId.contains(":")) return
    val aliasId = taskId.replace(':', '-')
    if (aliasId == taskId) return
    addAlias(aliasId, taskId, "Namespace alias for $taskId")
  }

  private fun registerGroupAliases(groupId: String, members: List<String>) {
    members.forEach { memberId ->
      val aliasSuffix = groupAliasSuffix(groupId, memberId)
      addAlias(
        aliasId = "$groupId:$aliasSuffix",
        targetId = memberId,
        description = "Group member alias for $memberId",
      )
    }
  }

  private fun groupAliasSuffix(groupId: String, taskId: String): String {
    return when {
      taskId.startsWith("$groupId-") -> taskId.removePrefix("$groupId-")
      taskId.endsWith("-$groupId") -> taskId.removeSuffix("-$groupId")
      else -> taskId
    }.ifBlank { taskId }
  }

  private fun dependencyTask(
    id: String,
    description: String,
    dependencies: List<String>,
  ): Task =
    SimpleTask(
      id = id,
      description = description,
      customDependencies = dependencies,
      permissions = emptySet(),
    ) { _, _ ->
      TaskResult.success("Synthetic task '$id' completed")
    }
}
