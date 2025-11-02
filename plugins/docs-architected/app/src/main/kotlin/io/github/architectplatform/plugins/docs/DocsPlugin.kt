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
 * - GitHub Pages publishing
 * - Automated workflow generation
 *
 * The plugin registers tasks in the following phases:
 * - INIT: Documentation structure and workflow initialization
 * - BUILD: Documentation building
 * - PUBLISH: Documentation publishing to GitHub Pages
 */
class DocsPlugin : ArchitectPlugin<DocsContext> {
  override val id = "docs-plugin"
  override val contextKey: String = "docs"

  override val ctxClass = DocsContext::class.java
  override var context: DocsContext = DocsContext()

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
   * Initializes documentation structure and workflows.
   *
   * This task:
   * 1. Creates documentation directory structure if it doesn't exist
   * 2. Sets up GitHub Actions workflow for documentation publishing
   * 3. Creates initial configuration files for the selected framework
   *
   * @param environment Execution environment providing services
   * @param projectContext The project context
   * @return TaskResult indicating success or failure
   */
  private fun initDocs(environment: Environment, projectContext: ProjectContext): TaskResult {
    val gitDir =
        findRepoRoot(projectContext.dir.toFile())
            ?: return TaskResult.failure("Git directory not found in project hierarchy.")

    val resourceExtractor = environment.service(ResourceExtractor::class.java)
    val results = mutableListOf<TaskResult>()

    // Create documentation directory if it doesn't exist
    val docsDir = File(gitDir, context.build.sourceDir)
    if (!docsDir.exists()) {
      docsDir.mkdirs()
      val readmeFile = File(docsDir, "index.md")
      readmeFile.writeText(
          """# Documentation
              |
              |Welcome to the documentation.
              |
              |## Getting Started
              |
              |Add your documentation here in Markdown format.
              |"""
              .trimMargin())
      results.add(TaskResult.success("Created documentation directory: ${context.build.sourceDir}"))
    }

    // Initialize GitHub Actions workflow for documentation publishing
    if (context.publish.enabled && context.publish.githubPages) {
      val workflowsDir = File(gitDir, ".github/workflows")
      if (!workflowsDir.exists()) {
        workflowsDir.mkdirs()
      }

      try {
        resourceExtractor
            .getResourceFileContent(this.javaClass.classLoader, "workflows/docs-publish.yml")
            .let { content ->
              val workflowFile = File(workflowsDir, "docs-publish.yml")
              workflowFile.writeText(
                  content
                      .replace("{{framework}}", context.build.framework)
                      .replace("{{sourceDir}}", context.build.sourceDir)
                      .replace("{{outputDir}}", context.build.outputDir)
                      .replace("{{branch}}", context.publish.branch))
              results.add(TaskResult.success("Created GitHub Actions workflow for documentation"))
            }
      } catch (e: Exception) {
        results.add(
            TaskResult.failure("Failed to create workflow: ${e.message ?: "Unknown error"}"))
      }
    }

    // Create framework-specific configuration files
    try {
      when (context.build.framework) {
        "mkdocs" -> {
          val configFile = File(gitDir, "mkdocs.yml")
          if (!configFile.exists()) {
            resourceExtractor
                .getResourceFileContent(this.javaClass.classLoader, "configs/mkdocs.yml")
                .let { content -> configFile.writeText(content) }
            results.add(TaskResult.success("Created MkDocs configuration file"))
          }
        }
        "docusaurus" -> {
          val configFile = File(gitDir, "docusaurus.config.js")
          if (!configFile.exists()) {
            resourceExtractor
                .getResourceFileContent(this.javaClass.classLoader, "configs/docusaurus.config.js")
                .let { content -> configFile.writeText(content) }
            results.add(TaskResult.success("Created Docusaurus configuration file"))
          }
        }
        "vuepress" -> {
          val configDir = File(gitDir, "${context.build.sourceDir}/.vuepress")
          if (!configDir.exists()) {
            configDir.mkdirs()
          }
          val configFile = File(configDir, "config.js")
          if (!configFile.exists()) {
            resourceExtractor
                .getResourceFileContent(this.javaClass.classLoader, "configs/vuepress.config.js")
                .let { content -> configFile.writeText(content) }
            results.add(TaskResult.success("Created VuePress configuration file"))
          }
        }
      }
    } catch (e: Exception) {
      results.add(
          TaskResult.failure(
              "Failed to create framework configuration: ${e.message ?: "Unknown error"}"))
    }

    return TaskResult.success("Documentation initialized successfully", results)
  }

