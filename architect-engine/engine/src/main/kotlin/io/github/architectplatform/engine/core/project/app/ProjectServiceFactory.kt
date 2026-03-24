package io.github.architectplatform.engine.core.project.app

import io.github.architectplatform.engine.cloud.CloudReporterService
import io.github.architectplatform.engine.core.config.EngineConfiguration
import io.github.architectplatform.engine.core.plugin.app.PluginLoader
import io.github.architectplatform.engine.core.project.app.repositories.ProjectRepository
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import java.util.Optional

@Factory
class ProjectServiceFactory {

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
    projectReporter: Optional<ProjectRegistrationReporter>,
    configValidator: ConfigValidator,
    @Property(
      name = EngineConfiguration.Project.CACHE_ENABLED,
      defaultValue = "true",
    )
    cacheEnabled: Boolean,
  ): ProjectService =
    ProjectService(
      projectRepository = projectRepository,
      configLoader = configLoader,
      pluginLoader = pluginLoader,
      projectReporter = projectReporter,
      configValidator = configValidator,
      cacheEnabled = cacheEnabled,
    )
}
