package io.github.architectplatform.engine.core.tasks

import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.core.config.EngineConfiguration
import io.github.architectplatform.engine.core.events.MicronautArchitectEventBus
import io.github.architectplatform.core.execution.BashCommandExecutor
import io.github.architectplatform.core.history.app.HistoryService
import io.github.architectplatform.core.tasks.application.LocalOutputCache
import io.github.architectplatform.core.tasks.application.RemoteOutputCache
import io.github.architectplatform.core.tasks.application.TaskCache
import io.github.architectplatform.core.tasks.application.TaskExecutor
import io.github.architectplatform.core.tasks.domain.TaskDependencyResolver
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import java.util.Optional

@Factory
class RuntimeServiceFactory {

  @Singleton
  fun historyService(): HistoryService = HistoryService()

  @Singleton
  fun bashCommandExecutor(
    @Property(
      name = EngineConfiguration.CommandExecutor.TIMEOUT_SECONDS,
      defaultValue = "${EngineConfiguration.CommandExecutor.DEFAULT_TIMEOUT_SECONDS}",
    )
    timeoutSeconds: Long,
    @Property(
      name = EngineConfiguration.CommandExecutor.REDIRECT_ERROR_STREAM,
      defaultValue = "${EngineConfiguration.CommandExecutor.DEFAULT_REDIRECT_ERROR_STREAM}",
    )
    redirectErrorStream: Boolean,
  ): BashCommandExecutor =
    BashCommandExecutor(
      timeoutSeconds = timeoutSeconds,
      redirectErrorStream = redirectErrorStream,
    )

  @Singleton
  fun taskCache(
    @Property(
      name = EngineConfiguration.Cache.ENABLED,
      defaultValue = "${EngineConfiguration.Cache.DEFAULT_ENABLED}",
    )
    cacheEnabled: Boolean,
    @Property(
      name = EngineConfiguration.Cache.TTL_SECONDS,
      defaultValue = "${EngineConfiguration.Cache.DEFAULT_TTL_SECONDS}",
    )
    ttlSeconds: Long,
  ): TaskCache =
    TaskCache(
      cacheEnabled = cacheEnabled,
      ttlSeconds = ttlSeconds,
    )

  @Singleton
  fun taskExecutor(
    environment: Environment,
    taskCache: TaskCache,
    eventBus: MicronautArchitectEventBus,
    @Property(
      name = EngineConfiguration.TaskExecution.PARALLEL_ENABLED,
      defaultValue = "${EngineConfiguration.TaskExecution.DEFAULT_PARALLEL_ENABLED}",
    )
    parallelExecutionEnabled: Boolean,
    @Property(
      name = EngineConfiguration.TaskExecution.MAX_CONCURRENT_TASKS,
      defaultValue = "${EngineConfiguration.TaskExecution.DEFAULT_MAX_CONCURRENT_TASKS}",
    )
    maxConcurrentTasks: Int,
    localOutputCache: Optional<LocalOutputCache>,
    remoteOutputCache: Optional<RemoteOutputCache>,
  ): TaskExecutor =
    TaskExecutor(
      environment = environment,
      taskCache = taskCache,
      eventBus = eventBus::invoke,
      dependencyResolver = TaskDependencyResolver(),
      parallelExecutionEnabled = parallelExecutionEnabled,
      maxConcurrentTasks = maxConcurrentTasks,
      outputCache = localOutputCache.orElse(null),
      outputCacheEnabled = localOutputCache.isPresent,
      remoteOutputCache = remoteOutputCache.orElse(null),
    )
}
