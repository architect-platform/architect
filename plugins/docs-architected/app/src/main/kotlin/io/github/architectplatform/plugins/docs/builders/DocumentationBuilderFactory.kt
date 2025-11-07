package io.github.architectplatform.plugins.docs.builders

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.plugins.docs.dto.BuildContext

/**
 * Factory for creating appropriate documentation builder based on framework.
 */
object DocumentationBuilderFactory {
    
    /**
     * Creates a documentation builder for the specified framework.
     * 
     * @param context The build context containing framework configuration
     * @param commandExecutor The command executor for running build commands
     * @return DocumentationBuilder instance for the specified framework
     * @throws IllegalArgumentException if the framework is not supported
     */
    fun createBuilder(context: BuildContext, commandExecutor: CommandExecutor): DocumentationBuilder {
        return when (context.framework.lowercase()) {
            "mkdocs" -> MkDocsBuilder(context, commandExecutor)
            "docusaurus" -> DocusaurusBuilder(context, commandExecutor)
            "vuepress" -> VuePressBuilder(context, commandExecutor)
            else -> throw IllegalArgumentException("Unsupported documentation framework: ${context.framework}")
        }
    }
}
