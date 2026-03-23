package io.github.architectplatform.api.core.tasks

/**
 * Declares a coarse-grained capability that a task may require at execution time.
 *
 * The engine uses these permissions to decide whether a task may launch subprocesses and,
 * when possible, to narrow subprocess sandboxing to the minimum requested capabilities.
 */
enum class TaskPermission(
  val wireName: String,
) {
  FILE_SYSTEM_READ("file-system:read"),
  FILE_SYSTEM_WRITE("file-system:write"),
  NETWORK_OUTBOUND("network:outbound"),
  PROCESS_EXEC("process:exec"),
  ;

  override fun toString(): String = wireName

  companion object {
    fun all(): Set<TaskPermission> = entries.toSet()

    fun fromWireName(value: String): TaskPermission =
      entries.firstOrNull { it.wireName == value }
        ?: throw IllegalArgumentException("Unknown task permission '$value'")

    fun fromWireNames(values: Collection<String>): Set<TaskPermission> =
      if (values.isEmpty()) {
        all()
      } else {
        values.map(::fromWireName).toSet()
      }
  }
}