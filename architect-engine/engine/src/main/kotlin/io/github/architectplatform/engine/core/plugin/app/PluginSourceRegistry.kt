package io.github.architectplatform.engine.core.plugin.app

import io.github.architectplatform.engine.core.common.Result
import io.github.architectplatform.engine.core.plugin.domain.PluginSource
import io.github.architectplatform.engine.core.plugin.domain.PluginSourceConfig
import jakarta.inject.Singleton
import java.io.File
import org.slf4j.LoggerFactory

/**
 * Registry that manages multiple plugin sources and selects the appropriate one
 * based on the plugin configuration.
 * 
 * This class implements the Strategy pattern by delegating plugin resolution
 * to the appropriate PluginSource implementation.
 */
@Singleton
class PluginSourceRegistry(
    private val sources: List<PluginSource>
) {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    /**
     * Resolves a plugin using the appropriate source based on the configuration type.
     * 
     * @param config The plugin configuration
     * @return Result containing the resolved plugin file or an error
     */
    fun resolve(config: PluginSourceConfig): Result<File> {
        val source = sources.firstOrNull { it.canHandle(config.type) }
        
        if (source == null) {
            logger.error("No plugin source found for type: ${config.type}")
            return Result.failure(
                "Unsupported plugin type: ${config.type}. Available types: ${getSupportedTypes()}",
                errorCode = "UNSUPPORTED_PLUGIN_TYPE"
            )
        }
        
        logger.debug("Resolving plugin '${config.name}' using source: ${source::class.simpleName}")
        return source.resolve(config)
    }
    
    /**
     * Returns a list of supported plugin types.
     */
    fun getSupportedTypes(): List<String> {
        return sources.map { it.getType() }
    }
    
    /**
     * Returns the number of registered plugin sources.
     */
    fun getSourceCount(): Int = sources.size
}
