package io.github.architectplatform.engine.mcp

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.architectplatform.engine.mcp.protocol.*
import io.github.architectplatform.engine.mcp.service.McpToolService
import io.micronaut.context.ApplicationContext
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

/**
 * Launcher for MCP stdio mode (for Claude Desktop integration).
 * This runs as a standalone process that communicates via stdin/stdout.
 */
fun main(args: Array<String>) {
  // Start Micronaut context
  val context = ApplicationContext.run()
  val mcpToolService = context.getBean(McpToolService::class.java)
  val objectMapper = context.getBean(ObjectMapper::class.java)
  val logger = LoggerFactory.getLogger("McpStdioLauncher")

  val reader = BufferedReader(InputStreamReader(System.`in`))
  val writer = PrintWriter(System.out, true)

  System.err.println("Architect Engine MCP Server running on stdio")
  logger.info("Architect Engine MCP Server starting on stdio")

  try {
    while (true) {
      val line = reader.readLine() ?: break
      if (line.isBlank()) continue

      try {
        val request = objectMapper.readValue<JsonRpcRequest>(line)
        val response = handleRequest(request, mcpToolService, objectMapper)
        val responseJson = objectMapper.writeValueAsString(response)
        writer.println(responseJson)
        writer.flush()
      } catch (e: Exception) {
        logger.error("Error processing request: $line", e)
        val errorResponse = JsonRpcResponse(
            id = null,
            error = JsonRpcError(
                code = -32700,
                message = "Parse error: ${e.message}"
            )
        )
        writer.println(objectMapper.writeValueAsString(errorResponse))
        writer.flush()
      }
    }
  } catch (e: Exception) {
    logger.error("Fatal error in MCP stdio handler", e)
    System.err.println("Fatal error: ${e.message}")
  } finally {
    context.close()
  }
}

private fun handleRequest(
    request: JsonRpcRequest,
    mcpToolService: McpToolService,
    objectMapper: ObjectMapper
): JsonRpcResponse {
  return try {
    when (request.method) {
      "initialize" -> handleInitialize(request)
      "tools/list" -> handleListTools(request, mcpToolService)
      "tools/call" -> handleCallTool(request, mcpToolService)
      else -> JsonRpcResponse(
          id = request.id,
          error = JsonRpcError(
              code = -32601,
              message = "Method not found: ${request.method}"
          )
      )
    }
  } catch (e: Exception) {
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

private fun handleListTools(request: JsonRpcRequest, mcpToolService: McpToolService): JsonRpcResponse {
  val tools = mcpToolService.generateTools()
  val result = ListToolsResult(tools = tools)
  return JsonRpcResponse(id = request.id, result = result)
}

private fun handleCallTool(request: JsonRpcRequest, mcpToolService: McpToolService): JsonRpcResponse {
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
