# Architect Engine - MCP Integration

The Architect Engine now includes built-in support for the [Model Context Protocol (MCP)](https://modelcontextprotocol.io), enabling AI agents to interact with the Architect platform.

## Features

### Dynamic Tool Generation
The MCP integration dynamically generates tools based on:
- **Registered projects**: Each project gets its own set of tools
- **Available tasks**: Each task in a project becomes an executable tool
- **Real-time updates**: Tools are regenerated on each request to reflect the current state

### Two Integration Modes

#### 1. HTTP API Mode (Default)
The engine exposes MCP endpoints via HTTP at `/api/mcp`:

```bash
# Start the engine normally
./gradlew run

# MCP endpoint available at:
# POST http://localhost:9292/api/mcp
```

#### 2. Stdio Mode (Claude Desktop)
Run the engine in stdio mode for Claude Desktop integration:

```bash
# Run MCP stdio server
./gradlew runMcpStdio
```

## Available Tools

### Static Tools

**architect_list_projects**
- Lists all registered projects
- No parameters required

**architect_register_project**
- Registers a new project with the engine
- Parameters:
  - `name` (string, required): Project name
  - `path` (string, required): Absolute path to project directory

### Dynamic Tools (Per Project)

For each registered project, the following tools are automatically created:

**architect_{project}_list_tasks**
- Lists all available tasks for the specified project
- Example: `architect_myproject_list_tasks`

**architect_{project}_{task}**
- Executes a specific task for a project
- Example: `architect_myproject_build`, `architect_myproject_test`
- Parameters:
  - `args` (array of strings, optional): Arguments to pass to the task

## Claude Desktop Configuration

Add to your Claude Desktop configuration file:

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "architect": {
      "command": "sh",
      "args": [
        "-c",
        "cd /path/to/architect/architect-engine/engine && ./gradlew runMcpStdio"
      ]
    }
  }
}
```

Or using a pre-built JAR:

```json
{
  "mcpServers": {
    "architect": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/architect-engine.jar",
        "mcp-stdio"
      ]
    }
  }
}
```

## HTTP API Usage

### Initialize
```json
POST /api/mcp
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {},
    "clientInfo": {
      "name": "test-client",
      "version": "1.0.0"
    }
  }
}
```

### List Tools
```json
POST /api/mcp
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list",
  "params": {}
}
```

### Call Tool
```json
POST /api/mcp
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "architect_list_projects",
    "arguments": {}
  }
}
```

## Example Workflows

### Register and Build a Project

1. **Register Project**
   ```
   Tool: architect_register_project
   Arguments: {
     "name": "my-app",
     "path": "/path/to/my-app"
   }
   ```

2. **List Tasks**
   ```
   Tool: architect_my-app_list_tasks
   ```

3. **Execute Build**
   ```
   Tool: architect_my-app_build
   Arguments: { "args": [] }
   ```

### Multi-Project Workflow

With multiple projects registered, you can:
```
Tool: architect_backend_build
Tool: architect_frontend_build
Tool: architect_backend_test
Tool: architect_frontend_test
```

## Architecture

The MCP integration is implemented as a core component of the engine:

```
architect-engine/
└── engine/
    └── src/main/kotlin/io/github/architectplatform/engine/
        └── mcp/
            ├── protocol/          # MCP protocol data classes
            │   └── McpProtocol.kt
            ├── service/           # Tool generation and execution
            │   └── McpToolService.kt
            ├── interfaces/        # HTTP API controller
            │   └── McpApiController.kt
            └── McpStdioLauncher.kt  # Stdio mode launcher
```

### Key Components

- **McpProtocol.kt**: MCP protocol message types (JSON-RPC)
- **McpToolService.kt**: Dynamic tool generation based on registered projects
- **McpApiController.kt**: HTTP endpoint at `/api/mcp`
- **McpStdioLauncher.kt**: Standalone launcher for stdio communication

## Benefits

1. **No Separate Server**: MCP functionality is built into the engine
2. **Always In Sync**: Tools reflect the current state of projects and tasks
3. **Plugin-Aware**: Automatically exposes tasks from all plugins
4. **Dual-Mode**: Works with both HTTP clients and Claude Desktop

## Development

### Testing MCP Stdio Mode
```bash
# Start the MCP stdio server
./gradlew runMcpStdio

# Send a test request (in another terminal)
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0.0"}}}' | ./gradlew runMcpStdio
```

### Testing HTTP API
```bash
# Start the engine
./gradlew run

# Test with curl
curl -X POST http://localhost:9292/api/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list",
    "params": {}
  }'
```

## Troubleshooting

### Engine Not Accessible
- Ensure the engine is running: `./gradlew run`
- Check the engine URL: default is `http://localhost:9292`

### Tools Not Appearing
- Register projects first: `architect_register_project`
- Tools are generated dynamically based on registered projects

### Stdio Mode Issues
- Check stderr output for error messages
- Ensure the engine can start with all dependencies
- Verify the working directory is correct

## Links

- [Model Context Protocol](https://modelcontextprotocol.io)
- [Architect Engine Documentation](./README.md)
- [Claude Desktop](https://claude.ai/desktop)
