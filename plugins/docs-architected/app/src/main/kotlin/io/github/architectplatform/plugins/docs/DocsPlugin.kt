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

  companion object {
    // Domain validation regex - validates RFC-compliant domain names
    // Matches: example.com, sub.example.com, my-site.github.io
    // Does not match: -example.com, example-.com, .example.com
    private val DOMAIN_VALIDATION_REGEX =
        Regex("^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?(\\.[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?)*$")

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
      val repoUrlPattern = Regex("""(?s)\{\{#repoUrl}}(.*?)\{\{/repoUrl}}""")
      result = repoUrlPattern.replace(result) { matchResult ->
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
      val repoUrlPattern = Regex("""(?s)\{\{#repoUrl}}.*?\{\{/repoUrl}}""")
      result = repoUrlPattern.replace(result, "")
      
      // Provide fallback values
      result = result
          .replace("{{repoUrl}}", "https://github.com/username/repo")
          .replace("{{repoName}}", "username/repo")
          .replace("{{organizationName}}", "username")
          .replace("{{projectName}}", "repo")
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
                      .replace("{{branch}}", context.publish.branch)
                      .replace("{{mkdocsVersion}}", context.build.mkdocsVersion)
                      .replace("{{mkdocsMaterialVersion}}", context.build.mkdocsMaterialVersion))
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
                .let { content -> 
                  val processedContent = replaceTemplatePlaceholders(content)
                  configFile.writeText(processedContent) 
                }
            results.add(TaskResult.success("Created MkDocs configuration file"))
          }
        }
        "docusaurus" -> {
          val configFile = File(gitDir, "docusaurus.config.js")
          if (!configFile.exists()) {
            resourceExtractor
                .getResourceFileContent(this.javaClass.classLoader, "configs/docusaurus.config.js")
                .let { content -> 
                  val processedContent = replaceTemplatePlaceholders(content)
                  configFile.writeText(processedContent) 
                }
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
                .let { content -> 
                  val processedContent = replaceTemplatePlaceholders(content)
                  configFile.writeText(processedContent) 
                }
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
            val mkdocsVer = sanitizeVersion(context.build.mkdocsVersion)
            val materialVer = sanitizeVersion(context.build.mkdocsMaterialVersion)
            commandExecutor.execute("pip3 install mkdocs==$mkdocsVer mkdocs-material==$materialVer", gitDir.toString())
            results.add(TaskResult.success("Installed MkDocs dependencies"))
          }
          "docusaurus" -> {
            if (File(gitDir, "package.json").exists()) {
              // Use npm ci for reproducible builds in CI/CD environments
              val packageLock = File(gitDir, "package-lock.json")
              val command = if (packageLock.exists()) "npm ci" else "npm install"
              commandExecutor.execute(command, gitDir.toString())
              results.add(TaskResult.success("Installed Docusaurus dependencies"))
            }
          }
          "vuepress" -> {
            if (File(gitDir, "package.json").exists()) {
              // Use npm ci for reproducible builds in CI/CD environments
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

    // Build documentation
    try {
      when (context.build.framework) {
        "mkdocs" -> {
          val sanitizedOutputDir = sanitizePath(context.build.outputDir)
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
        if (!isValidDomain(context.publish.domain)) {
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

      val sanitizedOutputDir = sanitizePath(context.build.outputDir)
      val sanitizedBranch = sanitizeBranch(context.publish.branch)
      
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
