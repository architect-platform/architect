package io.github.architectplatform.api.testing

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor

class ArchitectPluginTestKit<C : Any>(
  private val plugin: ArchitectPlugin<C>,
  private val projectDir: Path = Path.of("."),
  private val activeProfile: String = "default",
) {
  private val taskRegistry = InMemoryTaskRegistry()
  private val services = mutableMapOf<Class<*>, Any>()
  private val publishedEvents = mutableListOf<Any>()
  private val projectConfig = mutableMapOf<String, Any>()
  private var registered = false

  fun configure(config: Any): ArchitectPluginTestKit<C> {
    plugin.init(convertConfig(config))
    registered = false
    return this
  }

  fun withProjectConfig(config: Map<String, Any>): ArchitectPluginTestKit<C> {
    projectConfig.clear()
    projectConfig.putAll(config)
    return this
  }

  fun <T : Any> withService(type: Class<T>, service: T): ArchitectPluginTestKit<C> {
    services[type] = service
    return this
  }

  fun tasks(): List<Task> {
    ensureRegistered()
    return taskRegistry.all()
  }

  fun task(id: String): Task? {
    ensureRegistered()
    return taskRegistry.get(id)
  }

  fun executeTask(id: String, args: List<String> = emptyList()): TaskResult {
    ensureRegistered()
    val task = taskRegistry.get(id)
      ?: throw IllegalArgumentException("Task '$id' not found for plugin ${plugin.id}")
    return task.execute(
      TestEnvironment(services, publishedEvents, activeProfile),
      ProjectContext(projectDir, projectConfig.toMap()),
      args,
    )
  }

  fun publishedEvents(): List<Any> = publishedEvents.toList()

  private fun ensureRegistered() {
    if (registered) return
    taskRegistry.clear()
    plugin.register(taskRegistry)
    registered = true
  }

  @Suppress("UNCHECKED_CAST")
  private fun convertConfig(config: Any): C {
    if (plugin.ctxClass.isInstance(config)) {
      return config as C
    }
    if (config is Map<*, *>) {
      return instantiateFromMap(plugin.ctxClass.kotlin, config as Map<String, Any?>) as C
    }
    throw IllegalArgumentException(
      "Unsupported config type ${config::class.qualifiedName} for plugin ${plugin.id}"
    )
  }

  private fun instantiateFromMap(type: KClass<*>, values: Map<String, Any?>): Any {
    val constructor = type.primaryConstructor
      ?: throw IllegalArgumentException("Type ${type.qualifiedName} must have a primary constructor")

    val arguments = constructor.parameters.associateWith { parameter ->
      if (!values.containsKey(parameter.name)) {
        if (parameter.isOptional) {
          null
        } else {
          throw IllegalArgumentException("Missing required config key '${parameter.name}' for ${type.simpleName}")
        }
      } else {
        coerceValue(parameter.type, values[parameter.name])
      }
    }.filterValues { it != null }

    return constructor.callBy(arguments)
  }

  private fun coerceValue(targetType: KType, value: Any?): Any? {
    if (value == null) return null

    val classifier = targetType.classifier as? KClass<*> ?: return value
    if (classifier.isInstance(value)) return value

    return when (classifier) {
      String::class -> value.toString()
      Int::class -> when (value) {
        is Number -> value.toInt()
        is String -> value.toInt()
        else -> value
      }
      Long::class -> when (value) {
        is Number -> value.toLong()
        is String -> value.toLong()
        else -> value
      }
      Double::class -> when (value) {
        is Number -> value.toDouble()
        is String -> value.toDouble()
        else -> value
      }
      Float::class -> when (value) {
        is Number -> value.toFloat()
        is String -> value.toFloat()
        else -> value
      }
      Boolean::class -> when (value) {
        is Boolean -> value
        is String -> value.toBooleanStrict()
        else -> value
      }
      else -> {
        when {
          classifier.java.isEnum && value is String -> classifier.java.enumConstants.first { (it as Enum<*>).name == value }
          value is Map<*, *> && classifier.primaryConstructor != null -> {
            val nestedValues = value.entries
              .filter { it.key is String }
              .associate { it.key as String to it.value }
            instantiateFromMap(classifier, nestedValues)
          }
          List::class.createType().classifier == classifier && value is List<*> -> value
          Map::class.createType().classifier == classifier && value is Map<*, *> -> value
          else -> value
        }
      }
    }
  }

  private class InMemoryTaskRegistry : TaskRegistry {
    private val tasks = linkedMapOf<String, Task>()

    override fun add(task: Task) {
      require(task.id !in tasks) { "Task '${task.id}' already registered" }
      tasks[task.id] = task
    }

    override fun get(id: String): Task? = tasks[id]

    override fun all(): List<Task> = tasks.values.toList()

    fun clear() {
      tasks.clear()
    }
  }

  private class TestEnvironment(
    private val services: Map<Class<*>, Any>,
    private val publishedEvents: MutableList<Any>,
    private val activeProfile: String,
  ) : Environment {
    override fun <T> service(type: Class<T>): T {
      val service = services[type]
        ?: throw IllegalArgumentException("No service registered for ${type.name}")
      @Suppress("UNCHECKED_CAST")
      return service as T
    }

    override fun publish(event: Any) {
      publishedEvents += event
    }

    override fun profile(): String = activeProfile
  }
}