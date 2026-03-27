package io.github.architectplatform.engine.core.project.app

import io.github.architectplatform.engine.cloud.CloudReporterService
import io.github.architectplatform.core.config.EngineConfiguration
import io.github.architectplatform.engine.core.events.MicronautArchitectEventBus
import io.github.architectplatform.core.plugin.app.CommonPlugin
import io.github.architectplatform.core.plugin.app.GpgCommandRunner
import io.github.architectplatform.core.plugin.app.GpgPluginSignatureVerifier
import io.github.architectplatform.core.plugin.app.PluginDownloader
import io.github.architectplatform.core.plugin.app.PluginLoader
import io.github.architectplatform.core.plugin.app.ProjectPluginLoader
import io.github.architectplatform.core.plugin.app.SpiPluginLoader
import io.github.architectplatform.core.plugin.infra.CachedPluginDownloader
import io.github.architectplatform.core.plugin.infra.GitHubReleaseResolver
import io.github.architectplatform.engine.core.plugin.infra.MicronautRemoteContentFetcher
import io.github.architectplatform.core.project.app.ports.ConfigParser
import io.github.architectplatform.core.project.app.repositories.ProjectRepository
import io.github.architectplatform.core.project.infra.InMemoryProjectRepository
import io.github.architectplatform.core.project.infra.YamlConfigParser
import io.github.architectplatform.core.plugins.commits.CommitsPluginProvider
import io.github.architectplatform.core.plugins.inline.InlineTaskPluginProvider
import io.github.architectplatform.core.plugins.installers.InstallersPluginProvider
import io.github.architectplatform.core.plugins.workflows.code.CodePluginProvider
import io.github.architectplatform.core.plugins.workflows.core.CorePluginProvider
import io.github.architectplatform.core.plugins.workflows.hooks.HooksPluginProvider
import io.github.architectplatform.core.project.app.ConfigLoader
import io.github.architectplatform.core.project.app.ConfigValidator
import io.github.architectplatform.core.project.app.ProjectRegistrationReporter
import io.github.architectplatform.core.project.app.ProjectService
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import java.util.Optional

@Factory
class ProjectServiceFactory {

  @Singleton
  fun projectRepository(): ProjectRepository = InMemoryProjectRepository()

  @Singleton
  fun configParser(): ConfigParser = YamlConfigParser()

  @Singleton
  fun configLoader(configParser: ConfigParser): ConfigLoader = ConfigLoader(configParser)

  @Singleton
  fun configValidator(): ConfigValidator = ConfigValidator()

  @Singleton
  fun spiPluginLoader(): SpiPluginLoader = SpiPluginLoader()

  @Singleton
  fun corePluginProvider(): CommonPlugin = CorePluginProvider()

  @Singleton
  fun codePluginProvider(): CommonPlugin = CodePluginProvider()

  @Singleton
  fun hooksPluginProvider(): CommonPlugin = HooksPluginProvider()

  @Singleton
  fun inlineTaskPluginProvider(): CommonPlugin = InlineTaskPluginProvider()

  @Singleton
  fun commitsPluginProvider(): CommonPlugin = CommitsPluginProvider()

  @Singleton
  fun installersPluginProvider(): CommonPlugin = InstallersPluginProvider()

  @Singleton
  fun gpgCommandRunner(): GpgCommandRunner = GpgCommandRunner()

  @Singleton
  fun pluginSignatureVerifier(commandRunner: GpgCommandRunner): GpgPluginSignatureVerifier =
    GpgPluginSignatureVerifier(commandRunner)

  @Singleton
  fun pluginDownloader(
    remoteContentFetcher: MicronautRemoteContentFetcher,
    eventBus: MicronautArchitectEventBus,
  ): PluginDownloader =
    CachedPluginDownloader(
      remoteContentFetcher = remoteContentFetcher,
      eventBus = eventBus::invoke,
    )

  @Singleton
  fun gitHubReleaseResolver(
    remoteContentFetcher: MicronautRemoteContentFetcher,
    @Property(
      name = EngineConfiguration.PluginLoader.USER_AGENT,
      defaultValue = EngineConfiguration.PluginLoader.DEFAULT_USER_AGENT,
    )
    userAgent: String,
  ): GitHubReleaseResolver =
    GitHubReleaseResolver(
      remoteContentFetcher = remoteContentFetcher,
      userAgent = userAgent,
    )

  @Singleton
  fun pluginLoader(
    spiLoader: SpiPluginLoader,
    downloader: PluginDownloader,
    signatureVerifier: GpgPluginSignatureVerifier,
    internalPlugins: List<CommonPlugin>,
    releaseResolver: GitHubReleaseResolver,
    eventBus: MicronautArchitectEventBus,
  ): PluginLoader =
    ProjectPluginLoader(
      spiLoader = spiLoader,
      downloader = downloader,
      signatureVerifier = signatureVerifier,
      internalPlugins = internalPlugins,
      releaseResolver = releaseResolver,
      eventBus = eventBus::invoke,
    )

  @Singleton
  fun projectRegistrationReporter(
    cloudReporter: Optional<CloudReporterService>,
  ): Optional<ProjectRegistrationReporter> =
    cloudReporter.map { reporter ->
      ProjectRegistrationReporter { name, path, description ->
        reporter.reportProject(name, path, description)
      }
    }

  @Singleton
  fun projectService(
    projectRepository: ProjectRepository,
    configLoader: ConfigLoader,
    pluginLoader: PluginLoader,
    eventBus: MicronautArchitectEventBus,
    projectReporter: Optional<ProjectRegistrationReporter>,
    configValidator: ConfigValidator,
    @Property(
      name = EngineConfiguration.Project.CACHE_ENABLED,
      defaultValue = "true",
    )
    cacheEnabled: Boolean,
    @Property(
      name = EngineConfiguration.PluginSecurity.STRICT_MODE,
      defaultValue = "false",
    )
    pluginSecurityStrictMode: Boolean,
  ): ProjectService =
    ProjectService(
      projectRepository = projectRepository,
      configLoader = configLoader,
      pluginLoader = pluginLoader,
      projectReporter = projectReporter,
      configValidator = configValidator,
      cacheEnabled = cacheEnabled,
      pluginSecurityStrictMode = pluginSecurityStrictMode,
      eventBus = eventBus::invoke,
    )
}
