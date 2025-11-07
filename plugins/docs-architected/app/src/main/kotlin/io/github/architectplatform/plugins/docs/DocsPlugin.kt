package io.github.architectplatform.plugins.docs

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.execution.ResourceExtractor
import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase
import io.github.architectplatform.plugins.docs.builders.DocumentationBuilderFactory
import io.github.architectplatform.plugins.docs.dto.ComponentDocs
import io.github.architectplatform.plugins.docs.dto.DocsContext
import io.github.architectplatform.plugins.docs.publishers.GitHubPagesPublisher
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

/**
 * Architect plugin providing documentation management capabilities.
 *
 * This plugin integrates documentation building and publishing with:
 * - Support for multiple documentation frameworks (MkDocs, Docusaurus, VuePress)
 * - Markdown-based documentation
 * - GitHub Pages publishing (handled entirely within plugin, no external workflows)
 * - Automatic component discovery
 * - Dynamic configuration generation
 *
 * The plugin registers tasks in the following phases:
 * - INIT: Documentation structure initialization
 * - BUILD: Documentation building with auto-discovery
 * - PUBLISH: Documentation publishing to GitHub Pages (managed by plugin)
 */
class DocsPlugin : ArchitectPlugin<DocsContext> {
  override val id = "docs-plugin"
  override val contextKey: String = "docs"

  override val ctxClass = DocsContext::class.java
  override var context: DocsContext = DocsContext()

  companion object {
    // Template pattern for components list
    private val COMPONENTS_PATTERN = Regex("""(?s)\{\{#components}}(.*?)\{\{/components}}""")
    
    // Template pattern for repository URL
    private val REPO_URL_PATTERN = Regex("""(?s)\{\{#repoUrl}}(.*?)\{\{/repoUrl}}""")

    /**
     * Sanitizes a path to prevent command injection and directory traversal.
     * @deprecated Use SecurityUtils.sanitizePath() instead
     */
    @Deprecated("Use SecurityUtils.sanitizePath() instead", ReplaceWith("SecurityUtils.sanitizePath(path)", "io.github.architectplatform.plugins.docs.utils.SecurityUtils"))
    fun sanitizePath(path: String): String = io.github.architectplatform.plugins.docs.utils.SecurityUtils.sanitizePath(path)

    /**
     * Sanitizes a Git branch name for safe shell execution.
     * @deprecated Use SecurityUtils.sanitizeBranch() instead
     */
    @Deprecated("Use SecurityUtils.sanitizeBranch() instead", ReplaceWith("SecurityUtils.sanitizeBranch(branch)", "io.github.architectplatform.plugins.docs.utils.SecurityUtils"))
    fun sanitizeBranch(branch: String): String = io.github.architectplatform.plugins.docs.utils.SecurityUtils.sanitizeBranch(branch)

    /**
     * Sanitizes a version string for safe shell execution.
     * @deprecated Use SecurityUtils.sanitizeVersion() instead
     */
    @Deprecated("Use SecurityUtils.sanitizeVersion() instead", ReplaceWith("SecurityUtils.sanitizeVersion(version)", "io.github.architectplatform.plugins.docs.utils.SecurityUtils"))
    fun sanitizeVersion(version: String): String = io.github.architectplatform.plugins.docs.utils.SecurityUtils.sanitizeVersion(version)

    /**
     * Validates a domain name using RFC-compliant rules.
     * @deprecated Use SecurityUtils.isValidDomain() instead
     */
    @Deprecated("Use SecurityUtils.isValidDomain() instead", ReplaceWith("SecurityUtils.isValidDomain(domain)", "io.github.architectplatform.plugins.docs.utils.SecurityUtils"))
    fun isValidDomain(domain: String): Boolean = io.github.architectplatform.plugins.docs.utils.SecurityUtils.isValidDomain(domain)
  }

