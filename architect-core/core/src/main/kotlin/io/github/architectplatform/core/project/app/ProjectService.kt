package io.github.architectplatform.core.project.app

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.project.getKey
import io.github.architectplatform.core.config.EngineConfiguration
import io.github.architectplatform.core.domain.events.ProjectRegisteredEvent
import io.github.architectplatform.core.plugin.domain.events.ArchitectEventDTO
import io.github.architectplatform.core.plugin.app.PluginLoader
import io.github.architectplatform.core.project.domain.LoadedProjectPlugins
import io.github.architectplatform.core.project.app.repositories.ProjectRepository
import io.github.architectplatform.core.project.domain.ProjectDependencyGraph
import io.github.architectplatform.core.project.domain.Project
import io.github.architectplatform.core.tasks.infrastructure.InMemoryTaskRegistry
import io.github.architectplatform.core.project.infra.YamlLineTracker
import io.github.architectplatform.core.watch.FileWatchService
import java.io.File
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
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
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val configLoader: ConfigLoader,
    private val pluginLoader: PluginLoader,
    private val projectReporter: Optional<ProjectRegistrationReporter>,
    private val configValidator: ConfigValidator,
    private val cacheEnabled: Boolean = EngineConfiguration.Project.DEFAULT_CACHE_ENABLED,
    private val activeProfile: String = "default",
    private val projectWatchDebounceMs: Long = 250,
    private val pluginSecurityStrictMode: Boolean = EngineConfiguration.PluginSecurity.DEFAULT_STRICT_MODE,
    private val eventBus: ((io.github.architectplatform.core.domain.events.ArchitectEvent<*>) -> Unit)? = null,
) {

  private val logger = LoggerFactory.getLogger(this::class.java)
  private val dependencyGraphBuilder = ProjectDependencyGraphBuilder()
  private val registeredProjectPaths = ConcurrentHashMap<String, String>()
  private val invalidatedProjects = ConcurrentHashMap.newKeySet<String>()
  private val projectWatchers = ConcurrentHashMap<String, FileWatchService>()

  private val objectMapper =
      ObjectMapper().registerKotlinModule().apply { disable(FAIL_ON_UNKNOWN_PROPERTIES) }

  private fun loadProject(name: String, path: String): Project? {
    logger.info("Loading project $name from path $path")
    val loadResult = configLoader.loadWithRaw(path, activeProfile) ?: return null
    val rawConfig = loadResult.config
    val lineMap = YamlLineTracker.trackLines(loadResult.rawYaml)

    // Apply environment profile (deep-merge profile config on top of root)
    val projectConfig = ProfileMerger.merge(rawConfig, activeProfile)
    if (activeProfile != "default") {
      logger.info("Applied profile '$activeProfile' to project $name")
    }

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
        lineMap = lineMap,
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
    lineMap: Map<String, Int>,
  ): LoadedProjectPlugins {
    logger.debug("Loading plugins for project $projectName on first task access")
    val plugins = pluginLoader.load(projectContext)
    val taskRegistry = InMemoryTaskRegistry()
    val tasksByPluginId = mutableMapOf<String, MutableSet<String>>()
      plugins.forEach {
        try {
          val rawContext =
            try {
              if (it.id == "inline-tasks") {
                mapOf(
                  "tasks" to (projectConfig["tasks"] ?: emptyMap<String, Any>()),
                  "templates" to (projectConfig["templates"] ?: emptyMap<String, Any>()),
                )
              } else if (projectConfig.containsKey(it.contextKey)) {
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
        val beforeTaskIds = taskRegistry.all().map { task -> task.id }.toSet()
        it.register(taskRegistry)
        val afterTaskIds = taskRegistry.all().map { task -> task.id }.toSet()
        val registeredTaskIds = (afterTaskIds - beforeTaskIds)
        if (registeredTaskIds.isNotEmpty()) {
          tasksByPluginId.getOrPut(it.id) { mutableSetOf() }.addAll(registeredTaskIds)
          registerPluginNamespaceAliases(taskRegistry, it.id, it.contextKey, registeredTaskIds)
        }
      } catch (e: Exception) {
        logger.error("Failed to initialize plugin ${it.id} for project $projectName: ${e.message}", e)
      }
    }

    registerConfiguredGroups(taskRegistry, projectConfig)

    val pluginContextKeys = plugins.map { it.contextKey }.toSet()
    val validation = configValidator.validate(projectConfig, pluginContextKeys, plugins, lineMap)
    val policyValidation =
      PluginPermissionPolicyValidator.validate(
        plugins = plugins,
        taskRegistry = taskRegistry,
        tasksByPluginId = tasksByPluginId,
        strictMode = pluginSecurityStrictMode,
      )
    validation.warnings.forEach { logger.warn("Project $projectName: $it") }
    policyValidation.warnings.forEach { logger.warn("Project $projectName: $it") }
    validation.errors.forEach { logger.error("Project $projectName: $it") }
    policyValidation.errors.forEach { logger.error("Project $projectName: $it") }

    val allErrors = validation.errors + policyValidation.errors
    if (allErrors.isNotEmpty()) {
      throw ConfigValidationException(
          "Invalid architect.yml for project $projectName:\n${allErrors.joinToString("\n")}")
    }

    return LoadedProjectPlugins(
      plugins = plugins,
      taskRegistry = taskRegistry,
      pluginContextKeys = pluginContextKeys,
    )
  }

  private fun registerPluginNamespaceAliases(
    taskRegistry: InMemoryTaskRegistry,
    pluginId: String,
    contextKey: String,
    taskIds: Set<String>,
  ) {
    val namespace =
      contextKey.trim().takeIf { it.isNotBlank() }
        ?: pluginId.removeSuffix("-architected").takeIf { it.isNotBlank() }
        ?: return
    taskIds.sorted().forEach { taskId ->
      val aliasSuffix = namespacedAliasSuffix(taskId, namespace) ?: return@forEach
      taskRegistry.addAlias(
        aliasId = "$namespace:$aliasSuffix",
        targetId = taskId,
        description = "Plugin alias for $taskId",
      )
    }
  }

  private fun registerConfiguredGroups(
    taskRegistry: InMemoryTaskRegistry,
    projectConfig: Map<String, Any>,
  ) {
    val rawGroups = projectConfig["groups"] as? Map<*, *> ?: return
    rawGroups.forEach { (rawGroupId, rawMembers) ->
      val groupId = rawGroupId as? String ?: return@forEach
      val memberIds = (rawMembers as? List<*>)?.mapNotNull { it as? String }?.distinct().orEmpty()
      if (memberIds.isEmpty()) return@forEach

      taskRegistry.addGroup(
        groupId = groupId,
        memberIds = memberIds,
        description = "Task group '$groupId' runs ${memberIds.joinToString(", ")}",
      )

      memberIds.forEach { memberId ->
        val aliasSuffix = groupAliasSuffix(groupId, memberId)
        taskRegistry.addAlias(
          aliasId = "$groupId:$aliasSuffix",
          targetId = memberId,
          description = "Group member alias for $memberId",
        )
      }
    }
  }

  private fun namespacedAliasSuffix(taskId: String, namespace: String): String? {
    val normalizedNamespace = namespace.trim().takeIf { it.isNotBlank() } ?: return null
    return when {
      taskId.contains(":") -> null
      taskId == normalizedNamespace -> null
      taskId.startsWith("$normalizedNamespace-") -> taskId.removePrefix("$normalizedNamespace-")
      taskId.endsWith("-$normalizedNamespace") -> taskId.removeSuffix("-$normalizedNamespace")
      else -> null
    }?.takeIf { it.isNotBlank() }
  }

  private fun groupAliasSuffix(groupId: String, taskId: String): String {
    return when {
      taskId.startsWith("$groupId-") -> taskId.removePrefix("$groupId-")
      taskId.endsWith("-$groupId") -> taskId.removeSuffix("-$groupId")
      else -> taskId
    }.ifBlank { taskId }
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
    registeredProjectPaths[name] = path
    val project = projectRepository.get(name)
    if (project != null) {
      if (hasLocalPlugins(project.context.config)) {
        logger.debug("Project $name uses local plugins, reloading project state")
        reloadProject(name)
      } else if (cacheEnabled && invalidatedProjects.remove(name)) {
        logger.debug("Project $name cache invalidated, reloading project state")
        reloadProject(name)
      }
      logger.debug("Project $name already registered at path ${project.path}")
      return
    }
    val newProject =
        loadProject(name, path)
            ?: throw IllegalArgumentException("Failed to load project $name from path $path")
    projectRepository.save(name, newProject)
    startProjectWatcher(name, path)
    eventBus?.invoke(
      ArchitectEventDTO(
        id = "project.registered",
        event = ProjectRegisteredEvent(projectName = name, projectPath = path),
      ),
    )
    
    // Report project if an optional reporter is configured by the host runtime
    projectReporter.ifPresent { reporter ->
      val description = newProject.context.config.getKey<String>("project.description")
      reporter.reportProject(name, path, description)
    }
  }

  /**
   * Reloads a previously registered project from disk.
   *
   * @param name The unique name identifier of the project
   * @return The reloaded project
   * @throws IllegalArgumentException if the project is not registered or cannot be reloaded
   */
  fun reloadProject(name: String): Project {
    val existingProject = projectRepository.get(name)
      ?: throw IllegalArgumentException("Project $name is not registered")
    val reloadedProject = loadProject(name, existingProject.path)
      ?: throw IllegalArgumentException("Failed to reload project $name from path ${existingProject.path}")
    projectRepository.save(name, reloadedProject)
    invalidatedProjects.remove(name)
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
      if (!cacheEnabled || invalidatedProjects.remove(name)) {
        logger.debug("Cache is disabled, reloading project $name")
        val reloaded = loadProject(name, project.path) ?: return null
        projectRepository.save(name, reloaded)
        return reloaded
      } else {
        return project
      }
    }
    return null
  }

  private fun startProjectWatcher(name: String, path: String) {
    if (!cacheEnabled || projectWatchers.containsKey(name)) {
      return
    }

    val watcher =
      FileWatchService(
        rootPath = Path(path),
        debounceMs = projectWatchDebounceMs,
      ) { changedPath ->
        logger.debug("Detected change for project $name at $changedPath, invalidating cache entry")
        invalidatedProjects.add(name)
      }

    val existing = projectWatchers.putIfAbsent(name, watcher)
    if (existing == null) {
      watcher.startAsync()
    }
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
    return configValidator.validate(project.context.config, project.pluginContextKeys(), project.plugins)
  }

  /**
   * Builds a dependency graph for the registered project and its discovered subprojects.
   */
  fun buildDependencyGraph(name: String): ProjectDependencyGraph {
    val project = getProject(name)
      ?: throw IllegalArgumentException("Project $name is not registered")
    return dependencyGraphBuilder.build(project)
  }

  private fun hasLocalPlugins(config: Map<String, Any>): Boolean {
    val plugins = config["plugins"] as? List<*> ?: return false
    return plugins.filterIsInstance<Map<*, *>>().any { plugin ->
      plugin["type"] == "local"
    }
  }
}
