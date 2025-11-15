package io.github.architectplatform.engine.mcp.service

import io.github.architectplatform.engine.core.project.app.ProjectService
import io.github.architectplatform.engine.core.tasks.application.TaskService
import io.github.architectplatform.engine.mcp.protocol.*
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Service that generates MCP tools dynamically based on the Architect Engine state.
 * Tools are created for:
 * - Project management (list, register)
 * - Task execution (dynamically based on registered projects and their tasks)
 */
@Singleton
class McpToolService(
    private val projectService: ProjectService,
    private val taskService: TaskService
) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  /**
   * Generate tools dynamically based on the current state of the Engine.
   * This includes:
   * - Static tools for project management
   * - Dynamic tools for each registered project and its tasks
   */
  fun generateTools(): List<Tool> {
    val tools = mutableListOf<Tool>()

    // Add static project management tools
    tools.addAll(getProjectManagementTools())

    // Add dynamic project-specific tools
    try {
      val projects = projectService.getAllProjects()
      projects.forEach { project ->
        tools.addAll(generateProjectTools(project.name))
      }
    } catch (e: Exception) {
      logger.warn("Could not generate project tools: ${e.message}")
    }

    return tools
  }

  private fun getProjectManagementTools(): List<Tool> = listOf(
      Tool(
          name = "architect_list_projects",
          description = "List all projects registered with the Architect Engine. Returns project names and paths.",
          inputSchema = InputSchema(
              type = "object",
              properties = emptyMap(),
              required = emptyList()
          )
      ),
      Tool(
          name = "architect_register_project",
          description = "Register a project with the Architect Engine. This discovers available tasks for the project.",
          inputSchema = InputSchema(
              type = "object",
              properties = mapOf(
                  "name" to PropertySchema(
                      type = "string",
                      description = "The name of the project"
                  ),
                  "path" to PropertySchema(
                      type = "string",
                      description = "The absolute path to the project directory"
                  )
              ),
              required = listOf("name", "path")
          )
      )
  )

  private fun generateProjectTools(projectName: String): List<Tool> {
    val tools = mutableListOf<Tool>()

    try {
      val tasks = taskService.getAllTasks(projectName)

      // Add tool to list tasks for this project
      tools.add(Tool(
          name = "architect_${projectName}_list_tasks",
          description = "List all available tasks for project '$projectName'. Returns task IDs, descriptions, and phases.",
          inputSchema = InputSchema(
              type = "object",
              properties = emptyMap(),
              required = emptyList()
          )
      ))

      // Add a tool for each task
      tasks.forEach { task ->
        val taskDescription = task.description ?: "Execute task '${task.id}' for project '$projectName'"
        val phaseInfo = if (task.phase != null) " (Phase: ${task.phase})" else ""
        
        tools.add(Tool(
            name = "architect_${projectName}_${task.id}",
            description = "$taskDescription$phaseInfo",
            inputSchema = InputSchema(
                type = "object",
                properties = mapOf(
                    "args" to PropertySchema(
                        type = "array",
                        description = "Optional arguments to pass to the task",
                        items = PropertySchema(type = "string")
                    )
                ),
                required = emptyList()
            )
        ))
      }
    } catch (e: Exception) {
      logger.warn("Could not fetch tasks for project $projectName: ${e.message}")
    }

    return tools
  }

  /**
   * Execute a tool by name with the provided arguments.
   */
  fun executeTool(name: String, arguments: Map<String, Any?>?): CallToolResult {
    return try {
      when {
        name == "architect_list_projects" -> executeListProjects()
        name == "architect_register_project" -> executeRegisterProject(arguments)
        name.startsWith("architect_") && name.contains("_list_tasks") -> {
          val projectName = extractProjectName(name, "_list_tasks")
          executeListTasks(projectName)
        }
        name.startsWith("architect_") -> {
          // Extract project and task from the tool name: architect_<project>_<task>
          val parts = name.removePrefix("architect_").split("_", limit = 2)
          if (parts.size == 2) {
            val projectName = parts[0]
            val taskName = parts[1]
            executeTask(projectName, taskName, arguments)
          } else {
            CallToolResult(
                content = listOf(Content(text = "Invalid tool name format: $name")),
                isError = true
            )
          }
        }
        else -> CallToolResult(
            content = listOf(Content(text = "Unknown tool: $name")),
            isError = true
        )
      }
    } catch (e: Exception) {
      logger.error("Error executing tool $name", e)
      CallToolResult(
          content = listOf(Content(text = "Error executing tool: ${e.message}")),
          isError = true
      )
    }
  }

  private fun extractProjectName(toolName: String, suffix: String): String {
    return toolName.removePrefix("architect_").removeSuffix(suffix)
  }

  private fun executeListProjects(): CallToolResult {
    val projects = projectService.getAllProjects()
    val text = if (projects.isEmpty()) {
      "No projects registered."
    } else {
      "Registered projects:\n" + projects.joinToString("\n") { "- ${it.name} (${it.path})" }
    }
    return CallToolResult(content = listOf(Content(text = text)))
  }

  private fun executeRegisterProject(arguments: Map<String, Any?>?): CallToolResult {
    val name = arguments?.get("name") as? String
        ?: return CallToolResult(
            content = listOf(Content(text = "Missing required argument: name")),
            isError = true
        )
    val path = arguments["path"] as? String
        ?: return CallToolResult(
            content = listOf(Content(text = "Missing required argument: path")),
            isError = true
        )

    projectService.registerProject(name, path)
    val project = projectService.getProject(name)
        ?: return CallToolResult(
            content = listOf(Content(text = "Failed to register project")),
            isError = true
        )
    
    return CallToolResult(
        content = listOf(Content(text = "Project '${project.name}' registered successfully at ${project.path}"))
    )
  }

  private fun executeListTasks(projectName: String): CallToolResult {
    val tasks = taskService.getAllTasks(projectName)
    val text = if (tasks.isEmpty()) {
      "No tasks available for project '$projectName'."
    } else {
      "Available tasks for project '$projectName':\n" +
          tasks.joinToString("\n") { task ->
            val desc = if (task.description != null) ": ${task.description}" else ""
            val phase = if (task.phase != null) " [${task.phase}]" else ""
            "- ${task.id}$desc$phase"
          }
    }
    return CallToolResult(content = listOf(Content(text = text)))
  }

  private fun executeTask(projectName: String, taskName: String, arguments: Map<String, Any?>?): CallToolResult {
    val args = (arguments?.get("args") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    val executionId = taskService.executeTask(projectName, taskName, args)
    return CallToolResult(
        content = listOf(Content(text = "Task '$taskName' started for project '$projectName'\nExecution ID: ${executionId.value}\nYou can monitor progress at /api/executions/${executionId.value}"))
    )
  }
}
