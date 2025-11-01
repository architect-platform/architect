package io.github.architectplatform.engine.core.plugin.app

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext

/**
 * Interface for loading plugins into the Architect Engine.
 *
 * Implementations of this interface are responsible for discovering and
 * instantiating plugins based on the project context.
 */
interface PluginLoader {
  /**
   * Loads all available plugins for the given project context.
   *
   * @param context The project context containing configuration and metadata
   * @return List of loaded plugin instances
   */
  fun load(context: ProjectContext): List<ArchitectPlugin<*>>
}
