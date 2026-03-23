package io.github.architectplatform.engine.core.project.app

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.project.getKey
import io.github.architectplatform.engine.cloud.CloudReporterService
import io.github.architectplatform.engine.core.config.EngineConfiguration
import io.github.architectplatform.engine.core.plugin.app.PluginLoader
import io.github.architectplatform.engine.core.project.domain.LoadedProjectPlugins
import io.github.architectplatform.engine.core.project.app.repositories.ProjectRepository
import io.github.architectplatform.engine.core.project.domain.Project
import io.github.architectplatform.engine.core.tasks.infrastructure.InMemoryTaskRegistry
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import java.io.File
import java.util.Optional
import kotlin.io.path.Path
import org.slf4j.LoggerFactory

/**
 * Service responsible for managing project lifecycle including registration,
 * loading, and plugin initialization.
 *
 * This service handles:
 * - Loading project configurations from the filesystem
 * - Discovering and loading plugins for each project
 * - Managing project cache when enabled
 * - Supporting nested subproject structures
 *
 * @property projectRepository Repository for storing and retrieving projects
 * @property configLoader Loader for parsing project configuration files
 * @property pluginLoader Loader for discovering and instantiating plugins
 * @property cloudReporter Optional cloud reporter for tracking projects in the cloud
 */
