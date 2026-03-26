package io.github.architectplatform.api.core.tasks

import java.time.Duration
import java.time.Instant

/**
 * Metadata about a task execution, capturing timing, exit code, and executor information.
 *
 * This data class provides structured information about how a task was executed,
 * enabling performance monitoring, debugging, and inter-task data analysis.
 *
 * Example usage:
 * ```kotlin
 * val metadata = TaskMetadata(
 *   duration = Duration.ofMillis(1234),
 *   exitCode = 0,
 *   startedAt = Instant.now().minusMillis(1234),
 *   finishedAt = Instant.now(),
 *   executorInfo = "BashCommandExecutor"
 * )
 * TaskResult.success("Build completed", metadata = metadata)
 * ```
 *
 * @property duration The wall-clock duration of the task execution
 * @property exitCode The process exit code, if applicable (null for non-process tasks)
 * @property startedAt The instant when the task execution began
 * @property finishedAt The instant when the task execution completed
 * @property executorInfo Optional identifier of the executor that ran the task
 */
data class TaskMetadata(
  val duration: Duration? = null,
  val exitCode: Int? = null,
  val startedAt: Instant? = null,
  val finishedAt: Instant? = null,
  val executorInfo: String? = null,
)
