package io.github.architectplatform.engine.mcp.interfaces

import io.github.architectplatform.engine.mcp.protocol.*
import io.github.architectplatform.engine.mcp.service.McpToolService
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import org.slf4j.LoggerFactory

/**
 * REST API controller for MCP (Model Context Protocol) endpoints.
 * This provides HTTP access to MCP functionality.
 */
@Controller("/api/mcp")
@ExecuteOn(TaskExecutors.IO)
class McpApiController(
    private val mcpToolService: McpToolService
) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  @Post
  fun handleMcpRequest(@Body request: JsonRpcRequest): JsonRpcResponse {
    logger.info("Received MCP request: ${request.method}")
    
    return try {
      when (request.method) {
        "initialize" -> handleInitialize(request)
        "tools/list" -> handleListTools(request)
        "tools/call" -> handleCallTool(request)
        else -> JsonRpcResponse(
            id = request.id,
            error = JsonRpcError(
                code = -32601,
                message = "Method not found: ${request.method}"
            )
        )
      }
    } catch (e: Exception) {
      logger.error("Error handling MCP request method ${request.method}", e)
      JsonRpcResponse(
          id = request.id,
          error = JsonRpcError(
              code = -32603,
              message = "Internal error: ${e.message}"
          )
      )
    }
  }

  private fun handleInitialize(request: JsonRpcRequest): JsonRpcResponse {
    val result = InitializeResult(
        protocolVersion = "2024-11-05",
        capabilities = ServerCapabilities(tools = emptyMap()),
        serverInfo = ServerInfo(
            name = "architect-engine-mcp",
            version = "1.0.0"
        )
    )
    return JsonRpcResponse(id = request.id, result = result)
  }

  private fun handleListTools(request: JsonRpcRequest): JsonRpcResponse {
    val tools = mcpToolService.generateTools()
    val result = ListToolsResult(tools = tools)
    return JsonRpcResponse(id = request.id, result = result)
  }

  private fun handleCallTool(request: JsonRpcRequest): JsonRpcResponse {
    val params = request.params
    val name = params?.get("name") as? String
        ?: return JsonRpcResponse(
            id = request.id,
            error = JsonRpcError(
                code = -32602,
                message = "Missing required parameter: name"
            )
        )

    @Suppress("UNCHECKED_CAST")
    val arguments = params["arguments"] as? Map<String, Any?>

    val result = mcpToolService.executeTool(name, arguments)
    return JsonRpcResponse(id = request.id, result = result)
  }
}
