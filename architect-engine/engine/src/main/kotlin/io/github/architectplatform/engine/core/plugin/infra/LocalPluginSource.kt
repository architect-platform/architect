package io.github.architectplatform.engine.core.plugin.infra

import io.github.architectplatform.engine.core.common.Result
import io.github.architectplatform.engine.core.plugin.domain.PluginSource
import io.github.architectplatform.engine.core.plugin.domain.PluginSourceConfig
import jakarta.inject.Singleton
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists
import org.slf4j.LoggerFactory

/**
 * Plugin source that loads plugins from the local filesystem.
 */
@Singleton
class LocalPluginSource : PluginSource {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    override fun canHandle(type: String): Boolean = type == "local"
    
    override fun resolve(config: PluginSourceConfig): Result<File> {
        val path = config.path
        if (path == null) {
            return Result.failure("Local plugin path is required but not provided")
        }
        
        return Result.catching {
            val baseDir = config.baseDir ?: "."
            val localPath = Path(baseDir).resolve(path)
            
            logger.debug("Resolving local plugin from path: ${localPath.toAbsolutePath()}")
            
            if (!localPath.exists()) {
                throw IllegalArgumentException(
                    "Local plugin not found at: ${localPath.toAbsolutePath()}"
                )
            }
            
            val file = localPath.toFile()
            if (!file.isFile) {
                throw IllegalArgumentException(
                    "Plugin path is not a file: ${localPath.toAbsolutePath()}"
                )
            }
            
            logger.info("Successfully resolved local plugin: ${file.absolutePath}")
            file
        }
    }
}
