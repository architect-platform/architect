package io.github.architectplatform.api.core.plugins

import io.github.architectplatform.api.core.tasks.TaskRegistry

/**
 * Plugin interface for extending the Architect platform.
 *
 * Plugins are the primary extension mechanism in Architect. They register tasks during
 * initialization and can access a typed context for plugin-specific configuration.
 *
 * Example usage:
 * ```kotlin
 * class MyPlugin : ArchitectPlugin<MyContext> {
 *   override val id = "my-plugin"
 *   override val contextKey = "myContext"
 *   override val ctxClass = MyContext::class.java
 *   override lateinit var context: MyContext
 *
 *   override fun register(registry: TaskRegistry) {
 *     registry.add(MyTask())
 *   }
 * }
 * ```
 *
 * @param C The type of context this plugin uses
 */
interface ArchitectPlugin<C> {
  /**
   * Unique identifier for this plugin.
   */
  val id: String

  /**
   * Key used to look up the plugin's context in the configuration.
   */
  val contextKey: String

  /**
   * The class type of the plugin's context.
   */
  val ctxClass: Class<C>

  /**
   * The plugin's context instance, initialized during plugin setup.
   */
  var context: C

  /**
   * Initializes the plugin with its context.
   *
   * This method is called by the platform to provide the plugin with its configuration context.
   *
   * @param context The context object (will be cast to type C)
   */
  fun init(context: Any) {
    @Suppress("UNCHECKED_CAST")
    this.context = context as C
  }

  /**
   * Registers tasks with the task registry.
   *
   * This method is called during plugin initialization to allow the plugin to register
   * its tasks with the platform.
   *
   * @param registry The task registry where tasks should be registered
   */
  fun register(registry: TaskRegistry)
}
