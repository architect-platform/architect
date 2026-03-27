package io.github.architectplatform.core.execution

import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.core.config.EngineConfiguration
import io.github.architectplatform.core.events.EmbeddedEventBus
import io.github.architectplatform.core.history.app.HistoryService
import io.github.architectplatform.core.plugin.app.CommonPlugin
import io.github.architectplatform.core.plugin.app.GpgCommandRunner
import io.github.architectplatform.core.plugin.app.GpgPluginSignatureVerifier
import io.github.architectplatform.core.plugin.app.PluginSourceRegistry
import io.github.architectplatform.core.plugin.app.ProjectPluginLoader
import io.github.architectplatform.core.plugin.app.RemoteContentFetcher
import io.github.architectplatform.core.plugin.app.SpiPluginLoader
import io.github.architectplatform.core.plugin.infra.CachedPluginDownloader
import io.github.architectplatform.core.plugin.infra.GitHubPluginSource
import io.github.architectplatform.core.plugin.infra.GitHubReleaseResolver
import io.github.architectplatform.core.plugin.infra.HttpPluginSource
import io.github.architectplatform.core.plugin.infra.LocalPluginSource
import io.github.architectplatform.core.plugin.infra.RegistryPluginSource
import io.github.architectplatform.core.project.app.ApplicationEnvironment
import io.github.architectplatform.core.project.app.ConfigLoader
import io.github.architectplatform.core.project.app.ConfigValidator
import io.github.architectplatform.core.project.app.ProjectRegistrationReporter
import io.github.architectplatform.core.project.app.ProjectService
import io.github.architectplatform.core.project.infra.InMemoryProjectRepository
import io.github.architectplatform.core.project.infra.YamlConfigParser
import io.github.architectplatform.core.tasks.application.TaskCache
import io.github.architectplatform.core.tasks.application.TaskExecutor
import io.github.architectplatform.core.domain.events.ArchitectEvent
import io.github.architectplatform.core.plugins.commits.CommitsPluginProvider
import io.github.architectplatform.core.plugins.inline.InlineTaskPluginProvider
import io.github.architectplatform.core.plugins.installers.InstallersPluginProvider
import io.github.architectplatform.core.plugins.workflows.code.CodePluginProvider
import io.github.architectplatform.core.plugins.workflows.core.CorePluginProvider
import io.github.architectplatform.core.plugins.workflows.hooks.HooksPluginProvider
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
  fun getAllTasks(projectName: String): List<Task> {
    val project = projectService.getProject(projectName)
      ?: throw IllegalArgumentException("Project not found: $projectName")
    return project.taskRegistry.all().sortedBy { it.id }
  }

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
      outputCacheEnabled: Boolean = false,
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
          outputCache = if (outputCacheEnabled) io.github.architectplatform.core.tasks.application.LocalOutputCache() else null,
          outputCacheEnabled = outputCacheEnabled,
        )

      val configLoader = ConfigLoader(YamlConfigParser())
      val configValidator = ConfigValidator()
      val releaseResolver = GitHubReleaseResolver(remoteContentFetcher)
      val downloader = CachedPluginDownloader(remoteContentFetcher, eventBus::invoke)
      val signatureVerifier = GpgPluginSignatureVerifier(GpgCommandRunner())
      val spiLoader = SpiPluginLoader()
      val pluginLoader =
        ProjectPluginLoader(
          spiLoader = spiLoader,
          downloader = downloader,
          signatureVerifier = signatureVerifier,
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
          pluginSecurityStrictMode = EngineConfiguration.PluginSecurity.DEFAULT_STRICT_MODE,
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
