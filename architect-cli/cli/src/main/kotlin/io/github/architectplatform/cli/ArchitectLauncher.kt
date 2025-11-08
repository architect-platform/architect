package io.github.architectplatform.cli

import io.github.architectplatform.cli.client.EngineCommandClient
import io.github.architectplatform.cli.commands.EngineCommand
import io.github.architectplatform.cli.commands.LoginCommand
import io.github.architectplatform.cli.dto.RegisterProjectRequest
import io.micronaut.context.ApplicationContext
import jakarta.inject.Singleton
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters

/**
 * Main entry point for the Architect CLI application.
 *
 * This class provides a command-line interface for interacting with the Architect Engine,
 * enabling users to:
 * - Register projects with the engine
 * - Execute tasks within projects
 * - Manage authentication (via login command)
 * - Manage the Architect Engine lifecycle (via engine command)
 * - View available tasks for a project
 *
 * The launcher uses PicoCLI for command-line parsing and Micronaut for dependency injection.
 *
 * @property engineCommandClient HTTP client for communicating with the Architect Engine API
 */
@Singleton
@Command(
    name = "architect",
    description = ["Architect CLI - Task automation and workflow management"],
    mixinStandardHelpOptions = true,
    subcommands = [
        EngineCommand::class,
        LoginCommand::class
    ]
)
class ArchitectLauncher(
    private val engineCommandClient: EngineCommandClient
) : Runnable {

    /**
     * The task to execute (e.g., "build", "test").
     * If null, lists available tasks for the current project.
     */
    @Parameters(
        index = "0",
        description = ["Task to execute"],
        arity = "0..1",
        paramLabel = "<task>"
    )
    var task: String? = null

    /**
     * Additional arguments to pass to the task.
     */
    @Parameters(
        index = "1..*",
        description = ["Arguments for the task"],
        arity = "0..*",
        paramLabel = "<args>"
    )
    var taskArgs: List<String> = emptyList()

    /**
     * Enable plain output mode for CI environments.
     * When true, disables rich terminal UI and outputs simple text.
     */
    @Option(
        names = ["-p", "--plain"],
        description = ["Enable plain output (CI Environments)"]
    )
    var plain: Boolean = false

    /**
     * Main execution logic for the CLI.
     *
     * Flow:
     * 1. Register the current project with the engine
     * 2. If no task specified, list available tasks
     * 3. Otherwise, execute the specified task and display results
     */
    override fun run() {
        val projectPath = System.getProperty("user.dir")
        val projectName = extractProjectName(projectPath)

        println("📦 Registering project: $projectName")
        val request = RegisterProjectRequest(name = projectName, path = projectPath)
        engineCommandClient.registerProject(request)

        if (task == null) {
            listAvailableTasks(projectName)
            return
        }

        executeTask(projectName, task!!, taskArgs)
    }

    /**
     * Lists all available tasks for the project.
     *
     * @param projectName The name of the project
     */
    private fun listAvailableTasks(projectName: String) {
        val commands = engineCommandClient.getAllTasks(projectName)
        println("🧭 Available tasks:")
        commands.forEach { println("  ${it.id}") }
    }

    /**
     * Extracts project name from the project path.
     *
     * @param projectPath The full path to the project
     * @return The project name
     */
    private fun extractProjectName(projectPath: String): String {
        return projectPath.substringAfterLast("/").substringBeforeLast(".")
    }

    /**
     * Executes a task and displays progress.
     *
     * @param projectName The name of the project
     * @param taskName The name of the task to execute
     * @param taskArgs Arguments to pass to the task
     */
    private fun executeTask(projectName: String, taskName: String, taskArgs: List<String>) {
        val ui = ConsoleUI(taskName, plain)

        println()
        println("━".repeat(80))
        println("▶️  Executing task: $taskName")
        println("📦 Project: $projectName")
        println("━".repeat(80))
        println()

        runBlocking {
            val startTime = System.currentTimeMillis()
            try {
                val executionId = engineCommandClient.execute(projectName, taskName, taskArgs)
                val flow = engineCommandClient.getExecutionFlow(executionId)
                flow.collect { ui.process(it) }

                val duration = (System.currentTimeMillis() - startTime) / 1000.0
                if (ui.hasFailed) {
                    ui.completeWithError("Task failed (duration: ${"%.1f".format(duration)}s)")
                    exitProcess(1)
                } else {
                    ui.complete("Task completed successfully (duration: ${"%.1f".format(duration)}s)")
                    exitProcess(0)
                }
            } catch (e: Exception) {
                val duration = (System.currentTimeMillis() - startTime) / 1000.0
                println()
                println("❌ Task execution aborted")
                println("Error: ${e.message}")
                println()
                println("Stack Trace:")
                println(e.stackTraceToString())
                println()
                println("Duration: ${"%.1f".format(duration)}s")
                exitProcess(1)
            }
        }
    }

    companion object {
        /**
         * Application entry point.
         *
         * Initializes the Micronaut application context, retrieves the launcher bean,
         * executes the command-line arguments, and terminates with the appropriate exit code.
         *
         * @param args Command-line arguments passed to the application
         */
        @JvmStatic
        fun main(args: Array<String>) {
            val context = ApplicationContext.run()
            val launcher = context.getBean(ArchitectLauncher::class.java)
            val commandLine = CommandLine(launcher, ArchitectCommandFactory(context))
            val exitCode = commandLine.execute(*args)
            context.close()
            exitProcess(exitCode)
        }
    }
}

/**
 * Custom factory for creating command instances with dependency injection.
 */
class ArchitectCommandFactory(private val context: ApplicationContext) : CommandLine.IFactory {
    override fun <K : Any?> create(cls: Class<K>): K {
        return try {
            context.getBean(cls)
        } catch (e: Exception) {
            CommandLine.defaultFactory().create(cls)
        }
    }
}
