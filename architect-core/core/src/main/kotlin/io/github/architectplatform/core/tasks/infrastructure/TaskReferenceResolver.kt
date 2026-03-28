package io.github.architectplatform.core.tasks.infrastructure

import java.util.LinkedHashSet

/**
 * Shared task-reference resolution for registries and CLI task selection.
 */
object TaskReferenceResolver {
  fun generatedAliases(taskId: String): List<String> {
    val aliases = mutableListOf<String>()
    if (":" in taskId) {
      val hyphenAlias = taskId.replace(':', '-')
      if (hyphenAlias != taskId) aliases += hyphenAlias
      return aliases
    }
    val separatorIndex = taskId.indexOf('-')
    if (separatorIndex <= 0 || separatorIndex == taskId.lastIndex) return emptyList()
    aliases += taskId.substring(0, separatorIndex) + ":" + taskId.substring(separatorIndex + 1)
    return aliases
  }

  fun generatedAliasMap(taskIds: List<String>): Map<String, String> {
    val aliases = linkedMapOf<String, String>()
    taskIds.forEach { taskId ->
      generatedAliases(taskId).forEach { alias ->
        aliases.putIfAbsent(alias, taskId)
      }
    }
    return aliases
  }

  fun resolve(
    reference: String,
    taskIds: List<String>,
    aliases: Map<String, String> = emptyMap(),
    groups: Map<String, List<String>> = emptyMap(),
  ): List<String> {
    val orderedTaskIds = taskIds.distinct()
    val aliasesByTask = aliases.entries.groupBy({ it.value }, { it.key })
    val visitingGroups = mutableListOf<String>()

    fun exact(ref: String): List<String> =
      when {
        ref in orderedTaskIds -> listOf(ref)
        aliases.containsKey(ref) -> listOfNotNull(aliases[ref])
        else -> emptyList()
      }

    lateinit var resolveReference: (String) -> List<String>
    lateinit var resolveGroup: (String) -> List<String>

    fun resolveWildcard(prefix: String): List<String> {
      val resolved = LinkedHashSet<String>()
      if (groups.containsKey(prefix)) {
        resolved.addAll(resolveGroup(prefix))
      }
      orderedTaskIds.forEach { taskId ->
        val accessibleIds = listOf(taskId) + aliasesByTask[taskId].orEmpty()
        if (accessibleIds.any { it.startsWith("$prefix:") }) {
          resolved.add(taskId)
        }
      }
      return resolved.toList()
    }

    fun resolveGroupMember(ref: String): List<String> {
      val separator = ref.indexOf(':')
      if (separator <= 0 || separator == ref.lastIndex) return emptyList()
      val groupId = ref.substring(0, separator)
      val memberReference = ref.substring(separator + 1)
      val members = groups[groupId] ?: return emptyList()
      val directReference =
        members.firstOrNull { member ->
          member == memberReference ||
            member == ref ||
            groupAliasSuffix(groupId, member) == memberReference
        } ?: return emptyList()
      return resolveReference(directReference).ifEmpty {
        throw IllegalArgumentException(
          "Task group '$groupId' member '$memberReference' resolves to no tasks",
        )
      }
    }

    resolveGroup = { groupId ->
      if (groupId in visitingGroups) {
        val cycle = (visitingGroups + groupId).joinToString(" -> ")
        throw IllegalArgumentException("Circular task group reference detected: $cycle")
      }

      val members = groups[groupId]
      if (members == null) {
        emptyList()
      } else {
        visitingGroups += groupId
        try {
          val resolved = LinkedHashSet<String>()
          members.forEach { member ->
            val memberResolution = resolveReference(member)
            if (memberResolution.isEmpty()) {
              throw IllegalArgumentException(
                "Task group '$groupId' references unknown task, alias, or group '$member'",
              )
            }
            resolved.addAll(memberResolution)
          }
          resolved.toList()
        } finally {
          visitingGroups.remove(groupId)
        }
      }
    }

    resolveReference = { ref ->
      val exactMatch = exact(ref)
      if (exactMatch.isNotEmpty()) {
        exactMatch
      } else if (ref.endsWith(":*")) {
        resolveWildcard(ref.removeSuffix(":*"))
      } else {
        val groupMemberResolution = resolveGroupMember(ref)
        if (groupMemberResolution.isNotEmpty()) {
          groupMemberResolution
        } else if (groups.containsKey(ref)) {
          resolveGroup(ref)
        } else {
          emptyList()
        }
      }
    }

    return resolveReference(reference)
  }

  private fun groupAliasSuffix(groupId: String, taskId: String): String {
    return when {
      taskId.startsWith("$groupId-") -> taskId.removePrefix("$groupId-")
      taskId.endsWith("-$groupId") -> taskId.removeSuffix("-$groupId")
      else -> taskId
    }.ifBlank { taskId }
  }
}
