package io.github.architectplatform.engine.core.plugin.domain

import io.github.architectplatform.engine.core.common.Result
import java.io.File

/**
 * Represents a source from which plugins can be loaded.
 * 
 * This interface defines the strategy pattern for different plugin sources
 * (e.g., GitHub releases, local filesystem, Maven repositories).
 */
interface PluginSource {
    
    /**
     * Returns the type identifier that this source handles.
     * This should be a unique string identifying the plugin source type.
     */
    fun getType(): String
    
    /**
     * Returns true if this source can handle the given plugin configuration.
     */
    fun canHandle(type: String): Boolean = type == getType()
    
    /**
     * Resolves and downloads the plugin from this source.
     * 
     * @param config The plugin configuration
     * @return Result containing the plugin JAR file or an error
     */
    fun resolve(config: PluginSourceConfig): Result<File>
}

/**
 * Configuration for resolving a plugin from a source.
 */
data class PluginSourceConfig(
    val type: String,
    val name: String,
    val version: String,
    val repo: String? = null,
    val asset: String? = null,
    val path: String? = null,
    val pattern: String? = null,
    val baseDir: String? = null
)