  /**
   * Replaces template placeholders in configuration files with actual values.
   *
   * @param template The template content with placeholders
   * @return The template with all placeholders replaced
   */
  private fun replaceTemplatePlaceholders(template: String): String {
    var result = template
        .replace("{{siteName}}", context.build.siteName)
        .replace("{{siteDescription}}", context.build.siteDescription)
        .replace("{{siteAuthor}}", context.build.siteAuthor)
        .replace("{{primaryColor}}", context.build.primaryColor)
        .replace("{{accentColor}}", context.build.accentColor)

    // Handle optional repository URL and name
    if (context.build.repoUrl.isNotEmpty()) {
      result = result
          .replace("{{repoUrl}}", context.build.repoUrl)
          .replace("{{repoName}}", context.build.repoName.ifEmpty { "repository" })
      
      // Handle conditional sections with {{#repoUrl}}...{{/repoUrl}}
      result = REPO_URL_PATTERN.replace(result) { matchResult ->
        matchResult.groupValues[1].trim()
      }
      
      // Extract organization and project names from repo name
      val repoParts = context.build.repoName.split("/")
      if (repoParts.size == 2) {
        result = result
            .replace("{{organizationName}}", repoParts[0])
            .replace("{{projectName}}", repoParts[1])
      } else {
        result = result
            .replace("{{organizationName}}", "organization")
            .replace("{{projectName}}", "project")
      }
    } else {
      // Remove conditional sections if repoUrl is not provided
      result = REPO_URL_PATTERN.replace(result, "")
      
      // Provide fallback values
      result = result
          .replace("{{repoUrl}}", "https://github.com/username/repo")
          .replace("{{repoName}}", "username/repo")
          .replace("{{organizationName}}", "username")
          .replace("{{projectName}}", "repo")
    }

    // Handle component list for monorepo configuration
    if (context.build.components.isNotEmpty()) {
      result = COMPONENTS_PATTERN.replace(result) { matchResult ->
        val componentTemplate = matchResult.groupValues[1]
        context.build.components.joinToString("\n") { component ->
          componentTemplate
              .replace("{{name}}", component.name)
              .replace("{{path}}", component.path)
              .replace("{{docsPath}}", component.docsPath)
        }
      }
    } else {
      // Remove components sections if no components are configured
      result = COMPONENTS_PATTERN.replace(result, "")
    }

    return result
  }

  /**
   * Registers documentation-related tasks with the task registry.
   *
   * Registered tasks:
   * - docs-init: Initialize documentation structure and workflows
   * - docs-build: Build documentation from markdown sources
   * - docs-publish: Publish documentation to GitHub Pages
   *
   * @param registry The task registry to add tasks to
   */
  override fun register(registry: TaskRegistry) {
    registry.add(
        DocsTask(
            id = "docs-init",
            phase = CoreWorkflow.INIT,
            task = ::initDocs,
        ))

    registry.add(
        DocsTask(
            id = "docs-build",
            phase = CoreWorkflow.BUILD,
            task = ::buildDocs,
        ))

    registry.add(
        DocsTask(
            id = "docs-publish",
            phase = CoreWorkflow.PUBLISH,
            task = ::publishDocs,
        ))
  }

  /**
   * Task wrapper for documentation-specific operations.
   *
   * Executes a documentation task function and handles exceptions, converting them
   * to appropriate TaskResult objects.
   *
   * @property id Unique identifier for the task
   * @property phase The workflow phase this task belongs to
   * @property task The actual task implementation function
   */
  class DocsTask(
      override val id: String,
      private val phase: Phase,
      private val task: (Environment, ProjectContext) -> TaskResult
  ) : Task {

    override fun phase(): Phase = phase

    /**
     * Executes the documentation task with error handling.
     *
     * @param environment Execution environment providing services
     * @param projectContext The project context
     * @param args Additional arguments for the task
     * @return TaskResult indicating success or failure
     */
    override fun execute(
        environment: Environment,
        projectContext: ProjectContext,
        args: List<String>
    ): TaskResult {
      return try {
        task(environment, projectContext)
      } catch (e: Exception) {
        TaskResult.failure(
            "Docs task: $id failed with exception: ${e.message ?: "Unknown error"}")
      }
    }
  }

