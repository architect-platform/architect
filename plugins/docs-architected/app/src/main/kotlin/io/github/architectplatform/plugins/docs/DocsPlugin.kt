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
import io.github.architectplatform.plugins.docs.dto.DocsContext
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
    // Domain validation regex - validates RFC-compliant domain names
    // Matches: example.com, sub.example.com, my-site.github.io
    // Does not match: -example.com, example-.com, .example.com
    private val DOMAIN_VALIDATION_REGEX =
        Regex("^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?(\\.[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?)*$")

    // Template pattern for components list
    private val COMPONENTS_PATTERN = Regex("""(?s)\{\{#components}}(.*?)\{\{/components}}""")
    
    // Template pattern for repository URL
    private val REPO_URL_PATTERN = Regex("""(?s)\{\{#repoUrl}}(.*?)\{\{/repoUrl}}""")

    /**
     * Sanitizes a path to prevent command injection and directory traversal.
     * Allows alphanumeric characters, underscores, hyphens, forward slashes, and dots.
     * Prevents absolute paths, parent directory references, and special characters.
     *
     * @param path The path to sanitize
     * @return Sanitized path safe for shell commands
     */
    fun sanitizePath(path: String): String {
      // Remove any absolute path indicators
      val relativePath = path.removePrefix("/")
      // Remove parent directory references
      val noParentRefs = relativePath.replace("../", "").replace("/..", "")
      // Remove disallowed characters
      return noParentRefs.replace(Regex("[^a-zA-Z0-9/_.-]"), "")
    }

    /**
     * Sanitizes a Git branch name for safe shell execution.
     * Allows alphanumeric characters, underscores, hyphens, and forward slashes.
     *
     * @param branch The branch name to sanitize
     * @return Sanitized branch name
     */
    fun sanitizeBranch(branch: String): String {
      return branch.replace(Regex("[^a-zA-Z0-9/_-]"), "")
    }

    /**
     * Sanitizes a version string for safe shell execution.
     * Allows alphanumeric characters, dots, hyphens, and underscores.
     *
     * @param version The version string to sanitize
     * @return Sanitized version string
     */
    fun sanitizeVersion(version: String): String {
      return version.replace(Regex("[^a-zA-Z0-9._-]"), "")
    }

    /**
     * Validates a domain name using RFC-compliant rules.
     *
     * @param domain The domain to validate
     * @return True if the domain is valid, false otherwise
     */
    fun isValidDomain(domain: String): Boolean {
      return DOMAIN_VALIDATION_REGEX.matches(domain)
    }
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
   * Builds documentation from markdown sources.
   *
   * This task:
   * 1. Auto-discovers components with documentation
   * 2. Generates mkdocs.yml dynamically
   * 3. Aggregates component docs into temporary build structure
   * 4. Installs dependencies if needed
   * 5. Builds documentation
   * 6. Cleans up temporary files
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
    
    // Temporary files to clean up
    val tempMkDocsConfig = File(gitDir, "mkdocs.yml.tmp")
    val tempDocsDir = File(gitDir, ".docs-build-temp")

    try {
      // Step 1: Discover or use configured components
      val components = if (context.build.autoDiscoverComponents && context.build.components.isEmpty()) {
        discoverComponents(gitDir).also {
          results.add(TaskResult.success("Auto-discovered ${it.size} components with documentation"))
        }
      } else {
        context.build.components
      }
      
      // Step 2: Generate mkdocs.yml (for MkDocs framework only)
      if (context.build.framework == "mkdocs") {
        val mkdocsConfig = generateMkDocsConfig(gitDir, components)
        tempMkDocsConfig.writeText(mkdocsConfig)
        results.add(TaskResult.success("Generated mkdocs.yml configuration"))
      }
      
      // Step 3: Aggregate component documentation
      if (components.isNotEmpty()) {
        tempDocsDir.mkdirs()
        aggregateComponentDocs(gitDir, components, tempDocsDir)
        results.add(TaskResult.success("Aggregated documentation from ${components.size} components"))
      }

      // Step 4: Install dependencies if needed
      if (context.build.installDeps) {
        try {
          when (context.build.framework) {
            "mkdocs" -> {
              val mkdocsVer = sanitizeVersion(context.build.mkdocsVersion)
              val materialVer = sanitizeVersion(context.build.mkdocsMaterialVersion)
              
              val packages = listOf(
                "mkdocs==$mkdocsVer",
                "mkdocs-material==$materialVer"
              )
              
              commandExecutor.execute("pip3 install ${packages.joinToString(" ")}", gitDir.toString())
              results.add(TaskResult.success("Installed MkDocs dependencies"))
            }
            "docusaurus" -> {
              if (File(gitDir, "package.json").exists()) {
                val packageLock = File(gitDir, "package-lock.json")
                val command = if (packageLock.exists()) "npm ci" else "npm install"
                commandExecutor.execute(command, gitDir.toString())
                results.add(TaskResult.success("Installed Docusaurus dependencies"))
              }
            }
            "vuepress" -> {
              if (File(gitDir, "package.json").exists()) {
                val packageLock = File(gitDir, "package-lock.json")
                val command = if (packageLock.exists()) "npm ci" else "npm install"
                commandExecutor.execute(command, gitDir.toString())
                results.add(TaskResult.success("Installed VuePress dependencies"))
              }
            }
          }
        } catch (e: Exception) {
          results.add(
              TaskResult.failure(
                  "Failed to install dependencies: ${e.message ?: "Unknown error"}"))
          return TaskResult.failure("Build failed", results)
        }
      }

      // Step 5: Build documentation
      try {
        when (context.build.framework) {
          "mkdocs" -> {
            val sanitizedOutputDir = sanitizePath(context.build.outputDir)
            // Use temporary mkdocs.yml if it exists
            val configFlag = if (tempMkDocsConfig.exists()) {
              "-f ${tempMkDocsConfig.name}"
            } else ""
            
            val command = if (sanitizedOutputDir.isNotEmpty()) {
              "mkdocs build $configFlag -d $sanitizedOutputDir"
            } else {
              "mkdocs build $configFlag"
            }
            commandExecutor.execute(command, gitDir.toString())
            results.add(TaskResult.success("Built documentation with MkDocs"))
          }
          "docusaurus" -> {
            commandExecutor.execute("npm run build", gitDir.toString())
            results.add(TaskResult.success("Built documentation with Docusaurus"))
          }
          "vuepress" -> {
            commandExecutor.execute("npm run docs:build", gitDir.toString())
            results.add(TaskResult.success("Built documentation with VuePress"))
          }
          "manual" -> {
            results.add(
                TaskResult.success(
                    "Manual framework selected - no automatic build performed"))
          }
          else -> {
            return TaskResult.failure("Unsupported documentation framework: ${context.build.framework}")
          }
        }
      } catch (e: Exception) {
        return TaskResult.failure(
            "Failed to build documentation: ${e.message ?: "Unknown error"}", results)
      }
      
      return TaskResult.success("Documentation built successfully", results)
      
    } finally {
      // Step 6: Clean up temporary files
      if (tempMkDocsConfig.exists()) {
        tempMkDocsConfig.delete()
      }
      if (tempDocsDir.exists()) {
        tempDocsDir.deleteRecursively()
      }
    }
  }

  /**
   * Publishes documentation to GitHub Pages.
   *
   * This task:
   * 1. Ensures documentation is built
   * 2. Creates CNAME file for custom domains if configured
   * 3. Commits documentation to gh-pages branch
   * 4. Pushes to remote repository
   *
   * All operations are handled internally by the plugin using git commands.
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
    val results = mutableListOf<TaskResult>()

    // Ensure output directory exists
    val outputDir = File(gitDir, context.build.outputDir)
    if (!outputDir.exists()) {
      return TaskResult.failure(
          "Output directory not found: ${context.build.outputDir}. Please run docs-build first.")
    }

    try {
      // Step 1: Create CNAME file if custom domain is specified
      if (context.publish.cname && context.publish.domain.isNotEmpty()) {
        if (!isValidDomain(context.publish.domain)) {
          results.add(TaskResult.failure("Invalid domain format: ${context.publish.domain}"))
        } else {
          val cnameFile = File(outputDir, "CNAME")
          cnameFile.writeText(context.publish.domain)
          results.add(TaskResult.success("Created CNAME file for domain: ${context.publish.domain}"))
        }
      }

      val sanitizedBranch = sanitizeBranch(context.publish.branch)
      
      // Step 2: Get current branch
      val getCurrentBranch = "git rev-parse --abbrev-ref HEAD"
      commandExecutor.execute(getCurrentBranch, gitDir.toString())
      
      // Step 3: Create temporary directory for docs
      val tempDir = File(gitDir, ".docs-publish-temp")
      tempDir.mkdirs()
      
      // Step 4: Copy built documentation to temp directory
      outputDir.copyRecursively(tempDir, overwrite = true)
      
      // Step 5: Stash current changes
      commandExecutor.execute("git stash push -m 'Stashing changes before gh-pages deployment'", gitDir.toString())
      
      // Step 6: Check if gh-pages branch exists and checkout
      try {
        commandExecutor.execute("git show-ref --verify --quiet refs/heads/$sanitizedBranch", gitDir.toString())
        // Branch exists
        commandExecutor.execute("git checkout $sanitizedBranch", gitDir.toString())
      } catch (e: Exception) {
        // Branch doesn't exist, create orphan branch
        commandExecutor.execute("git checkout --orphan $sanitizedBranch", gitDir.toString())
        commandExecutor.execute("git rm -rf .", gitDir.toString())
      }
      
      // Step 7: Remove all files except .git
      val filesToRemove = gitDir.listFiles()?.filter { 
        it.name != ".git" && it.name != ".docs-publish-temp"
      } ?: emptyList()
      filesToRemove.forEach { it.deleteRecursively() }
      
      // Step 8: Copy files from temp directory
      tempDir.listFiles()?.forEach { file ->
        file.copyRecursively(File(gitDir, file.name), overwrite = true)
      }
      
      // Step 9: Create .nojekyll file to bypass Jekyll processing
      File(gitDir, ".nojekyll").writeText("")
      
      // Step 10: Add and commit
      commandExecutor.execute("git add .", gitDir.toString())
      try {
        commandExecutor.execute("git commit -m 'docs: update documentation'", gitDir.toString())
      } catch (e: Exception) {
        // No changes to commit - this is okay
        results.add(TaskResult.success("No documentation changes to commit"))
      }
      
      // Step 11: Push to remote
      commandExecutor.execute("git push origin $sanitizedBranch", gitDir.toString())
      
      // Step 12: Return to original branch
      commandExecutor.execute("git checkout -", gitDir.toString())
      
      // Step 13: Restore stashed changes
      try {
        commandExecutor.execute("git stash pop", gitDir.toString())
      } catch (e: Exception) {
        // No stashed changes - this is okay
      }
      
      // Step 14: Clean up temp directory
      if (tempDir.exists()) {
        tempDir.deleteRecursively()
      }
      
      results.add(
          TaskResult.success(
              "Published documentation to GitHub Pages on branch: ${context.publish.branch}"))
              
    } catch (e: Exception) {
      // Clean up temp directory on error
      val tempDir = File(gitDir, ".docs-publish-temp")
      if (tempDir.exists()) {
        tempDir.deleteRecursively()
      }
      
      return TaskResult.failure(
          "Failed to publish documentation: ${e.message ?: "Unknown error"}", results)
    }

    return TaskResult.success("Documentation published successfully", results)
  }
}
