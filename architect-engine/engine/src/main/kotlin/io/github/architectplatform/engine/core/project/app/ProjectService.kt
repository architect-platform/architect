package io.github.architectplatform.engine.core.project.app

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.engine.core.plugin.app.PluginLoader
import io.github.architectplatform.engine.core.project.app.repositories.ProjectRepository
import io.github.architectplatform.engine.core.project.domain.Project
import io.github.architectplatform.engine.core.tasks.infrastructure.InMemoryTaskRegistry
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import java.io.File
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
 */
@Singleton
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val configLoader: ConfigLoader,
    private val pluginLoader: PluginLoader,
) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  @Property(name = "architect.engine.core.project.cache.enabled", defaultValue = "true")
  var cacheEnabled: Boolean = true

  private val objectMapper =
      ObjectMapper().registerKotlinModule().apply { disable(FAIL_ON_UNKNOWN_PROPERTIES) }

  private fun loadProject(name: String, path: String): Project? {
    logger.info("Loading project $name from path $path")
    val projectConfig = configLoader.load(path) ?: return null
    val projectContext = ProjectContext(Path(path), projectConfig)

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

    logger.debug("Loading plugins for project $name at path $path")
    val plugins = pluginLoader.load(projectContext)
    val taskRegistry = InMemoryTaskRegistry()
    plugins.forEach {
      try {
        val rawContext =
            try {
              if (projectConfig.containsKey(it.contextKey)) {
                logger.debug(
                    "Project: $name, plugin ${it.id} - Context key ${it.contextKey} " +
                        "found in project config: ${projectConfig[it.contextKey]}")
                projectConfig[it.contextKey]
              } else {
                logger.debug(
                    "Project: $name, plugin ${it.id} - " +
                        "Context key ${it.contextKey} not found in project config, using default context")
                it.context
              }
            } catch (e: Exception) {
              logger.debug(
                  "Project: $name, plugin ${it.id} - " +
                      "Error retrieving context for key ${it.contextKey}: ${e.message}")
              null
            }

        logger.debug(
            "Project: $name, plugin ${it.id} - " + "Raw context for plugin ${it.id}: $rawContext")
        if (rawContext != null) {
          val pluginContext: Any =
              when (rawContext) {
                is List<*> -> {
                  // Config contains a list, so we deserialize as List<ctxClass>
                  rawContext.map { item -> objectMapper.convertValue(item, it.ctxClass) }
                }
                else -> {
                  // Config contains a single object (map), deserialize as ctxClass
                  objectMapper.convertValue(rawContext, it.ctxClass)
                }
              }
                  ?: throw IllegalArgumentException(
                      "Invalid context format for plugin ${it.id}: " +
                          "expected object or list, got ${rawContext::class.qualifiedName}")

          logger.debug(
              "Initializing plugin ${it.id} for project $name with context: $pluginContext")
          it.init(pluginContext)
        }
        it.register(taskRegistry)
      } catch (e: Exception) {
        logger.error("Failed to initialize plugin ${it.id} for project $name: ${e.message}", e)
      }
    }

    logger.info(
        "Loaded project $name at path $path with ${plugins.size} plugins and ${subProjects.size} subprojects")
    return Project(name, path, projectContext, plugins, subProjects, taskRegistry)
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
      logger.debug("Project $name already registered at path ${project.path}")
      return
    }
    val newProject =
        loadProject(name, path)
            ?: throw IllegalArgumentException("Failed to load project $name from path $path")
    projectRepository.save(name, newProject)
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
}
