package io.github.architectplatform.plugins.github

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.execution.ResourceExtractor
import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.project.getKey
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase
import io.github.architectplatform.plugins.github.dto.GithubContext
import io.github.architectplatform.plugins.github.dto.PipelineContext
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

class GithubPlugin : ArchitectPlugin<GithubContext> {
  override val id = "github-plugin"
  override val contextKey: String = "github"

  override val ctxClass = GithubContext::class.java
  override var context: GithubContext = GithubContext()

  override fun register(registry: TaskRegistry) {
    registry.add(
        GithubTask(
            id = "github-release-task",
            phase = CoreWorkflow.RELEASE,
            task = ::releaseTask,
        ))

    registry.add(
        GithubTask(
            id = "github-init-pipelines",
            phase = CoreWorkflow.INIT,
            task = ::initPipelines,
        ))

    registry.add(
        GithubTask(
            id = "github-init-dependencies",
            phase = CoreWorkflow.INIT,
            task = ::initDependencies,
        ))
  }

  class GithubTask(
      override val id: String,
      private val phase: Phase,
      private val task: (Environment, ProjectContext) -> TaskResult
  ) : Task {

    override fun phase(): Phase = phase

    override fun execute(
        environment: Environment,
        projectContext: ProjectContext,
        args: List<String>
    ): TaskResult {
      return try {
        task(environment, projectContext)
      } catch (e: Exception) {
        TaskResult.failure(
            "Github task: $id failed with exception: ${e.message ?: "Unknown error"}")
      }
    }
  }

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

  private fun initDependencies(
      environment: Environment,
      projectContext: ProjectContext
  ): TaskResult {
    if (context.deps.enabled.not()) {
      return TaskResult.success("Dependencies initialization skipped.")
    }
    val resourceExtractor = environment.service(ResourceExtractor::class.java)
    val gitDir =
        findRepoRoot(projectContext.dir.toFile())
            ?: return TaskResult.failure("Git directory not found in project hierarchy.")
    resourceExtractor.copyDirectoryFromResources(
        this.javaClass.classLoader,
        "dependencies/${context.deps.type}",
        gitDir.resolve(".github/").toPath())
    return TaskResult.success("Dependencies initialized successfully.")
  }

  private fun releaseTask(environment: Environment, projectContext: ProjectContext): TaskResult {
    if (context.release.enabled.not()) {
      return TaskResult.success("Release skipped as it is not enabled.")
    }
    val objectMapper = ObjectMapper()
    val standardGitAssets = listOf("**/*.gradle", "**/*.gradle.kts")
    val message = objectMapper.writeValueAsString(context.release.message)
    val assetsJson = objectMapper.writeValueAsString(context.release.assets)
    val allGitAssets = standardGitAssets + context.release.git_assets
    val gitAssetsjson = objectMapper.writeValueAsString(allGitAssets)

    val resourceExtractor = environment.service(ResourceExtractor::class.java)
    val commandExecutor = environment.service(CommandExecutor::class.java)

    resourceExtractor.copyFileFromResources(
        this.javaClass.classLoader, "releases/run.sh", projectContext.dir, "run.sh")
    resourceExtractor.copyFileFromResources(
        this.javaClass.classLoader,
        "releases/update-version.sh",
        projectContext.dir,
        "update-version.sh")
    commandExecutor.execute("chmod +x update-version.sh", projectContext.dir.toString())
    resourceExtractor
        .getResourceFileContent(this.javaClass.classLoader, "releases/.releaserc.json")
        .replace("{{name}}", projectContext.config.getKey<String>("project.name") ?: "unknown")
        .replace("{{message}}", message)
        .replace("{{assets}}", assetsJson)
        .replace("{{git_assets}}", gitAssetsjson)
        .let { result ->
          Files.write(Path(projectContext.dir.toString(), ".releaserc.json"), result.toByteArray())
        }

    commandExecutor.execute("./run.sh", projectContext.dir.toString())

    commandExecutor.execute("rm run.sh", projectContext.dir.toString())
    commandExecutor.execute("rm update-version.sh", projectContext.dir.toString())
    commandExecutor.execute("rm .releaserc.json", projectContext.dir.toString())
    return TaskResult.success("Release completed successfully")
  }

  private fun initPipelines(environment: Environment, projectContext: ProjectContext): TaskResult {
    val results =
        this.context.pipelines.map { pipeline ->
          return initSinglePipeline(pipeline, environment, projectContext)
        }
    return TaskResult.success("All pipelines initialized successfully.", results)
  }

  private fun initSinglePipeline(
      pipeline: PipelineContext,
      environment: Environment,
      projectContext: ProjectContext,
  ): TaskResult {
    val gitDir =
        findRepoRoot(projectContext.dir.toFile())
            ?: return TaskResult.failure("Git directory not found in project hierarchy.")
    val resourceRoot = "pipelines/"
    val resourceFile = resourceRoot + pipeline.type + ".yml"
    val pipelinesDir = File(gitDir, ".github/workflows")

    if (!pipelinesDir.exists()) {
      pipelinesDir.mkdirs()
    }

    val resourceExtractor = environment.service(ResourceExtractor::class.java)
    resourceExtractor.getResourceFileContent(this.javaClass.classLoader, resourceFile).let { content
      ->
      val filePath = File(pipelinesDir, "${pipeline.name}.yml")
      filePath.writeText(
          content
              .replace("{{name}}", pipeline.name)
              .replace("{{path}}", pipeline.path)
              .replace("{{branch}}", pipeline.branch))
    }
    return TaskResult.success("Pipeline ${pipeline.name} initialized successfully.")
  }
}
