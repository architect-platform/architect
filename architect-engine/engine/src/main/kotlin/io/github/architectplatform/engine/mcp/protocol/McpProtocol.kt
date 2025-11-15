package io.github.architectplatform.engine.mcp.protocol

import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.serde.annotation.Serdeable

/**
 * Model Context Protocol (MCP) message types and structures.
 */

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Any? = null,
    val method: String,
    val params: Map<String, Any?>? = null
)

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: Any? = null,
    val result: Any? = null,
    val error: JsonRpcError? = null
)

@Serdeable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: Any? = null
)

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class InitializeParams(
    val protocolVersion: String,
    val capabilities: Map<String, Any?>,
    val clientInfo: ClientInfo
)

@Serdeable
data class ClientInfo(
    val name: String,
    val version: String
)

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class InitializeResult(
    val protocolVersion: String,
    val capabilities: ServerCapabilities,
    val serverInfo: ServerInfo
)

@Serdeable
data class ServerInfo(
    val name: String,
    val version: String
)

@Serdeable
data class ServerCapabilities(
    val tools: Map<String, Any?>? = emptyMap()
)

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Tool(
    val name: String,
    val description: String,
    val inputSchema: InputSchema
)

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class InputSchema(
    val type: String = "object",
    val properties: Map<String, PropertySchema>? = null,
    val required: List<String>? = null
)

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PropertySchema(
    val type: String,
    val description: String? = null,
    val default: Any? = null,
    val items: PropertySchema? = null
)

@Serdeable
data class ListToolsResult(
    val tools: List<Tool>
)

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CallToolParams(
    val name: String,
    val arguments: Map<String, Any?>? = null
)

@Serdeable
data class CallToolResult(
    val content: List<Content>,
    val isError: Boolean? = null
)

@Serdeable
data class Content(
    val type: String = "text",
    val text: String
)
