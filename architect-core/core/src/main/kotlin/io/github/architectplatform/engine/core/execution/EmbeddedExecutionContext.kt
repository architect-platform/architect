package io.github.architectplatform.engine.core.execution

import io.github.architectplatform.engine.core.config.EngineConfiguration
import io.github.architectplatform.engine.core.events.EmbeddedEventBus
import io.github.architectplatform.engine.core.history.app.HistoryService
import io.github.architectplatform.engine.core.plugin.app.CommonPlugin
import io.github.architectplatform.engine.core.plugin.app.PluginSourceRegistry
import io.github.architectplatform.engine.core.plugin.app.ProjectPluginLoader
import io.github.architectplatform.engine.core.plugin.app.RemoteContentFetcher
import io.github.architectplatform.engine.core.plugin.app.SpiPluginLoader
import io.github.architectplatform.engine.core.plugin.infra.CachedPluginDownloader
import io.github.architectplatform.engine.core.plugin.infra.GitHubPluginSource
import io.github.architectplatform.engine.core.plugin.infra.GitHubReleaseResolver
import io.github.architectplatform.engine.core.plugin.infra.HttpPluginSource
import io.github.architectplatform.engine.core.plugin.infra.LocalPluginSource
import io.github.architectplatform.engine.core.plugin.infra.RegistryPluginSource
import io.github.architectplatform.engine.core.project.app.ApplicationEnvironment
import io.github.architectplatform.engine.core.project.app.ConfigLoader
import io.github.architectplatform.engine.core.project.app.ConfigValidator
import io.github.architectplatform.engine.core.project.app.ProjectRegistrationReporter
import io.github.architectplatform.engine.core.project.app.ProjectService
import io.github.architectplatform.engine.core.project.infra.InMemoryProjectRepository
import io.github.architectplatform.engine.core.project.infra.YamlConfigParser
import io.github.architectplatform.engine.core.tasks.application.TaskCache
import io.github.architectplatform.engine.core.tasks.application.TaskExecutor
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.github.architectplatform.engine.plugins.commits.CommitsPluginProvider
import io.github.architectplatform.engine.plugins.inline.InlineTaskPluginProvider
import io.github.architectplatform.engine.plugins.installers.InstallersPluginProvider
import io.github.architectplatform.engine.plugins.workflows.code.CodePluginProvider
import io.github.architectplatform.engine.plugins.workflows.core.CorePluginProvider
import io.github.architectplatform.engine.plugins.workflows.hooks.HooksPluginProvider
import java.util.Optional

/**
 * Wires core services for embedded execution without any DI container.
 */
class EmbeddedExecutionContext private constructor(
  val eventBus: EmbeddedEventBus<ArchitectEvent<*>>,
  val environment: ApplicationEnvironment,
  val taskExecutor: TaskExecutor,
  val projectService: ProjectService,
  val historyService: HistoryService,
  val pluginSourceRegistry: PluginSourceRegistry,
) {
  companion object {
    fun create(
      remoteContentFetcher: RemoteContentFetcher,
      internalPlugins: List<CommonPlugin> = defaultPlugins(),
      projectReporter: Optional<ProjectRegistrationReporter> = Optional.empty(),
      projectCacheEnabled: Boolean = EngineConfiguration.Project.DEFAULT_CACHE_ENABLED,
      taskCacheEnabled: Boolean = false,
      parallelExecutionEnabled: Boolean = EngineConfiguration.TaskExecution.DEFAULT_PARALLEL_ENABLED,
      activeProfile: String = "default",
      classloaderDebug: Boolean = false,
    ): EmbeddedExecutionContext {
      val eventBus = EmbeddedEventBus<ArchitectEvent<*>>()
      val historyService = HistoryService()
      val commandExecutor = BashCommandExecutor()
      val environment =
        ApplicationEnvironment(
          services = mapOf(
            HistoryService::class.java to historyService,
            io.github.architectplatform.api.components.execution.CommandExecutor::class.java to commandExecutor,
          ),
          eventBus = { event -> if (event is ArchitectEvent<*>) eventBus(event) },
          activeProfile = activeProfile,
        )
      val taskExecutor =
        TaskExecutor(
          environment = environment,
          taskCache = TaskCache(taskCacheEnabled),
          eventBus = eventBus::invoke,
          parallelExecutionEnabled = parallelExecutionEnabled,
        )

      val configLoader = ConfigLoader(YamlConfigParser())
      val configValidator = ConfigValidator()
      val releaseResolver = GitHubReleaseResolver(remoteContentFetcher)
      val downloader = CachedPluginDownloader(remoteContentFetcher, eventBus::invoke)
      val spiLoader = SpiPluginLoader()
      val pluginLoader =
        ProjectPluginLoader(
          spiLoader = spiLoader,
          downloader = downloader,
          internalPlugins = internalPlugins,
          releaseResolver = releaseResolver,
          eventBus = eventBus::invoke,
          classloaderDebug = classloaderDebug,
        )

      val projectService =
        ProjectService(
          projectRepository = InMemoryProjectRepository(),
          configLoader = configLoader,
          pluginLoader = pluginLoader,
          projectReporter = projectReporter,
          configValidator = configValidator,
          cacheEnabled = projectCacheEnabled,
          activeProfile = activeProfile,
        )

      val pluginSourceRegistry =
        PluginSourceRegistry(
          listOf(
            LocalPluginSource(),
            GitHubPluginSource(downloader, releaseResolver),
            RegistryPluginSource(remoteContentFetcher, downloader),
            HttpPluginSource(downloader),
          ),
        )

      return EmbeddedExecutionContext(
        eventBus = eventBus,
        environment = environment,
        taskExecutor = taskExecutor,
        projectService = projectService,
        historyService = historyService,
        pluginSourceRegistry = pluginSourceRegistry,
      )
    }

    private fun defaultPlugins(): List<CommonPlugin> =
      listOf(
        CorePluginProvider(),
        CodePluginProvider(),
        HooksPluginProvider(),
        InlineTaskPluginProvider(),
        CommitsPluginProvider(),
        InstallersPluginProvider(),
      )
  }
}
