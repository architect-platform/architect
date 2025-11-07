package io.github.architectplatform.plugins.docs.builders

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.plugins.docs.DocsPlugin
import io.github.architectplatform.plugins.docs.dto.BuildContext
import io.github.architectplatform.plugins.docs.dto.ComponentDocs
import java.io.File

/**
 * Documentation builder for MkDocs (Python-based documentation framework).
 * 
 * This builder:
 * - Creates and manages a Python virtual environment
 * - Installs MkDocs and Material theme in the venv
 * - Generates mkdocs.yml configuration
 * - Builds documentation using mkdocs build command
 * - Supports monorepo/component documentation aggregation
 */
class MkDocsBuilder(
    context: BuildContext,
    commandExecutor: CommandExecutor
) : DocumentationBuilder(context, commandExecutor) {
    
    private val venvDir = ".venv-docs"
    private var tempMkDocsConfig: File? = null
    private var tempDocsDir: File? = null
    
    override fun getName(): String = "MkDocs"
    
    /**
     * Sets up Python virtual environment for MkDocs.
     */
    override fun setupEnvironment(workingDir: File): TaskResult {
        val venvPath = File(workingDir, venvDir)
        
        return try {
            // Check if venv already exists
            if (venvPath.exists() && File(venvPath, "bin/activate").exists()) {
                TaskResult.success("Python virtual environment already exists at $venvDir")
            } else {
                // Create new virtual environment
                commandExecutor.execute("python3 -m venv $venvDir", workingDir.toString())
                TaskResult.success("Created Python virtual environment at $venvDir")
            }
        } catch (e: Exception) {
            TaskResult.failure("Failed to setup Python virtual environment: ${e.message}")
        }
    }
    
    /**
     * Installs MkDocs and dependencies in the virtual environment.
     */
    override fun installDependencies(workingDir: File): TaskResult {
        val mkdocsVer = DocsPlugin.sanitizeVersion(context.mkdocsVersion)
        val materialVer = DocsPlugin.sanitizeVersion(context.mkdocsMaterialVersion)
        
        val packages = listOf(
            "mkdocs==$mkdocsVer",
            "mkdocs-material==$materialVer"
        )
        
        return try {
            // Install packages in virtual environment
            val pipCommand = "$venvDir/bin/pip3 install ${packages.joinToString(" ")}"
            commandExecutor.execute(pipCommand, workingDir.toString())
            TaskResult.success("Installed MkDocs dependencies in virtual environment")
        } catch (e: Exception) {
            TaskResult.failure("Failed to install MkDocs dependencies: ${e.message}")
        }
    }
    
    /**
     * Generates mkdocs.yml configuration dynamically.
     */
    override fun generateConfiguration(workingDir: File, components: List<ComponentDocs>): TaskResult {
        return try {
            val mkdocsConfig = generateMkDocsConfig(components)
            tempMkDocsConfig = File(workingDir, "mkdocs.yml.tmp")
            tempMkDocsConfig?.writeText(mkdocsConfig)
            
            // If components exist, aggregate their docs
            if (components.isNotEmpty()) {
                tempDocsDir = File(workingDir, ".docs-build-temp")
                tempDocsDir?.mkdirs()
                aggregateComponentDocs(workingDir, components, tempDocsDir!!)
                TaskResult.success("Generated MkDocs configuration and aggregated ${components.size} components")
            } else {
                TaskResult.success("Generated MkDocs configuration")
            }
        } catch (e: Exception) {
            TaskResult.failure("Failed to generate MkDocs configuration: ${e.message}")
        }
    }
    
    /**
     * Builds documentation using mkdocs build command.
     */
    override fun build(workingDir: File): TaskResult {
        val sanitizedOutputDir = DocsPlugin.sanitizePath(context.outputDir)
        
        return try {
            // Use mkdocs from virtual environment
            val configFlag = if (tempMkDocsConfig?.exists() == true) {
                "-f ${tempMkDocsConfig!!.name}"
            } else ""
            
            val command = if (sanitizedOutputDir.isNotEmpty()) {
                "$venvDir/bin/mkdocs build $configFlag -d $sanitizedOutputDir"
            } else {
                "$venvDir/bin/mkdocs build $configFlag"
            }
            
            commandExecutor.execute(command, workingDir.toString())
            TaskResult.success("Built documentation with MkDocs")
        } catch (e: Exception) {
            TaskResult.failure("Failed to build documentation: ${e.message}")
        }
    }
    
    /**
     * Cleans up temporary files.
     */
    override fun cleanup(workingDir: File) {
        tempMkDocsConfig?.let { if (it.exists()) it.delete() }
        tempDocsDir?.let { if (it.exists()) it.deleteRecursively() }
    }
    
    /**
     * Generates mkdocs.yml configuration file dynamically.
     */
    private fun generateMkDocsConfig(components: List<ComponentDocs>): String {
        val nav = buildString {
            appendLine("  - Home: index.md")
            
            if (components.isNotEmpty()) {
                // Group components by category
                val coreComponents = components.filter { !it.path.startsWith("plugins/") }
                val pluginComponents = components.filter { it.path.startsWith("plugins/") }
                
                if (coreComponents.isNotEmpty()) {
                    appendLine("  - Core Components:")
                    for (component in coreComponents) {
                        appendLine("      - ${component.name}: ${component.path}/${component.docsPath}/index.md")
                    }
                }
                
                if (pluginComponents.isNotEmpty()) {
                    appendLine("  - Official Plugins:")
                    for (component in pluginComponents) {
                        appendLine("      - ${component.name}: ${component.path}/${component.docsPath}/index.md")
                    }
                }
            }
        }
        
        return """
site_name: ${context.siteName}
site_description: ${context.siteDescription}
site_author: ${context.siteAuthor}
${if (context.repoUrl.isNotEmpty()) """
repo_url: ${context.repoUrl}
repo_name: ${context.repoName.ifEmpty { "repository" }}
""" else ""}

theme:
  name: material
  palette:
    primary: ${context.primaryColor}
    accent: ${context.accentColor}
  features:
    - navigation.tabs
    - navigation.sections
    - navigation.expand
    - navigation.indexes
    - navigation.top
    - search.suggest
    - search.highlight
    - content.code.copy
    - content.tabs.link

nav:
$nav

markdown_extensions:
  - admonition
  - codehilite
  - pymdownx.superfences
  - pymdownx.tabbed:
      alternate_style: true
  - pymdownx.details
  - pymdownx.emoji:
      emoji_index: !!python/name:material.extensions.emoji.twemoji
      emoji_generator: !!python/name:material.extensions.emoji.to_svg
  - toc:
      permalink: true
  - attr_list
  - md_in_html

plugins:
  - search

${if (context.repoUrl.isNotEmpty()) """
extra:
  social:
    - icon: fontawesome/brands/github
      link: ${context.repoUrl}
""" else ""}
""".trimIndent()
    }
    
    /**
     * Aggregates component documentation into a temporary build structure.
     */
    private fun aggregateComponentDocs(gitDir: File, components: List<ComponentDocs>, tempDocsDir: File) {
        // Copy root docs
        val rootDocsDir = File(gitDir, context.sourceDir)
        if (rootDocsDir.exists()) {
            rootDocsDir.copyRecursively(tempDocsDir, overwrite = true)
        }
        
        // Copy component docs
        for (component in components) {
            val componentDocsSource = File(gitDir, "${component.path}/${component.docsPath}")
            val componentDocsTarget = File(tempDocsDir, "${component.path}/${component.docsPath}")
            
            if (componentDocsSource.exists()) {
                componentDocsTarget.parentFile.mkdirs()
                componentDocsSource.copyRecursively(componentDocsTarget, overwrite = true)
            }
        }
    }
}