  /**
   * Finds the root directory of the Git repository.
   *
   * Traverses up the directory tree from the starting directory until
   * a .git directory is found.
   *
   * @param startDir The directory to start searching from
   * @return The repository root directory, or null if not found
   */
  private fun findRepoRoot(startDir: File): File? {
    var currentDir: File? = startDir
    while (currentDir != null) {
      val gitDir = File(currentDir, ".git")
      if (gitDir.exists() && gitDir.isDirectory) {
        return currentDir
      }
      currentDir = currentDir.parentFile
    }
    return null
  }

  /**
   * Auto-discovers components with documentation folders.
   * 
   * Searches for directories containing a docs/ folder and returns them as ComponentDocs.
   * Excludes hidden directories, build artifacts, and the root docs directory.
   *
   * @param gitDir The repository root directory
   * @return List of discovered components
   */
  private fun discoverComponents(gitDir: File): List<ComponentDocs> {
    val discovered = mutableListOf<ComponentDocs>()
    val excludedDirs = setOf(".", ".git", ".github", "build", "target", "node_modules", ".gradle", "site")
    
    for (searchPath in context.build.componentPaths) {
      val searchDir = File(gitDir, searchPath)
      if (!searchDir.exists() || !searchDir.isDirectory) continue
      
      // For root directory, look for direct subdirectories with docs
      val dirsToCheck = if (searchPath == ".") {
        searchDir.listFiles()?.filter { 
          it.isDirectory && !excludedDirs.contains(it.name) && it.name != context.build.sourceDir
        } ?: emptyList()
      } else {
        // For other paths (like plugins), check all subdirectories
        searchDir.listFiles()?.filter { 
          it.isDirectory && !excludedDirs.contains(it.name)
        } ?: emptyList()
      }
      
      for (dir in dirsToCheck) {
        val docsDir = File(dir, "docs")
        if (docsDir.exists() && docsDir.isDirectory) {
          val relativePath = dir.relativeTo(gitDir).path
          val componentName = dir.name.split("-")
              .joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() } }
          
          discovered.add(ComponentDocs(
              name = componentName,
              path = relativePath,
              docsPath = "docs"
          ))
        }
      }
    }
    
    return discovered.sortedBy { it.path }
  }

  /**
   * Generates mkdocs.yml configuration file dynamically.
   *
   * @param gitDir The repository root directory
   * @param components List of components to include
   * @return Generated configuration content
   */
  private fun generateMkDocsConfig(gitDir: File, components: List<ComponentDocs>): String {
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
site_name: ${context.build.siteName}
site_description: ${context.build.siteDescription}
site_author: ${context.build.siteAuthor}
${if (context.build.repoUrl.isNotEmpty()) """
repo_url: ${context.build.repoUrl}
repo_name: ${context.build.repoName.ifEmpty { "repository" }}
""" else ""}

theme:
  name: material
  palette:
    primary: ${context.build.primaryColor}
    accent: ${context.build.accentColor}
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

${if (context.build.repoUrl.isNotEmpty()) """
extra:
  social:
    - icon: fontawesome/brands/github
      link: ${context.build.repoUrl}
""" else ""}
""".trimIndent()
  }

  /**
   * Aggregates component documentation into a temporary build structure.
   * 
   * @param gitDir The repository root directory
   * @param components List of components to aggregate
   * @param tempDocsDir Temporary directory for aggregated docs
   */
  private fun aggregateComponentDocs(gitDir: File, components: List<ComponentDocs>, tempDocsDir: File) {
    // Copy root docs
    val rootDocsDir = File(gitDir, context.build.sourceDir)
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

  /**
   * Initializes documentation structure.
   *
   * This task:
   * 1. Creates documentation directory structure if it doesn't exist
   * 2. Creates initial index.md if needed
   *
   * Note: mkdocs.yml is now generated dynamically at build time, not during init.
   * Publishing is handled entirely by the plugin without requiring GitHub Actions.
   *
   * @param environment Execution environment providing services
   * @param projectContext The project context
   * @return TaskResult indicating success or failure
   */
  private fun initDocs(environment: Environment, projectContext: ProjectContext): TaskResult {
    val gitDir =
        findRepoRoot(projectContext.dir.toFile())
            ?: return TaskResult.failure("Git directory not found in project hierarchy.")

    val results = mutableListOf<TaskResult>()

    // Create documentation directory if it doesn't exist
    val docsDir = File(gitDir, context.build.sourceDir)
    if (!docsDir.exists()) {
      docsDir.mkdirs()
      results.add(TaskResult.success("Created documentation directory: ${context.build.sourceDir}"))
      
      // Create initial index.md
      val indexFile = File(docsDir, "index.md")
      indexFile.writeText("""
        |# ${context.build.siteName}
        |
        |Welcome to the ${context.build.siteName}!
        |
        |## Overview
        |
        |Add your documentation content here.
        |
        |## Getting Started
        |
        |Add getting started guide here.
        |""".trimMargin())
      results.add(TaskResult.success("Created initial index.md"))
    }

    return TaskResult.success("Documentation initialized successfully", results)
  }
  // Remove old framework-specific config generation code - now handled dynamically in buildDocs

  /**
   * Builds documentation from markdown sources using appropriate builder.
   *
   * This task:
   * 1. Auto-discovers components with documentation
   * 2. Creates appropriate builder based on framework
   * 3. Delegates build process to the builder
   *
   * @param environment Execution environment providing services
   * @param projectContext The project context
   * @return TaskResult indicating success or failure
   */
  private fun buildDocs(environment: Environment, projectContext: ProjectContext): TaskResult {
    if (!context.build.enabled) {
      return TaskResult.success("Documentation building is disabled")
    }

    val gitDir =
        findRepoRoot(projectContext.dir.toFile())
            ?: return TaskResult.failure("Git directory not found in project hierarchy.")

    val commandExecutor = environment.service(CommandExecutor::class.java)
    val results = mutableListOf<TaskResult>()
    
    // Step 1: Discover or use configured components
    val components = if (context.build.autoDiscoverComponents && context.build.components.isEmpty()) {
      discoverComponents(gitDir).also {
        results.add(TaskResult.success("Auto-discovered ${it.size} components with documentation"))
      }
    } else {
      context.build.components
    }
    
    // Step 2: Handle manual framework (no automatic build)
    if (context.build.framework == "manual") {
      results.add(TaskResult.success("Manual framework selected - no automatic build performed"))
      return TaskResult.success("Documentation build skipped for manual framework", results)
    }
    
    // Step 3: Create appropriate builder and execute build
    return try {
      val builder = DocumentationBuilderFactory.createBuilder(context.build, commandExecutor)
      val buildResult = builder.executeBuild(gitDir, components)
      results.add(buildResult)
      
      if (buildResult.success) {
        TaskResult.success("Documentation built successfully", results)
      } else {
        TaskResult.failure("Documentation build failed", results)
      }
    } catch (e: IllegalArgumentException) {
      TaskResult.failure("Unsupported documentation framework: ${context.build.framework}", results)
    } catch (e: Exception) {
      TaskResult.failure("Failed to build documentation: ${e.message ?: "Unknown error"}", results)
    }
  }

  /**
   * Publishes documentation using appropriate publishing strategy.
   *
   * This task:
   * 1. Ensures documentation is built
   * 2. Creates appropriate publisher based on configuration
   * 3. Delegates publishing process to the publisher
   *
   * @param environment Execution environment providing services
   * @param projectContext The project context
   * @return TaskResult indicating success or failure
   */
  private fun publishDocs(environment: Environment, projectContext: ProjectContext): TaskResult {
    if (!context.publish.enabled) {
      return TaskResult.success("Documentation publishing is disabled")
    }

    if (!context.publish.githubPages) {
      return TaskResult.success("GitHub Pages publishing is disabled")
    }

    val gitDir =
        findRepoRoot(projectContext.dir.toFile())
            ?: return TaskResult.failure("Git directory not found in project hierarchy.")

    val commandExecutor = environment.service(CommandExecutor::class.java)
    val outputDir = File(gitDir, context.build.outputDir)
    
    // Create publisher and execute publish
    return try {
      val publisher = GitHubPagesPublisher(context.publish, commandExecutor)
      publisher.executePublish(gitDir, outputDir)
    } catch (e: Exception) {
      TaskResult.failure("Failed to publish documentation: ${e.message ?: "Unknown error"}")
    }
  }
}
