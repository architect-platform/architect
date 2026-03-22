package io.github.architectplatform.api.core.tasks.cache

/**
 * Describes the cacheable inputs and outputs of a task.
 *
 * When a task declares a [CacheDescriptor], the executor can compute a deterministic
 * cache key from all inputs. If a matching cached result exists, the task is skipped.
 *
 * @property inputs Items that contribute to the cache key (files, config values, env vars, etc.)
 * @property outputs Items produced by the task that should be stored in the cache
 */
data class CacheDescriptor(
  val inputs: List<CacheInput>,
  val outputs: List<CacheOutput> = emptyList(),
)

/**
 * A single input that contributes to a task's cache key.
 */
sealed class CacheInput {
  /**
   * A set of files matched by a glob pattern. The hash is computed from the content
   * of all matched files.
   */
  data class FileSet(val glob: String) : CacheInput()

  /**
   * A project configuration value. The hash is computed from the resolved value.
   */
  data class ConfigValue(val key: String) : CacheInput()

  /**
   * An environment variable. The hash is computed from the current value (or "unset" sentinel).
   */
  data class EnvVar(val name: String) : CacheInput()

  /**
   * The stdout of a shell command. Useful for tool-version inputs (e.g. `java -version`).
   * The hash is computed from the command output.
   */
  data class CommandOutput(val command: String) : CacheInput()
}

/**
 * A single output produced by a task that should be stored in the cache.
 */
sealed class CacheOutput {
  /**
   * A set of output files matched by a glob pattern.
   */
  data class FileSet(val glob: String) : CacheOutput()

  /**
   * Standard output captured during task execution.
   */
  data object Stdout : CacheOutput()
}