@Singleton
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val configLoader: ConfigLoader,
    private val pluginLoader: PluginLoader,
    private val cloudReporter: Optional<CloudReporterService>,
    private val configValidator: ConfigValidator,
) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  @Property(name = EngineConfiguration.Project.CACHE_ENABLED, defaultValue = "${EngineConfiguration.Project.DEFAULT_CACHE_ENABLED}")
  var cacheEnabled: Boolean = true

  private val objectMapper =
      ObjectMapper().registerKotlinModule().apply { disable(FAIL_ON_UNKNOWN_PROPERTIES) }

  private fun loadProject(name: String, path: String): Project? {
    logger.info("Loading project $name from path $path")
    val projectConfig = configLoader.load(path) ?: return null

    val projectContext = ProjectContext(Path(path), projectConfig)

    val registrationValidation = configValidator.validate(projectConfig)
    if (registrationValidation.errors.isNotEmpty()) {
      throw ConfigValidationException(
          "Invalid architect.yml for project $name:\n${registrationValidation.errors.joinToString("\n")}")
    }

    // Call this method for every subfolder and build the subProjects list
    val subProjects = mutableListOf<Project>()
    val dir = File(path)
    dir.listFiles()?.forEach { file ->
      if (file.isDirectory) {
        val subProject =
            loadProject(
                file.name,
                file.absolutePath,
            )
        if (subProject != null) {
          subProjects.add(subProject)
        }
      }
    }

    logger.debug("Deferring plugin loading for project $name at path $path until task access")
    val lazyPluginLoader = {
      loadPluginsForProject(
        projectName = name,
        projectConfig = projectConfig,
        projectContext = projectContext,
      )
    }

    logger.info(
        "Loaded project $name at path $path with deferred plugins and ${subProjects.size} subprojects")

    return Project(name, path, projectContext, subProjects = subProjects, lazyPluginLoader = lazyPluginLoader)
  }

  private fun loadPluginsForProject(
    projectName: String,
    projectConfig: Map<String, Any>,
    projectContext: ProjectContext,
  ): LoadedProjectPlugins {
    logger.debug("Loading plugins for project $projectName on first task access")
    val plugins = pluginLoader.load(projectContext)
    val taskRegistry = InMemoryTaskRegistry()
    plugins.forEach {
      try {
        val rawContext =
            try {
              if (projectConfig.containsKey(it.contextKey)) {
                logger.debug(
                    "Project: $projectName, plugin ${it.id} - Context key ${it.contextKey} " +
                        "found in project config: ${projectConfig[it.contextKey]}")
                projectConfig[it.contextKey]
              } else {
                logger.debug(
                    "Project: $projectName, plugin ${it.id} - " +
                        "Context key ${it.contextKey} not found in project config, using default context")
                it.context
              }
            } catch (e: Exception) {
              logger.debug(
                  "Project: $projectName, plugin ${it.id} - " +
                      "Error retrieving context for key ${it.contextKey}: ${e.message}")
              null
            }

        logger.debug(
            "Project: $projectName, plugin ${it.id} - Raw context for plugin ${it.id}: $rawContext")
        if (rawContext != null) {
          val pluginContext: Any =
              when (rawContext) {
                is List<*> -> rawContext.map { item -> objectMapper.convertValue(item, it.ctxClass) }
                else -> objectMapper.convertValue(rawContext, it.ctxClass)
              }
                  ?: throw IllegalArgumentException(
                      "Invalid context format for plugin ${it.id}: expected object or list, got ${rawContext::class.qualifiedName}")

          logger.debug(
              "Initializing plugin ${it.id} for project $projectName with context: $pluginContext")
          it.init(pluginContext)
        }
        it.register(taskRegistry)
      } catch (e: Exception) {
        logger.error("Failed to initialize plugin ${it.id} for project $projectName: ${e.message}", e)
      }
    }

    val pluginContextKeys = plugins.map { it.contextKey }.toSet()
    val validation = configValidator.validate(projectConfig, pluginContextKeys)
    validation.warnings.forEach { logger.warn("Project $projectName: $it") }
    validation.errors.forEach { logger.error("Project $projectName: $it") }
    if (validation.errors.isNotEmpty()) {
      throw ConfigValidationException(
          "Invalid architect.yml for project $projectName:\n${validation.errors.joinToString("\n")}")
    }

    return LoadedProjectPlugins(
      plugins = plugins,
      taskRegistry = taskRegistry,
      pluginContextKeys = pluginContextKeys,
    )
  }

  /**
   * Registers a new project in the engine.
   *
   * If the project is already registered, this operation is idempotent and will not
   * re-register the project.
   *
   * @param name The unique name identifier for the project
   * @param path The filesystem path to the project root directory
   * @throws IllegalArgumentException if the project cannot be loaded from the given path
   */
  fun registerProject(name: String, path: String) {
    val project = projectRepository.get(name)
    if (project != null) {
      if (hasLocalPlugins(project.context.config)) {
        logger.debug("Project $name uses local plugins, reloading project state")
        reloadProject(name)
      }
      logger.debug("Project $name already registered at path ${project.path}")
      return
    }
    val newProject =
        loadProject(name, path)
            ?: throw IllegalArgumentException("Failed to load project $name from path $path")
    projectRepository.save(name, newProject)
    
    // Report project to cloud if cloud reporting is enabled
    cloudReporter.ifPresent { reporter ->
      val description = newProject.context.config.getKey<String>("project.description")
      reporter.reportProject(name, path, description)
    }
  }

  fun reloadProject(name: String): Project {
    val existingProject = projectRepository.get(name)
      ?: throw IllegalArgumentException("Project $name is not registered")
    val reloadedProject = loadProject(name, existingProject.path)
      ?: throw IllegalArgumentException("Failed to reload project $name from path ${existingProject.path}")
    projectRepository.save(name, reloadedProject)
    return reloadedProject
  }

  /**
   * Retrieves a project by name.
   *
   * When caching is disabled, the project will be reloaded from the filesystem
   * on each call to ensure the latest configuration.
   *
   * @param name The unique name identifier of the project
   * @return The project instance, or null if not found
   */
  fun getProject(name: String): Project? {
    val project = projectRepository.get(name)
    if (project != null) {
      if (!cacheEnabled) {
        logger.debug("Cache is disabled, reloading project $name")
        return loadProject(name, project.path)
      } else {
        return project
      }
    }
    return null
  }

  /**
   * Returns all registered projects.
   *
   * @return List of all projects currently registered in the engine
   */
  fun getAllProjects(): List<Project> {
    return projectRepository.getAll()
  }

  /**
   * Validates the configuration of an already-registered project.
   *
   * @param name The unique name identifier of the project
   * @return Validation result with errors and warnings
   * @throws IllegalArgumentException if the project is not registered
   */
  fun validateProject(name: String): ValidationResult {
    val project = getProject(name)
        ?: throw IllegalArgumentException("Project $name is not registered")
    return configValidator.validate(project.context.config, project.pluginContextKeys())
  }

  private fun hasLocalPlugins(config: Map<String, Any>): Boolean {
    val plugins = config["plugins"] as? List<*> ?: return false
    return plugins.filterIsInstance<Map<*, *>>().any { plugin ->
      plugin["type"] == "local"
    }
  }
}