  /**
   * Builds documentation from markdown sources.
   *
   * This task:
   * 1. Installs documentation framework dependencies if needed
   * 2. Executes the build command for the selected framework
   * 3. Validates the output
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

    // Install dependencies if needed
    if (context.build.installDeps) {
      try {
        when (context.build.framework) {
          "mkdocs" -> {
            commandExecutor.execute("pip3 install mkdocs==1.5.3 mkdocs-material==9.5.3", gitDir.toString())
            results.add(TaskResult.success("Installed MkDocs dependencies"))
          }
          "docusaurus" -> {
            if (File(gitDir, "package.json").exists()) {
              commandExecutor.execute("npm install", gitDir.toString())
              results.add(TaskResult.success("Installed Docusaurus dependencies"))
            }
          }
          "vuepress" -> {
            if (File(gitDir, "package.json").exists()) {
              commandExecutor.execute("npm install", gitDir.toString())
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

    // Build documentation
    try {
      when (context.build.framework) {
        "mkdocs" -> {
          // Sanitize output directory to prevent command injection
          val sanitizedOutputDir = context.build.outputDir.replace(Regex("[^a-zA-Z0-9/_-]"), "")
          val command = if (sanitizedOutputDir.isNotEmpty()) {
            "mkdocs build -d $sanitizedOutputDir"
          } else {
            "mkdocs build"
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
  }

  /**
   * Publishes documentation to GitHub Pages.
   *
   * This task:
   * 1. Ensures documentation is built
   * 2. Deploys to GitHub Pages using gh-pages branch
   * 3. Configures custom domain if specified
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

    // Create CNAME file if custom domain is specified
    if (context.publish.cname && context.publish.domain.isNotEmpty()) {
      try {
        // Validate domain format (basic validation)
        val domainRegex = Regex("^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?(\\.[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?)*$")
        if (!domainRegex.matches(context.publish.domain)) {
          results.add(TaskResult.failure("Invalid domain format: ${context.publish.domain}"))
        } else {
          val cnameFile = File(outputDir, "CNAME")
          cnameFile.writeText(context.publish.domain)
          results.add(TaskResult.success("Created CNAME file for domain: ${context.publish.domain}"))
        }
      } catch (e: Exception) {
        results.add(
            TaskResult.failure("Failed to create CNAME file: ${e.message ?: "Unknown error"}"))
      }
    }

    // Use the provided publish script to deploy to GitHub Pages
    try {
      val resourceExtractor = environment.service(ResourceExtractor::class.java)
      resourceExtractor.copyFileFromResources(
          this.javaClass.classLoader, "scripts/publish-ghpages.sh", projectContext.dir, "publish-ghpages.sh")

      commandExecutor.execute("chmod +x publish-ghpages.sh", gitDir.toString())

      // Sanitize inputs to prevent command injection
      val sanitizedOutputDir = context.build.outputDir.replace(Regex("[^a-zA-Z0-9/_.-]"), "")
      val sanitizedBranch = context.publish.branch.replace(Regex("[^a-zA-Z0-9/_-]"), "")
      
      val publishCommand =
          "./publish-ghpages.sh $sanitizedOutputDir $sanitizedBranch"
      commandExecutor.execute(publishCommand, gitDir.toString())

      commandExecutor.execute("rm publish-ghpages.sh", gitDir.toString())

      results.add(
          TaskResult.success(
              "Published documentation to GitHub Pages on branch: ${context.publish.branch}"))
    } catch (e: Exception) {
      return TaskResult.failure(
          "Failed to publish documentation: ${e.message ?: "Unknown error"}", results)
    }

    return TaskResult.success("Documentation published successfully", results)
  }
}
