package io.github.architectplatform.plugins.docs.builders

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.plugins.docs.dto.BuildContext
import io.github.architectplatform.plugins.docs.dto.ComponentDocs
import java.io.File

/**
 * Documentation builder for VuePress (Vue-powered static site generator).
 * 
 * This builder:
 * - Ensures Node.js environment is ready
 * - Installs VuePress dependencies via npm/yarn/pnpm
 * - Generates .vuepress/config.js if needed
 * - Builds documentation using npm run docs:build
 */
class VuePressBuilder(
    context: BuildContext,
    commandExecutor: CommandExecutor
) : NodeJsDocumentationBuilder(context, commandExecutor) {
    
    override fun getName(): String = "VuePress"
    
    /**
     * Generates .vuepress/config.js configuration if it doesn't exist.
     */
    override fun generateConfiguration(workingDir: File, components: List<ComponentDocs>): TaskResult {
        val vuepressDir = File(workingDir, "${context.sourceDir}/.vuepress")
        val configFile = File(vuepressDir, "config.js")
        
        return if (configFile.exists()) {
            TaskResult.success("VuePress configuration already exists")
        } else {
            try {
                vuepressDir.mkdirs()
                val config = generateVuePressConfig()
                configFile.writeText(config)
                TaskResult.success("Generated VuePress configuration")
            } catch (e: Exception) {
                TaskResult.failure("Failed to generate VuePress configuration: ${e.message}")
            }
        }
    }
    
    /**
     * Builds documentation using npm run docs:build.
     */
    override fun build(workingDir: File): TaskResult {
        return try {
            val packageLock = File(workingDir, "package-lock.json")
            val yarnLock = File(workingDir, "yarn.lock")
            val pnpmLock = File(workingDir, "pnpm-lock.yaml")
            
            val command = when {
                pnpmLock.exists() -> "pnpm docs:build"
                yarnLock.exists() -> "yarn docs:build"
                else -> "npm run docs:build"
            }
            
            commandExecutor.execute(command, workingDir.toString())
            TaskResult.success("Built documentation with VuePress")
        } catch (e: Exception) {
            TaskResult.failure("Failed to build documentation: ${e.message}")
        }
    }
    
    /**
     * Generates a basic VuePress config.js configuration.
     */
    private fun generateVuePressConfig(): String {
        return """
module.exports = {
  title: '${context.siteName}',
  description: '${context.siteDescription}',
  ${if (context.repoUrl.isNotEmpty()) "base: '/'" else "base: '/'"},
  
  themeConfig: {
    ${if (context.repoUrl.isNotEmpty()) """
    repo: '${context.repoName}',
    repoLabel: 'GitHub',
    """ else ""}
    
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guide', link: '/guide/' },
    ],
    
    sidebar: [
      '/',
      '/guide/',
    ],
    
    lastUpdated: 'Last Updated',
    
    smoothScroll: true
  },
  
  plugins: [
    '@vuepress/plugin-back-to-top',
    '@vuepress/plugin-medium-zoom',
  ]
}
""".trimIndent()
    }
}
